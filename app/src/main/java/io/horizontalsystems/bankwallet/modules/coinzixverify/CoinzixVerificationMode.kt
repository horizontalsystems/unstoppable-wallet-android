package io.horizontalsystems.bankwallet.modules.coinzixverify

sealed class CoinzixVerificationMode {
    abstract val twoFactorTypes: List<TwoFactorType>

    data class Login(val token: String, val secret: String, override val twoFactorTypes: List<TwoFactorType>) : CoinzixVerificationMode()
    data class Withdraw(val withdrawId: String, override val twoFactorTypes: List<TwoFactorType>) : CoinzixVerificationMode()
}

enum class TwoFactorType(val code: Int) {
    Email(1), Authenticator(2);

    companion object {
        val map = TwoFactorType.values().associateBy(TwoFactorType::code)
        fun fromCode(code: Int?): TwoFactorType? = map[code]
    }
}
