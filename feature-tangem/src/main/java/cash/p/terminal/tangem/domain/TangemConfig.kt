package cash.p.terminal.tangem.domain

import cash.p.terminal.wallet.BuildConfig
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.entities.BlockchainType

object TangemConfig {

    // Delay to prevent scanning too fast to avoid TangemSdkError
    const val SCAN_DELAY = 1000L

    val getDefaultTokens by lazy {
        listOfNotNull(
            TokenQuery(BlockchainType.Bitcoin, TokenType.Derived(TokenType.Derivation.Bip84)),
            TokenQuery(BlockchainType.Ethereum, TokenType.Native),
            TokenQuery(BlockchainType.BinanceSmartChain, TokenType.Native),
            TokenQuery(
                BlockchainType.BinanceSmartChain,
                TokenType.Eip20(BuildConfig.PIRATE_CONTRACT)
            ),
            TokenQuery(
                BlockchainType.BinanceSmartChain,
                TokenType.Eip20(BuildConfig.COSANTA_CONTRACT)
            ),
        )
    }
    /**
     * List of blockchain types that are excluded from hardware wallet support.
     * This is used to filter out tokens that cannot be enabled on hardware wallets.
     */
    private val excludedBlockChainTypeForHardwareWallet by lazy {
        setOf(
            BlockchainType.Zcash,
            BlockchainType.ECash
        )
    }

    private val excludedTokenTypesForHardwareWallet by lazy {
        setOf(
            TokenType.Derived(TokenType.Derivation.Bip86) // Taproot derivation is not supported on hardware wallets
        )
    }

    fun isExcludedForHardwareCard(token: Token): Boolean {
        return isExcludedForHardwareCard(token.blockchainType, token.type)
    }

    fun isExcludedForHardwareCard(token: TokenQuery): Boolean {
        return isExcludedForHardwareCard(token.blockchainType, token.tokenType)
    }

    fun isExcludedForHardwareCard(blockchainType: BlockchainType, tokenType: TokenType): Boolean {
        return blockchainType in excludedBlockChainTypeForHardwareWallet ||
                tokenType in excludedTokenTypesForHardwareWallet
    }
}