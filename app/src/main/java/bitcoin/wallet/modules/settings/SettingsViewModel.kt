package bitcoin.wallet.modules.settings

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import bitcoin.wallet.core.managers.Factory
import io.reactivex.disposables.CompositeDisposable

class SettingsViewModel : ViewModel(){

    val wordListBackedUp = MutableLiveData<Boolean>()
    private val disposables = CompositeDisposable()

    fun init() {
        wordListBackedUp.value = Factory.wordsManager.wordListBackedUp

        disposables.add(Factory.wordsManager.wordListBackedUpSubject.subscribe {
            wordListBackedUp.value = it
        })
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}
