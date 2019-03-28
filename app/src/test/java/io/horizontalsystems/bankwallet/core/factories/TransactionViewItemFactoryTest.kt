package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import org.junit.Assert
import org.junit.Test
import java.util.*

class TransactionViewItemFactoryTest {

    private val txViewItemFactory = TransactionViewItemFactory()
    private val fromTxAddress = TransactionAddress("fromAddress", false)
    private val myTxAddress = TransactionAddress("myAddress", true)
    private val toTxAddress = TransactionAddress("toAddress", false)
    private val hash = "efwewegweg32rf234"
    private val bitCoin = Coin("Bitcoin", "BTC", CoinType.Bitcoin)
    private val rate = CurrencyValue(Currency(code = "USD", symbol = "$"), 3900.toBigDecimal())
    private val lastBlockHeight = 1000

    private val txRecordOutgoing = TransactionRecord(hash).apply {
        blockHeight = 900
        amount = 123.toBigDecimal()
        timestamp = 1553769996L
        from = listOf(myTxAddress)
        to = listOf(toTxAddress)
    }

    private val txRecordIncoming = TransactionRecord(hash).apply {
        blockHeight = 900
        amount = 123.toBigDecimal()
        timestamp = 1553769996L
        from = listOf(fromTxAddress)
        to = listOf(myTxAddress)
    }

    @Test
    fun getItem_incoming() {
        val txItem = TransactionItem(bitCoin, txRecordIncoming)
        val currencyValue = CurrencyValue(rate.currency, txRecordIncoming.amount * rate.value)
        val incoming = true

        val expectedItem = TransactionViewItem(
                txRecordIncoming.transactionHash,
                txItem.coin,
                CoinValue(txItem.coin.code, txRecordIncoming.amount),
                currencyValue,
                txRecordIncoming.from.firstOrNull { it.mine != incoming }?.address,
                null,
                incoming,
                Date(txRecordIncoming.timestamp * 1000),
                TransactionStatus.Completed
        )

        val item = txViewItemFactory.item(txItem, lastBlockHeight, 6, rate)

        Assert.assertEquals(item, expectedItem)
    }

    @Test
    fun getItem_outgoing() {
        val txItem = TransactionItem(bitCoin, txRecordOutgoing)
        val currencyValue = CurrencyValue(rate.currency, txRecordOutgoing.amount * rate.value)
        val incoming = true

        val expectedItem = TransactionViewItem(
                txRecordOutgoing.transactionHash,
                txItem.coin,
                CoinValue(txItem.coin.code, txRecordOutgoing.amount),
                currencyValue,
                null,
                txRecordOutgoing.from.firstOrNull { !it.mine }?.address,
                incoming,
                Date(txRecordOutgoing.timestamp * 1000),
                TransactionStatus.Completed
        )

        val item = txViewItemFactory.item(txItem, lastBlockHeight, 6, rate)

        Assert.assertEquals(item, expectedItem)
    }
}
