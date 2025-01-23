package cash.p.terminal.network.changenow.api

object ChangeNowHelper {
    const val CHANGE_NOW_URL = "changenow.io"
    fun getViewTransactionUrl(transactionId: String): String {
        return "https://changenow.io/exchange/txs/$transactionId"
    }
}