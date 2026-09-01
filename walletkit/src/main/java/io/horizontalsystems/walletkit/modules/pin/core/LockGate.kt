package io.horizontalsystems.walletkit.modules.pin.core

import io.horizontalsystems.walletkit.modules.main.MainModule.MainNavigation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Decides when the PIN keypad has to cover the screen.
 *
 * The app lock ([isLockedFlow]) is a single boolean, but the Market tab and its read-only
 * sub-pages (pages created with `HSPage(accessibleWhileLocked = true)`) may be browsed without
 * unlocking. The keypad is therefore shown only when the app is locked AND one of:
 *  - the Market tab is disabled in settings (nothing public to show, behave as a full lock),
 *  - the screen on top is not accessible while locked (a wallet page, or the main page with a
 *    non-Market tab selected),
 *  - the user asked for something that needs the wallet ([requireUnlocked]); the action is held
 *    and replayed once the PIN is entered, or dropped if the request is cancelled.
 *
 * The UI reports what is currently on screen through [selectedTab] and
 * [currentPageAccessibleWhileLocked]; everything else is derived. Pure Kotlin so it can be unit
 * tested without Android.
 */
class LockGate(
    private val isLockedFlow: StateFlow<Boolean>,
    private val marketsTabEnabledFlow: StateFlow<Boolean>,
    scope: CoroutineScope,
) {
    private val _showUnlockFlow = MutableStateFlow(false)

    /** True while the PIN keypad must be drawn over the content. */
    val showUnlockFlow: StateFlow<Boolean> = _showUnlockFlow.asStateFlow()
    val showUnlock: Boolean get() = _showUnlockFlow.value

    private val _unlockRequestedFlow = MutableStateFlow(false)

    /** True while the keypad is shown because of a [requireUnlocked] call (cancellable). */
    val unlockRequestedFlow: StateFlow<Boolean> = _unlockRequestedFlow.asStateFlow()
    val unlockRequested: Boolean get() = _unlockRequestedFlow.value

    private var pendingAction: (() -> Unit)? = null
    private val restrictedLockListeners = mutableListOf<() -> Unit>()

    val isLocked: Boolean get() = isLockedFlow.value

    /** Locked, but the Market tab is available so the app can stay usable in read-only mode. */
    val isRestricted: Boolean get() = isLocked && marketsTabEnabledFlow.value

    /** Main tab currently selected; null until the main screen reports one. */
    var selectedTab: MainNavigation? = null
        set(value) {
            field = value
            recompute()
        }

    /** Whether the page on top of the nav back stack may be shown while locked. */
    var currentPageAccessibleWhileLocked: Boolean = true
        set(value) {
            field = value
            recompute()
        }

    init {
        scope.launch {
            var wasLocked = isLockedFlow.value
            isLockedFlow.collect { locked ->
                if (!locked) {
                    val action = pendingAction
                    pendingAction = null
                    _unlockRequestedFlow.value = false
                    recompute()
                    action?.let { runDeferred(it) }
                } else {
                    if (!wasLocked && marketsTabEnabledFlow.value && currentPageAccessibleWhileLocked) {
                        // Locked while browsing something public: let the main screen fall back
                        // to the Market tab so the user is not faced with the keypad.
                        restrictedLockListeners.toList().forEach { runDeferred(it) }
                    }
                    recompute()
                }
                wasLocked = locked
            }
        }
        scope.launch {
            marketsTabEnabledFlow.collect { recompute() }
        }
    }

    fun isTabAccessibleWhileLocked(tab: MainNavigation): Boolean = tab == MainNavigation.Market

    /**
     * Runs [action] now if the wallet is available, otherwise shows the keypad and runs it right
     * after a successful unlock. A newer request replaces a pending one.
     */
    fun requireUnlocked(action: () -> Unit) {
        if (!isLocked) {
            action.invoke()
            return
        }
        pendingAction = action
        _unlockRequestedFlow.value = true
        recompute()
    }

    /** Dismisses a keypad shown by [requireUnlocked] and drops the held action. */
    fun cancelUnlockRequest() {
        pendingAction = null
        _unlockRequestedFlow.value = false
        recompute()
    }

    /**
     * Called when the app locks while a public screen is showing and the Market tab is enabled.
     * The main screen uses it to switch to the Market tab.
     */
    fun addRestrictedLockListener(listener: () -> Unit) {
        restrictedLockListeners.add(listener)
    }

    fun removeRestrictedLockListener(listener: () -> Unit) {
        restrictedLockListeners.remove(listener)
    }

    // A throwing callback must not cancel the isLockedFlow collector: the gate would stop
    // recomputing and showUnlockFlow would go stale on every later lock transition.
    private fun runDeferred(block: () -> Unit) {
        try {
            block.invoke()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "LockGate callback failed")
        }
    }

    private fun currentScreenAccessible(): Boolean {
        val tab = selectedTab
        return currentPageAccessibleWhileLocked && (tab == null || isTabAccessibleWhileLocked(tab))
    }

    private fun recompute() {
        _showUnlockFlow.value = when {
            !isLocked -> false
            !marketsTabEnabledFlow.value -> true
            unlockRequested -> true
            else -> !currentScreenAccessible()
        }
    }
}
