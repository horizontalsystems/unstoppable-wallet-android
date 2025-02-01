package cash.p.terminal.core.storage

import cash.p.terminal.entities.ChangeNowTransaction
import cash.p.terminal.wallet.Token

class ChangeNowTransactionsStorage(appDatabase: AppDatabase) {

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
        statuses: List<String>,
        limit: Int
    ) = dao.getAll(
        coinUid = token.coin.uid,
        blockchainType = token.blockchainType.uid,
        address = address,
        statuses = statuses,
        limit = limit
    )

    fun getByTokenIn(
        token: Token,
        timestamp: Long
    ) = dao.getByTokenIn(
        coinUid = token.coin.uid,
        blockchainType = token.blockchainType.uid,
        dateFrom = timestamp - THRESHOLD_MSEC,
        dateTo = timestamp + THRESHOLD_MSEC
    )

    fun getByTokenOut(
        token: Token,
        timestamp: Long
    ) = dao.getByTokenIn(
        coinUid = token.coin.uid,
        blockchainType = token.blockchainType.uid,
        dateFrom = timestamp - THRESHOLD_MSEC,
        dateTo = timestamp + THRESHOLD_MSEC
    )
}
