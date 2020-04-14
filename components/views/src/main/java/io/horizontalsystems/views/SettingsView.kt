package io.horizontalsystems.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.CompoundButton
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.cell_label.view.*
import kotlinx.android.synthetic.main.cell_label_right.view.*
import kotlinx.android.synthetic.main.view_setting_item.view.*

class SettingsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    fun showLabel(text: String?) {
        cellLabel.text = text
    }

    fun showIcon(drawable: Drawable?) {
        cellIcon.showIf(drawable != null)
        drawable?.let { cellIcon.bind(it) }
    }

    fun showRightLabel(text: String?) {
        cellLabelRight.text = text
        cellLabelRight.showIf(text != null)
    }

    fun showRightIcon(drawable: Drawable?) {
        rightIcon.showIf(drawable != null)
        drawable?.let { rightIcon.setImageDrawable(drawable) }
    }

    fun showAttention(show: Boolean) {
        attentionIcon.showIf(show)
    }

    fun showBottomBorder(visible: Boolean) {
        bottomBorder.showIf(visible)
    }

    fun showSwitch(isChecked: Boolean, listener: CompoundButton.OnCheckedChangeListener) {
        switchView.setOnCheckedChangeListener(null)
        switchView.isChecked = isChecked
        switchView.visibility = View.VISIBLE
        switchView.setOnCheckedChangeListener(listener)
    }

    fun switchToggle() {
        switchView.toggle()
    }

    init {
        inflate(context, R.layout.view_setting_item, this)

        val attributes = context.obtainStyledAttributes(attrs, R.styleable.SettingsView)
        try {
            showIcon(attributes.getDrawable(R.styleable.SettingsView_icon))
            showRightIcon(attributes.getDrawable(R.styleable.SettingsView_iconRight))
            showLabel(attributes.getString(R.styleable.SettingsView_title))
            showRightLabel(attributes.getString(R.styleable.SettingsView_value))
            showBottomBorder(attributes.getBoolean(R.styleable.SettingsView_bottomBorder, false))
        } finally {
            attributes.recycle()
        }
    }
}
