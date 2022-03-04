package io.horizontalsystems.bankwallet.modules.walletconnect.entity

import androidx.room.Entity
import com.trustwallet.walletconnect.models.WCPeerMeta
import com.trustwallet.walletconnect.models.session.WCSession
import java.util.*

@Entity(primaryKeys = ["remotePeerId"])
data class WalletConnectSession(
        val chainId: Int,
        val accountId: String,
        val session: WCSession,
        val peerId: String,
        val remotePeerId: String,
        val remotePeerMeta: WCPeerMeta,
        val isAutoSign: Boolean = false,
        val date: Date = Date()
)
