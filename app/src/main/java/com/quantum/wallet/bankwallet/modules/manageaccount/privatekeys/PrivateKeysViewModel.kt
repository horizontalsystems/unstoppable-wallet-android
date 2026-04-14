package com.quantum.wallet.bankwallet.modules.manageaccount.privatekeys

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.quantum.wallet.bankwallet.core.managers.EvmBlockchainManager
import com.quantum.wallet.bankwallet.core.managers.toStellarWallet
import com.quantum.wallet.bankwallet.core.toRawHexString
import com.quantum.wallet.bankwallet.entities.Account
import com.quantum.wallet.bankwallet.entities.AccountType
import com.quantum.wallet.bankwallet.modules.manageaccount.showextendedkey.ShowExtendedKeyModule
import com.quantum.wallet.bankwallet.modules.manageaccount.showmonerokey.ShowMoneroKeyModule
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.hdwalletkit.HDExtendedKey
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.stellarkit.StellarKit
import io.horizontalsystems.tronkit.network.Network
import java.math.BigInteger
import io.horizontalsystems.tronkit.transaction.Signer as TronSigner

class PrivateKeysViewModel(
    account: Account,
    evmBlockchainManager: EvmBlockchainManager,
) : ViewModel() {

    var viewState by mutableStateOf(PrivateKeysModule.ViewState())
        private set

    init {

        val ethereumPrivateKey = when (val accountType = account.type) {
            is AccountType.Mnemonic -> {
                val chain = evmBlockchainManager.getChain(BlockchainType.Ethereum)
                toHexString(Signer.privateKey(accountType.words, accountType.passphrase, chain))
            }

            is AccountType.EvmPrivateKey -> toHexString(accountType.key)
            else -> null
        }

        val tronPrivateKey = when (val accountType = account.type) {
            is AccountType.Mnemonic -> {
                val privateKey = TronSigner.privateKey(
                    accountType.seed,
                    Network.Mainnet
                )
                toHexString(privateKey)
            }

            is AccountType.TronPrivateKey -> toHexString(accountType.key)
            else -> null
        }

        val hdExtendedKey = (account.type as? AccountType.HdExtendedKey)?.hdExtendedKey

        val bip32RootKey = if (account.type is AccountType.Mnemonic) {
            val seed = Mnemonic().toSeed(account.type.words, account.type.passphrase)
            HDExtendedKey(seed, HDWallet.Purpose.BIP44)
        } else if (hdExtendedKey?.derivedType == HDExtendedKey.DerivedType.Master) {
            hdExtendedKey
        } else {
            null
        }

        var accountExtendedDisplayType = ShowExtendedKeyModule.DisplayKeyType.AccountPrivateKey(true)
        val accountExtendedPrivateKey = bip32RootKey
            ?: if (hdExtendedKey?.derivedType == HDExtendedKey.DerivedType.Account && !hdExtendedKey.isPublic) {
                accountExtendedDisplayType = ShowExtendedKeyModule.DisplayKeyType.AccountPrivateKey(false)
                hdExtendedKey
            } else {
                null
            }

        val stellarSecretKey = try {
            val stellarWallet = account.type.toStellarWallet()
            StellarKit.getSecretSeed(stellarWallet)
        } catch (e: Throwable) {
            null
        }

        val moneroKeys = ShowMoneroKeyModule.getPrivateMoneroKeys(account)

        viewState = PrivateKeysModule.ViewState(
            evmPrivateKey = ethereumPrivateKey,
            tronPrivateKey = tronPrivateKey,
            bip32RootKey = bip32RootKey?.let {
                PrivateKeysModule.ExtendedKey(it, ShowExtendedKeyModule.DisplayKeyType.Bip32RootKey)
            },
            accountExtendedPrivateKey = accountExtendedPrivateKey?.let {
                PrivateKeysModule.ExtendedKey(it, accountExtendedDisplayType)
            },
            stellarSecretKey = stellarSecretKey,
            moneroKeys = moneroKeys
        )
    }

    private fun toHexString(key: BigInteger): String {
        return key.toByteArray().let {
            if (it.size > 32) {
                it.copyOfRange(1, it.size)
            } else {
                it
            }.toRawHexString()
        }
    }
}
