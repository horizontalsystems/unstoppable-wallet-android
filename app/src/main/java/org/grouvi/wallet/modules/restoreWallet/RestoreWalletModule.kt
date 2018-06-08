package org.grouvi.wallet.modules.restoreWallet

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import android.content.Intent
import org.grouvi.wallet.R
import org.grouvi.wallet.SingleLiveEvent
import org.grouvi.wallet.lib.WalletDataManager

object RestoreWalletModule {

    interface IView {
        var presenter: IPresenter

        fun showInvalidWordsError()
    }

    interface IPresenter {
        var view: IView
        var interactor: IInteractor
        var router: IRouter

        fun onRestoreButtonClick(words: List<String>)
    }

    interface IInteractor {
        var delegate: IInteractorDelegate
        var walletRestorer: IWalletRestorer

        fun restoreWallet(words: List<String>)
    }

    interface IRouter {
        fun navigateToMainScreen()
    }

    interface IInteractorDelegate {
        fun didRestoreWallet()
        fun failureRestoreWallet(exception: InvalidWordsException)
    }

    // helper

    interface IWalletRestorer {
        @Throws(InvalidWordsException::class)
        fun restoreWallet(words: List<String>)
    }

    class InvalidWordsException : Exception()

    fun start(context: Context) {
        val intent = Intent(context, RestoreWalletActivity::class.java)
        context.startActivity(intent)
    }

    fun initModule(view: IView, router: IRouter) {
        val presenter = RestoreWalletModulePresenter()
        val interactor = RestoreWalletModuleInteractor()

        view.presenter = presenter

        presenter.view = view
        presenter.interactor = interactor
        presenter.router = router

        interactor.delegate = presenter
        interactor.walletRestorer = WalletDataManager
    }

}

class RestoreWalletViewModel : ViewModel(), RestoreWalletModule.IView, RestoreWalletModule.IRouter {

    override lateinit var presenter: RestoreWalletModule.IPresenter

    val errorLiveData = MutableLiveData<Int>()
    val navigateToMainScreenLiveEvent = SingleLiveEvent<Void>()

    fun init() {
        RestoreWalletModule.initModule(this, this)
    }

    override fun navigateToMainScreen() {
        navigateToMainScreenLiveEvent.call()
    }

    override fun showInvalidWordsError() {
        errorLiveData.value = R.string.error
    }
}

class RestoreWalletModulePresenter : RestoreWalletModule.IPresenter, RestoreWalletModule.IInteractorDelegate {

    override lateinit var view: RestoreWalletModule.IView
    override lateinit var interactor: RestoreWalletModule.IInteractor
    override lateinit var router: RestoreWalletModule.IRouter

    override fun onRestoreButtonClick(words: List<String>) {
        interactor.restoreWallet(words)
    }

    override fun didRestoreWallet() {
        router.navigateToMainScreen()
    }

    override fun failureRestoreWallet(exception: RestoreWalletModule.InvalidWordsException) {
        view.showInvalidWordsError()
    }
}

class RestoreWalletModuleInteractor : RestoreWalletModule.IInteractor {

    override lateinit var delegate: RestoreWalletModule.IInteractorDelegate
    override lateinit var walletRestorer: RestoreWalletModule.IWalletRestorer

    override fun restoreWallet(words: List<String>) {
        try {
            walletRestorer.restoreWallet(words)
            delegate.didRestoreWallet()
        } catch (e: RestoreWalletModule.InvalidWordsException) {
            delegate.failureRestoreWallet(e)
        }

    }

}