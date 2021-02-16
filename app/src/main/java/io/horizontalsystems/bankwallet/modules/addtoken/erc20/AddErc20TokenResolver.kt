package io.horizontalsystems.bankwallet.modules.addtoken.erc20

import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.modules.addtoken.IAddEvmTokenResolver

class AddErc20TokenResolver(
        testMode: Boolean,
        etherscanApiKey: String
) : IAddEvmTokenResolver {

    override val apiUrl = if (testMode) "https://api-ropsten.bscscan.io/" else "https://api.etherscan.io/"

    override val explorerKey = etherscanApiKey

    override fun doesCoinMatchReference(coin: Coin, reference: String): Boolean {
        return coin.type is CoinType.Erc20 && coin.type.address.equals(reference, ignoreCase = true)
    }

    override fun coinType(address: String): CoinType {
        return CoinType.Erc20(address)
    }

}
