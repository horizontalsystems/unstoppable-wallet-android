package io.horizontalsystems.walletkit.modules.transactions

import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.modules.transactionInfo.ColorName
import io.horizontalsystems.walletkit.modules.transactionInfo.ColoredValue
import io.horizontalsystems.walletkit.modules.transactions.TransactionViewItem.Icon
import io.horizontalsystems.walletkit.modules.transactions.TransactionViewItem.Title
import io.horizontalsystems.walletkit.modules.transactions.TransactionViewItem.Type
import io.horizontalsystems.walletkit.uiv3.components.TransactionIconState
import io.horizontalsystems.marketkit.models.BlockchainType
import java.util.Date

// Temporary: every transaction type in every state, shown on top of the list in debug builds
// while the transaction cell is redesigned. Remove together with its call in TransactionsViewModel.
object DummyTransactions {

    const val HEADER = "DESIGN PREVIEW"

    fun viewItems(): List<TransactionViewItem> {
        val xmr = coin("monero", "XMR")
        val eth = coin("ethereum", "ETH")
        val oneInch = coin("1inch", "1INCH")
        val usdc = coin("usd-coin", "USDC")
        val usdt = coin("tether", "USDT")
        val zec = coin("zcash", "ZEC")

        val rows = listOf(
            Row(Type.Receive, xmr, "from unknown", value("+0.00345", ColorName.Remus), value("$10,536.84")),
            Row(Type.Send, eth, "to b1121...12345", value("-0.00345", ColorName.Leah), value("$536.84")),
            Row(Type.SendToSelf, oneInch, "To yourself", value("0.00345", ColorName.Leah), value("$10,536.84")),
            Row(Type.Approve, usdc, "Approved", value("∞", ColorName.Leah), value("Unlimited")),
            Row(Type.Swap, Title.Pair(xmr, usdt), "Swapped", value("+8,345", ColorName.Remus), value("-6.84", ColorName.Leah)),
            Row(Type.Shield, zec, "Shielded", value("0.00345", ColorName.Leah), value("$536.84")),
            Row(Type.Unshield, zec, "Unshielded", value("0.00345", ColorName.Leah), value("$536.84")),
            Row(Type.Migrate, zec, "to Ironwood", value("0.00345", ColorName.Leah), value("$536.84")),
            Row(Type.Contract, usdt, "Contract Call", value("-50", ColorName.Leah), value("$48.84")),
            Row(Type.Contract, named("Contract Call", BlockchainType.BinanceSmartChain), "0x1111...2222", null, null),
            Row(Type.Unknown, named("Unknown Transaction", BlockchainType.Ethereum), "Transaction can not be parsed", null, null),
            Row(Type.Unknown, named("Transfer TRC10 Asset", BlockchainType.Tron), "Transaction can not be parsed", null, null),
        )

        val states = listOf(TransactionIconState.Pending, TransactionIconState.Completed, TransactionIconState.Failed)

        return rows.flatMapIndexed { rowIndex, row ->
            states.map { state ->
                TransactionViewItem(
                    uid = "dummy_${rowIndex}_${row.type.name}_${state.name}",
                    type = row.type,
                    state = state,
                    title = row.title,
                    subtitle = row.type.stateText(state)?.let { Translator.getString(it) } ?: row.completedSubtitle,
                    primaryValue = row.primaryValue,
                    secondaryValue = row.secondaryValue,
                    date = Date(),
                )
            }
        }
    }

    private class Row(
        val type: Type,
        val title: Title,
        val completedSubtitle: String,
        val primaryValue: ColoredValue?,
        val secondaryValue: ColoredValue?,
    )

    private fun coin(uid: String, code: String) = Title.Single(
        code,
        Icon.Regular("https://cdn.blocksdecoded.com/coin-icons/32px/$uid@3x.png", null, R.drawable.coin_placeholder)
    )

    private fun named(text: String, blockchainType: BlockchainType) = Title.Single(text, Icon.Platform(blockchainType))

    private fun value(text: String, color: ColorName = ColorName.Grey) = ColoredValue(text, color)
}
