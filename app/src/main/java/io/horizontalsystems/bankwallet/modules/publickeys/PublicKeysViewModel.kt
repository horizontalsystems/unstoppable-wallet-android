package io.horizontalsystems.bankwallet.modules.publickeys

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.BitcoinCashCoinType
import io.horizontalsystems.bitcoincash.MainNetBitcoinCash
import io.horizontalsystems.bitcoinkit.MainNet
import io.horizontalsystems.dashkit.MainNetDash
import io.horizontalsystems.hdwalletkit.ExtendedKeyCoinType
import io.horizontalsystems.hdwalletkit.HDExtendedKeyVersion
import io.horizontalsystems.hdwalletkit.HDKeychain
import io.horizontalsystems.hdwalletkit.HDWallet.Purpose
import io.horizontalsystems.litecoinkit.MainNetLitecoin

class PublicKeysViewModel(
    account: Account,
) : ViewModel() {
    private val seed: ByteArray?

    init {
        seed = if (account.type is AccountType.Mnemonic) {
            account.type.seed
        } else {
            null
        }
    }

    fun bitcoinPublicKeys(derivation: AccountType.Derivation): String? {
        seed ?: return null
        val network = MainNet()
        val keychain = HDKeychain(seed)

        return keysJson(keychain, purpose(derivation), network.coinType)
    }

    fun bitcoinCashPublicKeys(coinType: BitcoinCashCoinType): String? {
        seed ?: return null
        val keychain = HDKeychain(seed)

        val network = when (coinType) {
            BitcoinCashCoinType.type0 -> MainNetBitcoinCash(MainNetBitcoinCash.CoinType.Type0)
            BitcoinCashCoinType.type145 -> MainNetBitcoinCash(MainNetBitcoinCash.CoinType.Type145)
        }
        return keysJson(keychain, Purpose.BIP44, network.coinType)
    }

    fun litecoinPublicKeys(derivation: AccountType.Derivation): String? {
        seed ?: return null
        val network = MainNetLitecoin()
        val keychain = HDKeychain(seed)

        return keysJson(
            keychain,
            purpose(derivation),
            network.coinType,
            ExtendedKeyCoinType.Litecoin
        )
    }

    fun dashKeys(): String? {
        seed ?: return null
        val network = MainNetDash()
        val keychain = HDKeychain(seed)

        return keysJson(keychain, Purpose.BIP44, network.coinType)
    }

    private fun keysJson(
        keychain: HDKeychain,
        purpose: Purpose,
        coinType: Int,
        extendedKeyCoinType: ExtendedKeyCoinType = ExtendedKeyCoinType.Bitcoin,
    ): String {
        val publicKeys = (0..4).mapNotNull { accountIndex ->
            val key = keychain.getKeyByPath("m/${purpose.value}'/$coinType'/$accountIndex'")
            val version =
                HDExtendedKeyVersion.initFrom(purpose, extendedKeyCoinType, isPrivate = false)
            version?.let { key.serializePublic(it.value) }
        }
        return publicKeys.toString()
    }

    private fun purpose(derivation: AccountType.Derivation): Purpose = when (derivation) {
        AccountType.Derivation.bip44 -> Purpose.BIP44
        AccountType.Derivation.bip49 -> Purpose.BIP49
        AccountType.Derivation.bip84 -> Purpose.BIP84
    }

}
