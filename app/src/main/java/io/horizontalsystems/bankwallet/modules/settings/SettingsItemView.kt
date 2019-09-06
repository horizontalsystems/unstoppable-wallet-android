package io.horizontalsystems.bankwallet.modules.settings

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R

class SettingsItemView : ConstraintLayout {

    init {
        inflate(context, R.layout.view_setting_item, this)
    }

    private var attrTitle: String? = null
    private var attrIcon: Drawable? = null
    private var attrArrow: Boolean = false

    private lateinit var settingIconImageView: ImageView
    private lateinit var titleTextView: TextView
    private lateinit var selectedValueTextView: TextView
    private lateinit var switch: Switch
    private lateinit var badgeImageView: ImageView
    private lateinit var arrowImageView: ImageView

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        loadAttributes(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        loadAttributes(attrs)
    }

    private fun loadAttributes(attrs: AttributeSet) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.SettingsItemView, 0, 0)
        try {
            attrTitle = ta.getString(R.styleable.SettingsItemView_setting_title)
            attrIcon = ta.getDrawable(R.styleable.SettingsItemView_setting_icon)
            attrArrow = ta.getBoolean(R.styleable.SettingsItemView_setting_arrow, false)
        } finally {
            ta.recycle()
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        settingIconImageView = findViewById(R.id.setting_icon)
        titleTextView = findViewById(R.id.setting_title)
        selectedValueTextView = findViewById(R.id.setting_value)
        switch = findViewById(R.id.setting_switch)
        switch.isClickable = false
        badgeImageView = findViewById(R.id.setting_badge)
        arrowImageView = findViewById(R.id.setting_arrow_right)

        settingIconImageView.apply {
            visibility = if (attrIcon == null) View.GONE else View.VISIBLE
            setImageDrawable(attrIcon)
        }

        titleTextView.text = attrTitle
        badgeImageView.visibility = View.GONE
        arrowImageView.visibility = if (attrArrow) View.VISIBLE else View.GONE
    }

    var switchIsChecked: Boolean = false
        set(isChecked) {
            switch.setOnCheckedChangeListener(null)
            switch.isChecked = isChecked
            switch.visibility = View.VISIBLE
            field = isChecked
            switch.setOnCheckedChangeListener(switchOnCheckedChangeListener)
            invalidate()
        }

    var switchOnCheckedChangeListener: CompoundButton.OnCheckedChangeListener? = null
        set(value) {
            field = value
            switch.setOnCheckedChangeListener(value)
            invalidate()
        }

    fun switchToggle() {
        switch.toggle()
    }

    var selectedValue: String? = null
        set(value) {
            selectedValueTextView.visibility = if (value != null) View.VISIBLE else View.GONE
            selectedValueTextView.text = value
            field = value
            invalidate()
        }

    fun setInfoBadgeVisibility(isVisible: Boolean) {
        badgeImageView.visibility = if (isVisible) View.VISIBLE else View.GONE
    }
}
