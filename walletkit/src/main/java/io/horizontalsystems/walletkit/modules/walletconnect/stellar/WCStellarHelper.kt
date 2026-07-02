package io.horizontalsystems.walletkit.modules.walletconnect.stellar

import io.horizontalsystems.walletkit.modules.sendevmtransaction.SectionViewItem
import io.horizontalsystems.walletkit.modules.sendevmtransaction.ValueType
import io.horizontalsystems.walletkit.modules.sendevmtransaction.ViewItem
import org.stellar.sdk.Transaction
import org.stellar.sdk.operations.Operation

object WCStellarHelper {

    fun getTransactionViewItems(transaction: Transaction, xdr: String): List<SectionViewItem> {
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
        )
    }
}
