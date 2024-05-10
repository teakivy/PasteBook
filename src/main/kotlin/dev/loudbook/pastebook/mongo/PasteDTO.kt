package dev.loudbook.pastebook.mongo

import org.springframework.data.annotation.Id

class PasteDTO(@Id var id: String?, val title: String, val created: Long, val reportBook: Boolean = false, val unlisted: Boolean = false, val wrap: Boolean = false)