package com.mystudylog.auth

import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 카카오 디벨로퍼스 앱(REST API 키)이 아직 없을 때 로컬 개발/테스트용으로만 사용하는 우회 로그인.
 * dev 프로필에서만 등록되며, 운영 환경(application-prod.yml)에는 절대 노출되지 않는다.
 */
@RestController
@RequestMapping("/api/auth")
@Profile("dev")
class DevAuthController(private val authService: AuthService) {

    @PostMapping("/dev-login")
    fun devLogin(@RequestBody request: DevLoginRequest): AuthResult = authService.devLogin(request)
}
