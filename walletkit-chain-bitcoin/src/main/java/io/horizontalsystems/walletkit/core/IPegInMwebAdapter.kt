package io.horizontalsystems.walletkit.core

import io.horizontalsystems.litecoinkit.LitecoinKit
import java.math.BigDecimal

/**
 * The Litecoin MWEB wallet's side of a peg-in: a Litecoin wallet of the same account pays
 * an MWEB address, and the MWEB daemon turns that payment into a peg-in transaction and
 * broadcasts it. Implemented by the MWEB adapter, used by the Litecoin send service.
 * [feeRate] is litoshi per virtual byte; amounts and fees are in LTC.
 */
interface IPegInMwebAdapter {
    /** The MWEB part of a peg-in fee (one kernel, one output), paid out of the Litecoin inputs. Pure arithmetic, safe on any thread. */
    fun pegInMwebFee(feeRate: Int): BigDecimal

    /** Litecoin fee plus MWEB fee for moving [amount] from [litecoinKit]; throws when the balance does not cover it. */
    suspend fun estimatePegInFee(litecoinKit: LitecoinKit, amount: BigDecimal, feeRate: Int): BigDecimal

    /** Builds, signs and broadcasts the peg-in; returns the Litecoin transaction hash. */
    suspend fun pegIn(litecoinKit: LitecoinKit, address: String, amount: BigDecimal, feeRate: Int): String
}
