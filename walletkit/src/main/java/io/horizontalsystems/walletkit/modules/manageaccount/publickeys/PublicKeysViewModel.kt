package io.horizontalsystems.walletkit.modules.manageaccount.publickeys

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.walletkit.core.managers.EvmBlockchainManager
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
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
            tronAddress = tronAddress,
            chainKeyRows = chainKeyRows
        )
    }

}
