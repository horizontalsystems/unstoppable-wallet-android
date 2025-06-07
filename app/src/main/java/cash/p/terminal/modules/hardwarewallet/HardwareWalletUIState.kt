package cash.p.terminal.modules.hardwarewallet

sealed interface HardwareWalletError {
    object CardNotActivated : HardwareWalletError
}