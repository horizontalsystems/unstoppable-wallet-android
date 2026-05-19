package cash.p.terminal.core.managers

import io.horizontalsystems.litecoinkit.MainNetLitecoin

class LitecoinBirthdayProvider {
    private val mainNetLitecoin = MainNetLitecoin()

    fun getLatestCheckpointBlockHeight(): Long {
        return mainNetLitecoin.lastCheckpoint.block.height.toLong()
    }
}
