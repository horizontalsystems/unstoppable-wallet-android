package bitcoin.wallet.core.managers

import bitcoin.wallet.core.INetworkManager
import io.reactivex.Completable
import io.realm.ObjectServerError
import io.realm.SyncCredentials
import io.realm.SyncUser

class LoginManager(private val networkManager: INetworkManager) {

    fun login(words: List<String>): Completable = Completable.create { emitter ->

        val credentials = SyncCredentials.usernamePassword("bakyt", "123")

        val authURL = "https://grouvi-wallet.us1a.cloud.realm.io"

        SyncUser.logInAsync(credentials, authURL, object : SyncUser.Callback<SyncUser> {
            override fun onSuccess(user: SyncUser) {
                emitter.onComplete()
            }

            override fun onError(error: ObjectServerError) {
                emitter.onError(error)
            }
        })
    }

}