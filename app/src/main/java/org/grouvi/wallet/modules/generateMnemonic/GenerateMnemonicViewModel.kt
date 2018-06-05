package org.grouvi.wallet.modules.generateMnemonic

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel

class GenerateMnemonicViewModel : ViewModel(), GenerateMnemonicModule.IView, GenerateMnemonicModule.IRouter {

    override lateinit var presenter: GenerateMnemonicModule.IPresenter

    val mnemonicWords = MutableLiveData<List<String>>()

    fun init() {
        GenerateMnemonicModule.initModule(this, this)

        presenter.start()
    }

    override fun showMnemonicWords(words: List<String>) {
        mnemonicWords.value = words
    }

    override fun openMnemonicWordsConfirmation() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}