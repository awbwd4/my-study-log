package com.mystudylog.auth

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

enum class UserType { TEACHER, STUDENT }

@Entity
@Table(name = "users")
class User(
    @Id
    val id: String = "",

    @Enumerated(EnumType.STRING)
    var type: UserType? = null,

    var passwordHash: String? = null,

    @Column(nullable = false)
    var isLoginEnabled: Boolean = true,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
)
