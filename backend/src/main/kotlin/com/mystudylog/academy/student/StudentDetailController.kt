package com.mystudylog.academy.student

import com.mystudylog.common.NotFoundException
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class StudentDetailRequest(val seq: Int)
data class StudentDetailResponse(val id: Long, val studentId: Long, val seq: Int) {
    companion object {
        fun from(d: StudentDetail) = StudentDetailResponse(d.id, d.student.id, d.seq)
    }
}

@RestController
@RequestMapping("/api/students/{studentId}/details")
class StudentDetailController(
    private val studentDetailRepository: StudentDetailRepository,
    private val studentRepository: StudentRepository,
) {

    private fun findStudent(studentId: Long): Student =
        studentRepository.findById(studentId).orElseThrow { NotFoundException("학생을 찾을 수 없습니다") }
            .also { assertCanAccessStudent(it) }

    @GetMapping
    fun list(@PathVariable studentId: Long): List<StudentDetailResponse> {
        findStudent(studentId)
        return studentDetailRepository.findByStudentId(studentId).map(StudentDetailResponse::from)
    }

    @PostMapping
    fun create(@PathVariable studentId: Long, @RequestBody request: StudentDetailRequest): StudentDetailResponse {
        val student = findStudent(studentId)
        val detail = studentDetailRepository.save(StudentDetail(student = student, seq = request.seq))
        return StudentDetailResponse.from(detail)
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable studentId: Long, @PathVariable id: Long) {
        findStudent(studentId)
        val detail = studentDetailRepository.findById(id).orElseThrow { NotFoundException("찾을 수 없습니다") }
        studentDetailRepository.delete(detail)
    }
}
