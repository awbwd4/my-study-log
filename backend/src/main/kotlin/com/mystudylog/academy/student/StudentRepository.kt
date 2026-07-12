package com.mystudylog.academy.student

import org.springframework.data.jpa.repository.JpaRepository

interface StudentRepository : JpaRepository<Student, Long> {
    fun findByUserId(userId: String): Student?
    fun findBySchoolClassId(schoolClassId: Long): List<Student>
}
