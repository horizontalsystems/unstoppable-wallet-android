package bitcoin.wallet.modules.wallet

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import bitcoin.wallet.core.subscribeAsync
import bitcoin.wallet.entities.Coin
import bitcoin.wallet.lib.WalletDataManager
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable

object WalletModule {

    interface IView {
        var presenter: IPresenter

        fun showCoinItems(coinItems: List<Coin>)
        fun showTotalBalance(balance: Double)
    }

    interface IPresenter {
        fun start()

        var view: IView
        var interactor: IInteractor
        var router: IRouter
    }

    interface IInteractor {
        fun retrieveInitialData()

        var delegate: IInteractorDelegate
        var coinsDataProvider: ICoinsDataProvider
    }

    interface IRouter

    interface IInteractorDelegate {
        fun didCoinItemsRetrieved(coinItems: List<Coin>)
        fun didTotalBalanceRetrieved(totalBalance: Double)
    }

    // helpers

    interface ICoinsDataProvider {
        fun getCoins() : Flowable<List<Coin>>
    }


    fun init(view: IView, router: IRouter) {

        val presenter = WalletModulePresenter()
        val interactor = WalletModuleInteractor()

        presenter.view = view
        presenter.router = router
        presenter.interactor = interactor

        interactor.delegate = presenter
        interactor.coinsDataProvider = WalletDataManager

        view.presenter = presenter
    }

}

class WalletViewModel : ViewModel(), WalletModule.IView, WalletModule.IRouter {

    override lateinit var presenter: WalletModule.IPresenter

    val coinItemsLiveData = MutableLiveData<List<Coin>>()
    val totalBalanceLiveData = MutableLiveData<Double>()

    fun init() {
        WalletModule.init(this, this)

        presenter.start()
    }

    override fun showCoinItems(coinItems: List<Coin>) {
        coinItemsLiveData.value = coinItems
    }

    override fun showTotalBalance(balance: Double) {
        totalBalanceLiveData.value = balance
    }
}

class WalletModulePresenter : WalletModule.IPresenter, WalletModule.IInteractorDelegate {

    override lateinit var view: WalletModule.IView
    override lateinit var interactor: WalletModule.IInteractor
    override lateinit var router: WalletModule.IRouter

    override fun start() {
        interactor.retrieveInitialData()
    }

    override fun didCoinItemsRetrieved(coinItems: List<Coin>) {
        view.showCoinItems(coinItems)
    }

    override fun didTotalBalanceRetrieved(totalBalance: Double) {
        view.showTotalBalance(totalBalance)
    }
}

class WalletModuleInteractor : WalletModule.IInteractor {
    override lateinit var delegate: WalletModule.IInteractorDelegate
    override lateinit var coinsDataProvider: WalletModule.ICoinsDataProvider

    override fun retrieveInitialData() {
        coinsDataProvider.getCoins().subscribeAsync(CompositeDisposable(),
                onNext = { coins ->
                    delegate.didCoinItemsRetrieved(coins)

                    delegate.didTotalBalanceRetrieved(coins.sumByDouble { it.amountFiat })
                },
                onError = {

                })


    }
}