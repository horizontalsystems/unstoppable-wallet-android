package io.horizontalsystems.bankwallet.modules.send.submodules.memo

import androidx.lifecycle.MutableLiveData

class SendMemoView : SendMemoModule.IView {

    val maxLength = MutableLiveData<Int>()

    override fun setMaxLength(maxLength: Int) {
        this.maxLength.value = maxLength
    }
}
