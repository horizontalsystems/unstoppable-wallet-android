package bitcoin.wallet

import android.os.Bundle
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.support.v7.app.AppCompatActivity
import bitcoin.wallet.core.App
import bitcoin.wallet.core.security.EncryptionManager
import bitcoin.wallet.modules.guest.GuestModule
import bitcoin.wallet.modules.main.MainModule
import java.security.UnrecoverableKeyException

class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            redirectToCorrectPage()
        } catch (e: Exception) {
            when (e) {
                is KeyPermanentlyInvalidatedException,
                is UnrecoverableKeyException -> EncryptionManager.showKeysInvalidatedAlert(this)
            }
        }
    }

    private fun redirectToCorrectPage() {
        if (!EncryptionManager.isDeviceLockEnabled(this)) {
            EncryptionManager.showNoDeviceLockWarning(this)
            return
        } else if (App.wordsManager.isLoggedIn) {
            MainModule.start(this)
        } else {
            GuestModule.start(this)
        }
        finish()
    }

}
