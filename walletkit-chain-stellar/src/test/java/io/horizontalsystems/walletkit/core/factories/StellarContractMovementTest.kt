package io.horizontalsystems.walletkit.core.factories

import io.horizontalsystems.stellarkit.room.Operation.ContractBalanceChange
import io.horizontalsystems.stellarkit.room.StellarAsset
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class StellarContractMovementTest {

    private val account = "GACCOUNT"
    private val other = "GOTHER"
    private val usdc = StellarAsset.Asset("USDC", "GISSUER")

    private fun change(from: String?, to: String?, asset: StellarAsset, amount: String) =
        ContractBalanceChange("transfer", from, to, asset, BigDecimal(amount))

    @Test
    fun soldOneAssetReceivedAnother_isSwap() {
        val movement = StellarContractMovement.resolve(
            listOf(
                change(account, other, StellarAsset.Native, "5"),
                change(other, account, usdc, "0.86"),
            ),
            account
        )

        assertEquals(
            StellarContractMovement.Swap(
                sold = StellarContractMovement.Movement(StellarAsset.Native, BigDecimal("-5")),
                received = StellarContractMovement.Movement(usdc, BigDecimal("0.86")),
            ),
            movement
        )
    }

    @Test
    fun repeatedMovementsOfOneAsset_areSummed() {
        val movement = StellarContractMovement.resolve(
            listOf(
                change(account, other, StellarAsset.Native, "3"),
                change(account, other, StellarAsset.Native, "2"),
                change(other, account, usdc, "0.86"),
            ),
            account
        )

        val swap = movement as StellarContractMovement.Swap
        assertEquals(BigDecimal("-5"), swap.sold.amount)
    }

    @Test
    fun assetOnBothSides_isNetted() {
        // 5 XLM out, 1 XLM refunded back within the same call: net 4 sold.
        val movement = StellarContractMovement.resolve(
            listOf(
                change(account, other, StellarAsset.Native, "5"),
                change(other, account, StellarAsset.Native, "1"),
                change(other, account, usdc, "0.70"),
            ),
            account
        )

        val swap = movement as StellarContractMovement.Swap
        assertEquals(BigDecimal("-4"), swap.sold.amount)
    }

    @Test
    fun multipleDistinctAssetsOneDirection_isUnrepresentable() {
        val movement = StellarContractMovement.resolve(
            listOf(
                change(account, other, StellarAsset.Native, "5"),
                change(account, other, usdc, "1"),
            ),
            account
        )

        assertEquals(StellarContractMovement.Unrepresentable, movement)
    }

    @Test
    fun oneDirectionalMovement_isTransferWithCounterparty() {
        val movement = StellarContractMovement.resolve(
            listOf(change(account, other, StellarAsset.Native, "5")),
            account
        )

        assertEquals(
            StellarContractMovement.Outgoing(
                movement = StellarContractMovement.Movement(StellarAsset.Native, BigDecimal("-5")),
                counterparty = other,
            ),
            movement
        )
    }

    @Test
    fun changesNotTouchingTheAccount_areNone() {
        val movement = StellarContractMovement.resolve(
            listOf(change(other, "GTHIRD", StellarAsset.Native, "5")),
            account
        )

        assertEquals(StellarContractMovement.None, movement)
    }
}
