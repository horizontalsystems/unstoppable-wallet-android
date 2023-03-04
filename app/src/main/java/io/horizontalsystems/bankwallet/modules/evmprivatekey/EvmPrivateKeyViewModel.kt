package io.horizontalsystems.bankwallet.modules.evmprivatekey

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.toRawHexString
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.marketkit.models.BlockchainType
import java.math.BigInteger

class EvmPrivateKeyViewModel(
    account: Account,
    evmBlockchainManager: EvmBlockchainManager
) : ViewModel() {

    val ethereumPrivateKey by lazy {
        when (val accountType = account.type) {
            is AccountType.Mnemonic -> {
                val chain = evmBlockchainManager.getChain(BlockchainType.Ethereum)
                toHexString(Signer.privateKey(accountType.words, accountType.passphrase, chain))
            }
            is AccountType.EvmPrivateKey -> toHexString(accountType.key)
            else -> ""
        }
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
