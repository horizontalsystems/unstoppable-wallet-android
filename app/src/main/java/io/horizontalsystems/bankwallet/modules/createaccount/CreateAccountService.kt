package io.horizontalsystems.bankwallet.modules.createaccount

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.factories.AccountFactory
import io.horizontalsystems.bankwallet.core.managers.WordsManager
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.coinkit.CoinKit
import io.horizontalsystems.coinkit.models.CoinType
import io.reactivex.subjects.BehaviorSubject

class CreateAccountService(
        private val accountFactory: AccountFactory,
        private val wordsManager: WordsManager,
        private val accountManager: IAccountManager,
        private val walletManager: IWalletManager,
        private val coinKit: CoinKit
) : Clearable {

    val allKinds: Array<CreateAccountModule.Kind> = CreateAccountModule.Kind.values()

    var kind: CreateAccountModule.Kind = CreateAccountModule.Kind.Mnemonic12
        set(value) {
            field = value
            kindObservable.onNext(value)
        }
    val kindObservable = BehaviorSubject.createDefault(kind)

    override fun clear() = Unit

    fun createAccount() {
        val accountType = resolveAccountType()
        val account = accountFactory.account(accountType, AccountOrigin.Created, false)

        accountManager.save(account)
        activateDefaultWallets(account)
    }

    private fun activateDefaultWallets(account: Account) {
        val defaultCoinTypes = listOf(CoinType.Bitcoin, CoinType.Ethereum, CoinType.BinanceSmartChain)

        val wallets = mutableListOf<Wallet>()

        for (coinType in defaultCoinTypes) {
            val coin = coinKit.getCoin(coinType) ?: continue

            val defaultSettingsArray = coinType.defaultSettingsArray

            if (defaultSettingsArray.isEmpty()) {
                wallets.add(Wallet(coin, account))
            } else {
                defaultSettingsArray.forEach { coinSettings ->
                    val configuredCoin = ConfiguredCoin(coin, coinSettings)
                    wallets.add(Wallet(configuredCoin, account))
                }
            }
        }

        walletManager.save(wallets)
    }

    private fun resolveAccountType() = when (kind) {
        CreateAccountModule.Kind.Mnemonic12 -> mnemonicAccountType(12)
        CreateAccountModule.Kind.Mnemonic24 -> mnemonicAccountType(24)
    }

    private fun mnemonicAccountType(wordCount: Int): AccountType {
        val words = wordsManager.generateWords(wordCount)
        return AccountType.Mnemonic(words, null)
    }

}
