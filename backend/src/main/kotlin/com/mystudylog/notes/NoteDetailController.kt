package com.mystudylog.notes

import com.mystudylog.academy.student.assertCanAccessStudent
import com.mystudylog.common.FileStorageService
import com.mystudylog.common.NotFoundException
import java.time.LocalDate
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

data class NoteDetailRequest(
    val body: String,
    val submittedAnswer: String?,
    val isActuallyWrong: Boolean,
    val questionType: QuestionType,
    val registeredDate: LocalDate,
    val isRetake: Boolean,
    val originalQuestionKey: String?,
)

data class NoteDetailResponse(
    val id: Long,
    val noteId: Long,
    val body: String,
    val submittedAnswer: String?,
    val isActuallyWrong: Boolean,
    val questionType: QuestionType,
    val registeredDate: LocalDate,
    val isRetake: Boolean,
    val originalQuestionKey: String?,
    val imagePath: String?,
) {
    companion object {
        fun from(d: WrongAnswerNoteDetail) = NoteDetailResponse(
            d.id, d.wrongAnswerNote.id, d.body, d.submittedAnswer, d.isActuallyWrong,
            d.questionType, d.registeredDate, d.isRetake, d.originalQuestionKey, d.imagePath,
        )
    }
}

@RestController
@RequestMapping("/api/notes/{noteId}/details")
class NoteDetailController(
    private val noteRepository: WrongAnswerNoteRepository,
    private val detailRepository: WrongAnswerNoteDetailRepository,
    private val fileStorageService: FileStorageService,
) {

    @GetMapping
    fun list(@PathVariable noteId: Long): List<NoteDetailResponse> {
        findNote(noteId)
        return detailRepository.findByWrongAnswerNoteId(noteId).map(NoteDetailResponse::from)
    }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun create(
        @PathVariable noteId: Long,
        @RequestPart("data") request: NoteDetailRequest,
        @RequestPart("image", required = false) image: MultipartFile?,
    ): NoteDetailResponse {
        val note = findNote(noteId)
        val detail = WrongAnswerNoteDetail(
            wrongAnswerNote = note,
            body = request.body,
            submittedAnswer = request.submittedAnswer,
            isActuallyWrong = request.isActuallyWrong,
            questionType = request.questionType,
            registeredDate = request.registeredDate,
            isRetake = request.isRetake,
            originalQuestionKey = request.originalQuestionKey,
            imagePath = image?.takeUnless { it.isEmpty }?.let { fileStorageService.store(it) },
        )
        return NoteDetailResponse.from(detailRepository.save(detail))
    }

    @PutMapping("/{id}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun update(
        @PathVariable noteId: Long,
        @PathVariable id: Long,
        @RequestPart("data") request: NoteDetailRequest,
        @RequestPart("image", required = false) image: MultipartFile?,
    ): NoteDetailResponse {
        findNote(noteId)
        val detail = findDetail(noteId, id)
        detail.body = request.body
        detail.submittedAnswer = request.submittedAnswer
        detail.isActuallyWrong = request.isActuallyWrong
        detail.questionType = request.questionType
        detail.registeredDate = request.registeredDate
        detail.isRetake = request.isRetake
        detail.originalQuestionKey = request.originalQuestionKey
        image?.takeUnless { it.isEmpty }?.let { detail.imagePath = fileStorageService.store(it) }
        return NoteDetailResponse.from(detailRepository.save(detail))
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable noteId: Long, @PathVariable id: Long) {
        findNote(noteId)
        detailRepository.delete(findDetail(noteId, id))
    }

    private fun findNote(noteId: Long): WrongAnswerNote {
        val note = noteRepository.findById(noteId).orElseThrow { NotFoundException("오답노트를 찾을 수 없습니다") }
        assertCanAccessStudent(note.student)
        return note
    }

    private fun findDetail(noteId: Long, id: Long): WrongAnswerNoteDetail {
        val detail = detailRepository.findById(id).orElseThrow { NotFoundException("오답노트세부를 찾을 수 없습니다") }
        if (detail.wrongAnswerNote.id != noteId) throw NotFoundException("오답노트세부를 찾을 수 없습니다")
        return detail
    }
}
