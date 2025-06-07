package cash.p.terminal.tangem.domain.card

import com.tangem.common.card.Card
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import io.horizontalsystems.core.entities.BlockchainType

sealed interface CardConfig {

    val mandatoryCurves: List<EllipticCurve>

    fun primaryCurve(blockchain: BlockchainType): EllipticCurve?

    companion object {

        fun createConfig(card: Card): CardConfig {
            if (card.firmwareVersion >= FirmwareVersion.Ed25519Slip0010Available) {
                return Wallet2CardConfig
            }
            if (card.settings.isBackupAllowed && card.settings.isHDWalletAllowed &&
                card.firmwareVersion >= FirmwareVersion.MultiWalletAvailable
            ) {
                return MultiWalletCardConfig
            }
            if (card.supportedCurves.size == 1 &&
                card.supportedCurves.contains(EllipticCurve.Ed25519)
            ) {
                return EdSingleCurrencyCardConfig
            }
            return GenericCardConfig(card.settings.maxWalletsCount)
        }
    }
}
