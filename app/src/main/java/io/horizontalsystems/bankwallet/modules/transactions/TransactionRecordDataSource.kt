package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsModule.FetchData
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.core.entities.Currency
import java.math.BigDecimal

class TransactionRecordDataSource(
        private val poolRepo: PoolRepo,
        private val itemsDataSource: TransactionItemDataSource,
        private val limit: Int,
        private val viewItemFactory: TransactionViewItemFactory,
        private val metadataDataSource: TransactionMetadataDataSource) {

    val itemsCopy
        get() = itemsDataSource.items.map { it.copy() }

    val allShown
        get() = poolRepo.activePools.all { it.allShown }

    fun getFetchDataList(): List<FetchData> = poolRepo.activePools.mapNotNull {
        it.getFetchData(limit)
    }

    fun handleNextRecords(records: Map<TransactionWallet, List<TransactionRecord>>) {
        records.forEach { (wallet, transactionRecords) ->
            poolRepo.getPool(wallet)?.add(transactionRecords)
        }
    }

    fun handleUpdatedRecords(records: List<TransactionRecord>, wallet: TransactionWallet): Boolean {
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

    private fun transactionViewItem(
        wallet: TransactionWallet,
        record: TransactionRecord
    ): TransactionViewItem {
        val lastBlockInfo = metadataDataSource.getLastBlockInfo(wallet.source)
        val mainAmountCurrencyValue = record.mainValue?.let { mainValue ->
            metadataDataSource.getRate(mainValue.coin, record.timestamp)?.let {
                CurrencyValue(it.currency, mainValue.value)
            }
        }
        return viewItemFactory.item(wallet, record, lastBlockInfo, mainAmountCurrencyValue)
    }

    fun setWallets(wallets: List<TransactionWallet>) {
        poolRepo.allPools.forEach {
            it.resetFirstUnusedIndex()
        }
        poolRepo.activatePools(wallets)
        itemsDataSource.clear()
    }

    fun handleUpdatedWallets(wallets: List<TransactionWallet>) {
        poolRepo.deactivateAllPools()

        setWallets(wallets)
    }

    fun setRate(rateValue: BigDecimal, coin: Coin, currency: Currency, timestamp: Long): Boolean {
        metadataDataSource.setRate(rateValue, coin, currency, timestamp)

        var hasUpdate = false
        itemsDataSource.items.forEachIndexed { index, item ->
            if (item.record.mainValue?.coin == coin && item.record.timestamp == timestamp) {
                itemsDataSource.items[index] = transactionViewItem(item.wallet, item.record)

                hasUpdate = true
            }
        }

        return hasUpdate
    }

    fun setLastBlock(wallet: TransactionWallet, lastBlockInfo: LastBlockInfo?): Boolean {
        lastBlockInfo ?: return false

        val oldBlockInfo = metadataDataSource.getLastBlockInfo(wallet.source)
        metadataDataSource.setLastBlockInfo(lastBlockInfo, wallet.source)

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
            if (item.wallet == wallet && (item.record.changedBy(oldBlockInfo, lastBlockInfo))) {
                itemsDataSource.items[index] = transactionViewItem(item.wallet, item.record)

                hasUpdate = true
            }
        }

        return hasUpdate
    }

    fun clearRates() {
        metadataDataSource.clearRates()
        itemsDataSource.items.forEach { it.clearRates() }
    }

    fun handleUpdatedLastBlockInfos(lastBlockInfos: MutableList<Pair<TransactionWallet, LastBlockInfo?>>) {
        lastBlockInfos.forEach { (wallet, lastBlockInfo) ->
            setLastBlock(wallet, lastBlockInfo)
        }
    }
}
