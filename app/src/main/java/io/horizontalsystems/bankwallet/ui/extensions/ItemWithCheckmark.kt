package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_item_with_checkmark.view.*

class ItemWithCheckmark @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.view_item_with_checkmark, this)
    }

    fun bind(title: String, subtitle: String, checked: Boolean) {
        itemTitle.text = title
        itemSubtitle.text = subtitle
        checkMark.isVisible = checked
    }

    fun setEnabledState(enabled: Boolean) {
        isEnabled = enabled
        alpha = if (enabled) 1f else 0.5f
    }

    fun setChecked(checked: Boolean) {
        checkMark.isVisible = checked
    }

}
