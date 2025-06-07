package cash.p.terminal.modules.createaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.providers.PredefinedBlockchainSettingsProvider
import cash.p.terminal.wallet.PassphraseValidator
import org.koin.compose.koinInject

object CreateAccountModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CreateAdvancedAccountViewModel(
                accountFactory = App.accountFactory,
                wordsManager = App.wordsManager,
                accountManager = App.accountManager,
                walletActivator = App.walletActivator,
                passphraseValidator = PassphraseValidator(),
                predefinedBlockchainSettingsProvider = PredefinedBlockchainSettingsProvider(
                    App.restoreSettingsManager,
                    App.zcashBirthdayProvider
                )
            ) as T
        }
    }

    enum class Kind(val wordsCount: Int) {
        Mnemonic12(12),
        Mnemonic15(15),
        Mnemonic18(18),
        Mnemonic21(21),
        Mnemonic24(24);

        val title = cash.p.terminal.strings.helpers.Translator.getString(
            R.string.CreateWallet_N_Words,
            wordsCount
        )

        val titleLong: String
            get() = if (this == Mnemonic12) cash.p.terminal.strings.helpers.Translator.getString(
                R.string.CreateWallet_N_WordsRecommended,
                wordsCount
            )
            else title
    }
}
