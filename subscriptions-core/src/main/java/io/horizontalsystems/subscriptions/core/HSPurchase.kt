package io.horizontalsystems.subscriptions.core

data class HSPurchase(
    val status: Status
) {
    enum class Status {
        Pending, Purchased
    }
}

data class HSPurchaseFailure(
    val code: String,
    override val message: String,
) : Exception()
