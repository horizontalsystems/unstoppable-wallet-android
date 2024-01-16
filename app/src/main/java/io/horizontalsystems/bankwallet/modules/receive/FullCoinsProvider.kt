package cash.p.terminal.modules.receive
>>>>>>>> 11b2c0855 (Refactor Receive Address module navigation):app/src/main/java/cash.p.terminal/modules/receive/FullCoinsProvider.kt

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
import io.horizontalsystems.marketkit.models.TokenType

class FullCoinsProvider(
    private val marketKit: MarketKitWrapper,
    val activeAccount: Account
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
