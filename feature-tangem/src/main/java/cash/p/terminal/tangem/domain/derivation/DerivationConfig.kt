package cash.p.terminal.tangem.domain.derivation

import cash.p.terminal.tangem.domain.address.AddressType
import com.tangem.crypto.hdWallet.DerivationPath
import io.horizontalsystems.core.entities.BlockchainType

@Suppress("UnnecessaryAbstractClass")
abstract class DerivationConfig {

    abstract fun derivations(blockchainType: BlockchainType, customPurpose: String): Map<AddressType, DerivationPath>
}