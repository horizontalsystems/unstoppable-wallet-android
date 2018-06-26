package bitcoin.wallet.core

import io.realm.Realm
import io.realm.SyncUser

object RealmFactory {

    fun createWalletRealm() : Realm {
        val realmURL = "realms://grouvi-wallet.us1a.cloud.realm.io/default"

        val config = SyncUser.current()
                .createConfiguration(realmURL)
                .fullSynchronization()
                .build()

        return Realm.getInstance(config)
    }

}
