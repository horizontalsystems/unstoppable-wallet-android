package cash.p.terminal.modules.hardwarewallet

import cash.p.terminal.wallet.AccountType

sealed interface HardwareWalletError {
    object CardNotActivated : HardwareWalletError
}