package cash.p.terminal.tangem.domain.sdk

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.tangem.Log
import com.tangem.TangemSdk
import com.tangem.common.authentication.AuthenticationManager
import com.tangem.sdk.extensions.unsubscribe
import com.tangem.sdk.nfc.NfcManager
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.IPinComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class CardSdkProvider(
    private val backgroundManager: BackgroundManager,
    private val pinComponent: IPinComponent,
    private val sdkInitializer: SdkInitializer = DefaultSdkInitializer,
) {

    private val observer = Observer()

    private var holder: Holder? = null
    private var lockObservationScope: CoroutineScope? = null

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
        observeLockState()

        Log.info { "Tangem SDK owner registered" }
    }

    fun cancelSession() {
        holder?.nfcManager?.reader?.stopSession(cancelled = true)
    }

    // Closes the active NFC reading dialog when the app is locked: the dialog
    // lives in its own Window and would otherwise overlay the PIN unlock screen.
    private fun observeLockState() {
        val scope = MainScope()
        lockObservationScope = scope
        // UNDISPATCHED ensures the collector is subscribed before register() returns,
        // so a lock emitted right after registration is not missed.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            pinComponent.isLockedFlow.collect { isLocked ->
                if (isLocked) cancelSession()
            }
        }
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
        val components = sdkInitializer.create(activity)

        holder = Holder(
            activity = activity,
            nfcManager = components.nfcManager,
            authenticationManager = components.authenticationManager,
            sdk = components.sdk,
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
            nfcManager.onStop(activity)
            nfcManager.onDestroy(activity)
            nfcManager.unsubscribe(activity)
            authenticationManager.unsubscribe(activity)

            activity.lifecycle.removeObserver(observer)
        }

        lockObservationScope?.cancel()
        lockObservationScope = null

        holder = null

        Log.info { "Tangem SDK unsubscribed and cleaned up" }
    }

    inner class Observer : DefaultLifecycleObserver {

        override fun onDestroy(owner: LifecycleOwner) {
            Log.info { "Tangem SDK owner destroyed" }

            val currentHolder = holder ?: return
            if (currentHolder.activity !== owner) return

            currentHolder.activity.lifecycle.removeObserver(observer)
            lockObservationScope?.cancel()
            lockObservationScope = null
            holder = null
        }
    }

    data class Holder(
        val activity: FragmentActivity,
        val sdk: TangemSdk,
        val nfcManager: NfcManager,
        val authenticationManager: AuthenticationManager,
    )
}
