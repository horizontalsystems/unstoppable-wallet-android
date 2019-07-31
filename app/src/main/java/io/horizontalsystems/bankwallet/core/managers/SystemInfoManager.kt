package io.horizontalsystems.bankwallet.core.managers

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISystemInfoManager

class SystemInfoManager : ISystemInfoManager {

    override val appVersion: String = BuildConfig.VERSION_NAME

    override val isSystemLockOff: Boolean
        get() {
            val keyguardManager = App.instance.getSystemService(Activity.KEYGUARD_SERVICE) as KeyguardManager
            return !keyguardManager.isDeviceSecure
        }

    override val hasFingerprintSensor: Boolean
        get() {
            val fingerprintManager = FingerprintManagerCompat.from(App.instance)
            return fingerprintManager.isHardwareDetected
        }

    override val hasEnrolledFingerprints: Boolean
        get() {
            val fingerprintManager = FingerprintManagerCompat.from(App.instance)
            return when {
                fingerprintManager.isHardwareDetected -> {
                    val keyguardManager = App.instance.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                    keyguardManager.isKeyguardSecure && fingerprintManager.hasEnrolledFingerprints()
                }
                else -> false
            }
        }

}
