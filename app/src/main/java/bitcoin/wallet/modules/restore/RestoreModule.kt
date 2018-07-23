package bitcoin.wallet.modules.restore

import android.content.Context
import android.content.Intent
import bitcoin.wallet.blockchain.BlockchainManager
import bitcoin.wallet.core.managers.Factory

object RestoreModule {

    interface IView {
        fun showInvalidWordsError()
        fun authenticateToRestoreWallet()
    }

    interface IViewDelegate {
        fun restoreDidClick(words: List<String>)
        fun cancelDidClick()
    }

    interface IInteractor {
        fun restore(words: List<String>)
    }

    interface IInteractorDelegate {
        fun didRestore()
        fun didFailToRestore(error: Throwable)
    }

    interface IRouter {
        fun navigateToMain()
        fun close()
    }

    fun start(context: Context) {
        val intent = Intent(context, RestoreWalletActivity::class.java)
        context.startActivity(intent)
    }

    fun initModule(view: RestoreViewModel, router: IRouter) {
        val interactor = RestoreInteractor(Factory.mnemonicManager, BlockchainManager)
        val presenter = RestorePresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}
