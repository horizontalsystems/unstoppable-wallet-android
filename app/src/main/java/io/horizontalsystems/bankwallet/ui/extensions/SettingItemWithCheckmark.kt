package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.ListPosition
import kotlinx.android.synthetic.main.view_settings_item_with_checkmark.view.*

class SettingItemWithCheckmark @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.view_settings_item_with_checkmark, this)
    }

    fun bind(title: String, subtitle: String, checked: Boolean, onClick: () -> Unit, listPosition: ListPosition) {
        itemTitle.text = title
        itemSubtitle.text = subtitle
        checkMark.isVisible = checked
        setOnClickListener { onClick.invoke() }
        backgroundView.setBackgroundResource(listPosition.getBackground())
    }

}
