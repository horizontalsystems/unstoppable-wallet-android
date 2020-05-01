package io.horizontalsystems.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.synthetic.main.view_manage_account_view.view.*

class ManageAccountView : ConstraintLayout {

    init {
        inflate(context, R.layout.view_manage_account_view, this)
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun bind(titleText: String, type: AccountButtonItemType, showAttentionIcon: Boolean = false, isLast: Boolean = false) {
        title.text = titleText

        getTextColorForType(type)?.let {
            title.setTextColor(it)
        }

        rightArrow.visibility = if (type == AccountButtonItemType.Regular) View.VISIBLE else View.GONE
        attentionIcon.visibility = if (showAttentionIcon) View.VISIBLE else View.GONE

        setBackgroundResource(getBackgroundResource(isLast))
    }

    private fun getBackgroundResource(isLast: Boolean) = when {
        isLast -> R.drawable.manage_account_last_button_background
        else -> R.drawable.clickable_background_color_lawrence
    }

    private fun getTextColorForType(type: AccountButtonItemType): Int? {
        val colorAttr = when (type) {
            AccountButtonItemType.Regular -> R.attr.ColorOz
            AccountButtonItemType.DangerAction -> R.attr.ColorLucian
            AccountButtonItemType.Action -> R.attr.ColorJacob
        }

        return LayoutHelper.getAttr(colorAttr, context.theme)
    }
}

enum class AccountButtonItemType {
    Regular,
    Action,
    DangerAction
}
