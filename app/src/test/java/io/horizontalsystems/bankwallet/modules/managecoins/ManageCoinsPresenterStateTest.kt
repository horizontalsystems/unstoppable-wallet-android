package io.horizontalsystems.bankwallet.modules.managecoins

import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ManageCoinsPresenterStateTest {

    private lateinit var state: ManageCoinsModule.ManageCoinsPresenterState

    private val bitCoin = Coin("Bitcoin", "BTC", CoinType.Bitcoin)
    private val bitCashCoin = Coin("Bitcoin Cash", "BCH", CoinType.BitcoinCash)
    private val ethereumCoin = Coin("Ethereum", "ETH", CoinType.Ethereum)
    private val enabledCoins = mutableListOf(bitCoin, ethereumCoin)
    private val allCoins = mutableListOf(bitCoin, ethereumCoin, bitCashCoin)

    @Before
    fun setUp() {
        state = ManageCoinsModule.ManageCoinsPresenterState()
    }

    @Test
    fun add() {
        state.allCoins = allCoins
        state.enabledCoins = enabledCoins
        state.add(bitCashCoin)

        val expectedEnabledCoins = mutableListOf(bitCoin, ethereumCoin, bitCashCoin)
        Assert.assertEquals(state.enabledCoins, expectedEnabledCoins)
    }

    @Test
    fun remove() {
        state.allCoins = allCoins
        state.enabledCoins = enabledCoins
        state.remove(bitCoin)

        val expectedEnabledCoins = mutableListOf(ethereumCoin)
        Assert.assertEquals(state.enabledCoins, expectedEnabledCoins)
    }

    @Test
    fun move() {
        state.allCoins = allCoins
        state.enabledCoins = enabledCoins
        state.move(bitCoin, 1)

        val expectedEnabledCoins = mutableListOf(ethereumCoin, bitCoin)
        Assert.assertEquals(state.enabledCoins, expectedEnabledCoins)
    }

    @Test
    fun disabled() {
        state.allCoins = allCoins
        state.enabledCoins = mutableListOf(bitCoin, bitCashCoin)

        val expectedDisabledCoins = mutableListOf(ethereumCoin)
        Assert.assertEquals(expectedDisabledCoins, state.disabledCoins)
    }

}
