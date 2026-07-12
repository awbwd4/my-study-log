package com.mystudylog.academy.teacher

import org.springframework.data.jpa.repository.JpaRepository

interface TeacherRepository : JpaRepository<Teacher, Long> {
    fun findByUserId(userId: String): Teacher?
}
