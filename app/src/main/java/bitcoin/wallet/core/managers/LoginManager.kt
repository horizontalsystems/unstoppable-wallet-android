package bitcoin.wallet.core.managers

import bitcoin.wallet.WalletManager
import bitcoin.wallet.bitcoin.BitcoinBlockchainService
import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.core.INetworkManager
import bitcoin.wallet.core.RealmManager
import io.reactivex.Completable
import io.reactivex.Observable

open class LoginManager(private val networkManager: INetworkManager, protected val walletManager: WalletManager, protected val realmManager: RealmManager, protected val localStorage: ILocalStorage) {

    open fun login(words: List<String>): Completable = Observable.just(words)
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

class LoginManagerLocal(networkManager: INetworkManager, walletManager: WalletManager, realmManager: RealmManager, localStorage: ILocalStorage) : LoginManager(networkManager, walletManager, realmManager, localStorage) {

    override fun login(words: List<String>): Completable = Observable.just(words)
            .flatMapCompletable {
                BitcoinBlockchainService.initNewWallet()

                Completable.complete()
            }
            .doOnComplete {
                localStorage.saveWords(words)
            }

}
