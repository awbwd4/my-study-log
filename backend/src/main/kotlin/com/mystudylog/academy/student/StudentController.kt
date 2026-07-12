package com.mystudylog.academy.student

import com.mystudylog.academy.school.SchoolRepository
import com.mystudylog.academy.schoolclass.SchoolClassRepository
import com.mystudylog.academy.teacher.TeacherRepository
import com.mystudylog.auth.currentPrincipal
import com.mystudylog.common.ForbiddenException
import com.mystudylog.common.NotFoundException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

data class StudentProfileRequest(
    val phone: String,
    val kakaoOpenChatLink: String?,
    val schoolClassId: Long?,
    val schoolId: Long?,
)

data class StudentProfileResponse(
    val id: Long,
    val name: String,
    val phone: String,
    val kakaoOpenChatLink: String?,
    val schoolClassId: Long?,
    val schoolId: Long?,
) {
    companion object {
        fun from(s: Student) = StudentProfileResponse(s.id, s.name, s.phone, s.kakaoOpenChatLink, s.schoolClass?.id, s.school?.id)
    }
}

@RestController
class StudentController(
    private val studentRepository: StudentRepository,
    private val schoolClassRepository: SchoolClassRepository,
    private val schoolRepository: SchoolRepository,
    private val teacherRepository: TeacherRepository,
) {

    @GetMapping("/api/me/student")
    @PreAuthorize("hasRole('STUDENT')")
    fun getMyProfile(): StudentProfileResponse =
        StudentProfileResponse.from(myStudent())

    @PutMapping("/api/me/student")
    @PreAuthorize("hasRole('STUDENT')")
    fun updateMyProfile(@RequestBody request: StudentProfileRequest): StudentProfileResponse {
        val student = myStudent()
        student.phone = request.phone
        student.kakaoOpenChatLink = request.kakaoOpenChatLink
        student.schoolClass = request.schoolClassId?.let {
            schoolClassRepository.findById(it).orElseThrow { NotFoundException("반을 찾을 수 없습니다") }
        }
        student.school = request.schoolId?.let {
            schoolRepository.findById(it).orElseThrow { NotFoundException("학교를 찾을 수 없습니다") }
        }
        return StudentProfileResponse.from(studentRepository.save(student))
    }

    @GetMapping("/api/students")
    @PreAuthorize("hasRole('TEACHER')")
    fun listByClass(@RequestParam classId: Long): List<StudentProfileResponse> {
        val teacher = teacherRepository.findByUserId(currentPrincipal().userId)
            ?: throw NotFoundException("강사 프로필을 찾을 수 없습니다")
        val schoolClass = schoolClassRepository.findById(classId).orElseThrow { NotFoundException("반을 찾을 수 없습니다") }
        if (schoolClass.teacher.id != teacher.id) throw ForbiddenException()
        return studentRepository.findBySchoolClassId(classId).map(StudentProfileResponse::from)
    }

    private fun myStudent(): Student =
        studentRepository.findByUserId(currentPrincipal().userId)
            ?: throw NotFoundException("학생 프로필을 찾을 수 없습니다")
}
