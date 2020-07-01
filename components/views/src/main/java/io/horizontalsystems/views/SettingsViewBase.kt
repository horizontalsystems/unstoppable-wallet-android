package io.horizontalsystems.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.synthetic.main.view_settings_left.view.*
import kotlinx.android.synthetic.main.view_settings_item.view.*

abstract class SettingsViewBase @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    private val singleLineHeight = 44f
    private val doubleLineHeight = 60f

    fun showSubtitle(text: String?) {
        settingsSubtitle.text = text
        settingsSubtitle.isVisible = text != null
        layoutParams?.height = LayoutHelper.dp(if (text == null) singleLineHeight else doubleLineHeight, context)
    }

    fun showTitle(text: String?) {
        settingsTitle.text = text
    }

    fun showIcon(drawable: Drawable?) {
        settingsIcon.isVisible = drawable != null
        settingsIcon.setImageDrawable(drawable)
    }

    fun showBottomBorder(visible: Boolean) {
        bottomBorder.isVisible = visible
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (!settingsSubtitle.text.isNullOrBlank()) {
            layoutParams.height = LayoutHelper.dp(doubleLineHeight, context)
        }
    }
}
