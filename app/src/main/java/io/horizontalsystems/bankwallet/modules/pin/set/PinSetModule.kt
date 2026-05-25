package io.horizontalsystems.bankwallet.modules.pin.set

object PinSetModule {

    enum class SetStage {
        Enter,
        Confirm
    }

    data class PinSetViewState(
        val stage: SetStage,
        val enteredCount: Int,
        val finished: Boolean,
        val reverseSlideAnimation: Boolean,
        val error: String?,
    )

}