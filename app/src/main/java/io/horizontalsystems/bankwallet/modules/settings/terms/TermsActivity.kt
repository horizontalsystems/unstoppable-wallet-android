package io.horizontalsystems.bankwallet.modules.settings.terms

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import kotlinx.android.synthetic.main.activity_terms_settings.*

class TermsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms_settings)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    companion object {
        fun start(context: Activity) {
            val intent = Intent(context, TermsActivity::class.java)
            context.startActivity(intent)
        }
    }
}
