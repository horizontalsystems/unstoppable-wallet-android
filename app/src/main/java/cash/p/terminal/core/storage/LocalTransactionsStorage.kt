package cash.p.terminal.core.storage

import cash.p.terminal.entities.ChangeNowTransaction
import cash.p.terminal.wallet.Token
import java.math.BigDecimal

class LocalTransactionsStorage(appDatabase: AppDatabase) {

    private val dao by lazy { appDatabase.changeNowTransactionsDao() }

    private companion object {
        const val THRESHOLD_MSEC = 30000
    }

    fun save(
        changeNowTransaction: ChangeNowTransaction
    ) = dao.insert(changeNowTransaction)

    fun getAll(
        token: Token,
        address: String,
        statusesExcluded: List<String>,
        limit: Int
    ) = dao.getAll(
        coinUid = token.coin.uid,
        blockchainType = token.blockchainType.uid,
        address = address,
        statusesExcluded = statusesExcluded,
        limit = limit
    )

    fun getTransaction(transactionId: String) = dao.getTransaction(transactionId)

    fun getByTokenIn(
        token: Token,
        amountIn: BigDecimal?,
        timestamp: Long
    ) = dao.getByTokenIn(
        coinUid = token.coin.uid,
        amountIn = amountIn,
        blockchainType = token.blockchainType.uid,
        dateFrom = timestamp - THRESHOLD_MSEC,
        dateTo = timestamp + THRESHOLD_MSEC
    )

    fun getByOutgoingRecordUid(
        outgoingRecordUid: String
    ) = dao.getByOutgoingRecordUid(
        outgoingRecordUid = outgoingRecordUid
    )

    fun getByTokenOut(
        token: Token,
        timestamp: Long
    ) = dao.getByTokenOut(
        coinUid = token.coin.uid,
        blockchainType = token.blockchainType.uid,
        dateFrom = timestamp - THRESHOLD_MSEC,
        dateTo = timestamp + THRESHOLD_MSEC
    )
}
