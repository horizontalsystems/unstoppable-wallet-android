package cash.p.terminal.tangem.domain.card

//import cash.p.terminal.tangem.domain.derivation.DerivationStyle
//import cash.p.terminal.tangem.domain.derivationPath
import com.tangem.crypto.hdWallet.DerivationPath
import io.horizontalsystems.core.entities.BlockchainType

/*
sealed class DerivationParams {

    data class Default(val style: DerivationStyle) : DerivationParams()

    data class Custom(val path: DerivationPath) : DerivationParams()

    fun getPath(blockchain: BlockchainType): DerivationPath? {
        return when (this) {
            is Custom -> path
            is Default -> blockchain.derivationPath(style)
        }
    }
}
*/
