package cash.p.terminal.modules.evmprivatekey

import androidx.lifecycle.ViewModel
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.core.toRawHexString
import cash.p.terminal.entities.Account
import cash.p.terminal.entities.AccountType
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
