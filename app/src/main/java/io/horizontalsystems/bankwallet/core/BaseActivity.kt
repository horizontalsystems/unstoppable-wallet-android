package io.horizontalsystems.bankwallet.core

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.core.CoreApp

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.decorView.layoutDirection = if (CoreApp.instance.isLocaleRTL()) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        //make bottom gesture navigation bar color same as bottom navigation bar
        window.navigationBarColor = ContextCompat.getColor(this, R.color.blade)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(CoreApp.instance.localeAwareContext(newBase))
    }
}
