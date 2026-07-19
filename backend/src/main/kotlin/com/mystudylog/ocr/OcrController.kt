package com.mystudylog.ocr

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

data class OcrExtractResponse(val text: String?)

/**
 * 사진을 찍은 직후(오답노트 항목을 아직 저장하기 전) 바로 텍스트를 인식해서
 * 프론트가 본문 입력칸을 즉시 채울 수 있게 해주는 엔드포인트. 로그인한 사용자면
 * 누구나 호출 가능 — 특정 오답노트/학생에 종속되지 않는 상태 없는(stateless) 유틸리티.
 */
@RestController
@RequestMapping("/api/ocr")
class OcrController(private val ocrClient: OcrClient) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun extract(@RequestPart("image") image: MultipartFile): OcrExtractResponse =
        OcrExtractResponse(ocrClient.extractText(image))
}
