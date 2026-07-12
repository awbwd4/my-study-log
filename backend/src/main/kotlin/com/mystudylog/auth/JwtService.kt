package com.mystudylog.auth

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey

private const val PURPOSE_ACCESS = "access"
private const val PURPOSE_PROFILE = "profile"

@Service
class JwtService(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.expiration-ms}") private val accessExpirationMs: Long,
    @Value("\${jwt.temp-expiration-ms}") private val tempExpirationMs: Long,
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    fun generateAccessToken(userId: String, type: UserType): String =
        build(userId, accessExpirationMs) {
            it.claim("purpose", PURPOSE_ACCESS).claim("type", type.name)
        }

    fun generateTempToken(userId: String): String =
        build(userId, tempExpirationMs) {
            it.claim("purpose", PURPOSE_PROFILE)
        }

    fun parseAccessToken(token: String): AuthPrincipal? {
        val claims = parseClaims(token) ?: return null
        if (claims["purpose"] != PURPOSE_ACCESS) return null
        val type = (claims["type"] as? String)?.let { UserType.valueOf(it) }
        return AuthPrincipal(userId = claims.subject, type = type)
    }

    fun parseTempToken(token: String): String? {
        val claims = parseClaims(token) ?: return null
        if (claims["purpose"] != PURPOSE_PROFILE) return null
        return claims.subject
    }

    private fun build(subject: String, expirationMs: Long, customize: (io.jsonwebtoken.JwtBuilder) -> Unit): String {
        val now = Date()
        val builder = Jwts.builder()
            .subject(subject)
            .issuedAt(now)
            .expiration(Date(now.time + expirationMs))
        customize(builder)
        return builder.signWith(key).compact()
    }

    private fun parseClaims(token: String): Claims? =
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
        } catch (ex: JwtException) {
            null
        }
}
