package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.core.helpers.DateHelper

class TransactionSectionHeader {
    private val headers = mutableMapOf<Int, String>()

    val itemDecoration = RecyclerSectionItemDecoration(false, object : RecyclerSectionItemDecoration.SectionCallback {
        override fun isSection(position: Int) = headers.containsKey(position)
        override fun getSectionHeader(position: Int) = headers[position] ?: ""
    })

    fun updateList(items: List<TransactionViewItem>) {
        headers.clear()

        var prevShortDate: String? = null
        items.forEachIndexed { index, transactionViewItem ->
            transactionViewItem.date?.let { date ->
                val shortDate = DateHelper.shortDate(date, "MMMM d", "MMMM d, yyyy").uppercase()
                if (shortDate != prevShortDate) {
                    headers[index] = shortDate
                    prevShortDate = shortDate
                }
            }
        }
    }
}
