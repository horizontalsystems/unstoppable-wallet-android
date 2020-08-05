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
    val showInvalidAddressError = MutableLiveData<Boolean>()
    val showExistingCoinWarning = MutableLiveData<Boolean>()
    val showProgressbar = MutableLiveData<Boolean>()
    val showSuccess = SingleLiveEvent<Unit>()
    val showAddButton = MutableLiveData<Boolean>()
    val coinLiveData = MutableLiveData<ViewItem?>()

    private var disposable: Disposable? = null
    private var coin: Coin? = null


    fun onTextChange(text: CharSequence?) {
        showTrashButton.postValue(!text.isNullOrEmpty())
        showPasteButton.postValue(text.isNullOrEmpty())

        if (text.isNullOrEmpty()) {
            resetView()
            return
        }

        val contractAddress = text.toString()

        try {
            validateAddress(contractAddress)
        } catch (e: Exception) {
            showInvalidAddressError.postValue(true)
            return
        }

        existingCoin(contractAddress)?.let { coin ->
            coinLiveData.postValue(getViewItem(coin))
            showExistingCoinWarning.postValue(true)
            return
        }

        showProgressbar.postValue(true)

        fetchCoin(contractAddress)
    }

    override fun onCleared() {
        disposable?.dispose()
        super.onCleared()
    }

    private fun resetView() {
        coin = null
        showAddButton.postValue(false)
        showProgressbar.postValue(false)
        showInvalidAddressError.postValue(false)
        coinLiveData.postValue(null)
        showExistingCoinWarning.postValue(false)
    }

    private fun fetchCoin(contractAddress: String) {
        disposable?.dispose()

        disposable = erc20ContractInfoProvider.getCoin(contractAddress)
                .subscribeOn(Schedulers.io())
                .subscribe({ fetchedCoin ->
                    coin = fetchedCoin
                    showProgressbar.postValue(false)
                    coinLiveData.postValue(getViewItem(fetchedCoin))
                    showAddButton.postValue(true)
                }, {
                    showProgressbar.postValue(false)
                    showInvalidAddressError.postValue(true)
                })
    }

    private fun getViewItem(coin: Coin) =
            ViewItem(coin.title, coin.code, coin.decimal)

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

    data class ViewItem(val coinName: String, val symbol: String, val decimal: Int)

}