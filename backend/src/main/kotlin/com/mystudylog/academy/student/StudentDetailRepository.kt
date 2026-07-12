package com.mystudylog.academy.student

import org.springframework.data.jpa.repository.JpaRepository

interface StudentDetailRepository : JpaRepository<StudentDetail, Long> {
    fun findByStudentId(studentId: Long): List<StudentDetail>
}
