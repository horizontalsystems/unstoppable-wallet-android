package io.horizontalsystems.bankwallet.modules.addtoken.bep20

import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.modules.addtoken.IAddEvmTokenResolver

class AddBep20TokenResolver(
        testMode: Boolean,
        bscscanApiKey: String
) : IAddEvmTokenResolver {

    override val apiUrl = if (testMode) "https://api-testnet.bscscan.com/" else "https://api.bscscan.com/"

    override val explorerKey = bscscanApiKey

    override fun doesCoinMatchReference(coin: Coin, reference: String): Boolean {
        return coin.type is CoinType.Bep20 && coin.type.address.equals(reference, ignoreCase = true)
    }

    override fun coinType(address: String): CoinType {
        return CoinType.Bep20(address)
    }

}
