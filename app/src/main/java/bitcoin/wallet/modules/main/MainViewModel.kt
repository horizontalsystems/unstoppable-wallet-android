package bitcoin.wallet.modules.main

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import bitcoin.wallet.core.App
import io.reactivex.disposables.CompositeDisposable

class MainViewModel : ViewModel(){

    val wordListBackedUp = MutableLiveData<Boolean>()
    private val disposables = CompositeDisposable()

    fun init() {
        wordListBackedUp.value = App.wordsManager.isBackedUp

        disposables.add(App.wordsManager.backedUpSubject.subscribe {
            wordListBackedUp.value = it
        })
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}
