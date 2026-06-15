package cash.p.terminal.modules.balance

import cash.p.terminal.core.adapters.zcash.ZcashAdapter
import cash.p.terminal.modules.displayoptions.DisplayDiffOptionType
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.WalletFactory
import cash.p.terminal.wallet.balance.BalanceItem
import cash.p.terminal.wallet.balance.BalanceViewType
import cash.p.terminal.wallet.entities.BalanceData
import cash.p.terminal.wallet.zcashTransparentWallet
import io.horizontalsystems.core.IAppNumberFormatter
import io.horizontalsystems.core.entities.Currency
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.math.BigDecimal

class BalanceViewItemFactoryDisplaySyncStateTest {

    private val numberFormatter = mockk<IAppNumberFormatter>()
    private val factory = BalanceViewItemFactory()
    private val currency = Currency("USD", "$", 2, 0)
    private val wallet = WalletFactory.previewWallet()

    @Before
    fun setUp() {
        stopKoin()
        every { numberFormatter.formatCoinFull(any(), any(), any()) } returns "1"

        startKoin {
            modules(
                module {
                    single { numberFormatter }
                }
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
        unmockkAll()
    }

    @Test
    fun viewItem_balanceSyncedTxNotSynced_displaysTxErrorWithoutBlockingActions() {
        val viewItem = factory.viewItem(
            item = balanceItem(transactionsSyncState = AdapterState.NotSynced(historyError)),
            currency = currency,
            hideBalance = false,
            watchAccount = false,
            balanceViewType = BalanceViewType.CoinThenFiat,
            isSwappable = true,
            displayDiffOptionType = DisplayDiffOptionType.BOTH,
        )

        assertTrue(viewItem.failedIconVisible)
        assertEquals(historyError.message, viewItem.errorMessage)
        assertTrue(viewItem.sendEnabled)
        assertTrue(viewItem.swapEnabled)
        assertFalse(viewItem.primaryValue.dimmed)
    }

    @Test
    fun viewItem2_balanceSyncedTxNotSynced_displaysTxErrorWithoutBlockingActions() {
        val viewItem = factory.viewItem2(
            item = balanceItem(transactionsSyncState = AdapterState.NotSynced(historyError)),
            currency = currency,
            roundingAmount = false,
            hideBalance = false,
            watchAccount = false,
            isSwipeToDeleteEnabled = true,
            balanceViewType = BalanceViewType.CoinThenFiat,
            networkAvailable = true,
            showStackingUnpaid = false,
            displayDiffOptionType = DisplayDiffOptionType.BOTH,
        )

        assertTrue(viewItem.failedIconVisible)
        assertEquals(historyError.message, viewItem.errorMessage)
        assertTrue(viewItem.sendEnabled)
        assertTrue(viewItem.swapEnabled)
        assertFalse(viewItem.primaryValue.dimmed)
    }

    @Test
    fun viewItem_transparentZcashPendingOnly_hidesShieldFunds() {
        val viewItem = factory.viewItem(
            item = balanceItem(
                wallet = zcashTransparentWallet(),
                balanceData = BalanceData(
                    available = BigDecimal.ZERO,
                    pending = ZcashAdapter.MINERS_FEE + BigDecimal.ONE
                )
            ),
            currency = currency,
            hideBalance = false,
            watchAccount = false,
            balanceViewType = BalanceViewType.CoinThenFiat,
            isSwappable = true,
            displayDiffOptionType = DisplayDiffOptionType.BOTH,
        )

        assertFalse(viewItem.isShowShieldFunds)
    }

    @Test
    fun viewItem_transparentZcashAvailableAboveFee_showsShieldFunds() {
        val viewItem = factory.viewItem(
            item = balanceItem(
                wallet = zcashTransparentWallet(),
                balanceData = BalanceData(
                    available = ZcashAdapter.MINERS_FEE + BigDecimal.ONE,
                    pending = BigDecimal.ZERO
                )
            ),
            currency = currency,
            hideBalance = false,
            watchAccount = false,
            balanceViewType = BalanceViewType.CoinThenFiat,
            isSwappable = true,
            displayDiffOptionType = DisplayDiffOptionType.BOTH,
        )

        assertTrue(viewItem.isShowShieldFunds)
    }

    private fun balanceItem(
        state: AdapterState = AdapterState.Synced,
        transactionsSyncState: AdapterState = state,
        wallet: Wallet = this.wallet,
        balanceData: BalanceData = BalanceData(available = BigDecimal.ONE),
    ) = BalanceItem(
        wallet = wallet,
        balanceData = balanceData,
        state = state,
        sendAllowed = true,
        coinPrice = null,
        transactionsSyncState = transactionsSyncState,
    )

    private companion object {
        val historyError = Exception("history endpoint down")
    }
}
