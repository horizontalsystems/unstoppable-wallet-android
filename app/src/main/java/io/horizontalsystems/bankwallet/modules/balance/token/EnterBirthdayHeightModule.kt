package io.horizontalsystems.bankwallet.modules.balance.token

import java.time.LocalDate

object EnterBirthdayHeightModule {

    data class UiState(
        val birthdayHeight: Long? = null,
        val birthdayHeightText: String? = null,
        val blockDateText: String? = null,
        val rescanButtonEnabled: Boolean = false,
        val rescanLoading: Boolean = false,
        val closeAfterRescan: Boolean = false,
        val firstBlockDate: LocalDate? = null
    )
}
