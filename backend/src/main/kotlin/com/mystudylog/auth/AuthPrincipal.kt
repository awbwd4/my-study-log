package com.mystudylog.auth

data class AuthPrincipal(
    val userId: String,
    val type: UserType?,
)
