package io.horizontalsystems.bankwallet.core.managers

import android.app.KeyguardManager
import android.content.Context
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat
import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISystemInfoManager
import io.horizontalsystems.bankwallet.entities.BiometryType

class SystemInfoManager: ISystemInfoManager {
    override var appVersion: String = BuildConfig.VERSION_NAME

    override var biometryType: BiometryType = BiometryType.NONE
        get() {
            return when {
                phoneHasFingerprintSensor() -> BiometryType.FINGER
                else -> BiometryType.NONE
            }
        }

    override fun phoneHasFingerprintSensor(): Boolean {
        val fingerprintManager = FingerprintManagerCompat.from(App.instance)
        return fingerprintManager.isHardwareDetected
    }

    override fun touchSensorCanBeUsed(): Boolean {
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
