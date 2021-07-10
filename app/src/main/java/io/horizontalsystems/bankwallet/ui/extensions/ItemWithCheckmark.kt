package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R

class ItemWithCheckmark @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    private var itemTitle: TextView
    private var itemSubtitle: TextView
    private var checkMark: ImageView

    init {
        val rootView = inflate(context, R.layout.view_item_with_checkmark, this)
        itemTitle = rootView.findViewById(R.id.itemTitle)
        itemSubtitle = rootView.findViewById(R.id.itemSubtitle)
        checkMark = rootView.findViewById(R.id.checkMark)
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
