package io.horizontalsystems.bankwallet.modules.restore

import android.os.Bundle
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity

class RestoreActivity: BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_restore)

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragmentContainerView, RestoreFragment.instance())
            commit()
        }

    }

}
