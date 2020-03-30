package io.horizontalsystems.bankwallet.modules.intro

import io.horizontalsystems.core.SingleLiveEvent

class IntroRouter : IntroModule.IRouter {
    val navigateToWelcomeLiveEvent = SingleLiveEvent<Unit>()
    
    override fun navigateToWelcome() {
        navigateToWelcomeLiveEvent.call()
    }
}
