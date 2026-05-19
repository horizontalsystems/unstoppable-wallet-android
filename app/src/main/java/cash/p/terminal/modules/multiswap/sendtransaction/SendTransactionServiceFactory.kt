package cash.p.terminal.modules.multiswap.sendtransaction

import android.util.Log
import cash.p.terminal.core.UnsupportedException
import cash.p.terminal.modules.multiswap.sendtransaction.services.SendTransactionServiceBitcoin
import cash.p.terminal.modules.multiswap.sendtransaction.services.SendTransactionServiceEvm
import cash.p.terminal.modules.multiswap.sendtransaction.services.SendTransactionServiceStellar
import cash.p.terminal.modules.multiswap.sendtransaction.services.SendTransactionServiceSolana
import cash.p.terminal.modules.multiswap.sendtransaction.services.SendTransactionServiceTon
import cash.p.terminal.modules.multiswap.sendtransaction.services.SendTransactionServiceTron
import cash.p.terminal.modules.multiswap.sendtransaction.services.SendTransactionServiceMonero
import cash.p.terminal.modules.multiswap.sendtransaction.services.SendTransactionServiceZCash
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.entities.BlockchainType
import org.koin.java.KoinJavaComponent.inject
import kotlin.getValue

object SendTransactionServiceFactory {
    fun create(token: Token): ISendTransactionService<*> = try {
        Log.d("SendTransactionServiceFactory", "create: $token")
        when (val tokenType = token.type) {
            is TokenType.Derived -> {
                when (token.blockchainType) {
                    BlockchainType.Bitcoin -> {
                        SendTransactionServiceBitcoin(token)
                    }

                    BlockchainType.Litecoin -> {
                        SendTransactionServiceBitcoin(token)
                    }

                    else -> throw UnsupportedException("Unsupported token type: $tokenType")
                }
            }

            is TokenType.AddressTyped -> {
                if (token.blockchainType == BlockchainType.BitcoinCash) {
                    SendTransactionServiceBitcoin(token)
                } else throw UnsupportedException("Unsupported token type: $tokenType")
            }

            is TokenType.AddressSpecTyped -> {
                if (token.blockchainType == BlockchainType.Zcash) {
                    SendTransactionServiceZCash(token)
                } else throw UnsupportedException("Unsupported token type: $tokenType")
            }

            TokenType.Mweb -> {
                if (token.blockchainType == BlockchainType.Litecoin) {
                    SendTransactionServiceBitcoin(token)
                } else throw UnsupportedException("Unsupported token type: $tokenType")
            }

            TokenType.Native -> when (token.blockchainType) {
                BlockchainType.Dash,
                BlockchainType.Dogecoin,
                BlockchainType.ECash -> {
                    SendTransactionServiceBitcoin(token)
                }

                BlockchainType.Zcash -> {
                    SendTransactionServiceZCash(token)
                }

                BlockchainType.Ethereum,
                BlockchainType.BinanceSmartChain,
                BlockchainType.Polygon,
                BlockchainType.Avalanche,
                BlockchainType.Optimism,
                BlockchainType.Base,
                BlockchainType.ZkSync,
                BlockchainType.Gnosis,
                BlockchainType.Fantom,
                BlockchainType.ArbitrumOne -> {
                    SendTransactionServiceEvm(token)
                }

                BlockchainType.Solana -> {
                    SendTransactionServiceSolana(token)
                }

                BlockchainType.Tron -> {
                    SendTransactionServiceTron(token)
                }

                BlockchainType.Stellar -> {
                    val accountManager: IAccountManager by inject(IAccountManager::class.java)
                    val activeAccount = accountManager.activeAccount ?: throw IllegalStateException("No active account")
                    SendTransactionServiceStellar(account = activeAccount, token = token)
                }

                BlockchainType.Ton -> {
                    SendTransactionServiceTon(token)
                }

                BlockchainType.Monero -> {
                    SendTransactionServiceMonero(token)
                }

                else -> throw UnsupportedException("Unsupported token type: $tokenType")
            }

            is TokenType.Eip20 -> {
                if (token.blockchainType == BlockchainType.Tron) {
                    SendTransactionServiceTron(token)
                } else {
                    SendTransactionServiceEvm(token)
                }
            }

            is TokenType.Spl -> SendTransactionServiceSolana(token)
            is TokenType.Jetton -> SendTransactionServiceTon(token)

            is TokenType.Asset,
            is TokenType.Unsupported -> throw UnsupportedException("Unsupported token type: $tokenType")
        }
    } catch (e: Exception) {
        e.printStackTrace()
        throw UnsupportedException(e.message ?: "")
    }
}
