package cash.p.terminal.modules.receivemain

import cash.p.terminal.core.isCustom
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.core.nativeTokenQueries
import cash.p.terminal.core.sortedByFilter
import cash.p.terminal.core.supported
import cash.p.terminal.core.supports
import cash.p.terminal.entities.Account
import cash.p.terminal.entities.Wallet
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
            marketKit.tokens(tmpQuery).map { it.fullCoin }
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
