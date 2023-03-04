package io.horizontalsystems.bankwallet.modules.pin

import android.os.Bundle
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

object PinModule {

    const val RESULT_OK = 1
    const val RESULT_CANCELLED = 2
    const val PIN_COUNT = 6

    const val keyAttachedToLockScreen = "attached_to_lock_screen"
    const val keyInteractionType = "interaction_type"
    const val keyShowCancel = "show_cancel"
    const val requestKey = "pin_request_key"
    const val requestType = "pin_request_type"
    const val requestResult = "pin_request_result"

    fun forSetPin(): Bundle {
        return arguments(PinInteractionType.SET_PIN, true)
    }

    fun forEditPin(): Bundle {
        return arguments(PinInteractionType.EDIT_PIN, false)
    }

    fun forUnlock(): Bundle {
        return arguments(PinInteractionType.UNLOCK, true)
    }

    private fun arguments(interactionType: PinInteractionType, showCancel: Boolean) =
        Bundle(2).apply {
            putParcelable(keyInteractionType, interactionType)
            putBoolean(keyShowCancel, showCancel)
        }
}

@Parcelize
enum class PinInteractionType : Parcelable {
    SET_PIN,
    UNLOCK,
    EDIT_PIN
}
