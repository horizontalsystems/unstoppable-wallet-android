package io.horizontalsystems.bankwallet

import android.os.Bundle
import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.appcompat.app.AppCompatActivity
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.security.EncryptionManager
import io.horizontalsystems.bankwallet.modules.backup.BackupModule
import io.horizontalsystems.bankwallet.modules.backup.BackupPresenter
import io.horizontalsystems.bankwallet.modules.guest.GuestModule
import io.horizontalsystems.bankwallet.modules.pin.PinModule
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
        } else if (!App.authManager.isLoggedIn) {
            GuestModule.start(this)
        } else if (!App.localStorage.iUnderstand) {
            BackupModule.start(this, BackupPresenter.DismissMode.SET_PIN)
        } else if(App.secureStorage.pinIsEmpty()) {
            PinModule.startForSetPin(this)
        } else {
            PinModule.startForUnlockFromAppStart()
        }
        finish()
    }

}
