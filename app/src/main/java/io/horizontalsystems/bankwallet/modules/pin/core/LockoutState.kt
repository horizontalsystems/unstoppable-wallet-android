package io.horizontalsystems.bankwallet.modules.pin.core

import java.util.*

sealed class LockoutState {
    data class Unlocked(val hasFailedAttempts: Boolean) : LockoutState()
    data class Locked(val until: Date) : LockoutState()
}
