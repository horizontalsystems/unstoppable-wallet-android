package io.horizontalsystems.bankwallet.modules.intro

import androidx.lifecycle.ViewModel

class IntroPresenter(val router: IntroModule.IRouter) : ViewModel() {

    fun start() {
        router.navigateToWelcome()
    }

}
