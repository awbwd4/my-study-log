package com.mystudylog.academy.school

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class SchoolRequest(val name: String)
data class SchoolResponse(val id: Long, val name: String) {
    companion object {
        fun from(school: School) = SchoolResponse(school.id, school.name)
    }
}

@RestController
@RequestMapping("/api/schools")
class SchoolController(private val schoolRepository: SchoolRepository) {

    @GetMapping
    fun list(): List<SchoolResponse> = schoolRepository.findAll().map(SchoolResponse::from)

    @PostMapping
    fun create(@RequestBody request: SchoolRequest): SchoolResponse =
        SchoolResponse.from(schoolRepository.save(School(name = request.name)))
}
