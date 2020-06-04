package io.horizontalsystems.bankwallet.modules.addErc20token

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import java.lang.Exception
import java.util.concurrent.TimeUnit

class AddErc20TokenViewModel : ViewModel() {

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

        disposable = Single.just(Coin("TSC","TestCoin","TSC",8, CoinType.Bitcoin))
                .delay(12, TimeUnit.SECONDS)
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
        //("Not yet implemented")
        return null
    }

    @Throws
    private fun validateAddress(contractAddress: String) {
        //("Not yet implemented")
    }

    fun onAddClick() {
        val coin = coin ?: return
        save(coin)
        showSuccess.call()
    }

    private fun save(coin: Coin) {
        //TODO("Not yet implemented")
    }

    data class ViewItem(val coinName: String, val symbol: String, val decimal: Int)

}