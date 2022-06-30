package io.horizontalsystems.bankwallet.modules.createaccount

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.PassphraseValidator
import io.horizontalsystems.bankwallet.core.managers.WalletActivator
import io.horizontalsystems.bankwallet.core.managers.WordsManager
import io.horizontalsystems.bankwallet.core.providers.PredefinedBlockchainSettingsProvider
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.reactivex.subjects.BehaviorSubject

class CreateAccountService(
    private val accountFactory: IAccountFactory,
    private val wordsManager: WordsManager,
    private val accountManager: IAccountManager,
    private val walletActivator: WalletActivator,
    private val passphraseValidator: PassphraseValidator,
    private val predefinedBlockchainSettingsProvider: PredefinedBlockchainSettingsProvider,
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
        val account = accountFactory.account(accountFactory.getNextAccountName(), accountType, AccountOrigin.Created, false)

        accountManager.save(account)
        activateDefaultWallets(account)
        predefinedBlockchainSettingsProvider.prepareNew(account, BlockchainType.Zcash)
    }

    private fun activateDefaultWallets(account: Account) {
        val tokenQueries = listOf(
            TokenQuery(BlockchainType.Bitcoin, TokenType.Native),
            TokenQuery(BlockchainType.Ethereum, TokenType.Native),
            TokenQuery(BlockchainType.BinanceSmartChain, TokenType.Native),
            TokenQuery(BlockchainType.Polygon, TokenType.Native),
            TokenQuery(BlockchainType.Zcash, TokenType.Native)
        )
        walletActivator.activateWallets(account, tokenQueries)
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
