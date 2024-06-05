package dev.loudbook.pastebook.data

import org.springframework.data.annotation.Id

@Suppress("unused")
data class PastePrivateDTO(@Id var id: String?, val title: String, val created: Long, var discordID: Long?, val reportBook: Boolean = false, val unlisted: Boolean = false, val wrap: Boolean = false, val creatorIP: String, val expires: Long){
    fun toPublicDTO(user: UserDTO) = PasteDTO(id, user, title, created, reportBook, unlisted, wrap, expires)
}