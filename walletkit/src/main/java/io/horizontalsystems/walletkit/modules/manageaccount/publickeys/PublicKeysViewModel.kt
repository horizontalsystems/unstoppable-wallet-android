package io.horizontalsystems.walletkit.modules.manageaccount.publickeys

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.walletkit.core.managers.EvmKitManagerRegistry
import io.horizontalsystems.walletkit.core.managers.EvmBlockchainManager
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.tronkit.network.Network
import io.horizontalsystems.tronkit.transaction.Signer as TronSigner

class PublicKeysViewModel(
    account: Account,
    evmBlockchainManager: EvmBlockchainManager,
) : ViewModel() {

    var viewState by mutableStateOf(PublicKeysModule.ViewState())
        private set

    init {
        val evmAddress: String? = when (val accountType = account.type) {
            is AccountType.Mnemonic -> {
                val chain = EvmKitManagerRegistry.getChain(BlockchainType.Ethereum)
                Signer.address(accountType.words, accountType.passphrase, chain).eip55
            }

            is AccountType.EvmPrivateKey -> {
                Signer.address(accountType.key).eip55
            }

            else -> null
        }

        val tronAddress: String? = when (val accountType = account.type) {
            is AccountType.Mnemonic -> {
                val privateKey = TronSigner.privateKey(accountType.seed, Network.Mainnet)
                TronSigner.address(privateKey, Network.Mainnet).base58
            }

            is AccountType.TronPrivateKey -> {
                TronSigner.address(accountType.key, Network.Mainnet).base58
            }

            else -> null
        }

        val chainKeyRows = ChainRegistry.all.flatMap { it.publicKeyRows(account) }

        viewState = PublicKeysModule.ViewState(
            evmAddress = evmAddress,
            tronAddress = tronAddress,
            chainKeyRows = chainKeyRows
        )
    }

}
