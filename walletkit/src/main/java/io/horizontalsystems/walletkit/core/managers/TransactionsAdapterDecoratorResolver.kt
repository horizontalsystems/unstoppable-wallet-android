package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.core.ITransactionsAdapter
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource

/**
 * Registration seam for per-source transactions-adapter decoration (iOS analog:
 * TransactionsAdapterDecoratorFactory). An app can wrap the built adapter to
 * reshape or overlay records — e.g. the GasFree pipeline reshapes Tron records
 * and overlays its own pending rows. Returning null keeps the stock adapter.
 */
object TransactionsAdapterDecoratorResolver {

    interface Provider {
        fun decorate(adapter: ITransactionsAdapter, source: TransactionSource): ITransactionsAdapter?
    }

    @Volatile
    var provider: Provider? = null
}
