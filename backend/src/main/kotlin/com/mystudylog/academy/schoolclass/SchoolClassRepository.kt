package com.mystudylog.academy.schoolclass

import org.springframework.data.jpa.repository.JpaRepository

interface SchoolClassRepository : JpaRepository<SchoolClass, Long> {
    fun findByTeacherId(teacherId: Long): List<SchoolClass>
}
