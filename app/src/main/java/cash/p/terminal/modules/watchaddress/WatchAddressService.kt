package cash.p.terminal.modules.watchaddress

import cash.p.terminal.core.IAccountFactory
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.core.managers.WalletActivator
import cash.p.terminal.core.order
import cash.p.terminal.core.supports
import io.horizontalsystems.core.entities.BlockchainType
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.tokenTypeDerivation

class WatchAddressService(
    private val accountManager: cash.p.terminal.wallet.IAccountManager,
    private val walletActivator: WalletActivator,
    private val accountFactory: IAccountFactory,
    private val marketKit: MarketKitWrapper,
    private val evmBlockchainManager: EvmBlockchainManager,
) {

    fun nextWatchAccountName() = accountFactory.getNextWatchAccountName()

    fun tokens(accountType: cash.p.terminal.wallet.AccountType): List<cash.p.terminal.wallet.Token> {
        val tokenQueries = buildList {
            when (accountType) {
                is cash.p.terminal.wallet.AccountType.Cex,
                is cash.p.terminal.wallet.AccountType.Mnemonic,
                is cash.p.terminal.wallet.AccountType.EvmPrivateKey -> Unit // N/A
                is cash.p.terminal.wallet.AccountType.SolanaAddress -> {
                    if (BlockchainType.Solana.supports(accountType)) {
                        add(TokenQuery(BlockchainType.Solana, TokenType.Native))
                    }
                }

                is cash.p.terminal.wallet.AccountType.TronAddress -> {
                    if (BlockchainType.Tron.supports(accountType)) {
                        add(TokenQuery(BlockchainType.Tron, TokenType.Native))
                    }
                }

                is cash.p.terminal.wallet.AccountType.EvmAddress -> {
                    evmBlockchainManager.allMainNetBlockchains.forEach { blockchain ->
                        if (blockchain.type.supports(accountType)) {
                            add(TokenQuery(blockchain.type, TokenType.Native))
                        }
                    }
                }

                is cash.p.terminal.wallet.AccountType.BitcoinAddress -> {
                    add(TokenQuery(accountType.blockchainType, accountType.tokenType))
                }

                is cash.p.terminal.wallet.AccountType.TonAddress -> {
                    if (BlockchainType.Ton.supports(accountType)) {
                        add(TokenQuery(BlockchainType.Ton, TokenType.Native))
                    }
                }

                is cash.p.terminal.wallet.AccountType.HdExtendedKey -> {
                    if (BlockchainType.Bitcoin.supports(accountType)) {
                        accountType.hdExtendedKey.purposes.forEach { purpose ->
                            add(TokenQuery(BlockchainType.Bitcoin, TokenType.Derived(purpose.tokenTypeDerivation)))
                        }
                    }

                    if (BlockchainType.Dash.supports(accountType)) {
                        add(TokenQuery(BlockchainType.Dash, TokenType.Native))
                    }

                    if (BlockchainType.BitcoinCash.supports(accountType)) {
                        TokenType.AddressType.values().map {
                            add(TokenQuery(BlockchainType.BitcoinCash, TokenType.AddressTyped(it)))
                        }
                    }

                    if (BlockchainType.Litecoin.supports(accountType)) {
                        accountType.hdExtendedKey.purposes.map { purpose ->
                            add(TokenQuery(BlockchainType.Litecoin, TokenType.Derived(purpose.tokenTypeDerivation)))
                        }
                    }

                    if (BlockchainType.ECash.supports(accountType)) {
                        add(TokenQuery(BlockchainType.ECash, TokenType.Native))
                    }
                }
            }
        }

        return marketKit.tokens(tokenQueries)
            .sortedBy { it.blockchainType.order }
    }

    fun watchAll(accountType: cash.p.terminal.wallet.AccountType, name: String?) {
        watchTokens(accountType, tokens(accountType), name)
    }

    fun watchTokens(accountType: cash.p.terminal.wallet.AccountType, tokens: List<cash.p.terminal.wallet.Token>, name: String? = null) {
        val accountName = name ?: accountFactory.getNextWatchAccountName()
        val account = accountFactory.watchAccount(accountName, accountType)

        accountManager.save(account)

        try {
            walletActivator.activateTokens(account, tokens)
        } catch (e: Exception) {
        }
    }
}
