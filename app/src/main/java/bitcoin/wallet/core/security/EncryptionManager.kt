package bitcoin.wallet.core.security

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import bitcoin.wallet.core.IEncryptionManager

class EncryptionManager : IEncryptionManager {

    private val keyStoreWrapper = KeyStoreWrapper()

    @Synchronized
    override fun encrypt(data: String): String {

        var masterKey = keyStoreWrapper.getAndroidKeyStoreSymmetricKey(MASTER_KEY)

        if (masterKey == null) {
            masterKey = keyStoreWrapper.createAndroidKeyStoreSymmetricKey(MASTER_KEY)
        }
        return CipherWrapper().encrypt(data, masterKey)
    }

    @Synchronized
    override fun decrypt(data: String): String {
        val masterKey = keyStoreWrapper.getAndroidKeyStoreSymmetricKey(MASTER_KEY)
        return CipherWrapper().decrypt(data, masterKey)
    }

    companion object {
        const val MASTER_KEY = "MASTER_KEY"

        fun showAuthenticationScreen(activity: Activity, requestCode: Int) {
            val mKeyguardManager = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            val intent: Intent? = mKeyguardManager.createConfirmDeviceCredentialIntent("Authorization required", "")
            activity.startActivityForResult(intent, requestCode)
        }

    }

}
