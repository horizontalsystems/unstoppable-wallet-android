package io.horizontalsystems.bankwallet.modules.addtoken

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable

class AddTokenViewModel(
        private val addTokenService: AddTokenService,
        val titleTextRes: Int,
        val hintTextRes: Int) : ViewModel() {

    val loadingLiveData = MutableLiveData<Boolean>()
    val showWarningLiveData = MutableLiveData<Boolean>()
    val showErrorLiveData = MutableLiveData<Throwable?>()
    val viewItemLiveData = MutableLiveData<AddTokenModule.ViewItem?>()
    val showTrashButton = MutableLiveData<Boolean>()
    val showPasteButton = MutableLiveData<Boolean>()
    val showAddButton = MutableLiveData<Boolean>()
    val showSuccess = SingleLiveEvent<Unit>()

    private var disposables = CompositeDisposable()

    init {
        observeState()
    }

    private fun observeState() {
        addTokenService.stateObservable
                .subscribe {
                    sync(it)
                }
                .let { disposables.add(it) }
    }

    fun onTextChange(text: CharSequence?) {
        showTrashButton.postValue(!text.isNullOrEmpty())
        showPasteButton.postValue(text.isNullOrEmpty())

        val reference = text.toString().trim()

        addTokenService.set(reference)
    }


    override fun onCleared() {
        addTokenService.onCleared()
        disposables.clear()
        super.onCleared()
    }

    fun onAddClick() {
        addTokenService.save()
        showSuccess.call()
    }

    private fun sync(state: AddTokenModule.State) {
        loadingLiveData.postValue(state == AddTokenModule.State.Loading)

        viewItemLiveData.postValue(getViewItemByState(state))

        showWarningLiveData.postValue(state is AddTokenModule.State.AlreadyExists)

        showAddButton.postValue(state is AddTokenModule.State.Fetched)

        showErrorLiveData.postValue((state as? AddTokenModule.State.Failed)?.error)
    }

    private fun getViewItemByState(state: AddTokenModule.State): AddTokenModule.ViewItem? {
        return when (state) {
            is AddTokenModule.State.AlreadyExists -> getViewItem(state.coin)
            is AddTokenModule.State.Fetched -> getViewItem(state.coin)
            else -> null
        }
    }

    private fun getViewItem(coin: Coin) =
            AddTokenModule.ViewItem(coin.title, coin.code, coin.decimal)

}
