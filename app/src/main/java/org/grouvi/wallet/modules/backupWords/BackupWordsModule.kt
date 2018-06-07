package org.grouvi.wallet.modules.backupWords

import android.content.Context
import android.content.Intent
import org.grouvi.wallet.lib.WalletDataManager
import org.grouvi.wallet.log
import java.util.*

object BackupWordsModule {
    interface IView {
        var presenter: IPresenter

        fun showWords(words: List<String>)
        fun showConfirmationWithIndexes(indexes: List<Int>)
        fun hideWords()
        fun hideConfirmation()
        fun showConfirmationError()
    }

    interface IPresenter {

        var view: IView
        var interactor: IInteractor
        var router: IRouter

        fun cancelDidTap()
        fun showWordsDidTap()
        fun hideWordsDidTap()
        fun showConfirmationDidTap()
        fun hideConfirmationDidTap()
        fun validateDidTap(confirmationWords: HashMap<Int, String>)

    }

    interface IInteractor {
        var delegate: IInteractorDelegate
        var wordsProvider: IWordsProvider

        fun fetchWords()
        fun fetchConfirmationIndexes()
        fun validate(confirmationWords: HashMap<Int, String>)
    }

    interface IRouter {
        fun close()
    }

    interface IInteractorDelegate {
        fun didFetchWords(words: List<String>)
        fun didFetchConfirmationIndexes(indexes: List<Int>)
        fun didValidateSuccess()
        fun didValidateFailure()

    }

    interface IWordsProvider {
        var mnemonicWords: List<String>
    }

    interface CreateWalletRandomIndexesProvider {
        fun getRandomIndexes(count: Int) : List<Int>
    }


    fun start(context: Context) {
        val intent = Intent(context, BackupWordsActivity::class.java)
        context.startActivity(intent)
    }

    fun init(view: IView, router: IRouter) {
        val presenter = BackupWordsModulePresenter()
        val interactor = BackupWordsModuleInteractor()

        presenter.view = view
        presenter.router = router
        presenter.interactor = interactor

        interactor.delegate = presenter
        interactor.wordsProvider = WalletDataManager
        interactor.random = Random()

        view.presenter = presenter
    }
}

class BackupWordsModulePresenter : BackupWordsModule.IPresenter, BackupWordsModule.IInteractorDelegate {
    override lateinit var view: BackupWordsModule.IView
    override lateinit var interactor: BackupWordsModule.IInteractor
    override lateinit var router: BackupWordsModule.IRouter

    // presenter

    override fun cancelDidTap() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun showWordsDidTap() {
        interactor.fetchWords()
    }

    override fun hideWordsDidTap() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun showConfirmationDidTap() {
        interactor.fetchConfirmationIndexes()
    }

    override fun hideConfirmationDidTap() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun validateDidTap(confirmationWords: HashMap<Int, String>) {
        interactor.validate(confirmationWords)
    }

// interactor delegate

    override fun didFetchWords(words: List<String>) {
        view.showWords(words)
    }

    override fun didFetchConfirmationIndexes(indexes: List<Int>) {
        view.showConfirmationWithIndexes(indexes)
    }

    override fun didValidateSuccess() {
        router.close()
    }

    override fun didValidateFailure() {
        view.showConfirmationError()
    }
}

class BackupWordsModuleInteractor : BackupWordsModule.IInteractor {
    override lateinit var delegate: BackupWordsModule.IInteractorDelegate

    override lateinit var wordsProvider: BackupWordsModule.IWordsProvider

    lateinit var random: Random


    override fun fetchWords() {
        wordsProvider.mnemonicWords.log("Mnemonic")
        delegate.didFetchWords(wordsProvider.mnemonicWords)
    }

    override fun fetchConfirmationIndexes() {
        val numberOfWords = wordsProvider.mnemonicWords.size

        delegate.didFetchConfirmationIndexes(listOf(random.nextInt(numberOfWords), random.nextInt(numberOfWords)))
    }

    override fun validate(confirmationWords: HashMap<Int, String>) {
        var valid = true

        for ((i, w) in confirmationWords) {
            if (wordsProvider.mnemonicWords[i] != w.trim()) {
                valid = false
                break
            }
        }

        if (valid) {
            delegate.didValidateSuccess()
        } else {
            delegate.didValidateFailure()
        }

    }
}


