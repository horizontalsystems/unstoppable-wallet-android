package io.horizontalsystems.bankwallet.modules.createaccount

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.ui.selector.ViewItemWrapper
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable

class CreateAccountViewModel(private val service: CreateAccountService, private val clearables: List<Clearable>) : ViewModel() {

    val kindLiveData = MutableLiveData<String>()
    val showErrorLiveEvent = SingleLiveEvent<String>()
    val finishLiveEvent = SingleLiveEvent<Unit>()

    private val disposables = CompositeDisposable()

    val kindViewItems = service.allKinds.map {
        ViewItemWrapper(it.title, it)
    }

    var selectedKindViewItem: ViewItemWrapper<CreateAccountModule.Kind>
        get() = ViewItemWrapper(service.kind.title, service.kind)
        set(value) {
            service.kind = value.item
        }

    init {
        service.kindObservable
                .subscribe {
                    sync(it)
                }
                .let {
                    disposables.add(it)
                }

        sync(service.kind)
    }

    private fun sync(kind: CreateAccountModule.Kind) {
        kindLiveData.postValue(kind.title)
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposables.clear()
    }

    fun onClickCreate() {
        try {
            service.createAccount()
            finishLiveEvent.postValue(Unit)
        } catch (t: Throwable) {
            showErrorLiveEvent.postValue(t.localizedMessage ?: t.javaClass.simpleName)
        }
    }
}
