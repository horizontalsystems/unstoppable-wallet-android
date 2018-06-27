package bitcoin.wallet.modules.restore

import bitcoin.wallet.core.IMnemonic
import bitcoin.wallet.core.managers.LoginManager
import bitcoin.wallet.core.subscribeAsync
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable

class RestoreInteractor(private val mnemonic: IMnemonic, private val loginManager: LoginManager) : RestoreModule.IInteractor {

    var delegate: RestoreModule.IInteractorDelegate? = null

    override fun restore(words: List<String>) {
        val validationObservable = if (mnemonic.validateWords(words)) {
            Observable.just(words)
        } else {
            Observable.error(Exception())
        }

        validationObservable
                .flatMapCompletable {
                    loginManager.login(it)
                }
                .subscribeAsync(CompositeDisposable(),
                        onComplete = {
                            delegate?.didRestore()
                        },
                        onError = {
                            delegate?.didFailToRestore()
                        })

    }

}
