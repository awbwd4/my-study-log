package com.mystudylog.auth

import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.body

data class KakaoUserMeResponse(val id: Long)

@Component
class KakaoClient(
    restClientBuilder: RestClient.Builder,
) {
    private val restClient = restClientBuilder.baseUrl("https://kapi.kakao.com").build()

    fun fetchKakaoUserId(accessToken: String): String {
        val response = try {
            restClient.get()
                .uri("/v2/user/me")
                .header("Authorization", "Bearer $accessToken")
                .retrieve()
                .body<KakaoUserMeResponse>()
        } catch (ex: RestClientException) {
            throw InvalidKakaoTokenException()
        } ?: throw InvalidKakaoTokenException()
        return response.id.toString()
    }
}

class InvalidKakaoTokenException : RuntimeException("유효하지 않은 카카오 액세스 토큰입니다")
