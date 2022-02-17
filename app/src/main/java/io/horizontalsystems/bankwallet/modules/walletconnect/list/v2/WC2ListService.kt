package io.horizontalsystems.bankwallet.modules.walletconnect.list.v2

import com.walletconnect.walletconnectv2.client.WalletConnect
import com.walletconnect.walletconnectv2.client.WalletConnectClient
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SessionManager

class WC2ListService(private val sessionManager: WC2SessionManager) {

    val sessions: List<WalletConnect.Model.SettledSession>
        get() = WalletConnectClient.getListOfSettledSessions()


}
