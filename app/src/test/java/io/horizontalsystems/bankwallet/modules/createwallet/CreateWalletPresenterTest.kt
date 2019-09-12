package io.horizontalsystems.bankwallet.modules.createwallet

import com.nhaarman.mockito_kotlin.*
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.FeaturedCoin
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object CreateWalletPresenterTest : Spek({
    val view by memoized { mock<CreateWalletModule.IView>() }
    val router by memoized { mock<CreateWalletModule.IRouter>() }
    val interactor by memoized { mock<CreateWalletModule.IInteractor>() }
    val state by memoized { mock<CreateWalletModule.State>() }
    val presenter by memoized { CreateWalletPresenter(view, router, interactor, state) }

    val titleBtc = "Bitcoin"
    val codeBtc = "BTC"
    val titleEth = "Ethereum"
    val codeEth = "ETH"

    val coinBtc = mock<Coin> {
        on { title } doReturn titleBtc
        on { code } doReturn codeBtc
    }
    val coinEth = mock<Coin> {
        on { title } doReturn titleEth
        on { code } doReturn codeEth
    }
    val featuredCoinBtc = mock<FeaturedCoin> {
        on { coin } doReturn coinBtc
        on { enabledByDefault } doReturn true
    }
    val featuredCoinEth = mock<FeaturedCoin> {
        on { coin } doReturn coinEth
        on { enabledByDefault } doReturn false
    }

    describe("#viewDidLoad") {
        describe("common") {
            val coinViewItemBtc = CreateWalletModule.CoinViewItem(titleBtc, codeBtc, true)
            val coinViewItemEth = CreateWalletModule.CoinViewItem(titleEth, codeEth, false)

            beforeEachTest {
                whenever(interactor.featuredCoins).thenReturn(listOf(featuredCoinBtc, featuredCoinEth))
                presenter.viewDidLoad()
            }

            it("sets coin items to view") {
                verify(view).setItems(listOf(coinViewItemBtc, coinViewItemEth))
            }

            it("sets coins to state") {
                verify(state).coins = listOf(featuredCoinBtc.coin, featuredCoinEth.coin)
            }
        }

        describe("when at least one item is selected by default") {
            beforeEachTest {
                whenever(interactor.featuredCoins).thenReturn(listOf(featuredCoinBtc, featuredCoinEth))
                presenter.viewDidLoad()
            }

            it("enables create button") {
                verify(view).setCreateEnabled(true)
            }

        }

        describe("when no items is selected by default") {
            beforeEachTest {
                whenever(interactor.featuredCoins).thenReturn(listOf(featuredCoinEth))
                presenter.viewDidLoad()
            }

            it("disables create button") {
                verify(view).setCreateEnabled(false)
            }
        }
    }

    describe("#didEnable") {
        describe("updating state") {
            beforeEach {
                whenever(state.enabledPositions).thenReturn(setOf(0, 5))
                presenter.didEnable(1)
            }

            it("adds position to enabled state") {
                verify(state).enabledPositions = setOf(0, 5, 1)
            }
        }

        describe("when all were disabled") {
            beforeEach {
                whenever(state.enabledPositions).thenReturn(setOf())
                presenter.didEnable(1)
            }

            it("enables create button") {
                verify(view).setCreateEnabled(true)
            }

        }

        describe("when at least one was enabled") {
            beforeEach {
                whenever(state.enabledPositions).thenReturn(setOf(0))
                presenter.didEnable(1)
            }

            it("does nothing") {
                verify(view, never()).setCreateEnabled(true)
            }
        }
    }

    describe("#didDisable") {
        describe("updating state") {
            beforeEach {
                whenever(state.enabledPositions).thenReturn(setOf(0, 5, 3))
                presenter.didDisable(0)
            }

            it("removes position from state") {
                verify(state).enabledPositions = setOf(5, 3)
            }
        }

        describe("when all become disabled") {
            beforeEach {
                whenever(state.enabledPositions).thenReturn(setOf(0))
                presenter.didDisable(0)
            }

            it("disables create button") {
                verify(view).setCreateEnabled(false)
            }
        }

        describe("when one least one stays enabled") {
            beforeEach {
                whenever(state.enabledPositions).thenReturn(setOf(0, 1))
                presenter.didDisable(0)
            }

            it("does nothing") {
                verify(view, never()).setCreateEnabled(false)
            }
        }
    }

    describe("#didCreate") {
        beforeEach {
            whenever(state.coins).thenReturn(listOf(coinBtc, coinEth))
            whenever(state.enabledPositions).thenReturn(setOf(1))

            presenter.didCreate()
        }

        it("creates wallet for enabled coins") {
            verify(interactor).createWallet(listOf(coinEth))
        }

        it("routes to main module") {
            verify(router).startMainModule()
        }
    }

})