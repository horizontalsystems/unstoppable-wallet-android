package cash.p.terminal.modules.blockchainsettings

import cash.p.terminal.core.managers.BtcBlockchainManager
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.core.managers.SolanaRpcSourceManager
import cash.p.terminal.core.order
import cash.p.terminal.wallet.MarketKitWrapper
import io.horizontalsystems.core.entities.Blockchain

internal object SupportedBlockchainsFactory {

    fun create(
        btcBlockchainManager: BtcBlockchainManager,
        evmBlockchainManager: EvmBlockchainManager,
        solanaRpcSourceManager: SolanaRpcSourceManager,
        marketKit: MarketKitWrapper,
    ): SupportedBlockchains {
        val solanaBlockchains = listOfNotNull(solanaRpcSourceManager.blockchain)
        val statusOnlyBlockchains = marketKit.blockchains(
            BlockchainSettingsModule.statusOnlyBlockchainTypes.map { it.uid }
        )

        return SupportedBlockchains(
            btcBlockchains = btcBlockchainManager.allBlockchains,
            evmBlockchains = evmBlockchainManager.allBlockchains,
            solanaBlockchains = solanaBlockchains,
            statusOnlyBlockchains = statusOnlyBlockchains,
        )
    }
}

internal data class SupportedBlockchains(
    val btcBlockchains: List<Blockchain>,
    val evmBlockchains: List<Blockchain>,
    val solanaBlockchains: List<Blockchain>,
    val statusOnlyBlockchains: List<Blockchain>,
) {
    val all: List<Blockchain>
        get() = (
            btcBlockchains +
                evmBlockchains +
                solanaBlockchains +
                statusOnlyBlockchains
            )
            .distinctBy { it.uid }
            .sortedBy { it.type.order }
}
