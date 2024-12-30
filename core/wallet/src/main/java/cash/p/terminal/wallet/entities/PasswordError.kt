package cash.p.terminal.wallet.entities

sealed class PasswordError : Throwable() {
    object PasswordInvalid : PasswordError()
}