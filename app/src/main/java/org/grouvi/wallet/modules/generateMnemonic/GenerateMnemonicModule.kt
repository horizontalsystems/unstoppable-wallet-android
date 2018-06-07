package org.grouvi.wallet.modules.generateMnemonic

import org.bitcoinj.core.Utils
import org.bitcoinj.wallet.DeterministicSeed
import org.grouvi.wallet.lib.WalletDataManager
import java.security.SecureRandom

object GenerateMnemonicModule {

    interface IView {
        var presenter: GenerateMnemonicModule.IPresenter
        fun showMnemonicWords(words: List<String>)
    }

    interface IPresenter {
        var interactor: GenerateMnemonicModule.IInteractor
        var view: IView
        var router: IRouter

        fun start()
        fun complete()
    }

    interface IInteractorDelegate {
        fun didGenerateMnemonic(words: List<String>)

    }

    interface IInteractor {
        var seedGenerator: ISeedGenerator
        var delegate: IInteractorDelegate
        var walletDataProvider: WalletDataManager

        fun generateMnemonic()
    }

    interface IRouter {
        fun openMnemonicWordsConfirmation()
    }

    // helpers

    interface ISeedGenerator {
        fun generateSeed(passphrase: String): DeterministicSeed
    }


    fun initModule(view: IView, router: IRouter) {
        val interactor = GenerateMnemonicModuleInteractor()
        val presenter = GenerateMnemonicModulePresenter()

        view.presenter = presenter

        presenter.view = view
        presenter.interactor = interactor
        presenter.router = router

        interactor.delegate = presenter
        interactor.seedGenerator = SeedGenerator()
        interactor.walletDataProvider = WalletDataManager
    }

}

class SeedGenerator : GenerateMnemonicModule.ISeedGenerator {
    override fun generateSeed(passphrase: String): DeterministicSeed =
            DeterministicSeed(SecureRandom(), DeterministicSeed.DEFAULT_SEED_ENTROPY_BITS, passphrase, Utils.currentTimeSeconds())
}

class GenerateMnemonicModulePresenter : GenerateMnemonicModule.IPresenter, GenerateMnemonicModule.IInteractorDelegate {
    override lateinit var interactor: GenerateMnemonicModule.IInteractor
    override lateinit var view: GenerateMnemonicModule.IView
    override lateinit var router: GenerateMnemonicModule.IRouter

    override fun start() {
        interactor.generateMnemonic()
    }

    override fun didGenerateMnemonic(words: List<String>) {
        view.showMnemonicWords(words)
    }

    override fun complete() {
        router.openMnemonicWordsConfirmation()
    }
}

class GenerateMnemonicModuleInteractor : GenerateMnemonicModule.IInteractor {
    override lateinit var seedGenerator: GenerateMnemonicModule.ISeedGenerator
    override lateinit var delegate: GenerateMnemonicModule.IInteractorDelegate
    override lateinit var walletDataProvider: WalletDataManager

    override fun generateMnemonic() {
        val seed = seedGenerator.generateSeed("")
        seed.mnemonicCode?.let {
            delegate.didGenerateMnemonic(it)
            walletDataProvider.mnemonicWords = it
        }
    }
}

