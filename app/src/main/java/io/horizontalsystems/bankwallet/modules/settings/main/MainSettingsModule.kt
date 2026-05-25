package io.horizontalsystems.bankwallet.modules.settings.main

object MainSettingsModule {

    sealed class CounterType {
        class SessionCounter(val number: Int) : CounterType()
        class PendingRequestCounter(val number: Int) : CounterType()
    }
}
