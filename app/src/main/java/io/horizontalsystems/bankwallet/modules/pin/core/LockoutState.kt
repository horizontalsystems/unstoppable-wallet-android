package io.horizontalsystems.bankwallet.modules.pin.core

import java.util.Date

sealed class LockoutState {
    data class Unlocked(val attemptsLeft: Int?) : LockoutState()
    data class Locked(val until: Date) : LockoutState()
}
