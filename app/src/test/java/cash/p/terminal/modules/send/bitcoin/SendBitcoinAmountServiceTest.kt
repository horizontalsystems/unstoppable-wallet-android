package cash.p.terminal.modules.send.bitcoin

import cash.p.terminal.core.ISendBitcoinAdapter
import cash.p.terminal.modules.amount.AmountValidator
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.BalanceData
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.bitcoincore.storage.UtxoFilters
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class SendBitcoinAmountServiceTest {

    private val adapter = mockk<ISendBitcoinAdapter>()
    private val adapterManager = mockk<IAdapterManager>()

    @Test
    fun refreshAvailableBalance_litecoinMwebDynamicZeroAndAdjustedPositive_keepsAdjustedBalance() {
        val wallet = wallet(TokenType.Mweb)
        val service = service(wallet)
        every { adapter.availableBalance(1, null, null, null, null, false, any<UtxoFilters>()) } returns BigDecimal.ZERO
        every { adapterManager.getAdjustedBalanceData(wallet) } returns BalanceData(BigDecimal("0.02600000"))

        service.setFeeRate(1)

        assertEquals(BigDecimal("0.02600000"), service.stateFlow.value.availableBalance)
    }

    @Test
    fun refreshAvailableBalance_publicDynamicZeroAndAdjustedPositive_keepsDynamicZero() {
        val wallet = wallet(TokenType.Derived(TokenType.Derivation.Bip84))
        val service = service(wallet)
        every { adapter.availableBalance(1, null, null, null, null, false, any<UtxoFilters>()) } returns BigDecimal.ZERO
        every { adapterManager.getAdjustedBalanceData(wallet) } returns BalanceData(BigDecimal("0.02600000"))

        service.setFeeRate(1)

        assertEquals(BigDecimal.ZERO, service.stateFlow.value.availableBalance)
    }

    private fun service(wallet: Wallet): SendBitcoinAmountService {
        return SendBitcoinAmountService(
            adapter = adapter,
            coinCode = "LTC",
            amountValidator = AmountValidator(),
            adapterManager = adapterManager,
            wallet = wallet
        )
    }

    private fun wallet(tokenType: TokenType): Wallet {
        val token = mockk<Token> {
            every { coin } returns Coin(uid = "litecoin", name = "Litecoin", code = "LTC")
            every { type } returns tokenType
            every { blockchainType } returns BlockchainType.Litecoin
            every { decimals } returns 8
        }
        val account = mockk<Account> {
            every { id } returns "account-id"
        }

        return mockk {
            every { this@mockk.token } returns token
            every { this@mockk.account } returns account
        }
    }
}
