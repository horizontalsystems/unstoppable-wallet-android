package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.core.factories.TransactionViewItemFactory
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsModule.FetchData
import io.horizontalsystems.core.entities.Currency
import java.math.BigDecimal

class TransactionRecordDataSource(
        private val poolRepo: PoolRepo,
        private val itemsDataSource: TransactionItemDataSource,
        private val limit: Int = 10,
        private val viewItemFactory: TransactionViewItemFactory,
        private val metadataDataSource: TransactionMetadataDataSource) {

    val items
        get() = itemsDataSource.items

    val allShown
        get() = poolRepo.activePools.all { it.allShown }

    fun itemIndexesForTimestamp(coin: Coin, timestamp: Long): List<Int> =
            itemsDataSource.itemIndexesForTimestamp(coin, timestamp)

    fun itemIndexesForPending(wallet: Wallet, thresholdBlockHeight: Int): List<Int> =
            itemsDataSource.itemIndexesForPending(wallet, thresholdBlockHeight)

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

    fun increasePage(): List<TransactionViewItem> {
        val unusedItems = mutableListOf<TransactionViewItem>()

        poolRepo.activePools.forEach { pool ->
            unusedItems.addAll(pool.unusedRecords.map { record ->
                transactionViewItem(pool.wallet, record)
            })
        }

        if (unusedItems.isEmpty()) return listOf()

        unusedItems.sortDescending()

        val usedItems = unusedItems.take(limit)

        itemsDataSource.add(usedItems)

        usedItems.forEach {
            poolRepo.getPool(it.wallet)?.increaseFirstUnusedIndex()
        }

        return usedItems
    }

    private fun transactionViewItem(wallet: Wallet, record: TransactionRecord): TransactionViewItem {
        val lastBlockInfo = metadataDataSource.getLastBlockInfo(wallet)
        val threshold = metadataDataSource.getConfirmationThreshold(wallet)
        val rate = metadataDataSource.getRate(wallet.coin, record.timestamp)

        return viewItemFactory.item(wallet, record, lastBlockInfo, threshold, rate)
    }

    fun setWallets(wallets: List<Wallet>) {
        poolRepo.allPools.forEach {
            it.resetFirstUnusedIndex()
        }
        poolRepo.activatePools(wallets)
        itemsDataSource.clear()
    }

    private fun handleUpdatedWallets(wallets: List<Wallet>) {
        val unusedWallets = poolRepo.allPools.map { it.wallet }.filter { !wallets.contains(it) }

        poolRepo.deactivatePools(unusedWallets)

        setWallets(wallets)
    }

    fun itemIndexesForLocked(wallet: Wallet, unlockingBefore: Long, oldBlockTimestamp: Long?): List<Int> {
        return itemsDataSource.itemIndexesForLocked(wallet, unlockingBefore, oldBlockTimestamp)
    }

    fun setRate(rateValue: BigDecimal, coin: Coin, currency: Currency, timestamp: Long): Boolean {
        metadataDataSource.setRate(rateValue, coin, currency, timestamp)

        val itemIndexes = itemIndexesForTimestamp(coin, timestamp)

        itemIndexes.forEach {
            val transactionViewItem = itemsDataSource.items[it]
            itemsDataSource.items[it] = transactionViewItem(transactionViewItem.wallet, transactionViewItem.record)
        }

        return itemIndexes.isNotEmpty()
    }

    fun setLastBlock(wallet: Wallet, lastBlockInfo: LastBlockInfo): Boolean {
        val oldBlockInfo = metadataDataSource.getLastBlockInfo(wallet)
        val threshold = metadataDataSource.getConfirmationThreshold(wallet)

        metadataDataSource.setLastBlockInfo(lastBlockInfo, wallet)

        if (oldBlockInfo == null) {
            return true
        }

        val indexes = itemIndexesForPending(wallet, oldBlockInfo.height - threshold).toMutableList()
        lastBlockInfo.timestamp?.let { lastBlockTimestamp ->
            val lockedIndexes = itemIndexesForLocked(wallet, lastBlockTimestamp, oldBlockInfo.timestamp)
            indexes.addAll(lockedIndexes)
        }

        indexes.forEach {
            val transactionViewItem = itemsDataSource.items[it]
            itemsDataSource.items[it] = transactionViewItem(transactionViewItem.wallet, transactionViewItem.record)
        }

        return indexes.isNotEmpty()
    }

    fun onUpdateWalletsData(allWalletsData: List<Triple<Wallet, Int, LastBlockInfo?>>) {
        allWalletsData.forEach { (wallet, confirmationThreshold, lastBlockHeight) ->
            metadataDataSource.setConfirmationThreshold(confirmationThreshold, wallet)
            lastBlockHeight?.let {
                metadataDataSource.setLastBlockInfo(it, wallet)
            }
        }

        val wallets = allWalletsData.map { it.first }

        handleUpdatedWallets(wallets)
    }

}

