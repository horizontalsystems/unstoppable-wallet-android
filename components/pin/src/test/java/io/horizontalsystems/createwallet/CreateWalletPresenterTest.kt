package io.horizontalsystems.bankwallet.modules.createwallet

import org.spekframework.spek2.Spek

object CreateWalletPresenterTest : Spek({
//    val view by memoized { mock<CreateWalletModule.IView>() }
//    val router by memoized { mock<CreateWalletModule.IRouter>() }
//    val interactor by memoized { mock<CreateWalletModule.IInteractor>() }
//    val state by memoized { mock<CreateWalletModule.State>() }
//    val coinViewItemFactory by memoized { mock<CoinViewItemFactory>() }
//    val presenter by memoized { CreateWalletPresenter(view, router, interactor, coinViewItemFactory, state) }
//
//    val titleBtc = "Bitcoin"
//    val codeBtc = "BTC"
//    val titleEth = "Ethereum"
//    val codeEth = "ETH"
//
//    val coinBtc = mock<Coin> {
//        on { title } doReturn titleBtc
//        on { code } doReturn codeBtc
//    }
//    val coinEth = mock<Coin> {
//        on { title } doReturn titleEth
//        on { code } doReturn codeEth
//    }
//
//    val featuredCoins = listOf(coinBtc, coinEth)
//    val coinViewItems = listOf<CreateWalletModule.CoinViewItem>(mock(), mock())
//
//    describe("#viewDidLoad") {
//        describe("common") {
//
//            beforeEachTest {
//                whenever(interactor.featuredCoins).thenReturn(featuredCoins)
//                whenever(coinViewItemFactory.createItems(featuredCoins, 0)).thenReturn(coinViewItems)
//
//                presenter.viewDidLoad()
//            }
//
//            it("sets coin view items to view") {
//                verify(view).setItems(coinViewItems)
//            }
//
//            it("sets coins to state") {
//                verify(state).coins = featuredCoins
//            }
//
//            it("sets default selected position to state") {
//                verify(state).selectedPosition = 0
//            }
//        }
//    }
//
//    describe("#didTapItem") {
//        val position = 123
//
//        beforeEach {
//            whenever(state.coins).thenReturn(featuredCoins)
//            whenever(coinViewItemFactory.createItems(featuredCoins, position)).thenReturn(coinViewItems)
//
//            presenter.didTapItem(position)
//        }
//
//        it("sets updated coin items to view") {
//            verify(view).setItems(coinViewItems)
//        }
//
//        it("sets selected position to state") {
//            verify(state).selectedPosition = position
//        }
//    }
//
//    describe("didClickCreate") {
//        describe("when interactor throws exception") {
//            val exception = EosUnsupportedException()
//
//            beforeEach {
//                whenever(state.coins).thenReturn(featuredCoins)
//                whenever(state.selectedPosition).thenReturn(1)
//                whenever(interactor.createWallet(any())).thenThrow(exception)
//
//                presenter.didClickCreate()
//            }
//
//            it("shows an error") {
//                verify(view).showError(exception)
//            }
//
//            it("doesn't route to main module") {
//                verify(router, never()).startMainModule()
//            }
//        }
//
//        describe("when interactor performs successfully") {
//            beforeEach {
//                whenever(state.coins).thenReturn(featuredCoins)
//                whenever(state.selectedPosition).thenReturn(1)
//
//                presenter.didClickCreate()
//            }
//
//            it("creates wallet for selected coin") {
//                verify(interactor).createWallet(coinEth)
//            }
//
//            it("routes to main module") {
//                verify(router).startMainModule()
//            }
//        }
//    }

})