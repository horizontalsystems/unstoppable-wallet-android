package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.IWordsManager
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.reactivex.subjects.PublishSubject

class WordsManager(private val localStorage: ILocalStorage) : IWordsManager {

    override var isBackedUp: Boolean
        get() = localStorage.isBackedUp
        set(value) {
            localStorage.isBackedUp = value
            backedUpSignal.onNext(Unit)
        }

    override var backedUpSignal = PublishSubject.create<Unit>()

    @Throws(Mnemonic.MnemonicException::class)
    override fun validate(words: List<String>) {
        Mnemonic().validate(words)
    }

    override fun generateWords(count: Int): List<String> {
        val strength = when (count) {
            24 -> Mnemonic.Strength.VeryHigh
            else -> Mnemonic.Strength.Default
        }

        return Mnemonic().generate(strength)
    }

}
