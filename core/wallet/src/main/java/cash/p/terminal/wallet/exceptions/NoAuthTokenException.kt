package cash.p.terminal.wallet.exceptions

class NoAuthTokenException(override val message: String = "Auth Token is not set or empty") : Exception()