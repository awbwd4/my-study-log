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

@Component
class OcrClient(
    restClientBuilder: RestClient.Builder,
    @Value("\${upstage.api-key}") private val apiKey: String,
) {
    private val restClient = restClientBuilder.baseUrl("https://api.upstage.ai").build()
    private val log = LoggerFactory.getLogger(OcrClient::class.java)

    /**
     * 시험지처럼 2단 이상으로 나뉜 사진을 찍으면, 옆 컬럼의 줄바꿈된 글자 조각이 실제 지문 사이에
     * 한 줄씩 섞여 들어온다(단순 OCR은 읽기 순서를 모르고 줄 단위로만 읽기 때문). 이 폭보다 좁은
     * 블록은 그런 "옆 컬럼에서 새어 들어온 조각"일 가능성이 매우 높아 결과에서 제외한다.
     * (실제 시험지 사진으로 검증: 제목/지문/선지는 폭 0.148~0.58, 새어 들어온 조각은 최대 0.132였음.
     * 그 사이인 0.14를 기준으로 삼음 — 세로로 짧게 나열되는 객관식 선지처럼 폭이 좁은 정상 블록도
     * 있어서 여유를 너무 크게 잡으면 선지가 같이 잘려나간다)
     */
    private val minElementWidthRatio = 0.14

    /**
     * 이미지에서 텍스트를 추출한다. Upstage Document Parse를 사용해 문서를 레이아웃 단위
     * (제목/문단/목록 등)로 인식한 뒤, 그중 폭이 충분히 넓은 블록만 순서대로 이어붙인다.
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

            response?.elements
                ?.filter { isWideEnough(it) && !it.content.text.isNullOrBlank() }
                ?.joinToString("\n\n") { it.content.text!!.trim() }
                ?.takeUnless { it.isBlank() }
        } catch (ex: RestClientException) {
            log.warn("OCR 텍스트 추출 실패, 수동 입력값으로 대체합니다", ex)
            null
        }
    }

    private fun isWideEnough(element: DocumentParseElement): Boolean {
        val xs = element.coordinates.map { it.x }
        if (xs.isEmpty()) return true
        return (xs.max() - xs.min()) >= minElementWidthRatio
    }
}
