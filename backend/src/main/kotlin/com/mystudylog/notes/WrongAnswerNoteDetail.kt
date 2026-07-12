package com.mystudylog.notes

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.time.LocalDate

enum class QuestionType { ESSAY, MULTIPLE_CHOICE }

@Entity
class WrongAnswerNoteDetail(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "wrong_answer_note_id", nullable = false)
    val wrongAnswerNote: WrongAnswerNote = WrongAnswerNote(),

    @Column(columnDefinition = "TEXT")
    var body: String = "",

    @Column(columnDefinition = "TEXT")
    var submittedAnswer: String? = null,

    var isActuallyWrong: Boolean = true,

    @Enumerated(EnumType.STRING)
    var questionType: QuestionType = QuestionType.MULTIPLE_CHOICE,

    var registeredDate: LocalDate = LocalDate.now(),

    var isRetake: Boolean = false,

    var originalQuestionKey: String? = null,

    var imagePath: String? = null,
)
