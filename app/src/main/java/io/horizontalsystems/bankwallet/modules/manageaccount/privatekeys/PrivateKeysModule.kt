package io.horizontalsystems.bankwallet.modules.manageaccount.privatekeys

import io.horizontalsystems.bankwallet.modules.manageaccount.showextendedkey.ShowExtendedKeyModule
import io.horizontalsystems.bankwallet.modules.manageaccount.showmonerokey.ShowMoneroKeyModule.MoneroKeys
import io.horizontalsystems.hdwalletkit.HDExtendedKey

object PrivateKeysModule {

    data class ViewState(
        val evmPrivateKey: String? = null,
        val tronPrivateKey: String? = null,
        val bip32RootKey: ExtendedKey? = null,
        val accountExtendedPrivateKey: ExtendedKey? = null,
        val stellarSecretKey: String? = null,
        val moneroKeys: MoneroKeys? = null
    )

    data class ExtendedKey(
        val hdKey: HDExtendedKey,
        val displayKeyType: ShowExtendedKeyModule.DisplayKeyType
    )
}