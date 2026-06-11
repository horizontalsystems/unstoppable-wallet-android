package cash.p.terminal.network.exolix.api

object ExolixHelper {
    const val EXOLIX_URL = "exolix.com"

    fun getViewTransactionUrl(transactionId: String): String {
        return "https://exolix.com/transaction/$transactionId"
    }
}
