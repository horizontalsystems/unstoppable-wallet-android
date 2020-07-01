package io.horizontalsystems.views

import android.content.Context
import android.util.AttributeSet
import android.widget.CompoundButton
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.view_settings_switch.view.*

class SettingsViewSwitch @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : SettingsViewBase(context, attrs, defStyleAttr) {

    fun showSwitch(isChecked: Boolean, listener: CompoundButton.OnCheckedChangeListener) {
        switchSettings.setOnCheckedChangeListener(null)
        switchSettings.isChecked = isChecked
        switchSettings.isVisible = true
        switchSettings.setOnCheckedChangeListener(listener)
    }

    fun switchToggle() {
        switchSettings.toggle()
    }

    init {
        inflate(context, R.layout.view_settings_switch, this)

        val attributes = context.obtainStyledAttributes(attrs, R.styleable.SettingsViewSwitch)
        try {
            showTitle(attributes.getString(R.styleable.SettingsViewSwitch_title))
            showSubtitle(attributes.getString(R.styleable.SettingsViewSwitch_subtitle))
            showIcon(attributes.getDrawable(R.styleable.SettingsViewSwitch_icon))
            showBottomBorder(attributes.getBoolean(R.styleable.SettingsViewSwitch_bottomBorder, false))
        } finally {
            attributes.recycle()
        }
    }
}
