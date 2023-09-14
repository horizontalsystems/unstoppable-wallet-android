package io.horizontalsystems.bankwallet.modules.receivemain

import io.horizontalsystems.bankwallet.core.isCustom
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.nativeTokenQueries
import io.horizontalsystems.bankwallet.core.sortedByFilter
import io.horizontalsystems.bankwallet.core.supported
import io.horizontalsystems.bankwallet.core.supports
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.Token

class FullCoinsProvider(
    private val marketKit: MarketKitWrapper,
    private val activeAccount: Account
) {
    private var activeWallets = listOf<Wallet>()
    private var predefinedTokens = listOf<Token>()

    private var query: String? = null

    fun setActiveWallets(wallets: List<Wallet>) {
        activeWallets = wallets

        updatePredefinedTokens()
    }

    private fun updatePredefinedTokens() {
        val allowedBlockchainTypes =
            BlockchainType.supported.filter { it.supports(activeAccount.type) }
        val tokenQueries = allowedBlockchainTypes
            .map { it.nativeTokenQueries }
            .flatten()
        val supportedNativeTokens = marketKit.tokens(tokenQueries)
        val activeTokens = activeWallets.map { it.token }
        predefinedTokens = activeTokens + supportedNativeTokens
    }

    fun setQuery(q: String) {
        query = q
    }

    fun getItems(): List<FullCoin> {
        val tmpQuery = query

        val fullCoins = if (tmpQuery.isNullOrBlank()) {
            val (custom, regular) = predefinedTokens.partition { it.isCustom }
            val coinUids = regular.map { it.coin.uid }
            custom.map { it.fullCoin } + marketKit.fullCoins(coinUids)
        } else if (isContractAddress(tmpQuery)) {
            val tokens = marketKit.tokens(tmpQuery)
            val coinUids = tokens.map { it.coin.uid }
            marketKit.fullCoins(coinUids)
        } else {
            marketKit.fullCoins(tmpQuery)
        }

        return fullCoins
            .sortedByFilter(tmpQuery ?: "")
            .sortedByDescending { fullCoin ->
                activeWallets.any { it.coin == fullCoin.coin }
            }
    }

    private fun isContractAddress(filter: String) = try {
        AddressValidator.validate(filter)
        true
    } catch (e: AddressValidator.AddressValidationException) {
        false
    }

}
