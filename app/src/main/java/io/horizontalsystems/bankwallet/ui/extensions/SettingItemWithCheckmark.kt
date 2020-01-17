package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_setting_item_with_checkmark.view.*

class SettingItemWithCheckmark: FrameLayout {

    init {
        inflate(context, R.layout.view_setting_item_with_checkmark, this)
    }

    private var showBottomBorder: Boolean = false
    private var title: String = ""
    private var subtitle: String = ""

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        loadAttributes(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        loadAttributes(attrs)
    }

    private fun loadAttributes(attrs: AttributeSet) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.SettingItemWithCheckmark, 0, 0)
        try {
            title = ta.getString(R.styleable.SettingItemWithCheckmark_title) ?: ""
            subtitle = ta.getString(R.styleable.SettingItemWithCheckmark_subtitle) ?: ""
            showBottomBorder = ta.getBoolean(R.styleable.SettingItemWithCheckmark_bottomBorder, false)
        } finally {
            ta.recycle()
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        itemTitle.text = title
        itemSubtitle.text = subtitle
        bottomBorderView.visibility = if (showBottomBorder) View.VISIBLE else View.GONE
    }

    fun setClick(onClick: () -> Unit){
        itemWrapper.setOnClickListener { onClick.invoke() }
    }

    fun bindSelection(selected: Boolean){
        checkMark.visibility = if (selected) View.VISIBLE else View.GONE
    }
}
