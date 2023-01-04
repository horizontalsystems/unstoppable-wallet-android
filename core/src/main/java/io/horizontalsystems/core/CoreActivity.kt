package io.horizontalsystems.core

import android.graphics.Color
import android.view.Gravity
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

abstract class CoreActivity : AppCompatActivity() {

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        if (CoreApp.instance.testMode) {
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
