package io.horizontalsystems.walletkit.modules.manageaccount.privatekeys

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.walletkit.core.managers.EvmKitManagerRegistry
import io.horizontalsystems.walletkit.core.managers.EvmBlockchainManager
import io.horizontalsystems.walletkit.core.toRawHexString
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.marketkit.models.BlockchainType
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
                val chain = EvmKitManagerRegistry.getChain(BlockchainType.Ethereum)
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

        val chainKeyRows = ChainRegistry.all.flatMap { it.privateKeyRows(account) }

        viewState = PrivateKeysModule.ViewState(
            evmPrivateKey = ethereumPrivateKey,
            tronPrivateKey = tronPrivateKey,
            chainKeyRows = chainKeyRows
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
