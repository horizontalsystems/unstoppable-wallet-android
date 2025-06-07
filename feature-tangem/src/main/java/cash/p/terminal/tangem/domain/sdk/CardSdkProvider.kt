package cash.p.terminal.tangem.domain.sdk

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.tangem.Log
import com.tangem.TangemSdk
import com.tangem.common.CardFilter
import com.tangem.common.authentication.AuthenticationManager
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.Config
import com.tangem.common.services.secure.SecureStorage
import com.tangem.crypto.bip39.Wordlist
import com.tangem.sdk.DefaultSessionViewDelegate
import com.tangem.sdk.extensions.getWordlist
import com.tangem.sdk.extensions.initAuthenticationManager
import com.tangem.sdk.extensions.initKeystoreManager
import com.tangem.sdk.extensions.initNfcManager
import com.tangem.sdk.extensions.unsubscribe
import com.tangem.sdk.nfc.AndroidNfcAvailabilityProvider
import com.tangem.sdk.nfc.NfcManager
import com.tangem.sdk.storage.create
import io.horizontalsystems.core.BackgroundManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class CardSdkProvider(
    private val backgroundManager: BackgroundManager
) {

    private val observer = Observer()

    private var holder: Holder? = null

    val sdk: TangemSdk
        get() = holder?.sdk ?: tryToRegisterWithForegroundActivity()

    fun register(activity: FragmentActivity) = runBlocking(Dispatchers.Main.immediate) {
        if (activity.isDestroyed || activity.isFinishing || activity.isChangingConfigurations) {
            val message =
                "Tangem SDK owner registration skipped: activity is destroyed or finishing"
            Log.info { message }
            return@runBlocking
        }

        if (holder != null) {
            unsubscribeAndCleanup()
        }

        initialize(activity)

        activity.lifecycle.addObserver(observer)

        Log.info { "Tangem SDK owner registered" }
    }

    private fun tryToRegisterWithForegroundActivity(): TangemSdk =
        runBlocking(Dispatchers.Main.immediate) {
            val warning =
                "Tangem SDK holder is null, trying to recreate it with foreground activity"
            Log.warning { warning }

            val activity = backgroundManager.currentActivity

            if (activity == null) {
                val error = "Tangem SDK holder is null and foreground activity is null"
                Log.error { error }
                error(error)
            }

            register(activity)

            val sdk = holder?.sdk

            if (sdk == null) {
                val error = "Tangem SDK is null after re-registering with foreground activity"
                Log.error { error }
                error(error)
            }

            return@runBlocking sdk
        }

    private fun initialize(activity: FragmentActivity) {
        val secureStorage = SecureStorage.create(activity)
        val nfcManager = TangemSdk.initNfcManager(activity)
        val authenticationManager = TangemSdk.initAuthenticationManager(activity)
        val keystoreManager = TangemSdk.initKeystoreManager(authenticationManager, secureStorage)

        val viewDelegate = DefaultSessionViewDelegate(nfcManager, activity)
        viewDelegate.sdkConfig = config

        val androidNfcAvailabilityProvider = AndroidNfcAvailabilityProvider(activity)
        val sdk = TangemSdk(
            reader = nfcManager.reader,
            viewDelegate = viewDelegate,
            nfcAvailabilityProvider = androidNfcAvailabilityProvider,
            secureStorage = secureStorage,
            authenticationManager = authenticationManager,
            keystoreManager = keystoreManager,
            wordlist = Wordlist.getWordlist(activity),
            config = config,
        )

        holder = Holder(
            activity = activity,
            nfcManager = nfcManager,
            authenticationManager = authenticationManager,
            sdk = sdk,
        )

        Log.info { "Tangem SDK initialized" }
    }

    private fun unsubscribeAndCleanup() {
        val currentHolder = holder

        if (currentHolder == null) {
            Log.info { "Tangem SDK already unsubscribed and cleaned up" }
            return
        }

        with(currentHolder) {
            nfcManager.unsubscribe(activity)
            authenticationManager.unsubscribe(activity)

            activity.lifecycle.removeObserver(observer)
        }

        holder = null

        Log.info { "Tangem SDK unsubscribed and cleaned up" }
    }

    inner class Observer : DefaultLifecycleObserver {

        override fun onDestroy(owner: LifecycleOwner) {
            Log.info { "Tangem SDK owner destroyed" }

            unsubscribeAndCleanup()
        }
    }

    data class Holder(
        val activity: FragmentActivity,
        val sdk: TangemSdk,
        val nfcManager: NfcManager,
        val authenticationManager: AuthenticationManager,
    )

    private companion object {

        val config = Config(
            linkedTerminal = true,
            allowUntrustedCards = true,
            filter = CardFilter(
                allowedCardTypes = FirmwareVersion.FirmwareType.entries.toList(),
                maxFirmwareVersion = FirmwareVersion(major = 6, minor = 33),
                batchIdFilter = CardFilter.Companion.ItemFilter.Deny(
                    items = setOf("0027", "0030", "0031", "0035"),
                ),
            ),
        )
    }
}
