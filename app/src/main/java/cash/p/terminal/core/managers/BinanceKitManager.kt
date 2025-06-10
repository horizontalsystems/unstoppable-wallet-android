package cash.p.terminal.core.managers

import cash.p.terminal.core.App
import cash.p.terminal.core.IBinanceKitManager
import cash.p.terminal.core.UnsupportedAccountException
import cash.p.terminal.core.UnsupportedException
import cash.p.terminal.core.storage.HardwarePublicKeyStorage
import cash.p.terminal.tangem.signer.HardwareWalletBinanceChainSigner
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.binancechainkit.BinanceChainKit
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.runBlocking

class BinanceKitManager(
    private val hardwarePublicKeyStorage: HardwarePublicKeyStorage
) : IBinanceKitManager {
    private var kit: BinanceChainKit? = null
    private var useCount = 0
    private var currentAccount: Account? = null

    override val binanceKit: BinanceChainKit?
        get() = kit

    override val statusInfo: Map<String, Any>?
        get() = kit?.statusInfo()

    override fun binanceKit(wallet: Wallet): BinanceChainKit {
        val account = wallet.account
        val accountType = account.type

        if (kit != null && currentAccount != account) {
            kit?.stop()
            kit = null
            currentAccount = null
        }

        if (kit == null) {
            if (accountType !is AccountType.Mnemonic &&
                accountType !is AccountType.HardwareCard
            )
                throw UnsupportedAccountException()

            useCount = 0

            kit = createKitInstance(accountType, account)
            currentAccount = account
        }

        useCount++
        return kit!!
    }

    private fun createKitInstance(accountType: AccountType, account: Account): BinanceChainKit {
        return when (accountType) {
            is AccountType.Mnemonic -> {
                BinanceChainKit.instance(
                    App.instance,
                    accountType.words,
                    accountType.passphrase,
                    account.id,
                    BinanceChainKit.NetworkType.MainNet
                )
            }

            is AccountType.HardwareCard -> {
                val hardwarePublicKey = runBlocking {
                    hardwarePublicKeyStorage.getKey(
                        account.id,
                        BlockchainType.BinanceSmartChain,
                        TokenType.Native
                    )
                } ?: throw UnsupportedException("Hardware card does not have a public key for BinanceSmartChain")

                val networkType = BinanceChainKit.NetworkType.MainNet
                val walletBinanceChainSigner = HardwareWalletBinanceChainSigner(
                    hardwarePublicKey = hardwarePublicKey,
                    cardId = accountType.cardId,
                    networkType = networkType
                )
                BinanceChainKit.instance(
                    context = App.instance,
                    walletId = account.id,
                    wallet = walletBinanceChainSigner,
                    networkType = networkType
                )
            }

            else -> throw UnsupportedAccountException()
        }.apply {
            refresh()
        }
    }

    override fun unlink(account: Account) {
        if (currentAccount != account) return

        useCount -= 1

        if (useCount < 1) {
            kit?.stop()
            kit = null
            currentAccount = null
        }
    }

}
