package com.mystudylog.academy.teacher

import com.mystudylog.auth.currentPrincipal
import com.mystudylog.common.NotFoundException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class TeacherProfileRequest(val name: String, val academyName: String)
data class TeacherProfileResponse(val id: Long, val name: String, val academyName: String) {
    companion object {
        fun from(teacher: Teacher) = TeacherProfileResponse(teacher.id, teacher.name, teacher.academyName)
    }
}

@RestController
@RequestMapping("/api/me/teacher")
@PreAuthorize("hasRole('TEACHER')")
class TeacherController(private val teacherRepository: TeacherRepository) {

    @GetMapping
    fun getProfile(): TeacherProfileResponse {
        val teacher = teacherRepository.findByUserId(currentPrincipal().userId)
            ?: throw NotFoundException("강사 프로필을 찾을 수 없습니다")
        return TeacherProfileResponse.from(teacher)
    }

    @PutMapping
    fun updateProfile(@RequestBody request: TeacherProfileRequest): TeacherProfileResponse {
        val teacher = teacherRepository.findByUserId(currentPrincipal().userId)
            ?: throw NotFoundException("강사 프로필을 찾을 수 없습니다")
        teacher.name = request.name
        teacher.academyName = request.academyName
        return TeacherProfileResponse.from(teacherRepository.save(teacher))
    }
}
