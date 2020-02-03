package io.horizontalsystems.bankwallet.modules.notifications

import io.horizontalsystems.core.SingleLiveEvent

class NotificationsRouter : NotificationsModule.IRouter {
    val openNotificationSettingsLiveEvent = SingleLiveEvent<Unit>()

    override fun openNotificationSettings() {
        openNotificationSettingsLiveEvent.call()
    }
}
