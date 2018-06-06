package org.grouvi.wallet.modules.confirmMnemonic

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import org.grouvi.wallet.R

class ConfirmMnemonicViewModel : ViewModel(), ConfirmMnemonicModule.IView, ConfirmMnemonicModule.IRouter {
    override lateinit var presenter: ConfirmMnemonicModule.IPresenter

    val wordPositionLiveData = MutableLiveData<Int>()
    val errorLiveData = MutableLiveData<Int>()

    fun init() {
        ConfirmMnemonicModule.initModule(this, this)

        presenter.start()
    }

    override fun showWordConfirmationForm(confirmationWordPosition: Int) {
        wordPositionLiveData.value = confirmationWordPosition
    }

    override fun showWordNotConfirmedError() {
        errorLiveData.value = R.string.error
    }
}