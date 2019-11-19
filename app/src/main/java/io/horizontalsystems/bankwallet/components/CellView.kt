package io.horizontalsystems.bankwallet.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.CompoundButton
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper
import kotlinx.android.synthetic.main.view_cell.view.*

class CellView : ConstraintLayout {

    private val singleLineHeight = 44f
    private val doubleLineHeight = 60f

    var coinIcon: String? = null
        set(value) {
            field = value
            field?.let { cellLeft.imageCoinCode = it }
        }

    var imageDrawable: Drawable? = null
        set(value) {
            field = value
            field?.let { cellLeft.imageDrawable = it }
        }

    var imageTint: ColorStateList? = null
        set(value) {
            field = value
            cellLeft.imageTint = value
        }

    var title: String? = null
        set(value) {
            field = value
            cellLeft.title = value
        }

    var subtitle: String? = null
        set(value) {
            field = value
            cellLeft.subtitle = value
            layoutParams?.height = LayoutHelper.dp(if(value == null) singleLineHeight else doubleLineHeight, context)
        }

    var rightTitle: String? = null
        set(value) {
            field = value
            cellRight.title = value
        }

    var dropDownText: String? = null
        set(value) {
            field = value
            cellRight.dropdownText = value
        }

    var checked: Boolean = false
        set(value) {
            field = value
            cellRight.checked = value
        }

    var bottomBorder: Boolean = false
        set(value) {
            field = value
            bottomShade.visibility = if (value) View.VISIBLE else View.INVISIBLE
        }

    var dropDownArrow: Boolean = false
        set(value) {
            field = value
            cellRight.dropDownArrow = value
        }

    var badgeImage: Boolean = false
        set(value) {
            field = value
            cellRight.badge = value
        }

    var rightArrow: Boolean = false
        set(value) {
            field = value
            cellRight.rightArrow = value
        }

    var switchIsChecked: Boolean? = null
        set(value) {
            field = value
            if (value != null) {
                cellRight.switchIsChecked = value
            }
        }

    var switchOnCheckedChangeListener: CompoundButton.OnCheckedChangeListener? = null
        set(value) {
            field = value
            cellRight.switchOnCheckedChangeListener = value
        }

    init {
        inflate(context, R.layout.view_cell, this)
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initialize(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialize(attrs)
    }

    fun switchToggle(){
        cellRight.switchToggle()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!subtitle.isNullOrBlank()) {
            layoutParams.height = LayoutHelper.dp(doubleLineHeight, context)
        }
    }

    private fun initialize(attrs: AttributeSet) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.CellView)
        try {
            title = ta.getString(R.styleable.CellView_title)
            subtitle = ta.getString(R.styleable.CellView_subtitle)
            rightTitle = ta.getString(R.styleable.CellView_rightTitle)
            imageDrawable = ta.getDrawable(R.styleable.CellView_imageDrawable)
            imageTint = ta.getColorStateList(R.styleable.CellView_imageTint)
            rightArrow = ta.getBoolean(R.styleable.CellView_rightArrow, false)
            bottomBorder = ta.getBoolean(R.styleable.CellView_bottomBorder, false)
        } finally {
            ta.recycle()
        }
    }
}
