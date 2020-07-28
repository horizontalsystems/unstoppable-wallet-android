package io.horizontalsystems.views

import android.content.Context
import android.util.AttributeSet
import android.widget.CompoundButton
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.view_settings_switch.view.*

class SettingsViewSwitch @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : SettingsViewBase(context, attrs, defStyleAttr) {

    private var onCheckedChangeListener: CompoundButton.OnCheckedChangeListener? = null

    fun showSwitch(isChecked: Boolean, listener: CompoundButton.OnCheckedChangeListener) {
        setOnCheckedChangeListener(listener)
        setChecked(isChecked)
    }

    fun switchToggle() {
        switchSettings.toggle()
    }

    fun setOnCheckedChangeListener(listener: CompoundButton.OnCheckedChangeListener?) {
        switchSettings.setOnCheckedChangeListener(listener)
        switchSettings.isVisible = true

        onCheckedChangeListener = listener
    }

    fun setChecked(checked: Boolean) {
        // set listener to null and set it back to prevent it from triggering
        switchSettings.setOnCheckedChangeListener(null)
        switchSettings.isChecked = checked
        switchSettings.setOnCheckedChangeListener(onCheckedChangeListener)
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
