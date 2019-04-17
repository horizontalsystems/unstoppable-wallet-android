package io.horizontalsystems.bankwallet.entities

data class FullTransactionItem(
        val titleResId: Int? = null,
        val title: String? = null,
        val value: String? = null,
        val clickable: Boolean = false,
        val url: String? = null,
        val icon: FullTransactionIcon? = null,
        val dimmed: Boolean = false
)

data class FullTransactionSection(val items: List<FullTransactionItem>)
data class FullTransactionRecord(
        val providerName: String,
        val sections: List<FullTransactionSection>
)

enum class FullTransactionIcon { TIME, PERSON, BLOCK, CHECK, TOKEN }
