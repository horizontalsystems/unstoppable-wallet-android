package cash.p.terminal.modules.enablecoin.restoresettings

data class BirthdayHeightConfigUiState(
    val birthdayHeight: String,
    val restoreAsNew: Boolean,
    val closeWithResult: TokenConfig? = null,
    val errorHeight: String? = null,
)
