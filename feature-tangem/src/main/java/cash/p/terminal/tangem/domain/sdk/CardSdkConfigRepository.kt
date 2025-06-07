package cash.p.terminal.tangem.domain.sdk

import com.tangem.TangemSdk
import com.tangem.common.UserCodeType
import com.tangem.common.core.CardIdDisplayFormat
import com.tangem.common.core.UserCodeRequestPolicy

class CardSdkConfigRepository(
    private val cardSdkProvider: CardSdkProvider
) {

    val sdk: TangemSdk
        get() = cardSdkProvider.sdk

    var isBiometricsRequestPolicy: Boolean
        get() = sdk.config.userCodeRequestPolicy is UserCodeRequestPolicy.AlwaysWithBiometrics
        set(value) {
            sdk.config.userCodeRequestPolicy = if (value) {
                UserCodeRequestPolicy.AlwaysWithBiometrics(codeType = UserCodeType.AccessCode)
            } else {
                UserCodeRequestPolicy.Default
            }
        }

    fun setAccessCodeRequestPolicy(isBiometricsRequestPolicy: Boolean) {
        sdk.config.userCodeRequestPolicy = if (isBiometricsRequestPolicy) {
            UserCodeRequestPolicy.AlwaysWithBiometrics(codeType = UserCodeType.AccessCode)
        } else {
            UserCodeRequestPolicy.Default
        }
    }

    fun resetCardIdDisplayFormat() {
        sdk.config.cardIdDisplayFormat = CardIdDisplayFormat.Full
    }

   /* fun updateCardIdDisplayFormat(productType: ProductType) {
        sdk.config.cardIdDisplayFormat = when (productType) {
            ProductType.Twins -> CardIdDisplayFormat.LastLuhn(numbers = 4)
            ProductType.Note,
            ProductType.Wallet,
            ProductType.Wallet2,
            ProductType.Start2Coin,
            ProductType.Ring,
            ProductType.Visa,
                -> CardIdDisplayFormat.Full
        }
    }

    fun getCommonSigner(cardId: String?, twinKey: TwinKey?): TransactionSigner {
        return transactionSignerFactory.createTransactionSigner(cardId = cardId, sdk = sdk, twinKey = twinKey)
    }*/

    fun isLinkedTerminal() = sdk.config.linkedTerminal

    fun setLinkedTerminal(isLinked: Boolean?) {
        sdk.config.linkedTerminal = isLinked
    }
}