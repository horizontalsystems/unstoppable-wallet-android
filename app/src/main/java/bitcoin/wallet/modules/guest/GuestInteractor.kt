package bitcoin.wallet.modules.guest

import bitcoin.wallet.core.IMnemonic
import bitcoin.wallet.core.managers.LoginManager
import bitcoin.wallet.core.subscribeAsync
import io.reactivex.disposables.CompositeDisposable

class GuestInteractor(private val mnemonic: IMnemonic, private val loginManager: LoginManager) : GuestModule.IInteractor {

    var delegate: GuestModule.IInteractorDelegate? = null

    override fun createWallet() {
        loginManager.login(mnemonic.generateWords()).subscribeAsync(CompositeDisposable(),
                onComplete = {
                    delegate?.didCreateWallet()
                },
                onError = {
                    delegate?.didFailToCreateWallet()
                })
    }

}
