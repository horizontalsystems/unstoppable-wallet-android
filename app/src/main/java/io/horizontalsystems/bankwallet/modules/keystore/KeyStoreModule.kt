package io.horizontalsystems.bankwallet.modules.keystore

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

object KeyStoreModule {

    @Parcelize
    enum class ModeType : Parcelable {
        NoSystemLock,
        InvalidKey,
        UserAuthentication
    }
}
