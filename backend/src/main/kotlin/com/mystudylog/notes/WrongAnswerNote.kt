package com.mystudylog.notes

import com.mystudylog.academy.student.Student
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.time.Instant

@Entity
class WrongAnswerNote(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    val student: Student = Student(),

    val createdAt: Instant = Instant.now(),
)
