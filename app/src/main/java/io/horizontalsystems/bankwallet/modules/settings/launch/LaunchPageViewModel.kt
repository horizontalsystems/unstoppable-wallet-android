package io.horizontalsystems.bankwallet.modules.settings.launch

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.LaunchPage

class LaunchPageViewModel(private val service: LaunchPageService) : ViewModel() {

    val optionsLiveData = MutableLiveData(service.options)

    fun onLaunchPageSelect(launchPage: LaunchPage) {
        service.selectLaunchPage(launchPage)
        optionsLiveData.postValue(service.options)
    }

}
