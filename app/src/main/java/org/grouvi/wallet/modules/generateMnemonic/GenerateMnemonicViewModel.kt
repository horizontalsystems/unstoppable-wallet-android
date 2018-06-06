package org.grouvi.wallet.modules.generateMnemonic

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import org.grouvi.wallet.SingleLiveEvent

class GenerateMnemonicViewModel : ViewModel(), GenerateMnemonicModule.IView, GenerateMnemonicModule.IRouter {

    override lateinit var presenter: GenerateMnemonicModule.IPresenter

    val mnemonicWords = MutableLiveData<List<String>>()
    val openMnemonicWordsConfirmationLiveEvent = SingleLiveEvent<Void>()

    fun init() {
        GenerateMnemonicModule.initModule(this, this)

        presenter.start()
    }

    override fun showMnemonicWords(words: List<String>) {
        mnemonicWords.value = words
    }

    override fun openMnemonicWordsConfirmation() {
        openMnemonicWordsConfirmationLiveEvent.call()
    }
}