package com.mystudylog.common

class NotFoundException(message: String) : RuntimeException(message)

class ForbiddenException(message: String = "접근 권한이 없습니다") : RuntimeException(message)

class BadRequestException(message: String) : RuntimeException(message)
