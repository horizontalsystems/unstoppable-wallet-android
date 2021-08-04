package io.horizontalsystems.bankwallet.modules.send.submodules.memo

import androidx.lifecycle.MutableLiveData

class SendMemoView : SendMemoModule.IView {

    val maxLength = MutableLiveData<Int>()
    val hidden = MutableLiveData<Boolean>()

    override fun setMaxLength(maxLength: Int) {
        this.maxLength.value = maxLength
    }

    override fun setHidden(hidden: Boolean) {
        this.hidden.value = hidden
    }
}
