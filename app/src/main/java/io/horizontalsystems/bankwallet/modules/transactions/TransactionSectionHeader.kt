package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.ui.RecyclerSectionItemDecoration

class TransactionSectionHeader {
    private var headers = mapOf<Int, String>()

    val itemDecoration = RecyclerSectionItemDecoration(false, object : RecyclerSectionItemDecoration.SectionCallback {
        override fun isSection(position: Int) = headers.containsKey(position)
        override fun getSectionHeader(position: Int) = headers[position] ?: ""
    })

    fun setHeaders(headers: Map<Int, String>) {
        this.headers = headers
    }
}
