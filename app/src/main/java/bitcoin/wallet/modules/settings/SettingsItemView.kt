package bitcoin.wallet.modules.settings

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import bitcoin.wallet.R

class SettingsItemView : ConstraintLayout {

    private var attrTitle: String? = null
    private var attrIcon: Drawable? = null

    private lateinit var settingIconImageView: ImageView
    private lateinit var titleTextView: TextView
    private lateinit var selectedValueTextView: TextView
    private lateinit var switch: Switch
    private lateinit var checkMarkImageView: ImageView
    private lateinit var badgeImageView: ImageView

    var titleTextColor: Int? = null
        set(colorId) {
            colorId?.let {
                titleTextView.setTextColor(resources.getColor(colorId, null))
            }
        }

    var switchIsChecked: Boolean = false
        set(isChecked) {
            switch.isChecked = isChecked
            switch.visibility = if (isChecked) View.VISIBLE else View.GONE
            invalidate()
        }

    var switchOnCheckedChangeListener: CompoundButton.OnCheckedChangeListener? = null
        set(value) {
            switch.setOnCheckedChangeListener(value)
        }

    fun switchToggle() {
        switch.toggle()
    }

    var selectedValue: String? = null
        set(value) {
            selectedValueTextView.visibility = if (value != null) View.VISIBLE else View.GONE
            selectedValueTextView.text = value
            invalidate()
        }

    var checkMarkIsShown: Boolean = false
        set(isShown) {
            checkMarkImageView.visibility = if (isShown) View.VISIBLE else View.GONE
            invalidate()
        }

    var badge: Drawable? = null
        set(badge) {
            badgeImageView.visibility = if (badge != null) View.VISIBLE else View.GONE
            badgeImageView.setImageDrawable(badge)
            invalidate()
        }

    constructor(context: Context) : super(context) {
        initializeViews()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initializeViews()
        loadAttributes(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initializeViews()
        loadAttributes(attrs)
    }


    private fun initializeViews() {
        ConstraintLayout.inflate(context, R.layout.view_setting_item, this)
    }

    private fun loadAttributes(attrs: AttributeSet) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.SettingsItemView, 0, 0)
        try {
            attrTitle = ta.getString(R.styleable.SettingsItemView_setting_title)
            attrIcon = ta.getDrawable(R.styleable.SettingsItemView_setting_icon)
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
        checkMarkImageView = findViewById(R.id.setting_check_mark)
        badgeImageView = findViewById(R.id.setting_badge)

        settingIconImageView.apply {
            visibility = if (attrIcon == null) View.GONE else View.VISIBLE
            setImageDrawable(attrIcon)
        }

        titleTextView.text = attrTitle

    }

}
