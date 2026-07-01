package io.horizontalsystems.bankwallet.modules.profeatures.yakauthorization

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class YakAuthorizationViewModel(val service: YakAuthorizationService) : ViewModel() {

    var stateLiveData = MutableLiveData<YakAuthorizationService.State>(YakAuthorizationService.State.Idle)

    init {
        viewModelScope.launch {
            service.stateFlow.collect { newState ->
                stateLiveData.postValue(newState)
            }
        }
    }

    fun onBannerClick() {
        stateLiveData.postValue(YakAuthorizationService.State.Authenticating)
        stateLiveData.postValue(YakAuthorizationService.State.NoYakNft)

//        viewModelScope.launch {
//            service.authenticate()
//        }
    }

    fun onActivateClick() {
        viewModelScope.launch {
            service.signConfirmed()
        }
    }

    fun reset() {
        service.authorizationCanceled()
    }

}
