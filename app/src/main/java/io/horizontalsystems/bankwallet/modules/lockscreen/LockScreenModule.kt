package io.horizontalsystems.bankwallet.modules.lockscreen

import android.app.Activity

object LockScreenModule {

    fun startForUnlock(context: Activity, requestCode: Int) {
        LockScreenActivity.startForResult(context, requestCode)
    }
}
