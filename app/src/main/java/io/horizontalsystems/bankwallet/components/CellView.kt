package io.horizontalsystems.bankwallet.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper
import kotlinx.android.synthetic.main.view_cell.view.*

class CellView : ConstraintLayout {

    private val singleLineHeight = 44f
    private val doubleLineHeight = 60f

    var icon: String? = null
        set(value) {
            field = value
            field?.let { cellIcon.bind(it) }
            cellIcon.visibility = if (value == null) View.GONE else View.VISIBLE
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
            cellRight.title = rightTitle
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

    var downArrow: Boolean = false
        set(value) {
            field = value
            cellRight.downArrow = value
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
            bottomBorder = ta.getBoolean(R.styleable.CellView_bottomBorder, false)
        } finally {
            ta.recycle()
        }
    }
}
