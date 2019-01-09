package io.horizontalsystems.bankwallet.entities

data class FullTransactionItem(
        val title: String,
        val value: String?,
        val clickable: Boolean = false,
        val url: String? = null,
        val icon: FullTransactionIcon = FullTransactionIcon.NONE
)

data class FullTransactionSection(
        val title: String? = null,
        val items: List<FullTransactionItem>
)

data class FullTransactionRecord(
        val resource: String,
        val sections: List<FullTransactionSection>
)

enum class FullTransactionIcon {
    NONE,
    HASH,
    PERSON
}
