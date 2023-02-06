package io.horizontalsystems.marketkit.models

enum class LinkType(val v: String) {
    Guide("guide"),
    Website("website"),
    Whitepaper("whitepaper"),
    Twitter("twitter"),
    Telegram("telegram"),
    Reddit("reddit"),
    Github("github");

    companion object {
        private val map = values().associateBy(LinkType::v)

        fun fromString(v: String) = map[v]
    }
}
