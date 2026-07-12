package com.mystudylog.auth

import com.mystudylog.academy.school.SchoolRepository
import com.mystudylog.academy.schoolclass.SchoolClassRepository
import com.mystudylog.academy.student.Student
import com.mystudylog.academy.student.StudentRepository
import com.mystudylog.academy.teacher.Teacher
import com.mystudylog.academy.teacher.TeacherRepository
import com.mystudylog.common.BadRequestException
import com.mystudylog.common.ForbiddenException
import com.mystudylog.common.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AuthService(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
    private val kakaoClient: KakaoClient,
    private val teacherRepository: TeacherRepository,
    private val studentRepository: StudentRepository,
    private val schoolClassRepository: SchoolClassRepository,
    private val schoolRepository: SchoolRepository,
) {

    fun loginWithKakao(accessToken: String): AuthResult {
        val kakaoId = kakaoClient.fetchKakaoUserId(accessToken)
        val user = userRepository.findById(kakaoId).orElseGet { userRepository.save(User(id = kakaoId)) }
        return issueResultFor(user)
    }

    fun completeProfile(request: RegisterProfileRequest): AuthResult {
        val userId = jwtService.parseTempToken(request.tempToken)
            ?: throw ForbiddenException("유효하지 않거나 만료된 토큰입니다")
        val user = userRepository.findById(userId).orElseThrow { NotFoundException("사용자를 찾을 수 없습니다") }
        applyProfile(user, request.type, request.name, request.academyName, request.phone, request.kakaoOpenChatLink, request.schoolClassId, request.schoolId)
        return issueResultFor(user)
    }

    fun devLogin(request: DevLoginRequest): AuthResult {
        var user = userRepository.findById(request.kakaoId).orElse(null)
        if (user == null) {
            user = userRepository.save(User(id = request.kakaoId))
        }
        if (user.type == null) {
            applyProfile(user, request.type, request.name, request.academyName, request.phone, request.kakaoOpenChatLink, request.schoolClassId, request.schoolId)
        }
        return issueResultFor(user)
    }

    private fun applyProfile(
        user: User,
        typeStr: String,
        name: String,
        academyName: String?,
        phone: String?,
        kakaoOpenChatLink: String?,
        schoolClassId: Long?,
        schoolId: Long?,
    ) {
        if (user.type != null) throw BadRequestException("이미 프로필이 등록된 사용자입니다")
        val type = runCatching { UserType.valueOf(typeStr) }.getOrElse { throw BadRequestException("type은 TEACHER 또는 STUDENT여야 합니다") }
        user.type = type
        userRepository.save(user)

        when (type) {
            UserType.TEACHER -> {
                val resolvedAcademyName = academyName ?: throw BadRequestException("academyName은 필수입니다")
                teacherRepository.save(Teacher(user = user, name = name, academyName = resolvedAcademyName))
            }
            UserType.STUDENT -> {
                val resolvedPhone = phone ?: throw BadRequestException("phone은 필수입니다")
                val schoolClass = schoolClassId?.let {
                    schoolClassRepository.findById(it).orElseThrow { NotFoundException("반을 찾을 수 없습니다") }
                }
                val school = schoolId?.let {
                    schoolRepository.findById(it).orElseThrow { NotFoundException("학교를 찾을 수 없습니다") }
                }
                studentRepository.save(
                    Student(
                        user = user,
                        name = name,
                        phone = resolvedPhone,
                        kakaoOpenChatLink = kakaoOpenChatLink,
                        schoolClass = schoolClass,
                        school = school,
                    )
                )
            }
        }
    }

    private fun issueResultFor(user: User): AuthResult {
        val type = user.type
        if (type == null) {
            return AuthResult(status = "NEEDS_PROFILE", token = jwtService.generateTempToken(user.id), type = null)
        }
        if (!user.isLoginEnabled) throw ForbiddenException("비활성화된 계정입니다")
        return AuthResult(status = "OK", token = jwtService.generateAccessToken(user.id, type), type = type.name)
    }
}
