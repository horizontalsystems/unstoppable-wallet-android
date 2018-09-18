package bitcoin.wallet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.UserNotAuthenticatedException
import android.util.Log
import bitcoin.wallet.core.managers.Factory
import bitcoin.wallet.core.security.EncryptionManager
import bitcoin.wallet.modules.main.MainModule
import java.security.UnrecoverableKeyException

class LauncherActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            redirectToCorrectPage()
        } catch (e: Exception) {
            Log.e("LauncherActivity", "Failed to redirectToCorrectPage", e)
            when (e) {
                is UserNotAuthenticatedException -> EncryptionManager.showAuthenticationScreen(this, AUTHENTICATE_TO_REDIRECT)
                is KeyPermanentlyInvalidatedException,
                is UnrecoverableKeyException -> EncryptionManager.showKeysInvalidatedAlert(this)
            }
        }
    }

    private fun redirectToCorrectPage() {
        if (!EncryptionManager.isDeviceLockEnabled(this)) {
            EncryptionManager.showNoDeviceLockWarning(this)
            return
        }//todo remove after testing
        Factory.preferencesManager.saveWords(listOf("used", "ugly", "meat", "glad", "balance", "divorce", "inner", "artwork", "hire", "invest", "already", "piano"))
        MainModule.start(this)
//        else if (Factory.preferencesManager.savedWords != null) {
//            MainModule.start(this)
//        } else {
//            GuestModule.start(this)
//        }
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == AUTHENTICATE_TO_REDIRECT) {
                redirectToCorrectPage()
            }
        }
    }

    companion object {
        const val AUTHENTICATE_TO_REDIRECT = 1
    }
}

fun Any?.log(label: String = "") {

    Log.e("AAA", "$label: $this")

}