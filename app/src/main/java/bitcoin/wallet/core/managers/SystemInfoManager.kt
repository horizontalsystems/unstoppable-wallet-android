package bitcoin.wallet.core.managers

import android.support.v4.BuildConfig
import bitcoin.wallet.core.ISystemInfoManager

class SystemInfoManager: ISystemInfoManager {
    override var appVersion: String = BuildConfig.VERSION_NAME
}
