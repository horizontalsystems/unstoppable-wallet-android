package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.core.helpers.DateHelper
import java.util.*

class TransactionSectionHeader {
    private val headers = mutableMapOf<Int, String>()

    val itemDecoration = RecyclerSectionItemDecoration(false, object : RecyclerSectionItemDecoration.SectionCallback {
        override fun isSection(position: Int) = headers.containsKey(position)
        override fun getSectionHeader(position: Int) = headers[position] ?: ""
    })

    fun updateList(items: List<TransactionViewItem>) {
        headers.clear()

        var prevSectionHeader: String? = null
        items.forEachIndexed { index, transactionViewItem ->
            transactionViewItem.date?.let { date ->
                val sectionHeader = formatDate(date)?.uppercase()

                if (sectionHeader != null && sectionHeader != prevSectionHeader) {
                    headers[index] = sectionHeader
                    prevSectionHeader = sectionHeader
                }
            }
        }
    }

    private fun formatDate(date: Date): String? {
        val calendar = Calendar.getInstance()
        calendar.time = date

        val today = Calendar.getInstance()
        if (calendar[Calendar.YEAR] == today[Calendar.YEAR] && calendar[Calendar.DAY_OF_YEAR] == today[Calendar.DAY_OF_YEAR]) {
            return null
        }

        val yesterday = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_MONTH, -1)
        if (calendar[Calendar.YEAR] == yesterday[Calendar.YEAR] && calendar[Calendar.DAY_OF_YEAR] == yesterday[Calendar.DAY_OF_YEAR]) {
            return Translator.getString(R.string.Timestamp_Yesterday)
        }

        return DateHelper.shortDate(date, "MMMM d", "MMMM d, yyyy")
    }
}
