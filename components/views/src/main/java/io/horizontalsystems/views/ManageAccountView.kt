package io.horizontalsystems.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_manage_account_view.view.*

class ManageAccountView : LinearLayout {

    init {
        inflate(context, R.layout.view_manage_account_view, this)
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun bind(title: String, type: AccountButtonItemType, showAttentionIcon: Boolean = false, isLast: Boolean = false, onClick: () -> Unit) {
        when (type) {
            AccountButtonItemType.SimpleButton -> {
                rightArrow.visibility = View.VISIBLE
                redTitle.visibility = View.GONE
                normalTitle.visibility = View.VISIBLE

                normalTitle.text = title
            }
            AccountButtonItemType.RedButton -> {
                rightArrow.visibility = View.GONE
                redTitle.visibility = View.VISIBLE
                normalTitle.visibility = View.GONE

                redTitle.text = title
            }
        }

        attentionIcon.visibility = if (showAttentionIcon) View.VISIBLE else View.GONE
        itemWrapper.setOnClickListener { onClick.invoke() }
        itemWrapper.setBackgroundResource(
                if (isLast) R.drawable.manage_account_last_button_background
                else R.drawable.clickable_background_color_lawrence
        )
    }
}

enum class AccountButtonItemType {
    SimpleButton,
    RedButton
}
