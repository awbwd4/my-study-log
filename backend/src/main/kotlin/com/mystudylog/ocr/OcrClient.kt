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

data class OcrPage(val text: String? = null)
data class OcrResponse(val text: String? = null, val pages: List<OcrPage>? = null)

@Component
class OcrClient(
    restClientBuilder: RestClient.Builder,
    @Value("\${upstage.api-key}") private val apiKey: String,
) {
    private val restClient = restClientBuilder.baseUrl("https://api.upstage.ai").build()
    private val log = LoggerFactory.getLogger(OcrClient::class.java)

    /**
     * 이미지에서 텍스트를 추출한다. API 키 미설정, 네트워크 오류, 응답 파싱 실패 등
     * 어떤 이유로든 실패하면 null을 반환할 뿐 예외를 던지지 않는다 — 오답노트 등록 자체는
     * OCR 성공 여부와 무관하게 항상 완료되어야 하기 때문.
     */
    fun extractText(image: MultipartFile): String? {
        if (apiKey.isBlank()) return null
        return try {
            val multipart = MultipartBodyBuilder().apply {
                part("document", image.resource)
                part("model", "ocr")
            }.build()

            val response = restClient.post()
                .uri("/v1/document-digitization")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Authorization", "Bearer $apiKey")
                .body(multipart)
                .retrieve()
                .body<OcrResponse>()

            val text = response?.text?.takeUnless { it.isBlank() }
                ?: response?.pages?.mapNotNull { it.text }?.joinToString("\n")?.takeUnless { it.isBlank() }
            text
        } catch (ex: RestClientException) {
            log.warn("OCR 텍스트 추출 실패, 수동 입력값으로 대체합니다", ex)
            null
        }
    }
}
