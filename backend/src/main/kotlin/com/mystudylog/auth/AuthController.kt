package com.mystudylog.auth

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/kakao")
    fun loginWithKakao(@RequestBody request: KakaoLoginRequest): AuthResult =
        authService.loginWithKakao(request.accessToken)

    @PostMapping("/register-profile")
    fun completeProfile(@RequestBody request: RegisterProfileRequest): AuthResult =
        authService.completeProfile(request)
}
