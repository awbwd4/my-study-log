package com.mystudylog.auth

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 카카오 디벨로퍼스 앱(REST API 키)이 아직 없을 때 로컬 개발/테스트 또는 임시 운영 검증용으로
 * 쓰는 우회 로그인. dev 프로필에서는 항상 켜져 있고, prod에서는 app.dev-login.enabled=true
 * (ENABLE_DEV_LOGIN 환경변수)를 명시적으로 설정했을 때만 켜진다 — 카카오 연동이 끝나면
 * 반드시 ENABLE_DEV_LOGIN을 지우거나 false로 되돌릴 것.
 */
@RestController
@RequestMapping("/api/auth")
@ConditionalOnProperty(prefix = "app.dev-login", name = ["enabled"], havingValue = "true")
class DevAuthController(private val authService: AuthService) {

    @PostMapping("/dev-login")
    fun devLogin(@RequestBody request: DevLoginRequest): AuthResult = authService.devLogin(request)
}
