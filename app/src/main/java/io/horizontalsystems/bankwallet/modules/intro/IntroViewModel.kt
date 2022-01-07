package io.horizontalsystems.bankwallet.modules.intro

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.ILocalStorage

class IntroViewModel(
        private val localStorage: ILocalStorage
): ViewModel() {

    fun onStartClicked() {
        localStorage.mainShowedOnce = true
    }

}
