package io.horizontalsystems.bankwallet.modules.blockchainsettings

import io.horizontalsystems.bankwallet.entities.AccountType.Derivation
import io.horizontalsystems.bankwallet.entities.BitcoinCashCoinType
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorViewItem
import io.horizontalsystems.coinkit.models.Coin

object BlockchainSettingsModule {

    data class Request(val coin: Coin, val type: RequestType)

    sealed class RequestType {
        class DerivationType(val derivations: List<Derivation>, val current: Derivation) : RequestType()
        class BitcoinCashType(val types: List<BitcoinCashCoinType>, val current: BitcoinCashCoinType) : RequestType()
    }

    data class Config(
            val coin: Coin,
            val title: String,
            val subtitle: String,
            val selectedIndex: Int,
            val viewItems: List<BottomSheetSelectorViewItem>
    )

}
