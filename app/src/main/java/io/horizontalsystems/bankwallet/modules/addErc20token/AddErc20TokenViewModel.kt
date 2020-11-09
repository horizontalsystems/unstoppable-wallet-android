package io.horizontalsystems.bankwallet.modules.addErc20token

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.IErc20ContractInfoProvider
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class AddErc20TokenViewModel(
        private val coinManager: ICoinManager,
        private val erc20ContractInfoProvider: IErc20ContractInfoProvider) : ViewModel() {

    val showTrashButton = MutableLiveData<Boolean>()
    val showPasteButton = MutableLiveData<Boolean>()
    val showSuccess = SingleLiveEvent<Unit>()
    val resultLiveData = MutableLiveData<AddErc20TokenModule.State>()

    private var disposable: Disposable? = null
    private var coin: Coin? = null


    fun onTextChange(text: CharSequence?) {
        showTrashButton.postValue(!text.isNullOrEmpty())
        showPasteButton.postValue(text.isNullOrEmpty())

        if (text.isNullOrEmpty()) {
            resetView()
            return
        }

        val contractAddress = text.toString().trim()

        try {
            validateAddress(contractAddress)
        } catch (e: Exception) {
            resultLiveData.postValue(AddErc20TokenModule.State.Failed(AddErc20TokenModule.InvalidAddress()))
            return
        }

        existingCoin(contractAddress)?.let { coin ->
            resultLiveData.postValue(AddErc20TokenModule.State.ExistingCoin(getViewItem(coin)))
            return
        }

        resultLiveData.postValue(AddErc20TokenModule.State.Loading)

        fetchCoin(contractAddress)
    }

    override fun onCleared() {
        disposable?.dispose()
        super.onCleared()
    }

    private fun resetView() {
        coin = null
        resultLiveData.postValue(AddErc20TokenModule.State.Empty)
    }

    private fun fetchCoin(contractAddress: String) {
        disposable?.dispose()

        disposable = erc20ContractInfoProvider.getCoin(contractAddress)
                .subscribeOn(Schedulers.io())
                .subscribe({ fetchedCoin ->
                    coin = fetchedCoin
                    resultLiveData.postValue(AddErc20TokenModule.State.Success(getViewItem(fetchedCoin)))
                }, {
                    resultLiveData.postValue(AddErc20TokenModule.State.Failed(it))
                })
    }

    private fun getViewItem(coin: Coin) =
            AddErc20TokenModule.ViewItem(coin.title, coin.code, coin.decimal)

    private fun existingCoin(contractAddress: String): Coin? {
        return coinManager.existingErc20Coin(contractAddress)
    }

    @Throws
    private fun validateAddress(contractAddress: String) {
        AddressValidator.validate(contractAddress)
    }

    fun onAddClick() {
        val coin = coin ?: return
        save(coin)
        showSuccess.call()
    }

    private fun save(coin: Coin) {
        coinManager.save(coin)
    }

}
