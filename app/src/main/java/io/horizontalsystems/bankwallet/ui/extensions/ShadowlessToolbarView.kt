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


    fun bind(title: String?, leftBtnItem: TopMenuItem? = null, rightBtnItem: TopMenuItem? = null) {
        title?.let { toolbarTitle.text = it }

        bindLeftButton(leftBtnItem)

        rightBtnItem?.let { rightItem ->
            rightItem.icon?.let { imageRes ->
                rightImageButton.setImageResource(imageRes)
                rightImageButton.visibility = View.VISIBLE
                rightItem.onClick?.let { click -> rightImageButton?.setOnClickListener { click.invoke() } }
            } ?:run {
                rightItem.text?.let { textRes ->
                    rightTextButton.visibility = View.VISIBLE
                    rightTextButton.setText(textRes)
                    rightItem.onClick?.let { click -> rightTextButton?.setOnClickListener { click.invoke() } }
                }
            }
        }
    }

    fun bindLeftButton(leftBtnItem: TopMenuItem? = null) {
        leftTextButton.visibility = View.GONE
        leftImageButton.visibility = View.GONE

        leftBtnItem?.let { leftItem ->
            leftItem.icon?.let {
                leftImageButton.setImageResource(it)
                leftImageButton.visibility = View.VISIBLE
                leftItem.onClick?.let { click -> leftImageButton?.setOnClickListener { click.invoke() } }
            } ?:run {
                leftItem.text?.let {
                    leftTextButton.visibility = View.VISIBLE
                    leftTextButton.setText(it)
                    leftItem.onClick?.let { click -> leftTextButton?.setOnClickListener { click.invoke() } }
                }
            }
        }
    }

    fun bindTitle(title: String) {
        toolbarTitle.text = title
    }

}

data class TopMenuItem(val icon: Int? = null, val text: Int? = null, val onClick: (() -> Unit)? = null)
