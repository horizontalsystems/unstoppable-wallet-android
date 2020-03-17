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

    private var title: String = ""
    private var subtitle: String = ""

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    fun bind(title: String, subtitle: String, onClick: () -> Unit, showBottomBorder: Boolean = false){
        itemTitle.text = title
        itemSubtitle.text = subtitle
        itemWrapper.setOnClickListener { onClick.invoke() }
        bottomBorderView.visibility = if (showBottomBorder) View.VISIBLE else View.GONE
    }

    fun setChecked(checked: Boolean){
        checkMark.visibility = if (checked) View.VISIBLE else View.GONE
    }
}
