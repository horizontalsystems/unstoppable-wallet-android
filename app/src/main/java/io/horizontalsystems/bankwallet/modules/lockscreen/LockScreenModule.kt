package io.horizontalsystems.bankwallet.modules.lockscreen

import androidx.appcompat.app.AppCompatActivity

object LockScreenModule {

    fun startForUnlock(context: AppCompatActivity, requestCode: Int) {
        LockScreenActivity.startForResult(context, requestCode)
    }
}
