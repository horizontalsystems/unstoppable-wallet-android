package io.horizontalsystems.bankwallet.core.managers

import android.app.Activity
import io.horizontalsystems.bankwallet.modules.keystore.KeyStoreModule
import io.horizontalsystems.core.IKeyStoreManager
import io.horizontalsystems.core.ISystemInfoManager

class KeyStoreChangeListener(private val systemInfoManager: ISystemInfoManager, private val keyStoreManager: IKeyStoreManager)
    : BackgroundManager.Listener {

    override fun willEnterForeground(activity: Activity) {
        when {
            systemInfoManager.isSystemLockOff -> {
                KeyStoreModule.startForNoSystemLock(activity)
            }
            keyStoreManager.isKeyInvalidated -> {
                KeyStoreModule.startForInvalidKey(activity)
            }
            keyStoreManager.isUserNotAuthenticated -> {
                KeyStoreModule.startForUserAuthentication(activity)
            }
        }
    }
}
