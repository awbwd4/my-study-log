package com.mystudylog.notes

import org.springframework.data.jpa.repository.JpaRepository

interface WrongAnswerNoteRepository : JpaRepository<WrongAnswerNote, Long> {
    fun findByStudentId(studentId: Long): List<WrongAnswerNote>
}

interface WrongAnswerNoteDetailRepository : JpaRepository<WrongAnswerNoteDetail, Long> {
    fun findByWrongAnswerNoteId(noteId: Long): List<WrongAnswerNoteDetail>
}

interface WordRepository : JpaRepository<Word, Long> {
    fun findByWrongAnswerNoteDetailId(detailId: Long): List<Word>
}
