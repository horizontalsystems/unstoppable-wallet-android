package io.horizontalsystems.views

import android.content.Context
import android.util.AttributeSet
import android.widget.CompoundButton
import android.widget.ImageView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible

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
        findViewById<ImageView>(R.id.attentionIcon)?.isVisible = show
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
        findViewById<SwitchCompat>(R.id.switchSettings)?.apply {
            isVisible = true
            notifyListener = false
            isChecked = checked
            notifyListener = true
        }
    }

    fun switchToggle() {
        findViewById<SwitchCompat>(R.id.switchSettings)?.toggle()
    }

    init {
        inflate(context, R.layout.view_settings_item_switch, this)

        findViewById<SwitchCompat>(R.id.switchSettings)?.setOnCheckedChangeListener(this)

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
