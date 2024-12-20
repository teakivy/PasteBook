package dev.loudbook.pastebook.controllers

import dev.loudbook.pastebook.BucketUtils
import dev.loudbook.pastebook.ContentScanner
import dev.loudbook.pastebook.IPUtils
import dev.loudbook.pastebook.data.PastePrivateDTO
import dev.loudbook.pastebook.data.R2Service
import dev.loudbook.pastebook.mongo.PasteRepository
import dev.loudbook.pastebook.mongo.UserService
import io.github.bucket4j.Bucket
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UploadController {
    @Autowired
    lateinit var r2Service: R2Service

    @Autowired
    lateinit var pasteRepository: PasteRepository

    @Autowired
    lateinit var userService: UserService

    private val bucket: Bucket = BucketUtils.getBucketPerMinutes(4)

    @PostMapping(value = ["/upload"])
    fun upload(request: HttpServletRequest, @RequestBody body: String): ResponseEntity<String> {
        if (!userService.processRequest(request)) {
            return ResponseEntity.status(403).body("Prohibited")
        }

        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(429).body("Rate limit exceeded")
        }

        val header = HttpHeaders()

        val fileID = generateRandomString()

        val sinceTheEpoch = System.currentTimeMillis()

        val title = request.getHeader("title") ?: return ResponseEntity.badRequest().body("Title is required")
        val reportBook = request.getHeader("reportBook")?.toBoolean() == true
        val wrap = request.getHeader("wrap")?.toBoolean() == true
        var expire = request.getHeader("expires")?.toLong() ?: (sinceTheEpoch + 8.64e+7).toLong()

        val hostDomain = request.getHeader("X-Domain-Name")

        if (expire < 60000) {
            return ResponseEntity.badRequest().body("Expire time too short")
        }

        if (expire < sinceTheEpoch) {
            expire += sinceTheEpoch
        }

        if (expire > (sinceTheEpoch + 2.765e+9)) {
            return ResponseEntity.badRequest().body("Expire time too long")
        }

        val filteredBody = ContentScanner.scanContent(body)

        val ip = IPUtils.getIPFromRequest(request) ?: return ResponseEntity.badRequest().body("Failed to get IP")

        val paste = PastePrivateDTO(fileID, title, sinceTheEpoch, reportBook, wrap, ip, expire)

        r2Service.uploadFile(fileID, filteredBody)
        pasteRepository.save(paste)

        if (hostDomain != null) {
            return ResponseEntity.ok().headers(header).body("https://$hostDomain/p/$fileID")
        }

        return ResponseEntity.ok().headers(header).body(fileID)
    }

    fun generateRandomString(length: Int = 5): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }
}