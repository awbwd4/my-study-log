package com.mystudylog.academy.student

import com.mystudylog.academy.school.School
import com.mystudylog.academy.schoolclass.SchoolClass
import com.mystudylog.auth.User
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne

@Entity
class Student(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @OneToOne
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    val user: User = User(),

    var name: String = "",
    var phone: String = "",
    var kakaoOpenChatLink: String? = null,

    @ManyToOne
    @JoinColumn(name = "school_class_id")
    var schoolClass: SchoolClass? = null,

    @ManyToOne
    @JoinColumn(name = "school_id")
    var school: School? = null,
)
