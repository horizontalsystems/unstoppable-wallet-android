package cash.p.terminal.core.adapters

import com.m2049r.xmrwallet.service.MoneroWalletService
import org.koin.java.KoinJavaComponent.inject

class MoneroAdapter() {
    private val moneroWalletService: MoneroWalletService by inject(MoneroWalletService::class.java)
}