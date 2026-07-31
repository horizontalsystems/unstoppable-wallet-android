package io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.stellar

import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.stellarkit.StellarKit
import io.horizontalsystems.stellarkit.room.StellarAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal

interface IStellarSender {
    fun getFee(): BigDecimal?

    // Returns the hash Horizon reported for the submitted transaction — the server's
    // StellarTracker verifies DEX swaps on Horizon by it.
    suspend fun sendTransaction(): String
}

class StellarSenderRegular(
    private val address: String,
    private val amount: BigDecimal,
    private val memo: String,
    private val stellarKit: StellarKit,
    private val token: Token
) : IStellarSender {
    override fun getFee() = stellarKit.sendFee

    override suspend fun sendTransaction(): String = withContext(Dispatchers.IO) {
        val memo = memo.ifBlank { null }

        when (val tokenType = token.tokenQuery.tokenType) {
            is TokenType.Native -> {
                stellarKit.sendNative(address, amount, memo)
            }

            is TokenType.Asset -> {
                val stellarAsset = StellarAsset.Asset(tokenType.code, tokenType.issuer)
                stellarKit.sendAsset(stellarAsset.id, address, amount, memo)
            }

            else -> throw IllegalArgumentException("Unsupported token type $tokenType")
        }
    }
}

class StellarSenderTransactionEnvelope(
    private val transactionEnvelope: String,
    private val stellarKit: StellarKit
) : IStellarSender {
    override fun getFee() = StellarKit.estimateFee(transactionEnvelope)

    override suspend fun sendTransaction(): String = withContext(Dispatchers.IO) {
        stellarKit.sendTransaction(transactionEnvelope)
    }
}
