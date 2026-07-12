package com.mystudylog.common

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

@Component
class FileStorageService(
    @Value("\${file.upload-dir}") private val uploadDir: String,
) {
    fun store(file: MultipartFile): String {
        val dir = Paths.get(uploadDir).toAbsolutePath().normalize()
        Files.createDirectories(dir)
        val extension = file.originalFilename?.substringAfterLast('.', "").orEmpty()
        val filename = UUID.randomUUID().toString() + if (extension.isNotBlank()) ".$extension" else ""
        file.transferTo(dir.resolve(filename).toFile())
        return "/uploads/$filename"
    }
}
