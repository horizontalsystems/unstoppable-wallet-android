package io.horizontalsystems.views

import android.content.Context
import android.util.AttributeSet
import android.widget.CompoundButton
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.view_settings_item_switch.view.*

class SettingsViewSwitch @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : SettingsViewBase(context, attrs, defStyleAttr), CompoundButton.OnCheckedChangeListener {

    private var onCheckedChangeListener: CompoundButton.OnCheckedChangeListener? = null
    private var notifyListener = true

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        if (notifyListener) {
            onCheckedChangeListener?.onCheckedChanged(buttonView, isChecked)
        }
    }

    fun showAttention(show: Boolean) {
        attentionIcon.isVisible = show
    }

    fun setOnCheckedChangeListener(listener: (Boolean) -> Unit) {
        onCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            listener(isChecked)
        }
    }

    fun setOnCheckedChangeListenerSingle(listener: (Boolean) -> Unit) {
        onCheckedChangeListener = object : SingleSwitchListener() {
            override fun onSingleSwitch(buttonView: CompoundButton?, isChecked: Boolean) {
                listener(isChecked)
            }
        }
    }

    fun setChecked(checked: Boolean) {
        switchSettings.isVisible = true

        notifyListener = false
        switchSettings.isChecked = checked
        notifyListener = true
    }

    fun switchToggle() {
        switchSettings.toggle()
    }

    init {
        inflate(context, R.layout.view_settings_item_switch, this)

        switchSettings.setOnCheckedChangeListener(this)

        val attributes = context.obtainStyledAttributes(attrs, R.styleable.SettingsViewSwitch)
        try {
            showTitle(attributes.getString(R.styleable.SettingsViewSwitch_title))
            showSubtitle(attributes.getString(R.styleable.SettingsViewSwitch_subtitle))
            showIcon(attributes.getDrawable(R.styleable.SettingsViewSwitch_icon))
            setListPosition(attributes.getInt(R.styleable.SettingsViewSwitch_position, 0))
        } finally {
            attributes.recycle()
        }
    }
}
