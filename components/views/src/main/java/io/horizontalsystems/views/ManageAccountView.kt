package io.horizontalsystems.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.synthetic.main.view_manage_account_view.view.*

class ManageAccountView : LinearLayout {

    init {
        inflate(context, R.layout.view_manage_account_view, this)
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun bind(titleText: String, type: AccountButtonItemType, showAttentionIcon: Boolean = false, isLast: Boolean = false, onClick: () -> Unit) {
        title.text = titleText

        getTextColorForType(type)?.let {
            title.setTextColor(it)
        }

        rightArrow.visibility = if (type == AccountButtonItemType.SimpleButton) View.VISIBLE else View.GONE
        attentionIcon.visibility = if (showAttentionIcon) View.VISIBLE else View.GONE
        itemWrapper.setOnClickListener { onClick.invoke() }
        itemWrapper.setBackgroundResource(
                if (isLast) R.drawable.manage_account_last_button_background
                else R.drawable.clickable_background_color_lawrence
        )
    }

    private fun getTextColorForType(type: AccountButtonItemType): Int? {
        val colorAttr = when (type) {
            AccountButtonItemType.SimpleButton -> R.attr.ColorOz
            AccountButtonItemType.RedButton -> R.attr.ColorLucian
            AccountButtonItemType.ActionButton -> R.attr.ColorJacob
        }

        return LayoutHelper.getAttr(colorAttr, context.theme)
    }
}

enum class AccountButtonItemType {
    SimpleButton,
    RedButton,
    ActionButton
}
