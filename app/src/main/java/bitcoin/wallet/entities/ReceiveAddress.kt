package bitcoin.wallet.entities

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class ReceiveAddress : RealmObject() {

    @PrimaryKey
    var code: String = ""

    var address: String = ""

}
