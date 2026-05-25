package io.horizontalsystems.bankwallet.modules.createaccount

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator

object CreateAccountModule {

    enum class Kind(val wordsCount: Int) {
        Mnemonic12(12),
        Mnemonic15(15),
        Mnemonic18(18),
        Mnemonic21(21),
        Mnemonic24(24);

        val title = Translator.getString(R.string.CreateWallet_N_Words, wordsCount)

        val titleLong: String
            get() = if (this == Mnemonic12) Translator.getString(R.string.CreateWallet_N_WordsRecommended, wordsCount)
            else title
    }
}
