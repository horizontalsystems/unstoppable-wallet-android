package io.horizontalsystems.bankwallet.entities

data class FaqMap(
    val section: HashMap<String, String>,
    val items: List<HashMap<String, Faq>>
)

data class Faq(
    val title: String,
    val markdown: String
)
