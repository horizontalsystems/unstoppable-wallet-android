package io.horizontalsystems.bankwallet.modules.walletconnect.stellar

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SectionViewItem
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.ValueType
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.ViewItem
import org.stellar.sdk.Transaction
import org.stellar.sdk.operations.Operation

object WCStellarHelper {

    fun getTransactionViewItems(transaction: Transaction, xdr: String, peerName: String): List<SectionViewItem> {
        val operationItems = transaction.operations.map { operation: Operation ->
            ViewItem.Value(
                "Operation",
                operation.javaClass.simpleName,
                ValueType.Regular
            )
        }

        return listOf(
            SectionViewItem(
                operationItems + listOf(
                    ViewItem.Input("Transaction XDR", xdr)
                )
            ),
            SectionViewItem(
                listOf(
                    ViewItem.Value(
                        Translator.getString(R.string.WalletConnect_SignMessageRequest_dApp),
                        peerName,
                        ValueType.Regular
                    ),
                    ViewItem.Value(
                        "Stellar",
                        "",
                        ValueType.Regular
                    )
                )
            )
        )
    }
}
