package io.horizontalsystems.walletkit.modules.walletconnect.stellar

import io.horizontalsystems.walletkit.modules.walletconnect.stellar.WCStellarTxSummary.AssetInfo
import io.horizontalsystems.walletkit.modules.walletconnect.stellar.WCStellarTxSummary.Op
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.stellar.sdk.Account
import org.stellar.sdk.Asset
import org.stellar.sdk.AssetTypeNative
import org.stellar.sdk.KeyPair
import org.stellar.sdk.Memo
import org.stellar.sdk.Network
import org.stellar.sdk.SignerKey
import org.stellar.sdk.Transaction
import org.stellar.sdk.TransactionBuilder
import org.stellar.sdk.operations.AccountMergeOperation
import org.stellar.sdk.operations.ManageDataOperation
import org.stellar.sdk.operations.Operation
import org.stellar.sdk.operations.PaymentOperation
import org.stellar.sdk.operations.SetOptionsOperation
import java.math.BigDecimal

class WCStellarTxSummaryTest {

    private val source = KeyPair.random().accountId
    private val destination = KeyPair.random().accountId
    private val issuer = KeyPair.random().accountId

    private fun transaction(vararg operations: Operation, memo: Memo? = null): Transaction {
        val builder = TransactionBuilder(Account(source, 1L), Network.PUBLIC)
            .setBaseFee(100)
            .setTimeout(0)
        operations.forEach { builder.addOperation(it) }
        memo?.let { builder.addMemo(it) }
        return builder.build()
    }

    @Test
    fun nativePayment_showsDestinationAmountAndMemo() {
        val tx = transaction(
            PaymentOperation.builder().destination(destination).asset(AssetTypeNative()).amount(BigDecimal("12.5")).build(),
            memo = Memo.text("order-42"),
        )

        val decoded = WCStellarTxSummary.decode(tx)

        assertEquals(source, decoded.sourceAccount)
        assertEquals("order-42", decoded.memo)
        assertEquals(
            listOf(Op.Payment(destination, BigDecimal("12.5000000"), AssetInfo("XLM", null), null)),
            decoded.operations
        )
        assertEquals("12.5 XLM", WCStellarTxSummary.formatAmount(BigDecimal("12.5000000"), AssetInfo("XLM", null)))
    }

    @Test
    fun creditAssetPayment_carriesIssuer() {
        val tx = transaction(
            PaymentOperation.builder()
                .destination(destination)
                .asset(Asset.createNonNativeAsset("USDC", issuer))
                .amount(BigDecimal("100"))
                .build()
        )

        val op = WCStellarTxSummary.decode(tx).operations.single() as Op.Payment

        assertEquals(AssetInfo("USDC", issuer), op.asset)
        assertEquals("100 USDC", WCStellarTxSummary.formatAmount(op.amount, op.asset))
    }

    @Test
    fun accountMerge_isDecodedWithDestination() {
        val tx = transaction(AccountMergeOperation.builder().destination(destination).build())

        assertEquals(Op.AccountMerge(destination, null), WCStellarTxSummary.decode(tx).operations.single())
    }

    @Test
    fun setOptions_carriesEveryFieldItChanges() {
        val newSigner = KeyPair.random().accountId
        val tx = transaction(
            SetOptionsOperation.builder()
                .signer(SignerKey.fromEd25519PublicKey(newSigner))
                .signerWeight(1)
                .masterKeyWeight(0)
                .lowThreshold(1)
                .mediumThreshold(1)
                .highThreshold(1)
                .homeDomain("example.com")
                .build()
        )

        assertEquals(
            Op.SetOptions(
                signer = newSigner,
                signerWeight = 1,
                masterKeyWeight = 0,
                lowThreshold = 1,
                mediumThreshold = 1,
                highThreshold = 1,
                setFlags = null,
                clearFlags = null,
                homeDomain = "example.com",
                inflationDestination = null,
                sourceAccount = null,
            ),
            WCStellarTxSummary.decode(tx).operations.single()
        )
    }

    @Test
    fun unsupportedOperation_isReportedAsUnknown() {
        val tx = transaction(ManageDataOperation.builder().name("k").value("v".toByteArray()).build())

        assertEquals(Op.Unknown("ManageData", null), WCStellarTxSummary.decode(tx).operations.single())
    }

    @Test
    fun noMemo_isNull() {
        val tx = transaction(
            PaymentOperation.builder().destination(destination).asset(AssetTypeNative()).amount(BigDecimal.ONE).build()
        )

        assertNull(WCStellarTxSummary.decode(tx).memo)
    }
}
