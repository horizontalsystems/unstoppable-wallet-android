package cash.p.terminal.tangem.domain.model

import com.tangem.crypto.hdWallet.DerivationPath
import io.horizontalsystems.core.entities.BlockchainType

internal data class BlockchainToDerive(
    val blockchainType: BlockchainType,
    val derivationPath: DerivationPath?,
)