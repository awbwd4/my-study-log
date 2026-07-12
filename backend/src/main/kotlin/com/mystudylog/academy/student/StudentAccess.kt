package com.mystudylog.academy.student

import com.mystudylog.auth.currentPrincipal
import com.mystudylog.common.ForbiddenException

fun assertCanAccessStudent(student: Student) {
    val principal = currentPrincipal()
    val isOwner = student.user.id == principal.userId
    val isTeacherOfClass = student.schoolClass?.teacher?.user?.id == principal.userId
    if (!isOwner && !isTeacherOfClass) throw ForbiddenException()
}
