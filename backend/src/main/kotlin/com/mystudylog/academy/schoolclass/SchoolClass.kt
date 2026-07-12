package com.mystudylog.academy.schoolclass

import com.mystudylog.academy.teacher.Teacher
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.time.LocalTime

@Entity
class SchoolClass(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    val teacher: Teacher = Teacher(),

    var dayOfWeek: String = "",
    var time: LocalTime = LocalTime.MIDNIGHT,
    var grade: String = "",
)
