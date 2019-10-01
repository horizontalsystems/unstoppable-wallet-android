package io.horizontalsystems.bankwallet.core.managers

import android.app.Activity
import io.horizontalsystems.bankwallet.core.IKeyStoreManager
import io.horizontalsystems.bankwallet.core.ISystemInfoManager
import io.horizontalsystems.bankwallet.modules.keystore.KeyStoreModule

class KeyStoreChangeListener(private val systemInfoManager: ISystemInfoManager,
                             private val keyStoreManager: IKeyStoreManager) : BackgroundManager.Listener {

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
