package io.horizontalsystems.bankwallet.modules.intro

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.core.SingleLiveEvent

class IntroViewModel(
        private val localStorage: ILocalStorage
): ViewModel() {

    val openMainLiveEvent = SingleLiveEvent<Unit>()

    fun onClickStart() {
        localStorage.mainShowedOnce = true
        openMainLiveEvent.call()
    }

}
