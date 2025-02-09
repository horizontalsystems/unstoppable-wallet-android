package cash.p.terminal.modules.transactions

import cash.p.terminal.network.changenow.domain.entity.TransactionStatusEnum


fun TransactionStatusEnum?.toUniversalStatus() = when (this) {
    null,
    TransactionStatusEnum.NEW,
    TransactionStatusEnum.WAITING -> TransactionStatus.Pending

    TransactionStatusEnum.CONFIRMING,
    TransactionStatusEnum.EXCHANGING,
    TransactionStatusEnum.SENDING,
    TransactionStatusEnum.VERIFYING -> TransactionStatus.Processing(
        (ordinal + 1) * (1f / (TransactionStatusEnum.FINISHED.ordinal + 1))
    )

    TransactionStatusEnum.FINISHED -> TransactionStatus.Completed

    TransactionStatusEnum.UNKNOWN,
    TransactionStatusEnum.FAILED,
    TransactionStatusEnum.REFUNDED -> TransactionStatus.Failed
}