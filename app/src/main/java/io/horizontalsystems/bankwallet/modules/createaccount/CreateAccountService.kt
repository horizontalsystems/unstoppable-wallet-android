package io.horizontalsystems.bankwallet.modules.createaccount

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.managers.PassphraseValidator
import io.horizontalsystems.bankwallet.core.managers.WordsManager
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.CoinType
import io.reactivex.subjects.BehaviorSubject

class CreateAccountService(
        private val accountFactory: IAccountFactory,
        private val wordsManager: WordsManager,
        private val accountManager: IAccountManager,
        private val walletManager: IWalletManager,
        private val passphraseValidator: PassphraseValidator,
        private val marketKit: MarketKit
) : Clearable {

    val allKinds: Array<CreateAccountModule.Kind> = CreateAccountModule.Kind.values()

    var kind: CreateAccountModule.Kind = CreateAccountModule.Kind.Mnemonic12
        set(value) {
            field = value
            kindObservable.onNext(value)
        }
    val kindObservable = BehaviorSubject.createDefault(kind)

    var passphraseEnabled: Boolean = false
        set(value) {
            field = value
            passphraseEnabledObservable.onNext(value)
        }
    val passphraseEnabledObservable = BehaviorSubject.createDefault(passphraseEnabled)

    var passphrase = ""
    var passphraseConfirmation = ""

    override fun clear() = Unit

    fun createAccount() {
        if (passphraseEnabled) {
            if (passphrase.isBlank()) throw CreateError.EmptyPassphrase
            if (passphrase != passphraseConfirmation) throw CreateError.InvalidConfirmation
        }

        val accountType = resolveAccountType()
        val account = accountFactory.account(accountType, AccountOrigin.Created, false)

        accountManager.save(account)
        activateDefaultWallets(account)
    }

    private fun activateDefaultWallets(account: Account) {
        val defaultCoinTypes = listOf(CoinType.Bitcoin, CoinType.Ethereum, CoinType.BinanceSmartChain)

        val wallets = mutableListOf<Wallet>()

        for (coinType in defaultCoinTypes) {
            val platformCoin = marketKit.platformCoin(coinType) ?: continue

            val defaultSettingsArray = coinType.defaultSettingsArray

            if (defaultSettingsArray.isEmpty()) {
                wallets.add(Wallet(platformCoin, account))
            } else {
                defaultSettingsArray.forEach { coinSettings ->
                    val configuredPlatformCoin = ConfiguredPlatformCoin(platformCoin, coinSettings)
                    wallets.add(Wallet(configuredPlatformCoin, account))
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
        return AccountType.Mnemonic(words, passphrase)
    }

    fun validatePassphrase(text: String?): Boolean {
        return passphraseValidator.validate(text)
    }

    sealed class CreateError : Throwable() {
        object EmptyPassphrase : CreateError()
        object InvalidConfirmation : CreateError()
    }

}
