package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_shadowless_toolbar.view.*

class ShadowlessToolbarView : ConstraintLayout {

    init {
        inflate(context, R.layout.view_shadowless_toolbar, this)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)


    fun bind(title: String, leftBtnItem: TopMenuItem? = null, rightBtnItem: TopMenuItem? = null) {
        toolbarTitle.text = title
        leftBtnItem?.let { item ->
            leftButton.visibility = View.VISIBLE
            leftButton.setImageResource(item.icon)
            leftButton?.setOnClickListener { item.onClick.invoke() }
        }

        rightBtnItem?.let { item ->
            rightButton.visibility = View.VISIBLE
            rightButton.setImageResource(item.icon)
            rightButton?.setOnClickListener { item.onClick.invoke() }
        }
    }

    fun bindTitle(title: String) {
        toolbarTitle.text = title
    }

    fun bindLeftButton(leftBtnItem: TopMenuItem) {
        leftButton.visibility = View.VISIBLE
        leftButton.setImageResource(leftBtnItem.icon)
        leftButton?.setOnClickListener { leftBtnItem.onClick.invoke() }
    }

}

data class TopMenuItem(val icon: Int, val onClick: (() -> (Unit)))
