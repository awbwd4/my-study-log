package com.mystudylog.auth

import com.mystudylog.common.ForbiddenException
import org.springframework.security.core.context.SecurityContextHolder

fun currentPrincipal(): AuthPrincipal =
    SecurityContextHolder.getContext().authentication?.principal as? AuthPrincipal
        ?: throw ForbiddenException("인증이 필요합니다")
