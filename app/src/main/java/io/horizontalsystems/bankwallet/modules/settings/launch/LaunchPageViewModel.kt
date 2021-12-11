package io.horizontalsystems.bankwallet.modules.settings.launch

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.LaunchPage
import io.horizontalsystems.bankwallet.ui.compose.Select

class LaunchPageViewModel(private val service: LaunchPageService) : ViewModel() {

    val optionsLiveData = MutableLiveData(getItems())

    fun onLaunchPageSelect(launchPage: LaunchPage) {
        service.selectLaunchPage(launchPage)
        optionsLiveData.postValue(getItems())
    }

    private fun getItems(): Select<LaunchPage> {
        return Select(service.selectedOption, service.options)
    }

}
