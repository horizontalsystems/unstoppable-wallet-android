package bitcoin.wallet

import android.os.Bundle
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.util.Log
import bitcoin.wallet.core.AdapterManager
import bitcoin.wallet.core.managers.Factory
import bitcoin.wallet.core.security.EncryptionManager
import bitcoin.wallet.modules.guest.GuestModule
import bitcoin.wallet.modules.main.MainModule
import java.security.UnrecoverableKeyException

class LauncherActivity : BaseActivity() {

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
        }

        safeExecuteWithKeystore(
                action = Runnable {
                    Factory.wordsManager.savedWords()?.let {
                        AdapterManager.initAdapters(it)
                    } ?: run { throw Exception() }
                },
                onSuccess = Runnable {
                    MainModule.start(this)
                    finish()
                },
                onFailure = Runnable {
                    GuestModule.start(this)
                    finish()
                }
        )
    }

}

fun Any?.log(label: String = "") {

    Log.e("AAA", "$label: $this")

}