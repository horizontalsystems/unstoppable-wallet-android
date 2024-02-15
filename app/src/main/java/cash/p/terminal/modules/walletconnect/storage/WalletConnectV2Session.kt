package cash.p.terminal.modules.walletconnect.storage
>>>>>>>> 3a48e845b (Refactor WalletConnect, use Web3Wallet API):app/src/main/java/cash/p/terminal/modules/walletconnect/storage/WalletConnectV2Session.kt

import androidx.room.Entity

@Entity(primaryKeys = ["accountId", "topic"])
data class WalletConnectV2Session(
        val accountId: String,
        val topic: String,
)
