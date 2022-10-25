package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.core.supports
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.derivation
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.reactivex.disposables.CompositeDisposable

class WatchAddressBlockchainManager(
    accountManager: IAccountManager,
    private val walletManager: IWalletManager,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val walletActivator: WalletActivator,
) {

    private val disposables = CompositeDisposable()

    init {
        accountManager.activeAccountObservable
            .subscribeIO {
                enableDisabledBlockchains(it.orElse(null))
            }
            .let {
                disposables.add(it)
            }
        enableDisabledBlockchains(accountManager.activeAccount)
    }

    private fun enableEvmBlockchains(account: Account) {
        val wallets = walletManager.getWallets(account)
        val enabledBlockchains = wallets.map { it.token.blockchain }
        val disabledBlockchains = evmBlockchainManager.allBlockchains
            .filter { !enabledBlockchains.contains(it) }

        if (disabledBlockchains.isEmpty()) {
            return
        }

        try {
            val tokenQueries = disabledBlockchains.map { TokenQuery(it.type, TokenType.Native) }
            walletActivator.activateWallets(account, tokenQueries)
        } catch (e: Exception) {

        }
    }

    private fun enableBtcBlockchains(account: Account, mnemonicDerivation: AccountType.Derivation) {
        val blockchainTypes: List<BlockchainType> = listOf(
            BlockchainType.Bitcoin,
            BlockchainType.BitcoinCash,
            BlockchainType.Litecoin,
            BlockchainType.Dash
        )
        val supportedBlockchainTypes = blockchainTypes.filter { it.supports(account.type) }

        val wallets = walletManager.getWallets(account)
        val enabledBlockchainTypes = wallets.map { it.token.blockchain.type }
        val disabledBlockchains = supportedBlockchainTypes.filter {
            !enabledBlockchainTypes.contains(it)
        }

        if (disabledBlockchains.isEmpty()) {
            return
        }

        try {
            val tokenQueries = disabledBlockchains.map { TokenQuery(it, TokenType.Native) }
            walletActivator.activateBtcWallets(mnemonicDerivation, account, tokenQueries)
        } catch (e: Exception) {

        }
    }

    private fun enableDisabledBlockchains(account: Account?) {
        account ?: return

        when(val type = account.type) {
            is AccountType.EvmAddress -> enableEvmBlockchains(account)
            is AccountType.HdExtendedKey -> {
                if (type.hdExtendedKey.info.isPublic) {
                    enableBtcBlockchains(account, type.hdExtendedKey.info.purpose.derivation)
                }
            }
            else -> Unit
        }
    }
}
