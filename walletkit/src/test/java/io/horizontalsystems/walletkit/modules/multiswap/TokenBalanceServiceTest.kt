package io.horizontalsystems.walletkit.modules.multiswap

import androidx.compose.runtime.Composable
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.BalanceData
import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.IAdapterManager
import io.horizontalsystems.walletkit.core.IBalanceAdapter
import io.horizontalsystems.walletkit.core.IReceiveAdapter
import io.horizontalsystems.walletkit.core.chain.ChainPlugin
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.chain.SpendPurpose
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.multiswap.action.ISwapProviderAction
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class TokenBalanceServiceTest {

    private class FakeAction(override val inProgress: Boolean) : ISwapProviderAction {
        @Composable
        override fun getTitle() = ""

        @Composable
        override fun getTitleInProgress() = ""

        @Composable
        override fun executor(navigation: HSNavigation, onActionCompleted: () -> Unit): () -> Unit = {}
    }

    private class FakeZcashPlugin : ChainPlugin {
        override val blockchainType = BlockchainType.Zcash
        override fun clearAccountData(accountId: String) = Unit

        var preparable: BigDecimal? = null
        var spendable: BigDecimal? = null
        var action: ISwapProviderAction? = null
        val requestedPurposes = mutableListOf<SpendPurpose>()

        override fun preparableBalance(token: Token) = preparable

        override fun spendableBalance(token: Token) = spendable

        override fun balancePreparationAction(
            token: Token,
            amount: BigDecimal,
            purpose: SpendPurpose,
        ): ISwapProviderAction? {
            requestedPurposes += purpose
            return action
        }
    }

    private class FakeBalanceAdapter(var available: BigDecimal) : IBalanceAdapter {
        override val balanceState: AdapterState = AdapterState.Synced
        override val balanceStateUpdatedFlow: Flow<Unit> = emptyFlow()
        override val balanceData: BalanceData get() = BalanceData(available)
        override val balanceUpdatedFlow: Flow<Unit> = emptyFlow()
    }

    private class FakeAdapterManager(private val adapter: IBalanceAdapter) : IAdapterManager {
        override val adaptersReadyFlow: Flow<Map<Wallet, IAdapter>> = emptyFlow()
        override fun startAdapterManager() = Unit
        override suspend fun refresh() = Unit
        override fun <T> getAdapterForWallet(wallet: Wallet): T? = null
        @Suppress("UNCHECKED_CAST")
        override fun <T> getAdapterForToken(token: Token): T? = adapter as T
        override fun getBalanceAdapterForWallet(wallet: Wallet): IBalanceAdapter? = null
        override fun getReceiveAdapterForWallet(wallet: Wallet): IReceiveAdapter? = null
        override fun refreshByWallet(wallet: Wallet) = Unit
    }

    private val zec = Token(
        coin = Coin("zcash", "Zcash", "ZEC"),
        blockchain = Blockchain(BlockchainType.Zcash, "Zcash", null),
        type = TokenType.Native,
        decimals = 8,
    )

    private lateinit var plugin: FakeZcashPlugin
    private lateinit var adapter: FakeBalanceAdapter
    private lateinit var service: TokenBalanceService

    @Before
    fun setUp() {
        plugin = FakeZcashPlugin()
        ChainRegistry.register(plugin)
        adapter = FakeBalanceAdapter(BigDecimal("2"))
        service = TokenBalanceService(FakeAdapterManager(adapter), SpendPurpose.Send)
        service.setToken(zec)
    }

    @Test
    fun balance_includesPreparableFunds() {
        plugin.preparable = BigDecimal("10")
        service.refresh()

        assertEquals(BigDecimal("12"), service.stateFlow.value.balance)
    }

    @Test
    fun amountWithinSpendable_needsNoAction() {
        plugin.preparable = BigDecimal("10")
        plugin.action = FakeAction(false)
        service.refresh()
        service.setAmount(BigDecimal("1.5"))

        val state = service.stateFlow.value
        assertNull(state.error)
        assertNull(state.preparationAction)
        assertEquals(emptyList<SpendPurpose>(), plugin.requestedPurposes)
    }

    @Test
    fun amountCoveredByPreparableFunds_offersAction() {
        val action = FakeAction(false)
        plugin.preparable = BigDecimal("10")
        plugin.action = action
        service.refresh()
        service.setAmount(BigDecimal("8.5"))

        val state = service.stateFlow.value
        assertNull(state.error)
        assertSame(action, state.preparationAction)
        assertEquals(listOf(SpendPurpose.Send), plugin.requestedPurposes)
    }

    @Test
    fun amountAboveAllFunds_isInsufficient() {
        plugin.preparable = BigDecimal("10")
        plugin.action = FakeAction(false)
        service.refresh()
        service.setAmount(BigDecimal("12.1"))

        val state = service.stateFlow.value
        assertEquals(SwapError.InsufficientBalanceFrom, state.error)
        assertNull(state.preparationAction)
    }

    @Test
    fun noActionFromChain_isInsufficient() {
        plugin.preparable = BigDecimal("10")
        plugin.action = null
        service.refresh()
        service.setAmount(BigDecimal("8.5"))

        assertEquals(SwapError.InsufficientBalanceFrom, service.stateFlow.value.error)
    }

    @Test
    fun preparedFundsBecomeSpendable_actionClears() {
        plugin.preparable = BigDecimal("10")
        plugin.action = FakeAction(true)
        service.refresh()
        service.setAmount(BigDecimal("8.5"))

        adapter.available = BigDecimal("12")
        plugin.preparable = BigDecimal.ZERO
        service.refresh()

        val state = service.stateFlow.value
        assertNull(state.error)
        assertNull(state.preparationAction)
    }

    @Test
    fun chainSpendable_narrowsAvailable() {
        // The wallet reports 2 available, but only 1.2 can be spent until a shielding confirms.
        val action = FakeAction(true)
        plugin.spendable = BigDecimal("1.2")
        plugin.preparable = BigDecimal("0.8")
        plugin.action = action
        service.refresh()
        service.setAmount(BigDecimal("1.5"))

        val state = service.stateFlow.value
        assertEquals(BigDecimal("2.0"), state.balance)
        assertNull(state.error)
        assertSame(action, state.preparationAction)
    }
}
