package io.horizontalsystems.walletkit.modules.multiswap.providers

import io.horizontalsystems.walletkit.modules.multiswap.providers.UnstoppableAPI.Response.Execution
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import java.math.BigDecimal
import java.math.BigInteger

/**
 * The rules a `thorchain_deposit` route must satisfy before its transaction is built. Pure
 * decisions on the route, kept apart from [USwapProvider] so they are testable without the app.
 */
object ThorChainDepositRoute {
    const val METHOD = "thorchain_deposit"

    /** A MsgDeposit on the settlement chain itself: the route names no vault. */
    const val COSMOS_MEMO_DELIVERY = "cosmos_memo"

    fun isVaultDeposit(execution: Execution?): Boolean = execution?.method == METHOD

    /**
     * A vault deposit must reach the vault: either the router's depositWithExpiry (native and
     * ERC-20 sells alike) or the inbound vault itself. Router, vault and `to` all come from the
     * same response, so this catches an inconsistent route, not a hostile server; the calldata
     * (vault, amount, memo) is not decoded. Routes of any other method pass untouched.
     */
    fun requireEvmTarget(to: String, execution: Execution) {
        if (execution.method != METHOD) return

        val allowed = listOfNotNull(execution.delivery?.router, execution.inboundAddress)
            .filter { it.isNotBlank() }
            .map { it.lowercase() }
        if (to.lowercase() !in allowed) {
            throw IllegalStateException("Vault deposit targets neither the router nor the vault")
        }
    }

    /**
     * The router call an EVM vault deposit must be: `depositWithExpiry(vault, asset, amount,
     * memo, expiry)` paying the inbound vault the previewed amount of the sold token with the
     * memo the route was checked against. The server builds the calldata, so this ties the
     * transaction that is signed to the memo and vault that were verified; a native sell carries
     * the amount as the call's value with the zero asset, an ERC-20 sell names the token and
     * sends no value. Routes of any other method pass untouched.
     */
    fun requireEvmCalldata(data: ByteArray, value: BigInteger, execution: Execution, tokenIn: Token, amountIn: BigDecimal) {
        if (execution.method != METHOD) return

        val hex = data.joinToString("") { "%02x".format(it) }
        if (!hex.startsWith(DEPOSIT_WITH_EXPIRY_SELECTOR) || hex.length < 8 + 64 * 5) {
            throw IllegalStateException("Vault deposit calldata is not depositWithExpiry")
        }
        fun word(index: Int) = hex.substring(8 + index * 64, 8 + (index + 1) * 64)
        val vault = "0x" + word(0).takeLast(40)
        val asset = "0x" + word(1).takeLast(40)
        val amount = BigInteger(word(2), 16)
        // The memo's offset and length come from the same untrusted calldata: both must stay
        // inside it, or the route is refused like any other inconsistent one.
        val memoOffset = 8 + boundedWord(word(3), hex.length / 2) * 2
        if (memoOffset + 64 > hex.length) throw IllegalStateException("Vault deposit calldata memo is out of range")
        val memoLength = boundedWord(hex.substring(memoOffset, memoOffset + 64), hex.length / 2) * 2
        if (memoOffset + 64 + memoLength > hex.length) throw IllegalStateException("Vault deposit calldata memo is out of range")
        val memo = String(hexToBytes(hex.substring(memoOffset + 64, memoOffset + 64 + memoLength)), Charsets.UTF_8)

        val inboundAddress = execution.inboundAddress?.lowercase() ?: throw IllegalStateException("No vault")
        if (vault != inboundAddress) throw IllegalStateException("Vault deposit calldata pays a different vault")
        if (memo != execution.memo) throw IllegalStateException("Vault deposit calldata carries a different memo")

        val expectedAmount = try {
            amountIn.movePointRight(tokenIn.decimals).toBigIntegerExact()
        } catch (e: ArithmeticException) {
            throw IllegalStateException("Amount has more decimals than the token")
        }
        if (amount != expectedAmount) throw IllegalStateException("Vault deposit calldata moves a different amount")
        when (val type = tokenIn.type) {
            TokenType.Native -> if (asset != ZERO_ADDRESS || value != expectedAmount) {
                throw IllegalStateException("Vault deposit calldata does not deposit the native coin")
            }
            is TokenType.Eip20 -> if (asset != type.address.lowercase() || value.signum() != 0) {
                throw IllegalStateException("Vault deposit calldata deposits a different token")
            }
            else -> throw IllegalStateException("Unsupported EVM token for a vault deposit")
        }
    }

    private const val DEPOSIT_WITH_EXPIRY_SELECTOR = "44bc937b"
    private const val ZERO_ADDRESS = "0x0000000000000000000000000000000000000000"

    // A word used as a byte offset or length within calldata of [limit] bytes.
    private fun boundedWord(word: String, limit: Int): Int {
        val value = BigInteger(word, 16)
        if (value.bitLength() > 31 || value.toInt() > limit) {
            throw IllegalStateException("Vault deposit calldata memo is out of range")
        }
        return value.toInt()
    }

    private fun hexToBytes(hex: String): ByteArray = ByteArray(hex.length / 2) { i ->
        hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }

    sealed class SettlementTransfer {
        /** MsgDeposit of the sell amount carrying the memo: the chain itself is the vault. */
        data class Deposit(val memo: String) : SettlementTransfer()

        /** Bank send to the inbound vault, carrying the memo. */
        data class Send(val inboundAddress: String, val memo: String) : SettlementTransfer()
    }

    /**
     * How a settlement-chain sell (RUNE, CACAO, a secured asset) executes the route. The server
     * labels every delivery that rides the chain's own memo field cosmos_memo, with or without
     * a vault: a route that names an inbound vault (Maya's THORChain inbound for RUNE) is a bank
     * send to it, one that names none is a MsgDeposit on the chain itself. A delivery of another
     * kind that names no vault contradicts itself and is refused rather than sent somewhere.
     */
    fun settlementTransfer(execution: Execution): SettlementTransfer {
        if (execution.method != METHOD) {
            throw IllegalStateException("Only vault deposits are supported on the settlement chains")
        }
        val memo = execution.memo ?: throw IllegalStateException("No THORChain deposit memo")
        val inboundAddress = execution.resolvedDepositAddress()
        val cosmosMemo = execution.delivery?.kind == COSMOS_MEMO_DELIVERY

        return when {
            inboundAddress != null -> SettlementTransfer.Send(inboundAddress, memo)
            cosmosMemo -> SettlementTransfer.Deposit(memo)
            else -> throw IllegalStateException("A ${execution.delivery?.kind} route names no vault")
        }
    }
}
