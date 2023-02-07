package cash.p.terminal.modules.watchaddress

import cash.p.terminal.core.IAccountFactory
import cash.p.terminal.core.IAccountManager
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.core.managers.WalletActivator
import cash.p.terminal.core.order
import cash.p.terminal.core.supports
import cash.p.terminal.entities.*
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType

class WatchAddressService(
    private val accountManager: IAccountManager,
    private val walletActivator: WalletActivator,
    private val accountFactory: IAccountFactory,
    private val marketKit: MarketKitWrapper,
    private val evmBlockchainManager: EvmBlockchainManager,
) {

    fun nextWatchAccountName() = accountFactory.getNextWatchAccountName()

    fun configuredTokens(accountType: AccountType) = buildList {
        when (accountType) {
            is AccountType.Mnemonic,
            is AccountType.EvmPrivateKey -> Unit // N/A
            is AccountType.SolanaAddress -> {
                token(BlockchainType.Solana, accountType)?.let {
                    add(ConfiguredToken(it))
                }
            }
            is AccountType.EvmAddress -> {
                evmBlockchainManager.allMainNetBlockchains.forEach { blockchain ->
                    token(blockchain.type, accountType)?.let { token ->
                        add(ConfiguredToken(token))
                    }
                }
            }
            is AccountType.HdExtendedKey -> {
                token(BlockchainType.Bitcoin, accountType)?.let { token ->
                    add(
                        ConfiguredToken(
                            token,
                            CoinSettings(mapOf(CoinSettingType.derivation to accountType.hdExtendedKey.info.purpose.derivation.value))
                        )
                    )
                }
                token(BlockchainType.Dash, accountType)?.let { token ->
                    add(ConfiguredToken(token))
                }
                token(BlockchainType.BitcoinCash, accountType)?.let { token ->
                    BitcoinCashCoinType.values().map { coinType ->
                        add(ConfiguredToken(token, CoinSettings(mapOf(CoinSettingType.bitcoinCashCoinType to coinType.value))))
                    }
                }
                token(BlockchainType.Litecoin, accountType)?.let { token ->
                    add(
                        ConfiguredToken(
                            token,
                            CoinSettings(mapOf(CoinSettingType.derivation to accountType.hdExtendedKey.info.purpose.derivation.value))
                        )
                    )
                }
            }
        }
    }.sortedBy { it.token.blockchainType.order }

    fun watchAll(accountType: AccountType, name: String?) {
        watchConfiguredTokens(accountType, configuredTokens(accountType), name)
    }

    fun watchConfiguredTokens(accountType: AccountType, configuredTokens: List<ConfiguredToken>, name: String? = null) {
        val accountName = name ?: accountFactory.getNextWatchAccountName()
        val account = accountFactory.watchAccount(accountName, accountType)

        accountManager.save(account)

        try {
            walletActivator.activateConfiguredTokens(account, configuredTokens)
        } catch (e: Exception) {
        }
    }

    private fun token(blockchainType: BlockchainType, accountType: AccountType): Token? =
        if (blockchainType.supports(accountType))
            marketKit.token(TokenQuery(blockchainType, TokenType.Native))
        else
            null
}
