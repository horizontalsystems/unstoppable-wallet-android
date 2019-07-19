package io.horizontalsystems.bankwallet.core.security

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IEncryptionManager
import io.horizontalsystems.bankwallet.lib.AlertDialogFragment
import io.horizontalsystems.bankwallet.modules.main.MainModule
import javax.crypto.Cipher

object EncryptionManager : IEncryptionManager {

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
                ?: throw KeyPermanentlyInvalidatedException()
        return CipherWrapper().decrypt(data, masterKey)
    }


    override fun getCryptoObject(): FingerprintManagerCompat.CryptoObject {
        var masterKey = keyStoreWrapper.getAndroidKeyStoreSymmetricKey(MASTER_KEY)

        if (masterKey == null) {
            masterKey = keyStoreWrapper.createAndroidKeyStoreSymmetricKey(MASTER_KEY)
        }

        val cipher = CipherWrapper().cipher
        cipher.init(Cipher.ENCRYPT_MODE, masterKey)

        return FingerprintManagerCompat.CryptoObject(cipher)
    }

    const val MASTER_KEY = "MASTER_KEY"

    fun showAuthenticationScreen(activity: Activity, requestCode: Int) {
        val mKeyguardManager = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val intent: Intent? = mKeyguardManager.createConfirmDeviceCredentialIntent(
                activity.getString(R.string.OSPin_Confirm_Title),
                activity.getString(R.string.OSPin_Confirm_Desciption)
        )
        activity.startActivityForResult(intent, requestCode)
    }

    fun showAuthenticationScreen(fragment: Fragment, requestCode: Int) {
        val mKeyguardManager = fragment.activity?.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val intent: Intent? = mKeyguardManager.createConfirmDeviceCredentialIntent(
                fragment.getString(R.string.OSPin_Confirm_Title),
                fragment.getString(R.string.OSPin_Confirm_Desciption)
        )
        fragment.startActivityForResult(intent, requestCode)
    }

    fun showKeysInvalidatedAlert(activity: FragmentActivity) {
        AlertDialogFragment.newInstance(R.string.Alert_KeysInvalidatedTitle, R.string.Alert_KeysInvalidatedDescription, R.string.Alert_Ok,
                object : AlertDialogFragment.Listener {
                    override fun onButtonClick() {
                        App.authManager.logout()
                        KeyStoreWrapper().removeAndroidKeyStoreKey(MASTER_KEY)
                        MainModule.startAsNewTask(activity)

                        activity.finish()
                    }
                }).show(activity.supportFragmentManager, "keys_invalidated_alert")
    }

    fun showNoDeviceLockWarning(activity: FragmentActivity) {
        AlertDialogFragment.newInstance(R.string.Alert_TitleWarning, R.string.Alert_NoDeviceLockDescription, R.string.Alert_Close,
                object : AlertDialogFragment.Listener {
                    override fun onButtonClick() {
                        activity.finish()
                    }
                }).show(activity.supportFragmentManager, "no_device_lock_alert")
    }

    fun isDeviceLockEnabled(ctx: Context): Boolean {
        val keyguardManager = ctx.getSystemService(Activity.KEYGUARD_SERVICE) as KeyguardManager
        return keyguardManager.isKeyguardSecure
    }


}
