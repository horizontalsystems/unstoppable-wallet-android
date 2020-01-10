package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_manage_account_button.view.*

class ManageAccountButton: LinearLayout {

    init {
        inflate(context, R.layout.view_manage_account_button, this)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)


    fun bind(title: String, type: AccountButtonItemType, showAttentionIcon: Boolean = false, onClick: () -> Unit){
        when(type){
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
    }
}

enum class AccountButtonItemType {
    SimpleButton,
    RedButton
}
