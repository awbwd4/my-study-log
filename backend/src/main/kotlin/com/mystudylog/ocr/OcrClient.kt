package com.mystudylog.ocr

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.body
import org.springframework.web.multipart.MultipartFile

data class DocumentParseCoordinate(val x: Double = 0.0, val y: Double = 0.0)
data class DocumentParseElementContent(val text: String? = null)
data class DocumentParseElement(
    val content: DocumentParseElementContent = DocumentParseElementContent(),
    val coordinates: List<DocumentParseCoordinate> = emptyList(),
)
data class DocumentParseResponse(val elements: List<DocumentParseElement>? = null)

private data class XRange(val min: Double, val max: Double)

@Component
class OcrClient(
    restClientBuilder: RestClient.Builder,
    @Value("\${upstage.api-key}") private val apiKey: String,
) {
    private val restClient = restClientBuilder.baseUrl("https://api.upstage.ai").build()
    private val log = LoggerFactory.getLogger(OcrClient::class.java)

    /** 메인 컬럼(가장 넓은 블록)의 좌우 경계 바깥/안쪽으로 이 정도(페이지 폭 비율)까지는 같은 컬럼으로 인정한다. */
    private val columnToleranceRatio = 0.05

    /**
     * 이미지에서 텍스트를 추출한다. Upstage Document Parse로 문서를 레이아웃 블록(제목/문단/목록 등)
     * 단위로 인식한 뒤, 그중 "학생이 실제로 찍으려 한 컬럼"에 속한 블록만 순서대로 이어붙인다.
     *
     * 시험지처럼 2단 이상으로 나뉜 페이지를 찍으면 옆 컬럼의 글자가 별도 블록으로 딸려 들어오는데,
     * 처음엔 "블록 폭이 좁으면 옆 컬럼 조각"이라고 가정했지만 사진에 옆 컬럼이 넓게 잡히면 그 조각도
     * 폭이 넓어져서 폭만으로는 못 걸러졌다. 대신 실제 지문(보통 가장 넓은 블록)의 좌우 위치를 "메인
     * 컬럼"으로 보고, 그 범위 밖에 있는(왼쪽/오른쪽 가장자리에 걸친) 블록은 제외한다 — 실제 시험지
     * 사진 여러 장으로 검증했을 때 옆 컬럼 조각은 항상 페이지 가장자리(x=0 또는 x=1 근처)에 걸쳐
     * 있었고, 메인 컬럼 범위 안에 완전히 들어오는 경우는 없었다.
     *
     * API 키 미설정, 네트워크 오류, 응답 파싱 실패 등 어떤 이유로든 실패하면 null을 반환할 뿐
     * 예외를 던지지 않는다 — 오답노트 등록 자체는 OCR 성공 여부와 무관하게 항상 완료되어야 하기 때문.
     */
    fun extractText(image: MultipartFile): String? {
        if (apiKey.isBlank()) return null
        return try {
            val multipart = MultipartBodyBuilder().apply {
                part("document", image.resource)
                part("model", "document-parse")
                part("output_formats", "[\"text\"]")
            }.build()

            val response = restClient.post()
                .uri("/v1/document-digitization")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Authorization", "Bearer $apiKey")
                .body(multipart)
                .retrieve()
                .body<DocumentParseResponse>()

            val elements = response?.elements?.filter { !it.content.text.isNullOrBlank() } ?: return null
            val mainColumn = mainColumnRange(elements) ?: return null

            elements
                .filter { isInColumn(it, mainColumn) }
                .joinToString("\n\n") { it.content.text!!.trim() }
                .takeUnless { it.isBlank() }
        } catch (ex: RestClientException) {
            log.warn("OCR 텍스트 추출 실패, 수동 입력값으로 대체합니다", ex)
            null
        }
    }

    /** 가장 넓은(폭이 큰) 블록을 "학생이 찍으려 한 지문"으로 보고 그 좌우 범위를 기준 컬럼으로 삼는다. */
    private fun mainColumnRange(elements: List<DocumentParseElement>): XRange? =
        elements.mapNotNull(::xRangeOf).maxByOrNull { it.max - it.min }

    private fun isInColumn(element: DocumentParseElement, column: XRange): Boolean {
        val range = xRangeOf(element) ?: return true
        return range.min >= column.min - columnToleranceRatio && range.max <= column.max + columnToleranceRatio
    }

    private fun xRangeOf(element: DocumentParseElement): XRange? {
        val xs = element.coordinates.map { it.x }
        if (xs.isEmpty()) return null
        return XRange(xs.min(), xs.max())
    }
}
