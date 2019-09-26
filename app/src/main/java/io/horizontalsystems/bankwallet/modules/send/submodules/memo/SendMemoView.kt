package io.horizontalsystems.bankwallet.modules.send.submodules.memo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SendMemoView : SendMemoModule.View {

    val maxLength = MutableLiveData<Int>()

    override fun setMaxLength(maxLength: Int) {
        this.maxLength.value = maxLength
    }
}
