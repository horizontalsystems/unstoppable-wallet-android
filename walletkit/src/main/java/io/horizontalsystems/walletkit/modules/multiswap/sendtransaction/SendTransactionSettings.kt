package io.horizontalsystems.walletkit.modules.multiswap.sendtransaction

import java.math.BigInteger

abstract class SendTransactionSettings {
    /**
     * The user's fee setting as a swap provider's backend can price a transaction with, or null
     * when the chain has no client-side fee setting. Kit-free so the swap layer can read it
     * without depending on a chain module.
     */
    open val networkFee: SwapNetworkFee? get() = null

    data class Btc(val feeRate: Int? = null) : SendTransactionSettings() {
        override val networkFee: SwapNetworkFee?
            get() = feeRate?.let { SwapNetworkFee.Utxo(it) }
    }

    class Tron : SendTransactionSettings()
    class Stellar : SendTransactionSettings()
    class Xrp : SendTransactionSettings()
    class Near : SendTransactionSettings()
    class Ton : SendTransactionSettings()
    class Zcash : SendTransactionSettings()
    class Monero : SendTransactionSettings()
    class Thorchain : SendTransactionSettings()
    class Zano : SendTransactionSettings()
    class Solana : SendTransactionSettings()
}

/** A fee setting in the units the chain prices transactions with. */
sealed class SwapNetworkFee {
    /** EVM legacy gas price, in wei. */
    data class Legacy(val gasPrice: BigInteger) : SwapNetworkFee()

    /** EVM EIP-1559 pair, in wei. */
    data class Eip1559(val maxFeePerGas: BigInteger, val maxPriorityFeePerGas: BigInteger) : SwapNetworkFee()

    /** UTXO fee rate, in sat/vB. */
    data class Utxo(val feeRate: Int) : SwapNetworkFee()
}
