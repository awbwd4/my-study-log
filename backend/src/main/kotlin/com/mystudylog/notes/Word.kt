package com.mystudylog.notes

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity
class Word(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "wrong_answer_note_detail_id", nullable = false)
    val wrongAnswerNoteDetail: WrongAnswerNoteDetail = WrongAnswerNoteDetail(),

    @Column(columnDefinition = "TEXT")
    var wordsJson: String = "[]",
)
