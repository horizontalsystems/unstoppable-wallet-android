package bitcoin.wallet.core.security

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import bitcoin.wallet.LauncherActivity
import bitcoin.wallet.core.IEncryptionManager
import bitcoin.wallet.core.managers.Factory
import bitcoin.wallet.lib.AlertKeysInvalidated

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

        fun showAuthenticationScreen(fragment: Fragment, requestCode: Int) {
            val mKeyguardManager = fragment.activity?.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            val intent: Intent? = mKeyguardManager.createConfirmDeviceCredentialIntent("Authorization required", "")
            fragment.startActivityForResult(intent, requestCode)
        }

        fun showKeysInvalidatedAlert(activity: FragmentActivity) {

            val alert = AlertKeysInvalidated()
            alert.listener = object : AlertKeysInvalidated.Listener {
                override fun onDismiss() {
                    KeyStoreWrapper().removeAndroidKeyStoreKey(MASTER_KEY)
                    Factory.preferencesManager.clearAll()

                    val intent = Intent(activity, LauncherActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    activity.startActivity(intent)

                    activity.finish()
                }
            }
            alert.show(activity.supportFragmentManager, "error_alert")
        }

    }

}
