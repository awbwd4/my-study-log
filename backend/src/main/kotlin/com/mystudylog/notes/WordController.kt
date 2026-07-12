package com.mystudylog.notes

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.mystudylog.academy.student.assertCanAccessStudent
import com.mystudylog.common.NotFoundException
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class WordItem(val word: String, val meaning: String)
data class WordsRequest(val items: List<WordItem>)
data class WordResponse(val id: Long, val detailId: Long, val items: List<WordItem>)

@RestController
@RequestMapping("/api/notes/{noteId}/details/{detailId}/words")
class WordController(
    private val noteRepository: WrongAnswerNoteRepository,
    private val detailRepository: WrongAnswerNoteDetailRepository,
    private val wordRepository: WordRepository,
    private val objectMapper: ObjectMapper,
) {

    @GetMapping
    fun list(@PathVariable noteId: Long, @PathVariable detailId: Long): List<WordResponse> {
        findDetail(noteId, detailId)
        return wordRepository.findByWrongAnswerNoteDetailId(detailId).map { it.toResponse() }
    }

    @PostMapping
    fun create(
        @PathVariable noteId: Long,
        @PathVariable detailId: Long,
        @RequestBody request: WordsRequest,
    ): WordResponse {
        val detail = findDetail(noteId, detailId)
        val word = Word(wrongAnswerNoteDetail = detail, wordsJson = objectMapper.writeValueAsString(request.items))
        return wordRepository.save(word).toResponse()
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable noteId: Long, @PathVariable detailId: Long, @PathVariable id: Long) {
        findDetail(noteId, detailId)
        val word = wordRepository.findById(id).orElseThrow { NotFoundException("단어를 찾을 수 없습니다") }
        if (word.wrongAnswerNoteDetail.id != detailId) throw NotFoundException("단어를 찾을 수 없습니다")
        wordRepository.delete(word)
    }

    private fun findDetail(noteId: Long, detailId: Long): WrongAnswerNoteDetail {
        val note = noteRepository.findById(noteId).orElseThrow { NotFoundException("오답노트를 찾을 수 없습니다") }
        assertCanAccessStudent(note.student)
        val detail = detailRepository.findById(detailId).orElseThrow { NotFoundException("오답노트세부를 찾을 수 없습니다") }
        if (detail.wrongAnswerNote.id != noteId) throw NotFoundException("오답노트세부를 찾을 수 없습니다")
        return detail
    }

    private fun Word.toResponse(): WordResponse =
        WordResponse(id, wrongAnswerNoteDetail.id, objectMapper.readValue(wordsJson))
}
