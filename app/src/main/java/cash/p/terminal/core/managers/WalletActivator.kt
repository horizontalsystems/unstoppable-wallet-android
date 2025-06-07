package cash.p.terminal.core.managers

import cash.p.terminal.wallet.useCases.GetHardwarePublicKeyForWalletUseCase
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.IHardwarePublicKeyStorage
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.TokenQuery
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent.inject

class WalletActivator(
    private val walletManager: IWalletManager,
    private val marketKit: MarketKitWrapper,
) {

    private val getHardwarePublicKeyForWalletUseCase: GetHardwarePublicKeyForWalletUseCase by inject<GetHardwarePublicKeyForWalletUseCase>(GetHardwarePublicKeyForWalletUseCase::class.java)

    @Deprecated("Use activateWalletsSuspended instead")
    fun activateWallets(account: Account, tokenQueries: List<TokenQuery>) {
        val wallets = tokenQueries.mapNotNull { tokenQuery ->
            marketKit.token(tokenQuery)?.let { token ->
                val hardwarePublicKey = runBlocking { getHardwarePublicKeyForWalletUseCase(account, tokenQuery) }
                Wallet(token, account, hardwarePublicKey)
            }
        }

        walletManager.save(wallets)
    }

    suspend fun activateWalletsSuspended(account: Account, tokenQueries: List<TokenQuery>) {
        val wallets = tokenQueries.mapNotNull { tokenQuery ->
            marketKit.token(tokenQuery)?.let { token ->
                Wallet(token, account, getHardwarePublicKeyForWalletUseCase(account, tokenQuery))
            }
        }

        walletManager.saveSuspended(wallets)
    }

    fun activateTokens(account: Account, tokens: List<Token>) {
        val wallets = mutableListOf<Wallet>()

        for (token in tokens) {
            val hardwarePublicKey = runBlocking { getHardwarePublicKeyForWalletUseCase(account, token) }
            wallets.add(Wallet(token, account, hardwarePublicKey))
        }

        walletManager.save(wallets)
    }

}
