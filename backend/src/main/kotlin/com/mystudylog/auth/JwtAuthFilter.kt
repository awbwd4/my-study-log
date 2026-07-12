package com.mystudylog.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.stereotype.Component

@Component
class JwtAuthFilter(
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader("Authorization")
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = header.substringAfter("Bearer ")
        val principal = jwtService.parseAccessToken(token)

        if (principal != null && SecurityContextHolder.getContext().authentication == null) {
            val user = userRepository.findById(principal.userId).orElse(null)
            if (user != null && user.isLoginEnabled) {
                val authorities = principal.type?.let { listOf(SimpleGrantedAuthority("ROLE_$it")) } ?: emptyList()
                val authToken = UsernamePasswordAuthenticationToken(principal, null, authorities)
                authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authToken
            }
        }

        filterChain.doFilter(request, response)
    }
}
