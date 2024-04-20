package dev.loudbook.pastebook.controllers

import com.google.gson.Gson
import com.google.gson.JsonParser
import dev.loudbook.pastebook.BucketUtils
import dev.loudbook.pastebook.mongo.PasteRepository
import io.github.bucket4j.Bucket
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ListController {
    @Autowired
    lateinit var pasteRepository: PasteRepository

    private val bucket: Bucket = BucketUtils.getBucketPerSeconds(1)

    @GetMapping("/list")
    fun list(): ResponseEntity<String> {
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(429).body("Rate limit exceeded")
        }

        val pastes = pasteRepository.findAll().toList()
        val json = JsonParser.parseString(Gson().toJson(pastes)).asJsonArray

        json.mapIndexed { _, element ->
            element.asJsonObject.remove("content")
        }

        return ResponseEntity.ok(json.toString())
    }
}