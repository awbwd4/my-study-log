package com.mystudylog.notes

import com.mystudylog.academy.student.Student
import com.mystudylog.academy.student.StudentRepository
import com.mystudylog.academy.student.assertCanAccessStudent
import com.mystudylog.auth.UserType
import com.mystudylog.auth.currentPrincipal
import com.mystudylog.common.BadRequestException
import com.mystudylog.common.ForbiddenException
import com.mystudylog.common.NotFoundException
import java.time.Instant
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

data class NoteCreateRequest(val studentId: Long? = null)
data class NoteResponse(val id: Long, val studentId: Long, val createdAt: Instant) {
    companion object {
        fun from(note: WrongAnswerNote) = NoteResponse(note.id, note.student.id, note.createdAt)
    }
}

@RestController
@RequestMapping("/api/notes")
class NoteController(
    private val noteRepository: WrongAnswerNoteRepository,
    private val studentRepository: StudentRepository,
) {

    @GetMapping
    fun list(@RequestParam(required = false) studentId: Long?): List<NoteResponse> {
        val student = resolveStudent(studentId)
        return noteRepository.findByStudentId(student.id).map(NoteResponse::from)
    }

    @PostMapping
    fun create(@RequestBody request: NoteCreateRequest): NoteResponse {
        val student = resolveStudent(request.studentId)
        return NoteResponse.from(noteRepository.save(WrongAnswerNote(student = student)))
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): NoteResponse {
        val note = findNote(id)
        return NoteResponse.from(note)
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
