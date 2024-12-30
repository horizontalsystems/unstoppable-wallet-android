package cash.p.terminal.modules.manageaccount.publickeys

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.modules.manageaccount.publickeys.PublicKeysModule.ExtendedPublicKey
import cash.p.terminal.modules.manageaccount.showextendedkey.ShowExtendedKeyModule.DisplayKeyType.AccountPublicKey
import cash.p.terminal.wallet.AccountType
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.hdwalletkit.HDExtendedKey
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.core.entities.BlockchainType

class PublicKeysViewModel(
    account: cash.p.terminal.wallet.Account,
    evmBlockchainManager: EvmBlockchainManager,
) : ViewModel() {

    var viewState by mutableStateOf(PublicKeysModule.ViewState())
        private set

    init {
        val evmAddress: String? = when (val accountType = account.type) {
            is cash.p.terminal.wallet.AccountType.Mnemonic -> {
                val chain = evmBlockchainManager.getChain(BlockchainType.Ethereum)
                Signer.address(accountType.words, accountType.passphrase, chain).eip55
            }
            is cash.p.terminal.wallet.AccountType.EvmPrivateKey -> {
                Signer.address(accountType.key).eip55
            }
            is cash.p.terminal.wallet.AccountType.EvmAddress -> accountType.address
            is cash.p.terminal.wallet.AccountType.SolanaAddress -> accountType.address
            is cash.p.terminal.wallet.AccountType.TronAddress -> accountType.address
            else -> null
        }

        val hdExtendedKey = (account.type as? cash.p.terminal.wallet.AccountType.HdExtendedKey)?.hdExtendedKey
        var accountPublicKey = AccountPublicKey(false)

        val publicKey = if (account.type is cash.p.terminal.wallet.AccountType.Mnemonic) {
            accountPublicKey = AccountPublicKey(true)
            val seed = Mnemonic().toSeed((account.type as AccountType.Mnemonic).words, (account.type as AccountType.Mnemonic).passphrase)
            HDExtendedKey(seed, HDWallet.Purpose.BIP44)
        } else if (hdExtendedKey?.derivedType == HDExtendedKey.DerivedType.Master) {
            accountPublicKey = AccountPublicKey(true)
            hdExtendedKey
        } else if (hdExtendedKey?.derivedType == HDExtendedKey.DerivedType.Account && !hdExtendedKey.isPublic) {
            hdExtendedKey
        } else if (hdExtendedKey?.derivedType == HDExtendedKey.DerivedType.Account && hdExtendedKey.isPublic) {
            hdExtendedKey
        } else {
            null
        }

        viewState = PublicKeysModule.ViewState(
            evmAddress = evmAddress,
            extendedPublicKey = publicKey?.let { ExtendedPublicKey(it, accountPublicKey) }
        )
    }

}
