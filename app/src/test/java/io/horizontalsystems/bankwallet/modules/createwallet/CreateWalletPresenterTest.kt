package io.horizontalsystems.bankwallet.modules.createwallet

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.entities.Coin
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

    describe("#viewDidLoad") {
        describe("common") {
            val coinViewItemBtc = CreateWalletModule.CoinViewItem(titleBtc, codeBtc)
            val coinViewItemEth = CreateWalletModule.CoinViewItem(titleEth, codeEth)

            beforeEachTest {
                whenever(interactor.featuredCoins).thenReturn(listOf(coinBtc, coinEth))
                presenter.viewDidLoad()
            }

            it("sets coin items to view") {
                verify(view).setItems(listOf(coinViewItemBtc, coinViewItemEth))
            }

            it("sets coins to state") {
                verify(state).coins = listOf(coinBtc, coinEth)
            }
        }
    }



    describe("#didTapItem") {
        beforeEach {
            whenever(state.coins).thenReturn(listOf(coinBtc, coinEth))

            presenter.didTapItem(1)
        }

        it("creates wallet for selected coin") {
            verify(interactor).createWallet(coinEth)
        }

        it("routes to main module") {
            verify(router).startMainModule()
        }
    }

})