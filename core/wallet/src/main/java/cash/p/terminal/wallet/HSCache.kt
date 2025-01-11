package cash.p.terminal.wallet

import java.io.File

object HSCache {
    var cacheDir: File? = null
    var cacheQuotaBytes: Long = 0
}
