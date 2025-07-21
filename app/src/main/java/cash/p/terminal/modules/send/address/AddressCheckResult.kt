package cash.p.terminal.modules.send.address

import cash.p.terminal.R

enum class AddressCheckResult {
    Clear,
    Detected,
    NotAvailable,
    NotAllowed,
    NotSupported;

    val title: Int
        get() = when (this) {
            Clear -> R.string.Send_Address_Error_Clear
            Detected -> R.string.Send_Address_Error_Detected
            NotAvailable -> R.string.NotAvailable
            NotAllowed -> R.string.NotAvailable
            NotSupported -> R.string.NotAvailable
        }
}
