package io.horizontalsystems.bankwallet.modules.manageaccount.publickeys

import io.horizontalsystems.bankwallet.modules.manageaccount.showextendedkey.ShowExtendedKeyModule.DisplayKeyType.AccountPublicKey
import io.horizontalsystems.bankwallet.modules.manageaccount.showmonerokey.ShowMoneroKeyModule.MoneroKeys
import io.horizontalsystems.hdwalletkit.HDExtendedKey

object PublicKeysModule {

    data class ViewState(
        val evmAddress: String? = null,
        val tronAddress: String? = null,
        val extendedPublicKey: ExtendedPublicKey? = null,
        val moneroKeys: MoneroKeys? = null
    )

    data class ExtendedPublicKey(
        val hdKey: HDExtendedKey,
        val accountPublicKey: AccountPublicKey
    )
}