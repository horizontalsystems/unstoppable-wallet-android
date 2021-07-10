package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.ListPosition

class SettingItemWithCheckmark @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    private var itemTitle: TextView
    private var itemSubtitle: TextView
    private var checkMark: ImageView
    private var backgroundView: View

    init {
        val rootView = inflate(context, R.layout.view_settings_item_with_checkmark, this)
        itemTitle = rootView.findViewById(R.id.itemTitle)
        itemSubtitle = rootView.findViewById(R.id.itemSubtitle)
        checkMark = rootView.findViewById(R.id.checkMark)
        backgroundView = rootView.findViewById(R.id.backgroundView)
    }

    fun bind(title: String, subtitle: String, checked: Boolean, onClick: () -> Unit, listPosition: ListPosition) {
        itemTitle.text = title
        itemSubtitle.text = subtitle
        checkMark.isVisible = checked
        setOnClickListener { onClick.invoke() }
        backgroundView.setBackgroundResource(listPosition.getBackground())
    }

}
