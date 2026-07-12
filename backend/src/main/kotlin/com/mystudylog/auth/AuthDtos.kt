package com.mystudylog.auth

data class KakaoLoginRequest(val accessToken: String)

data class RegisterProfileRequest(
    val tempToken: String,
    val type: String,
    val name: String,
    val academyName: String? = null,
    val phone: String? = null,
    val kakaoOpenChatLink: String? = null,
    val schoolClassId: Long? = null,
    val schoolId: Long? = null,
)

data class DevLoginRequest(
    val kakaoId: String,
    val type: String,
    val name: String,
    val academyName: String? = null,
    val phone: String? = null,
    val kakaoOpenChatLink: String? = null,
    val schoolClassId: Long? = null,
    val schoolId: Long? = null,
)

data class AuthResult(val status: String, val token: String, val type: String?)
