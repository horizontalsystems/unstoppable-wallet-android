package io.horizontalsystems.bankwallet.modules.managecoins

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import io.horizontalsystems.bankwallet.core.Wallet
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ManageCoinsPresenterStateTest {

    private lateinit var state: ManageWalletsModule.ManageWalletsPresenterState

    private val bitCoin = Coin("Bitcoin", "BTC", CoinType.Bitcoin)
    private val bitCashCoin = Coin("Bitcoin Cash", "BCH", CoinType.BitcoinCash)
    private val ethereumCoin = Coin("Ethereum", "ETH", CoinType.Ethereum)
    private val allCoins = mutableListOf(bitCoin, ethereumCoin, bitCashCoin)

    private val wallet1 = mock<Wallet> { on { coin } doReturn bitCoin }
    private val wallet2 = mock<Wallet> { on { coin } doReturn ethereumCoin }
    private val wallet3 = mock<Wallet> { on { coin } doReturn bitCashCoin }
    private val enabledCoins = mutableListOf(wallet1, wallet2)

    @Before
    fun setUp() {
        state = ManageWalletsModule.ManageWalletsPresenterState()
    }

    @Test
    fun enable() {
        state.allCoins = allCoins
        state.enabledCoins = enabledCoins
        state.enable(wallet3)

        Assert.assertEquals(state.enabledCoins, mutableListOf(wallet1, wallet2, wallet3))
    }

    @Test
    fun disable() {
        state.allCoins = allCoins
        state.enabledCoins = enabledCoins
        state.disable(wallet1)

        val expectedEnabledCoins = mutableListOf(wallet2)
        Assert.assertEquals(state.enabledCoins, expectedEnabledCoins)
    }

    @Test
    fun move() {
        state.allCoins = allCoins
        state.enabledCoins = enabledCoins
        state.move(wallet1, 1)

        val expectedEnabledCoins = mutableListOf(wallet2, wallet1)
        Assert.assertEquals(state.enabledCoins, expectedEnabledCoins)
    }

    @Test
    fun disabled() {
        state.allCoins = allCoins
        state.enabledCoins = mutableListOf(wallet1, wallet3)

        val expectedDisabledCoins = mutableListOf(ethereumCoin)
        Assert.assertEquals(expectedDisabledCoins, state.disabledCoins)
    }

}
