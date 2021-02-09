package io.horizontalsystems.bankwallet.modules.enablecoins

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.Disposable

class EnableCoinsViewModel(private val service: EnableCoinsService) : ViewModel() {

    val hudStateLiveData = MutableLiveData<HudState>()
    val confirmationLiveData = MutableLiveData<String>()

    private val disposable: Disposable

    init {
        service.stateAsync
                .subscribe {
                    handle(it)
                }.let {
                    disposable = it
                }

        handle(service.state)
    }

    fun onConfirmEnable() {
        service.approveEnable()
    }

    private fun handle(state: EnableCoinsService.State) {
        when (state) {
            is EnableCoinsService.State.Idle -> {
                hudStateLiveData.postValue(HudState.Hidden)
            }
            is EnableCoinsService.State.WaitingForApprove -> {
                hudStateLiveData.postValue(HudState.Hidden)
                confirmationLiveData.postValue(state.tokenType.title)
            }
            is EnableCoinsService.State.Loading -> {
                hudStateLiveData.postValue(HudState.Loading)
            }
            is EnableCoinsService.State.Success -> {
                hudStateLiveData.postValue(HudState.Success(state.coins.size))
            }
            is EnableCoinsService.State.Failure -> {
                hudStateLiveData.postValue(HudState.Error)
            }
        }
    }

    sealed class HudState {
        object Hidden : HudState()
        object Loading : HudState()
        class Success(val count: Int) : HudState()
        object Error : HudState()
    }
}
