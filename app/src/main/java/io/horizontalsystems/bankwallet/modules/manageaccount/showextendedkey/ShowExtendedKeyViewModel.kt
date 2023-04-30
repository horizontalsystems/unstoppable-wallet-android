package io.horizontalsystems.bankwallet.modules.manageaccount.showextendedkey

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.manageaccount.showextendedkey.ShowExtendedKeyModule.DisplayKeyType
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bitcoincash.MainNetBitcoinCash
import io.horizontalsystems.bitcoinkit.MainNet
import io.horizontalsystems.dashkit.MainNetDash
import io.horizontalsystems.hdwalletkit.ExtendedKeyCoinType
import io.horizontalsystems.hdwalletkit.HDExtendedKeyVersion
import io.horizontalsystems.hdwalletkit.HDKeychain
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.litecoinkit.MainNetLitecoin

class ShowExtendedKeyViewModel(
    private val keyChain: HDKeychain,
    val displayKeyType: DisplayKeyType,
    purpose: HDWallet.Purpose,
    extendedKeyCoinType: ExtendedKeyCoinType
) : ViewModel() {
    val purposes = HDWallet.Purpose.values()
    val blockchains: Array<Blockchain>
        get() = if (purpose != HDWallet.Purpose.BIP44) arrayOf(Blockchain.Bitcoin, Blockchain.Litecoin) else Blockchain.values()
    val accounts = 0..5

    var purpose: HDWallet.Purpose by mutableStateOf(purpose)
        private set
    var blockchain: Blockchain by mutableStateOf(extendedKeyCoinType.blockchain)
        private set
    var account: Int by mutableStateOf(0)
        private set

    val title: TranslatableString
        get() = when (displayKeyType) {
            is DisplayKeyType.AccountPrivateKey -> TranslatableString.ResString(R.string.AccountExtendedPrivateKey_Short)
            is DisplayKeyType.AccountPublicKey -> TranslatableString.ResString(R.string.AccountExtendedPublicKey_Short)
            DisplayKeyType.Bip32RootKey -> TranslatableString.ResString(R.string.Bip32RootKey)
        }

    val extendedKey: String
        get() {
            val key = if (displayKeyType.isDerivable)
                keyChain.getKeyByPath("m/${purpose.value}'/${blockchain.coinType}'/$account'")
            else
                keyChain.getKeyByPath("m")
            val version = HDExtendedKeyVersion.initFrom(purpose, blockchain.extendedKeyCoinType, displayKeyType.isPrivate)
            return if (displayKeyType.isPrivate) key.serializePrivate(version.value) else key.serializePublic(version.value)
        }

    fun set(purpose: HDWallet.Purpose) {
        this.purpose = purpose

        if (purpose != HDWallet.Purpose.BIP44 && (blockchain != Blockchain.Bitcoin && blockchain != Blockchain.Litecoin)) {
            blockchain = Blockchain.Bitcoin
        }
    }

    fun set(blockchain: Blockchain) {
        this.blockchain = blockchain
    }

    fun set(account: Int) {
        this.account = account
    }

    private val Blockchain.extendedKeyCoinType: ExtendedKeyCoinType
        get() = when (this) {
            Blockchain.Litecoin -> ExtendedKeyCoinType.Litecoin
            Blockchain.Bitcoin,
            Blockchain.BitcoinCash,
            Blockchain.Dash -> ExtendedKeyCoinType.Bitcoin
        }

    private val ExtendedKeyCoinType.blockchain: Blockchain
        get() =  when(this) {
            ExtendedKeyCoinType.Bitcoin -> Blockchain.Bitcoin
            ExtendedKeyCoinType.Litecoin -> Blockchain.Litecoin
        }

    enum class Blockchain(val title: String, val coinType: Int) {
        Bitcoin("Bitcoin", MainNet().coinType),
        BitcoinCash("Bitcoin Cash", MainNetBitcoinCash().coinType),
        Litecoin("Litecoin", MainNetLitecoin().coinType),
        Dash("Dash", MainNetDash().coinType)
    }
}
