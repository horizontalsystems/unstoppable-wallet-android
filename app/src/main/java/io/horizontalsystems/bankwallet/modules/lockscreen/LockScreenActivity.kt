package io.horizontalsystems.bankwallet.modules.lockscreen

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity

class LockScreenActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_lock_screen)
    }

    companion object {
        fun startForResult(context: Activity, requestCode: Int = 0) {
            val intent = Intent(context, LockScreenActivity::class.java)
            context.startActivityForResult(intent, requestCode)
        }
    }
}
