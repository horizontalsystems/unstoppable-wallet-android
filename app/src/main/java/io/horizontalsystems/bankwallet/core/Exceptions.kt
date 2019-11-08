package io.horizontalsystems.bankwallet.core

class UnsupportedAccountException : Exception()
class EosUnsupportedException : Exception()
class WrongAccountTypeForThisProvider : Exception()
class CoinException(val errorTextRes: Int?, val nonTranslatableText: String? = null) : Exception()

// Chart
class NoRateStats: Exception()
