package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_setting_item_with_checkmark.view.*

class SettingItemWithCheckmark: FrameLayout {

    init {
        inflate(context, R.layout.view_setting_item_with_checkmark, this)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    fun bind(title: String, subtitle: String, onClick: () -> Unit, showBottomBorder: Boolean = false){
        setTitle(title)
        setSubtitle(subtitle)
        toggleBottomBorder(showBottomBorder)
        itemWrapper.setOnClickListener { onClick.invoke() }
    }

    fun setEnabledState(enabled: Boolean) {
        itemWrapper.isEnabled = enabled
        itemWrapper.alpha = if (enabled) 1f else 0.5f
    }

    fun setChecked(checked: Boolean){
        checkMark.isVisible = checked
    }

    fun setTitle(v: CharSequence) {
        itemTitle.text = v
    }

    fun setSubtitle(v: CharSequence) {
        itemSubtitle.text = v
    }

    fun toggleBottomBorder(visible: Boolean) {
        bottomBorderView.isVisible = visible
    }

}
