package com.mystudylog.academy.schoolclass

import com.mystudylog.academy.teacher.TeacherRepository
import com.mystudylog.auth.currentPrincipal
import com.mystudylog.common.ForbiddenException
import com.mystudylog.common.NotFoundException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalTime

data class SchoolClassRequest(val dayOfWeek: String, val time: LocalTime, val grade: String)
data class SchoolClassResponse(val id: Long, val dayOfWeek: String, val time: LocalTime, val grade: String) {
    companion object {
        fun from(c: SchoolClass) = SchoolClassResponse(c.id, c.dayOfWeek, c.time, c.grade)
    }
}

@RestController
@RequestMapping("/api/classes")
@PreAuthorize("hasRole('TEACHER')")
class SchoolClassController(
    private val schoolClassRepository: SchoolClassRepository,
    private val teacherRepository: TeacherRepository,
) {

    private fun currentTeacher() =
        teacherRepository.findByUserId(currentPrincipal().userId)
            ?: throw NotFoundException("강사 프로필을 찾을 수 없습니다")

    private fun ownedClass(id: Long): SchoolClass {
        val schoolClass = schoolClassRepository.findById(id).orElseThrow { NotFoundException("반을 찾을 수 없습니다") }
        if (schoolClass.teacher.id != currentTeacher().id) throw ForbiddenException()
        return schoolClass
    }

    @GetMapping
    fun list(): List<SchoolClassResponse> =
        schoolClassRepository.findByTeacherId(currentTeacher().id).map(SchoolClassResponse::from)

    @PostMapping
    fun create(@RequestBody request: SchoolClassRequest): SchoolClassResponse {
        val schoolClass = SchoolClass(
            teacher = currentTeacher(),
            dayOfWeek = request.dayOfWeek,
            time = request.time,
            grade = request.grade,
        )
        return SchoolClassResponse.from(schoolClassRepository.save(schoolClass))
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody request: SchoolClassRequest): SchoolClassResponse {
        val schoolClass = ownedClass(id)
        schoolClass.dayOfWeek = request.dayOfWeek
        schoolClass.time = request.time
        schoolClass.grade = request.grade
        return SchoolClassResponse.from(schoolClassRepository.save(schoolClass))
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long) {
        schoolClassRepository.delete(ownedClass(id))
    }
}
