package io.horizontalsystems.bankwallet.entities

data class FullTransactionItem(
        val translationId: Int? = null,
        val title: String? = null,
        val value: String? = null,
        val valueUnit: Int? = null,
        val clickable: Boolean = false,
        val url: String? = null,
        val icon: FullTransactionIcon? = null
)

data class FullTransactionSection(
        val translationId: Int? = null,
        val items: List<FullTransactionItem>
)

data class FullTransactionRecord(
        val providerName: String,
        val sections: List<FullTransactionSection>
)

enum class FullTransactionIcon { HASH, PERSON }
