package cash.p.terminal.modules.manageaccount.publickeys

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.adapters.zcash.ZcashAdapter
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.modules.manageaccount.publickeys.PublicKeysModule.ExtendedPublicKey
import cash.p.terminal.modules.manageaccount.showextendedkey.ShowExtendedKeyModule.DisplayKeyType.AccountPublicKey
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.entities.TokenType.AddressSpecType
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.hdwalletkit.HDExtendedKey
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.Mnemonic
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

class PublicKeysViewModel(
    account: Account,
    evmBlockchainManager: EvmBlockchainManager,
) : ViewModel() {

    var viewState by mutableStateOf(PublicKeysModule.ViewState())
        private set

    private val marketKitWrapper: MarketKitWrapper by inject(MarketKitWrapper::class.java)
    private val adapterManager: IAdapterManager by inject(IAdapterManager::class.java)

    init {
        val evmAddress: String? = when (val accountType = account.type) {
            is AccountType.Mnemonic -> {
                val chain = evmBlockchainManager.getChain(BlockchainType.Ethereum)
                Signer.address(accountType.words, accountType.passphrase, chain).eip55
            }

            is AccountType.EvmPrivateKey -> {
                Signer.address(accountType.key).eip55
            }

            is AccountType.EvmAddress -> accountType.address
            is AccountType.SolanaAddress -> accountType.address
            is AccountType.TronAddress -> accountType.address
            else -> null
        }

        val hdExtendedKey = (account.type as? AccountType.HdExtendedKey)?.hdExtendedKey
        var accountPublicKey = AccountPublicKey(false)

        val publicKey = if (account.type is AccountType.Mnemonic) {
            accountPublicKey = AccountPublicKey(true)
            val seed = Mnemonic().toSeed(
                (account.type as AccountType.Mnemonic).words,
                (account.type as AccountType.Mnemonic).passphrase
            )
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

        viewModelScope.launch(Dispatchers.Default + CoroutineExceptionHandler { _, _ -> }) {
            requestZCashUfvk()
        }

        viewState = PublicKeysModule.ViewState(
            evmAddress = evmAddress,
            extendedPublicKey = publicKey?.let { ExtendedPublicKey(it, accountPublicKey) }
        )
    }

    private suspend fun requestZCashUfvk() {
        getZCashUfvk()?.let {
            viewState = viewState.copy(
                zcashUfvk = it
            )
        }
    }

    /***
     * Check all address types and find the first one that has a valid ufvk
     * @return ufvk or null if not found
     */
    private suspend fun getZCashUfvk(): String? {
        AddressSpecType.entries.forEach { type ->
            val tokenQuery = TokenQuery(
                BlockchainType.Zcash,
                TokenType.AddressSpecTyped(type)
            )
            marketKitWrapper.token(tokenQuery)
                ?.let { adapterManager.getAdapterForToken<ZcashAdapter>(it) }
                ?.getFirstAccount()?.ufvk?.let { zcashUfvk ->
                    return zcashUfvk
                }
        }

        return null
    }

}
