package com.quantum.wallet.bankwallet.modules.walletconnect.stellar

import com.quantum.wallet.bankwallet.modules.sendevmtransaction.SectionViewItem
import com.quantum.wallet.bankwallet.modules.sendevmtransaction.ValueType
import com.quantum.wallet.bankwallet.modules.sendevmtransaction.ViewItem
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
