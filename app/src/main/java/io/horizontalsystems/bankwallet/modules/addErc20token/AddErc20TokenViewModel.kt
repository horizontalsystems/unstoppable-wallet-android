package io.horizontalsystems.bankwallet.modules.addErc20token

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AddErc20TokenViewModel: ViewModel() {

    val showTrashButtonVisible = MutableLiveData<Boolean>()
    val showPasteButtonVisible = MutableLiveData<Boolean>()

    fun onTextChange(text: CharSequence?) {
        showTrashButtonVisible.postValue(text?.isEmpty() != true)
        showPasteButtonVisible.postValue(text?.isEmpty())
    }

}