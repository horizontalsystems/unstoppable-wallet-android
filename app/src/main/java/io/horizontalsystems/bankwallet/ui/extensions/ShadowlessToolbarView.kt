package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_shadowless_toolbar.view.*

class ShadowlessToolbarView : ConstraintLayout {

    constructor(context: Context) : super(context) {
        initializeViews()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initializeViews()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initializeViews()
    }

    private fun initializeViews() {
        ConstraintLayout.inflate(context, R.layout.view_shadowless_toolbar, this)
    }

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
