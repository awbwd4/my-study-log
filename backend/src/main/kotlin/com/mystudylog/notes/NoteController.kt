package com.mystudylog.notes

import com.mystudylog.academy.student.Student
import com.mystudylog.academy.student.StudentRepository
import com.mystudylog.academy.student.assertCanAccessStudent
import com.mystudylog.auth.UserType
import com.mystudylog.auth.currentPrincipal
import com.mystudylog.common.BadRequestException
import com.mystudylog.common.FileStorageService
import com.mystudylog.common.ForbiddenException
import com.mystudylog.common.NotFoundException
import com.mystudylog.ocr.OcrClient
import java.time.Instant
import java.time.LocalDate
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

data class NoteCreateRequest(val studentId: Long? = null)
data class NoteResponse(val id: Long, val studentId: Long, val createdAt: Instant, val detail: NoteDetailResponse?) {
    companion object {
        fun from(note: WrongAnswerNote, detail: WrongAnswerNoteDetail? = null) =
            NoteResponse(note.id, note.student.id, note.createdAt, detail?.let(NoteDetailResponse::from))
    }
}

@RestController
@RequestMapping("/api/notes")
class NoteController(
    private val noteRepository: WrongAnswerNoteRepository,
    private val detailRepository: WrongAnswerNoteDetailRepository,
    private val studentRepository: StudentRepository,
    private val fileStorageService: FileStorageService,
    private val ocrClient: OcrClient,
) {

    @GetMapping
    fun list(@RequestParam(required = false) studentId: Long?): List<NoteResponse> {
        val student = resolveStudent(studentId)
        val notes = noteRepository.findByStudentIdOrderByCreatedAtDesc(student.id)
        val detailsByNoteId = detailRepository.findByWrongAnswerNoteIdIn(notes.map { it.id })
            .associateBy { it.wrongAnswerNote.id }
        return notes.map { NoteResponse.from(it, detailsByNoteId[it.id]) }
    }

    @PostMapping
    fun create(@RequestBody request: NoteCreateRequest): NoteResponse {
        val student = resolveStudent(request.studentId)
        return NoteResponse.from(noteRepository.save(WrongAnswerNote(student = student)))
    }

    /**
     * 사진 여러 장을 한 번에 올리면 사진 한 장당 오답노트(+그 안의 문제 하나)를 통째로 만들어준다.
     * "오답노트 = 문제 하나"로 쓰는 게 기본 사용 방식이라, 학생이 시험지를 여러 장 찍어서 한꺼번에
     * 등록하는 흐름을 지원하기 위한 엔드포인트. 각 사진은 OCR로 본문을 채우고, 나머지 항목(제출답안/
     * 객관식·서술형/재응시 여부 등)은 학생이 목록에서 나중에 열어 채운다.
     */
    @PostMapping("/batch", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createBatch(
        @RequestPart("images") images: List<MultipartFile>,
        @RequestParam(required = false) studentId: Long?,
    ): List<NoteResponse> {
        val student = resolveStudent(studentId)
        return images.filter { !it.isEmpty }.map { image ->
            val note = noteRepository.save(WrongAnswerNote(student = student))
            val detail = detailRepository.save(
                WrongAnswerNoteDetail(
                    wrongAnswerNote = note,
                    body = ocrClient.extractText(image) ?: "",
                    isActuallyWrong = true,
                    questionType = QuestionType.MULTIPLE_CHOICE,
                    registeredDate = LocalDate.now(),
                    isRetake = false,
                    imagePath = fileStorageService.store(image),
                )
            )
            NoteResponse.from(note, detail)
        }
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): NoteResponse {
        val note = findNote(id)
        val detail = detailRepository.findByWrongAnswerNoteId(id).firstOrNull()
        return NoteResponse.from(note, detail)
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long) {
        noteRepository.delete(findNote(id))
    }

    private fun findNote(id: Long): WrongAnswerNote {
        val note = noteRepository.findById(id).orElseThrow { NotFoundException("오답노트를 찾을 수 없습니다") }
        assertCanAccessStudent(note.student)
        return note
    }

    private fun resolveStudent(studentId: Long?): Student {
        val principal = currentPrincipal()
        return when (principal.type) {
            UserType.STUDENT -> {
                val own = studentRepository.findByUserId(principal.userId)
                    ?: throw NotFoundException("학생 프로필을 찾을 수 없습니다")
                if (studentId != null && studentId != own.id) throw ForbiddenException()
                own
            }
            UserType.TEACHER -> {
                val id = studentId ?: throw BadRequestException("studentId는 필수입니다")
                val student = studentRepository.findById(id).orElseThrow { NotFoundException("학생을 찾을 수 없습니다") }
                assertCanAccessStudent(student)
                student
            }
            null -> throw ForbiddenException()
        }
    }
}
