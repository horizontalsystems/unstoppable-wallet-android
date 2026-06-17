package cash.p.terminal.modules.transactions

import io.horizontalsystems.core.entities.Blockchain

internal fun List<TransactionWallet>.filterBySelection(
    selectedWallet: TransactionWallet?,
    selectedBlockchain: Blockchain?
): List<TransactionWallet> = when {
    selectedWallet != null -> listOf(selectedWallet)
    selectedBlockchain != null -> filter { it.source.blockchain == selectedBlockchain }
    else -> this
}
