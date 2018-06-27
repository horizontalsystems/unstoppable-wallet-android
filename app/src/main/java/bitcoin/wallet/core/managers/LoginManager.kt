package bitcoin.wallet.core.managers

import bitcoin.wallet.WalletManager
import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.core.INetworkManager
import bitcoin.wallet.core.RealmManager
import io.reactivex.Completable
import io.reactivex.Observable

class LoginManager(private val networkManager: INetworkManager, private val walletManager: WalletManager, private val realmManager: RealmManager, private val localStorage: ILocalStorage) {

    fun login(words: List<String>): Completable = Observable.just(words)
            .map {
                walletManager.createWallet(it)
            }
            .flatMap { wallet ->
                networkManager.getJwtToken(wallet.getIdentity(), wallet.getPubKeys())
            }
            .flatMapCompletable { jwtToken ->
                realmManager.login(jwtToken)
            }
            .doOnComplete {
                localStorage.saveWords(words)
            }

}
