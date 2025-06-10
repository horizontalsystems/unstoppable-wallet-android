package cash.p.terminal.wallet.useCases

import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.TokenQuery

class WalletUseCase(
    private val walletManager: IWalletManager,
    private val accountManager: IAccountManager,
    private val adapterManager: IAdapterManager,
    private val getHardwarePublicKeyForWalletUseCase: GetHardwarePublicKeyForWalletUseCase,
    private val scanToAddUseCase: ScanToAddUseCase
) {
    fun getWallet(token: Token): Wallet? = walletManager.activeWallets.find {
        it.token == token
    }

    fun getWallet(coinUid: String, blockchainType: String): Wallet? =
        walletManager.activeWallets.find {
            it.token.coin.uid == coinUid && it.token.blockchainType.uid == blockchainType
        }

    suspend fun createWallets(tokensToAdd: Set<Token>): Boolean {
        val account = accountManager.activeAccount ?: return false
        return if (account.type is AccountType.HardwareCard) {
            createWalletsForHardwareWallet(
                account = account,
                tokensToAdd = tokensToAdd
            )
        } else {
            walletManager.save(tokensToAdd.map { Wallet(it, account, null) })
            return true
        }
    }

    private suspend fun createWalletsForHardwareWallet(
        account: Account,
        tokensToAdd: Set<Token>
    ): Boolean {
        var hardwarePublicKeys =
            tokensToAdd.mapNotNull { token ->
                getHardwarePublicKeyForWalletUseCase(
                    account = account,
                    blockchainType = token.blockchainType,
                    tokenType = token.type
                )?.let { hardwarePublicKey ->
                    token to hardwarePublicKey
                }
            }
        if (tokensToAdd.size == hardwarePublicKeys.size) {
            // We already had all hardware public keys
            walletManager.save(hardwarePublicKeys.map { (token, hardwarePublicKey) ->
                Wallet(token, account, hardwarePublicKey)
            })
            return true
        }
        val queryList = tokensToAdd.map {
            TokenQuery(
                blockchainType = it.blockchainType,
                tokenType = it.type
            )
        }
        val cardId = (account.type as AccountType.HardwareCard).cardId
        val allKeysCreated = scanToAddUseCase.addTokensByScan(
            blockchainsToDerive = queryList,
            cardId = cardId,
            accountId = account.id
        )
        hardwarePublicKeys =
            tokensToAdd.mapNotNull { token ->
                getHardwarePublicKeyForWalletUseCase(
                    account = account,
                    blockchainType = token.blockchainType,
                    tokenType = token.type
                )?.let { hardwarePublicKey ->
                    token to hardwarePublicKey
                }
            }
        walletManager.save(hardwarePublicKeys.map { (token, hardwarePublicKey) ->
            Wallet(token, account, hardwarePublicKey)
        })
        return allKeysCreated
    }

    suspend fun createWalletIfNotExists(token: Token): Wallet? =
        getWallet(token) ?: if (createWallets(setOf(token))) getWallet(token) else null

    fun getReceiveAddress(token: Token): String {
        val wallet = getWallet(token)
        requireNotNull(wallet) { "WalletUseCase: wallet for $token is not found" }
        val adapter = adapterManager.getReceiveAdapterForWallet(wallet)
        requireNotNull(adapter) { "WalletUseCase: adapter for $token is not found" }

        return adapter.receiveAddress
    }
}