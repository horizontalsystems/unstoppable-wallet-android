package cash.p.terminal.network.changenow.domain.entity

class TransactionStatus(
    val status: TransactionStatusEnum,
    val payinAddress: String,
    val payoutAddress: String,
    val fromCurrency: String,
    val toCurrency: String,
    val id: String,
    val updatedAt: String
)

enum class TransactionStatusEnum {
    NEW,
    WAITING,
    CONFIRMING,
    EXCHANGING,
    SENDING,
    FINISHED,
    FAILED,
    REFUNDED,
    VERIFYING,
    UNKNOWN
}

fun String.toStatus() = try {
    TransactionStatusEnum.valueOf(this.uppercase())
} catch (e: IllegalArgumentException) {
    TransactionStatusEnum.UNKNOWN
}
