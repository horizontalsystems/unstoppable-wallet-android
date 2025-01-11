package cash.p.terminal.wallet.exceptions

class InvalidAuthTokenException(override val message: String = "Auth Token is expired or invalid") : Exception()