package io.horizontalsystems.views

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
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
                rightImageButton.isVisible = true
                rightItem.onClick?.let { click -> rightImageButton?.setOnClickListener { click.invoke() } }
            } ?: run {
                rightItem.text?.let { textRes ->
                    rightTextButton.isVisible = true
                    rightTextButton.setText(textRes)
                    rightItem.onClick?.let { click -> rightTextButton?.setOnClickListener { click.invoke() } }
                }
            }
        }
    }

    fun bindLeftButton(leftBtnItem: TopMenuItem? = null) {
        leftTextButton.isVisible = false
        leftImageButton.isVisible = false

        leftBtnItem?.let { leftItem ->
            leftItem.icon?.let {
                leftImageButton.setImageResource(it)
                leftImageButton.isVisible = true
                leftItem.onClick?.let { click -> leftImageButton?.setOnClickListener { click.invoke() } }
            } ?: run {
                leftItem.text?.let {
                    leftTextButton.isVisible = true
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
