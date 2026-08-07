package io.horizontalsystems.walletkit.modules.send.bitcoin.utxoexpert

import io.horizontalsystems.marketkit.models.Token

object UtxoExpertModeModule {

    data class UiState(
        val availableBalanceInfo: InfoItem,
        val utxoItems: List<UnspentOutputViewItem>,
        val unselectAllIsEnabled: Boolean,
    )

    data class InfoItem(
        val value: String?,
        val subValue: String?,
    )

    data class UnspentOutputViewItem(
        val id: String,
        val outputIndex: Int,
        val date: String,
        val amountToken: String,
        val amountFiat: String,
        val address: String,
        val selected: Boolean,
    )
}
