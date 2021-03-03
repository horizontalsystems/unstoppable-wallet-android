package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.core.factories.TransactionViewItemFactory
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsModule.FetchData
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.core.entities.Currency
import java.math.BigDecimal

class TransactionRecordDataSource(
        private val poolRepo: PoolRepo,
        private val itemsDataSource: TransactionItemDataSource,
        private val limit: Int = 10,
        private val viewItemFactory: TransactionViewItemFactory,
        private val metadataDataSource: TransactionMetadataDataSource) {

    val itemsCopy
        get() = itemsDataSource.items.map { it.copy() }

    val allShown
        get() = poolRepo.activePools.all { it.allShown }

    fun getFetchDataList(): List<FetchData> = poolRepo.activePools.mapNotNull {
        it.getFetchData(limit)
    }

    fun handleNextRecords(records: Map<Wallet, List<TransactionRecord>>) {
        records.forEach { (wallet, transactionRecords) ->
            poolRepo.getPool(wallet)?.add(transactionRecords)
        }
    }

    fun handleUpdatedRecords(records: List<TransactionRecord>, wallet: Wallet): Boolean {
        val pool = poolRepo.getPool(wallet) ?: return false

        val updatedRecords = mutableListOf<TransactionRecord>()
        val insertedRecords = mutableListOf<TransactionRecord>()

        records.sorted().forEach {
            when (pool.handleUpdatedRecord(it)) {
                Pool.HandleResult.UPDATED -> updatedRecords.add(it)
                Pool.HandleResult.INSERTED -> insertedRecords.add(it)
                Pool.HandleResult.NEW_DATA -> {
                    if (itemsDataSource.shouldInsertRecord(it)) {
                        insertedRecords.add(it)
                        pool.increaseFirstUnusedIndex()
                    }
                }
                Pool.HandleResult.IGNORED -> {
                }
            }
        }

        if (!poolRepo.isPoolActiveByWallet(wallet)) return false

        val updatedItems = updatedRecords.map { transactionViewItem(wallet, it) }
        val insertedItems = insertedRecords.map { transactionViewItem(wallet, it) }

        itemsDataSource.handleModifiedItems(updatedItems, insertedItems)

        return true
    }

    fun increasePage(): Boolean {
        val unusedItems = mutableListOf<TransactionViewItem>()

        poolRepo.activePools.forEach { pool ->
            unusedItems.addAll(pool.unusedRecords.map { record ->
                transactionViewItem(pool.wallet, record)
            })
        }

        if (unusedItems.isEmpty()) return false

        unusedItems.sortDescending()

        val usedItems = unusedItems.take(limit)

        itemsDataSource.add(usedItems)

        usedItems.forEach {
            poolRepo.getPool(it.wallet)?.increaseFirstUnusedIndex()
        }

        return true
    }

    private fun transactionViewItem(wallet: Wallet, record: TransactionRecord): TransactionViewItem {
        val lastBlockInfo = metadataDataSource.getLastBlockInfo(wallet)
        val rate = metadataDataSource.getRate(wallet.coin, record.timestamp)

        return viewItemFactory.item(wallet, record, lastBlockInfo, rate)
    }

    fun setWallets(wallets: List<Wallet>) {
        poolRepo.allPools.forEach {
            it.resetFirstUnusedIndex()
        }
        poolRepo.activatePools(wallets)
        itemsDataSource.clear()
    }

    private fun handleUpdatedWallets(wallets: List<Wallet>) {
        poolRepo.deactivateAllPools()

        setWallets(wallets)
    }

    fun setRate(rateValue: BigDecimal, coin: Coin, currency: Currency, timestamp: Long): Boolean {
        metadataDataSource.setRate(rateValue, coin, currency, timestamp)

        var hasUpdate = false
        itemsDataSource.items.forEachIndexed { index, item ->
            if (item.wallet.coin == coin && item.record.timestamp == timestamp) {
                itemsDataSource.items[index] = transactionViewItem(item.wallet, item.record)

                hasUpdate = true
            }
        }

        return hasUpdate
    }

    fun setLastBlock(wallet: Wallet, lastBlockInfo: LastBlockInfo): Boolean {
        val oldBlockInfo = metadataDataSource.getLastBlockInfo(wallet)
        metadataDataSource.setLastBlockInfo(lastBlockInfo, wallet)

        if (oldBlockInfo == null) {
            itemsDataSource.items.forEachIndexed { index, item ->
                if (wallet == item.wallet) {
                    itemsDataSource.items[index] = transactionViewItem(item.wallet, item.record)
                }
            }

            return true
        }

        var hasUpdate = false
        itemsDataSource.items.forEachIndexed { index, item ->
            if (item.wallet == wallet && (item.isPending || item.becomesUnlocked(oldBlockInfo.timestamp, lastBlockInfo.timestamp))) {
                itemsDataSource.items[index] = transactionViewItem(item.wallet, item.record)

                hasUpdate = true
            }
        }

        return hasUpdate
    }

    fun onUpdateWalletsData(allWalletsData: List<Pair<Wallet, LastBlockInfo?>>) {
        allWalletsData.forEach { (wallet, lastBlockHeight) ->
            lastBlockHeight?.let {
                metadataDataSource.setLastBlockInfo(it, wallet)
            }
        }

        val wallets = allWalletsData.map { it.first }

        handleUpdatedWallets(wallets)
    }

    fun clearRates() {
        metadataDataSource.clearRates()
        itemsDataSource.items.forEach { it.clearRates() }
    }
}
