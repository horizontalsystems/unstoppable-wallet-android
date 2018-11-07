package io.horizontalsystems.bankwallet.core.managers

import android.app.KeyguardManager
import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import android.support.v4.BuildConfig
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
        val fingerprintManager = App.instance.getSystemService(Context.FINGERPRINT_SERVICE) as? FingerprintManager
        return fingerprintManager?.isHardwareDetected ?: false
    }

    override fun touchSensorCanBeUsed(): Boolean {
        val fingerprintManager = App.instance.getSystemService(Context.FINGERPRINT_SERVICE) as? FingerprintManager
        return when {
            fingerprintManager?.isHardwareDetected == true -> {
                val keyguardManager = App.instance.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                keyguardManager.isKeyguardSecure && fingerprintManager.hasEnrolledFingerprints()
            }
            else -> false
        }
    }
}
