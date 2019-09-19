package org.mozilla.rocket.content.games.vo

import org.mozilla.rocket.adapter.DelegateAdapter

data class Game(
    val id: Long,
    val name: String,
    val imageUrl: String,
    val linkUrl: String,
    val packageName: String = "",
    val type: String = "",
    val recentplay: Boolean = false,
    val playtime: Int = 0
) : DelegateAdapter.UiModel() {

    override fun toString(): String {
        val game2Str = "{\"id\" : \"$id\", \"name\" : \"$name\", \"imageUrl\" : \"$imageUrl\", \"linkUrl\":\"$linkUrl\", \"packageName\":\"$packageName\",\"type\":\"$type\", \"recentplay\":$recentplay}"
        return game2Str
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Game

        if (this.id == other.id) return true

        return false
    }
}