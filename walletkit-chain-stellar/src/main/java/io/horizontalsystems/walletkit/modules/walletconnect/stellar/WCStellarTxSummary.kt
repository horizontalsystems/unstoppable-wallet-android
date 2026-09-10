package io.horizontalsystems.walletkit.modules.walletconnect.stellar

import org.stellar.sdk.Asset
import org.stellar.sdk.AssetTypeCreditAlphaNum
import org.stellar.sdk.AssetTypeNative
import org.stellar.sdk.MemoHash
import org.stellar.sdk.MemoId
import org.stellar.sdk.MemoReturnHash
import org.stellar.sdk.MemoText
import org.stellar.sdk.Transaction
import org.stellar.sdk.operations.AccountMergeOperation
import org.stellar.sdk.operations.ChangeTrustOperation
import org.stellar.sdk.operations.CreateAccountOperation
import org.stellar.sdk.operations.ManageBuyOfferOperation
import org.stellar.sdk.operations.ManageSellOfferOperation
import org.stellar.sdk.operations.Operation
import org.stellar.sdk.operations.PathPaymentStrictReceiveOperation
import org.stellar.sdk.operations.PathPaymentStrictSendOperation
import org.stellar.sdk.operations.PaymentOperation
import org.stellar.sdk.operations.SetOptionsOperation
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Pure (Android-free) decoder of a Stellar transaction into what the WalletConnect sign screen
 * must show: per operation the destination, amount and asset, plus the transaction memo and
 * source account. Operation types this does not understand are reported as [Op.Unknown] so the
 * caller can warn about blind signing instead of showing only a class name.
 */
object WCStellarTxSummary {

    data class AssetInfo(val code: String, val issuer: String?) {
        val isNative: Boolean get() = issuer == null
    }

    sealed class Op {
        abstract val sourceAccount: String?

        data class Payment(val destination: String, val amount: BigDecimal, val asset: AssetInfo, override val sourceAccount: String?) : Op()
        data class CreateAccount(val destination: String, val startingBalance: BigDecimal, override val sourceAccount: String?) : Op()

        /** Sends the account's ENTIRE balance to [destination] and deletes the account. */
        data class AccountMerge(val destination: String, override val sourceAccount: String?) : Op()

        data class PathPaymentStrictSend(
            val destination: String,
            val sendAmount: BigDecimal,
            val sendAsset: AssetInfo,
            val destMin: BigDecimal,
            val destAsset: AssetInfo,
            override val sourceAccount: String?,
        ) : Op()

        data class PathPaymentStrictReceive(
            val destination: String,
            val sendMax: BigDecimal,
            val sendAsset: AssetInfo,
            val destAmount: BigDecimal,
            val destAsset: AssetInfo,
            override val sourceAccount: String?,
        ) : Op()

        /** [asset] is null for a liquidity-pool share trustline. */
        data class ChangeTrust(val asset: AssetInfo?, val limit: BigDecimal, override val sourceAccount: String?) : Op()

        data class ManageOffer(
            val isBuy: Boolean,
            val selling: AssetInfo,
            val buying: AssetInfo,
            val amount: BigDecimal,
            val price: BigDecimal,
            override val sourceAccount: String?,
        ) : Op()

        /**
         * Changes signers / thresholds / flags — can hand control of the account to someone else.
         * Every field the operation sets is carried so the screen can show exactly what changes;
         * a null field is left untouched by the operation.
         */
        data class SetOptions(
            val signer: String?,
            val signerWeight: Int?,
            val masterKeyWeight: Int?,
            val lowThreshold: Int?,
            val mediumThreshold: Int?,
            val highThreshold: Int?,
            val setFlags: Int?,
            val clearFlags: Int?,
            val homeDomain: String?,
            val inflationDestination: String?,
            override val sourceAccount: String?,
        ) : Op()

        data class Unknown(val typeName: String, override val sourceAccount: String?) : Op()
    }

    data class Decoded(
        val sourceAccount: String,
        /** Display form of the memo, or null for MemoNone. */
        val memo: String?,
        val operations: List<Op>,
    )

    fun decode(transaction: Transaction): Decoded = Decoded(
        sourceAccount = transaction.sourceAccount,
        memo = memoText(transaction),
        operations = transaction.operations.map { decode(it) },
    )

    private fun decode(operation: Operation): Op {
        val source = operation.sourceAccount
        return when (operation) {
            is PaymentOperation -> Op.Payment(operation.destination, operation.amount, asset(operation.asset), source)
            is CreateAccountOperation -> Op.CreateAccount(operation.destination, operation.startingBalance, source)
            is AccountMergeOperation -> Op.AccountMerge(operation.destination, source)
            is PathPaymentStrictSendOperation -> Op.PathPaymentStrictSend(
                operation.destination, operation.sendAmount, asset(operation.sendAsset), operation.destMin, asset(operation.destAsset), source
            )
            is PathPaymentStrictReceiveOperation -> Op.PathPaymentStrictReceive(
                operation.destination, operation.sendMax, asset(operation.sendAsset), operation.destAmount, asset(operation.destAsset), source
            )
            is ChangeTrustOperation -> Op.ChangeTrust(operation.asset.asset?.let { asset(it) }, operation.limit, source)
            is ManageSellOfferOperation -> Op.ManageOffer(
                false, asset(operation.selling), asset(operation.buying), operation.amount, price(operation.price.numerator, operation.price.denominator), source
            )
            is ManageBuyOfferOperation -> Op.ManageOffer(
                true, asset(operation.selling), asset(operation.buying), operation.amount, price(operation.price.numerator, operation.price.denominator), source
            )
            is SetOptionsOperation -> Op.SetOptions(
                signer = operation.signer?.encodedSignerKey,
                signerWeight = operation.signerWeight,
                masterKeyWeight = operation.masterKeyWeight,
                lowThreshold = operation.lowThreshold,
                mediumThreshold = operation.mediumThreshold,
                highThreshold = operation.highThreshold,
                setFlags = operation.setFlags,
                clearFlags = operation.clearFlags,
                homeDomain = operation.homeDomain,
                inflationDestination = operation.inflationDestination,
                sourceAccount = source,
            )
            else -> Op.Unknown(operation.javaClass.simpleName.removeSuffix("Operation"), source)
        }
    }

    private fun asset(asset: Asset): AssetInfo = when (asset) {
        is AssetTypeNative -> AssetInfo("XLM", null)
        is AssetTypeCreditAlphaNum -> AssetInfo(asset.code, asset.issuer)
        else -> AssetInfo(asset.toString(), null)
    }

    private fun price(numerator: Int, denominator: Int): BigDecimal =
        if (denominator == 0) BigDecimal.ZERO
        else BigDecimal(numerator).divide(BigDecimal(denominator), 7, RoundingMode.HALF_UP)

    private fun memoText(transaction: Transaction): String? = when (val memo = transaction.memo) {
        is MemoText -> memo.text
        is MemoId -> memo.id.toString()
        is MemoHash -> memo.hexValue
        is MemoReturnHash -> memo.hexValue
        else -> null
    }

    fun formatAmount(amount: BigDecimal, asset: AssetInfo): String =
        "${amount.stripTrailingZeros().toPlainString()} ${asset.code}"
}
