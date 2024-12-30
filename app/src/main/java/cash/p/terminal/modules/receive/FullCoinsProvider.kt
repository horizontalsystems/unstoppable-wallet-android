package cash.p.terminal.modules.receive

import cash.p.terminal.core.isCustom
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.core.nativeTokenQueries
import cash.p.terminal.core.sortedByFilter
import cash.p.terminal.core.supported
import cash.p.terminal.core.supports
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.core.entities.BlockchainType
import cash.p.terminal.wallet.entities.FullCoin
import cash.p.terminal.wallet.entities.TokenType

class FullCoinsProvider(
    private val marketKit: MarketKitWrapper,
    val activeAccount: cash.p.terminal.wallet.Account
) {
    private var activeWallets = listOf<cash.p.terminal.wallet.Wallet>()
    private var predefinedTokens = listOf<cash.p.terminal.wallet.Token>()

    private var query: String? = null

    fun setActiveWallets(wallets: List<cash.p.terminal.wallet.Wallet>) {
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

        val (customTokens, regularTokens) = predefinedTokens.partition { it.isCustom }

        val fullCoins = if (tmpQuery.isNullOrBlank()) {
            val coinUids = regularTokens.map { it.coin.uid }
            customTokens.map { it.fullCoin } + marketKit.fullCoins(coinUids)
        } else if (isContractAddress(tmpQuery)) {
            val customFullCoins = customTokens
                .filter {
                    val type = it.type
                    type is TokenType.Eip20 && type.address.contains(tmpQuery, true)
                }
                .map { it.fullCoin }

            customFullCoins + marketKit.tokens(tmpQuery).map { it.fullCoin }
        } else {
            val customFullCoins = customTokens
                .filter {
                    val coin = it.coin
                    coin.name.contains(tmpQuery, true) || coin.code.contains(tmpQuery, true)
                }
                .map { it.fullCoin }

            customFullCoins + marketKit.fullCoins(tmpQuery)
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
