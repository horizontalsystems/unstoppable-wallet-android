package io.horizontalsystems.core

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

abstract class CoreActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        window.decorView.layoutDirection = if (CoreApp.instance.isLocaleRTL()) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(CoreApp.instance.localeAwareContext(newBase))
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        if (CoreApp.buildConfigProvider.testMode) {
            val rootView = findViewById<ViewGroup>(android.R.id.content)
            val testLabelTv = TextView(this)
            testLabelTv.text = "Test"
            testLabelTv.setPadding(5, 3, 5, 3)
            testLabelTv.includeFontPadding = false
            testLabelTv.setBackgroundColor(Color.RED)
            testLabelTv.setTextColor(Color.WHITE)
            testLabelTv.textSize = 12f
            val layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
            layoutParams.gravity = Gravity.CENTER_HORIZONTAL
            testLabelTv.layoutParams = layoutParams
            rootView.addView(testLabelTv)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}
