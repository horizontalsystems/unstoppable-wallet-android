package bitcoin.wallet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.UserNotAuthenticatedException
import android.support.v7.app.AppCompatActivity
import android.util.Log
import bitcoin.wallet.core.managers.Factory
import bitcoin.wallet.core.security.EncryptionManager
import bitcoin.wallet.modules.guest.GuestModule
import bitcoin.wallet.modules.main.MainModule
import io.realm.SyncUser

class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            redirectToCorrectPage()
        } catch (exception: UserNotAuthenticatedException) {
            EncryptionManager.showAuthenticationScreen(this, AUTHENTICATE_TO_REDIRECT)
        } catch (exception: KeyPermanentlyInvalidatedException) {
            EncryptionManager.showKeysInvalidatedAlert(this)
        }
    }

    private fun redirectToCorrectPage() {
        if (Factory.preferencesManager.savedWords != null && SyncUser.current() != null) {
            MainModule.start(this)
        } else {
            GuestModule.start(this)
        }
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