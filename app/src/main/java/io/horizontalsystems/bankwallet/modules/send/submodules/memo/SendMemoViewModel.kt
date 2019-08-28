package io.horizontalsystems.bankwallet.modules.send.submodules.memo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SendMemoViewModel : ViewModel(), SendMemoModule.IView {

    lateinit var delegate: SendMemoModule.IViewDelegate

    val maxLength = MutableLiveData<Int>()

    fun init(maxLength: Int): SendMemoModule.IMemoModule {
        return SendMemoModule.init(this, maxLength)
    }

    override fun setMaxLength(maxLength: Int) {
        this.maxLength.value = maxLength
    }
}
