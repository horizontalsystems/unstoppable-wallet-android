package io.horizontalsystems.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.view_manage_account_view.view.*

class ManageAccountView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {

    init {
        inflate(context, R.layout.view_manage_account_view, this)

        val attributes = context.obtainStyledAttributes(attrs, R.styleable.ManageAccountView)
        try {
            setTitle(attributes.getString(R.styleable.ManageAccountView_title))
            showRightArrow(attributes.getBoolean(R.styleable.ManageAccountView_arrow, false))
            showAttentionIcon(attributes.getBoolean(R.styleable.ManageAccountView_attention, false))
            setBottomRounded(attributes.getBoolean(R.styleable.ManageAccountView_roundBottom, false))
            setTextColor(attributes.getColor(R.styleable.ManageAccountView_textColor, 0))
        } finally {
            attributes.recycle()
        }
    }

    fun setTitle(@StringRes titleText: Int) {
        title.setText(titleText)
    }

    private fun setTitle(titleText: String?) {
        title.text = titleText
    }

    private fun showAttentionIcon(showAttentionIcon: Boolean) {
        attentionIcon.isVisible = showAttentionIcon
    }

    private fun showRightArrow(show: Boolean) {
        rightArrow.isVisible = show
    }

    private fun setTextColor(type: Int) {
        title.setTextColor(type)
    }

    private fun setBottomRounded(isLast: Boolean = false) {
        setBackgroundResource(getBackgroundResource(isLast))
    }

    private fun getBackgroundResource(isLast: Boolean) = when {
        isLast -> R.drawable.manage_account_last_button_background
        else -> R.drawable.clickable_background_color_lawrence
    }
}
