package com.mystudylog.academy.teacher

import com.mystudylog.auth.User
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne

@Entity
class Teacher(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @OneToOne
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    val user: User = User(),

    var name: String = "",
    var academyName: String = "",
)
