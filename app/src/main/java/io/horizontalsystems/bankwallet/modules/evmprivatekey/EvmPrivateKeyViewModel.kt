package io.horizontalsystems.bankwallet.modules.evmprivatekey

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.toRawHexString
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.ethereumkit.core.stripHexPrefix
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.marketkit.models.BlockchainType

class EvmPrivateKeyViewModel(
    account: Account,
    evmBlockchainManager: EvmBlockchainManager
) : ViewModel() {

    val ethereumPrivateKey by lazy {
        when (val accountType = account.type) {
            is AccountType.Mnemonic -> {
                Signer.privateKey(
                    accountType.words,
                    accountType.passphrase,
                    evmBlockchainManager.getChain(BlockchainType.Ethereum)
                ).toByteArray().let {
                    if (it.size > 32) {
                        it.copyOfRange(1, it.size)
                    } else {
                        it
                    }.toRawHexString()
                }
            }
            is AccountType.EvmPrivateKey -> accountType.key.toHexString().stripHexPrefix()
            else -> ""
        }
    }

}
