package io.horizontalsystems.bankwallet.modules.coin.majorholders

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.CoinViewFactory
import io.horizontalsystems.bankwallet.modules.coin.MajorHolderItem
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch
import java.lang.Float.min
import java.net.UnknownHostException

class CoinMajorHoldersViewModel(
    private val service: CoinMajorHoldersService,
    private val factory: CoinViewFactory
) : ViewModel() {

    private val disposables = CompositeDisposable()

    var viewState by mutableStateOf<ViewState>(ViewState.Loading)
        private set

    var topWallets by mutableStateOf<List<MajorHolderItem>>(listOf())
        private set

    var semiPieChartValue by mutableStateOf(0f)
        private set

    var errorMessage by mutableStateOf<TranslatableString?>(null)
        private set

    init {
        service.stateObservable
            .subscribeIO { state ->

                viewModelScope.launch {

                    state.viewState?.let {
                        viewState = it
                    }


                    when (state) {
                        is DataState.Success -> {
                            val majorHolders = factory.getCoinMajorHolders(state.data)
                            topWallets = majorHolders
                            semiPieChartValue = getMajorAmount(majorHolders)
                        }
                        is DataState.Error -> {
                            state.errorOrNull?.let {
                                errorMessage = errorText(it)
                            }
                        }
                    }

                }
            }.let { disposables.add(it) }

        service.start()
    }

    private fun getMajorAmount(majorHolders: List<MajorHolderItem>): Float =
        min(majorHolders.sumOf { it.share }.toFloat(), 100f)

    private fun errorText(error: Throwable): TranslatableString {
        return when (error) {
            is UnknownHostException -> TranslatableString.ResString(R.string.Hud_Text_NoInternet)
            else -> TranslatableString.PlainString(error.message ?: error.javaClass.simpleName)
        }
    }

    fun onErrorClick() {
        service.refresh()
    }

    override fun onCleared() {
        disposables.clear()
        service.stop()
    }

    fun errorShown() {
        errorMessage = null
    }
}
