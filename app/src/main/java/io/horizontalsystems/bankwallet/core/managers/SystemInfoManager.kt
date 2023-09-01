package io.horizontalsystems.bankwallet.core.managers

import android.app.Activity
import android.app.KeyguardManager
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.core.ISystemInfoManager

class SystemInfoManager(appConfigProvider: AppConfigProvider) : ISystemInfoManager {

    override val appVersion: String = appConfigProvider.appVersion

    private val biometricManager by lazy { BiometricManager.from(App.instance) }

    override val isSystemLockOff: Boolean
        get() {
            val keyguardManager = App.instance.getSystemService(Activity.KEYGUARD_SERVICE) as KeyguardManager
            return !keyguardManager.isDeviceSecure
        }

    override val biometricAuthSupported: Boolean
        get() = biometricManager.canAuthenticate(BIOMETRIC_WEAK) == BIOMETRIC_SUCCESS

    override val deviceModel: String
        get() = "${Build.MANUFACTURER} ${Build.MODEL}"

    override val osVersion: String
        get() = "Android ${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})"

}
