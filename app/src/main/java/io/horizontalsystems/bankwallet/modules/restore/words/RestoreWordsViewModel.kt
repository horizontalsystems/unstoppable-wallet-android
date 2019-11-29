package io.horizontalsystems.bankwallet.modules.restore.words

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent

class RestoreWordsViewModel : ViewModel(), RestoreWordsModule.View, RestoreWordsModule.Router {

    lateinit var delegate: RestoreWordsModule.ViewDelegate

    val errorLiveData = MutableLiveData<Int>()
    val notifyRestored = SingleLiveEvent<Unit>()

    fun init(wordsCount: Int) {
        RestoreWordsModule.init(this, this, wordsCount)
    }

    // View

    override fun showError(error: Int) {
        errorLiveData.value = error
    }

    // Router

    override fun notifyRestored() {
        notifyRestored.call()
    }

}
