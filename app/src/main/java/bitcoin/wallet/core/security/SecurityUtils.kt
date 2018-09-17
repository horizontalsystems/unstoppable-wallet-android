package bitcoin.wallet.core.security

import android.app.KeyguardManager
import android.content.Context
import android.content.Context.FINGERPRINT_SERVICE
import android.hardware.fingerprint.FingerprintManager


object SecurityUtils {

    fun touchSensorCanBeUsed(context: Context): Boolean {
        val fingerprintManager = context.getSystemService(FINGERPRINT_SERVICE) as? FingerprintManager
        return when {
            fingerprintManager?.isHardwareDetected == true -> {
                val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                keyguardManager.isKeyguardSecure && fingerprintManager.hasEnrolledFingerprints()
            }
            else -> false
        }
    }

    fun phoneHasFingerprintSensor(context: Context): Boolean {
        val fingerprintManager = context.getSystemService(FINGERPRINT_SERVICE) as? FingerprintManager
        return fingerprintManager?.isHardwareDetected ?: false
    }

}
