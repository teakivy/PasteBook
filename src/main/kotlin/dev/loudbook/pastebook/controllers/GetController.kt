package dev.loudbook.pastebook.controllers

import com.google.gson.Gson
import dev.loudbook.pastebook.BucketUtils
import dev.loudbook.pastebook.data.R2Service
import dev.loudbook.pastebook.data.PastePrivateDTO
import dev.loudbook.pastebook.mongo.PasteRepository
import dev.loudbook.pastebook.mongo.UserService
import io.github.bucket4j.Bucket
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class GetController {
    @Autowired
    lateinit var r2Service: R2Service

    @Autowired
    lateinit var pasteRepository: PasteRepository

    @Autowired
    lateinit var userService: UserService

    private val bucket: Bucket = BucketUtils.getBucketPerSeconds(4)

    @GetMapping("/get/{id}/metadata")
    fun get(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<String> {
        if (!userService.processRequest(request)) {
            return ResponseEntity.status(403).body("Prohibited")
        }

        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(429).body("Rate limit exceeded")
        }

        val paste: PastePrivateDTO = pasteRepository.findDTOByID(id) ?: return ResponseEntity.notFound().build()

        val json = Gson().toJson(paste.toPublicDTO())
        return ResponseEntity.ok().body(json)
    }

    @GetMapping("/get/{id}/content")
    fun getContent(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<String> {
        if (!userService.processRequest(request)) {
            return ResponseEntity.status(403).body("Prohibited")
        }

        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(429).body(null)
        }

        val headers = HttpHeaders()
        headers.add("Content-Type", "text/plain; charset=utf-8")

        val paste = r2Service.getFile(id) ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).headers(headers).body(paste)
    }
}