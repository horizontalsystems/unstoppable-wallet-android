package io.horizontalsystems.walletkit.modules.multiswap.providers

import io.horizontalsystems.walletkit.modules.multiswap.providers.ThorChainDepositRoute.SettlementTransfer
import io.horizontalsystems.walletkit.modules.multiswap.providers.UnstoppableAPI.Response.Delivery
import io.horizontalsystems.walletkit.modules.multiswap.providers.UnstoppableAPI.Response.Execution
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

class ThorChainDepositRouteTest {

    private val router = "0xD37BbE5744D730a1d98d8DC97c42F0Ca46aD7146"
    private val vault = "0x6b4068070d95d87b321909bb21c8979fba2b8876"
    private val memo = "=:b:bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq:27504:uws:100"

    private fun execution(
        method: String = ThorChainDepositRoute.METHOD,
        inboundAddress: String? = vault,
        memo: String? = this.memo,
        delivery: Delivery? = Delivery(kind = "evm_contract_call", router = router, approval = null, shieldedMemoAddress = null, unsignedTx = null),
    ) = Execution(
        method = method,
        chain = null,
        transactions = null,
        approval = null,
        depositAddress = null,
        amount = null,
        asset = null,
        attachment = null,
        unsignedTx = null,
        protocol = null,
        inboundAddress = inboundAddress,
        memo = memo,
        delivery = delivery,
    )

    private fun delivery(kind: String, shieldedMemoAddress: String? = null) =
        Delivery(kind = kind, router = null, approval = null, shieldedMemoAddress = shieldedMemoAddress, unsignedTx = null)

    @Test
    fun `evm target may be the router or the vault, compared ignoring case`() {
        ThorChainDepositRoute.requireEvmTarget(router.lowercase(), execution())
        ThorChainDepositRoute.requireEvmTarget(vault.uppercase(), execution())
    }

    @Test
    fun `evm target elsewhere is refused`() {
        assertThrows(IllegalStateException::class.java) {
            ThorChainDepositRoute.requireEvmTarget("0x000000000000000000000000000000000000dEaD", execution())
        }
    }

    @Test
    fun `evm target without router or vault is refused`() {
        assertThrows(IllegalStateException::class.java) {
            ThorChainDepositRoute.requireEvmTarget(router, execution(inboundAddress = "", delivery = delivery("evm_contract_call")))
        }
    }

    @Test
    fun `evm target is not checked on other methods`() {
        ThorChainDepositRoute.requireEvmTarget("0x000000000000000000000000000000000000dEaD", execution(method = "signed_transaction"))
    }

    @Test
    fun `settlement sell with cosmos memo and no vault is a deposit`() {
        val transfer = ThorChainDepositRoute.settlementTransfer(execution(inboundAddress = null, delivery = delivery("cosmos_memo")))
        assertEquals(SettlementTransfer.Deposit(memo), transfer)
    }

    @Test
    fun `settlement sell with a vault is a send to it, whatever the delivery kind`() {
        val transfer = ThorChainDepositRoute.settlementTransfer(execution(inboundAddress = "thor1vault", delivery = delivery("cosmos_memo")))
        assertEquals(SettlementTransfer.Send("thor1vault", memo), transfer)
    }

    @Test
    fun `settlement sell refuses a missing vault beside any other delivery`() {
        assertThrows(IllegalStateException::class.java) {
            ThorChainDepositRoute.settlementTransfer(execution(inboundAddress = "", delivery = delivery("utxo_op_return")))
        }
    }

    @Test
    fun `settlement sell refuses a route without a memo or of another method`() {
        assertThrows(IllegalStateException::class.java) {
            ThorChainDepositRoute.settlementTransfer(execution(inboundAddress = null, memo = null, delivery = delivery("cosmos_memo")))
        }
        assertThrows(IllegalStateException::class.java) {
            ThorChainDepositRoute.settlementTransfer(execution(method = "transfer"))
        }
    }

    @Test
    fun `zcash deposit pays the transparent vault and memos the shielded address`() {
        val deposit = ThorChainDepositRoute.shieldedMemoDeposit(
            execution(inboundAddress = "t1V5s2VMSip7qtBVZkpi1mzbnkh7pmeEqtZ", delivery = delivery("utxo_op_return", "u1fcwchlpy2u0t9hdl8wcku8"))
        )
        assertEquals(
            ThorChainDepositRoute.ShieldedMemoDeposit("t1V5s2VMSip7qtBVZkpi1mzbnkh7pmeEqtZ", "u1fcwchlpy2u0t9hdl8wcku8", memo),
            deposit,
        )
    }

    @Test
    fun `zcash deposit refuses a vault without a shielded memo address`() {
        assertThrows(IllegalStateException::class.java) {
            ThorChainDepositRoute.shieldedMemoDeposit(execution(inboundAddress = "t1V5s2VMSip7qtBVZkpi1mzbnkh7pmeEqtZ", delivery = delivery("utxo_op_return", "")))
        }
    }

    @Test
    fun `zcash deposit refuses a shielded inbound address`() {
        assertThrows(IllegalStateException::class.java) {
            ThorChainDepositRoute.shieldedMemoDeposit(execution(inboundAddress = "u1inbound", delivery = delivery("utxo_op_return", "u1fcwchlpy2u0t9hdl8wcku8")))
        }
    }

    // --- EVM calldata ---

    private fun token(type: TokenType, decimals: Int) = Token(
        coin = Coin("uid", "Name", "CODE"),
        blockchain = Blockchain(BlockchainType.Ethereum, "Ethereum", null),
        type = type,
        decimals = decimals,
    )

    private val eth = token(TokenType.Native, 18)
    private val usdt = token(TokenType.Eip20("0xdAC17F958D2ee523a2206206994597C13D831ec7"), 6)

    // depositWithExpiry(address,address,uint256,string,uint256) as the router ABI encodes it
    private fun depositWithExpiry(vaultAddress: String, asset: String, amount: BigInteger, memo: String, selector: String = "44bc937b"): ByteArray {
        fun word(hex: String) = hex.padStart(64, '0')
        val memoBytes = memo.toByteArray(Charsets.UTF_8)
        val memoHex = memoBytes.joinToString("") { "%02x".format(it) }.padEnd((memoBytes.size + 31) / 32 * 64, '0')
        val hex = selector +
            word(vaultAddress.removePrefix("0x")) +
            word(asset.removePrefix("0x")) +
            word(amount.toString(16)) +
            word((5 * 32).toString(16)) +
            word("1792135482".toBigInteger().toString(16)) +
            word(memoBytes.size.toString(16)) +
            memoHex
        return ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    }

    private val amountEth = BigDecimal("0.01")
    private val amountEthWei = BigInteger("10000000000000000")

    @Test
    fun `evm calldata of a native sell pays the vault the amount with the memo`() {
        val data = depositWithExpiry(vault, "0x0000000000000000000000000000000000000000", amountEthWei, memo)
        ThorChainDepositRoute.requireEvmCalldata(data, amountEthWei, execution(), eth, amountEth)
    }

    @Test
    fun `evm calldata of a token sell names the token and sends no value`() {
        val data = depositWithExpiry(vault, "0xdac17f958d2ee523a2206206994597c13d831ec7", BigInteger("600000000"), memo)
        ThorChainDepositRoute.requireEvmCalldata(data, BigInteger.ZERO, execution(), usdt, BigDecimal("600"))
    }

    @Test
    fun `evm calldata is refused when the vault, memo, amount or asset differ`() {
        val other = "0x000000000000000000000000000000000000dEaD"
        val zero = "0x0000000000000000000000000000000000000000"
        assertThrows(IllegalStateException::class.java) {
            ThorChainDepositRoute.requireEvmCalldata(depositWithExpiry(other, zero, amountEthWei, memo), amountEthWei, execution(), eth, amountEth)
        }
        assertThrows(IllegalStateException::class.java) {
            ThorChainDepositRoute.requireEvmCalldata(depositWithExpiry(vault, zero, amountEthWei, "=:b:$other:1:uws:100"), amountEthWei, execution(), eth, amountEth)
        }
        assertThrows(IllegalStateException::class.java) {
            ThorChainDepositRoute.requireEvmCalldata(depositWithExpiry(vault, zero, amountEthWei.shiftLeft(1), memo), amountEthWei, execution(), eth, amountEth)
        }
        assertThrows(IllegalStateException::class.java) {
            ThorChainDepositRoute.requireEvmCalldata(depositWithExpiry(vault, zero, amountEthWei, memo), BigInteger.ZERO, execution(), eth, amountEth)
        }
        assertThrows(IllegalStateException::class.java) {
            ThorChainDepositRoute.requireEvmCalldata(depositWithExpiry(vault, zero, BigInteger("600000000"), memo), BigInteger.ZERO, execution(), usdt, BigDecimal("600"))
        }
        assertThrows(IllegalStateException::class.java) {
            ThorChainDepositRoute.requireEvmCalldata(depositWithExpiry(vault, zero, amountEthWei, memo, selector = "a9059cbb"), amountEthWei, execution(), eth, amountEth)
        }
    }

    @Test
    fun `evm calldata with a memo offset or length outside the data is refused`() {
        val zero = "0x0000000000000000000000000000000000000000"
        val sane = depositWithExpiry(vault, zero, amountEthWei, memo)
        val hex = sane.joinToString("") { "%02x".format(it) }
        fun withWord(index: Int, value: String) = (hex.substring(0, 8 + index * 64) + value.padStart(64, '0') + hex.substring(8 + (index + 1) * 64))
            .let { h -> ByteArray(h.length / 2) { h.substring(it * 2, it * 2 + 2).toInt(16).toByte() } }
        // offset past the end, an offset that does not fit an Int, a length past the end
        for (data in listOf(withWord(3, "ffff"), withWord(3, "f".repeat(64)), withWord(5, "ffff"))) {
            assertThrows(IllegalStateException::class.java) {
                ThorChainDepositRoute.requireEvmCalldata(data, amountEthWei, execution(), eth, amountEth)
            }
        }
    }

    @Test
    fun `evm calldata is not checked on other methods`() {
        ThorChainDepositRoute.requireEvmCalldata(ByteArray(0), BigInteger.ZERO, execution(method = "signed_transaction"), eth, amountEth)
    }
}
