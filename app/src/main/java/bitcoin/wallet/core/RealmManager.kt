package bitcoin.wallet.core

import io.realm.Realm
import io.realm.RealmConfiguration

class RealmManager {

    fun createWalletRealmLocal(): Realm {
        val config = RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .build()

        return Realm.getInstance(config)
    }

}
