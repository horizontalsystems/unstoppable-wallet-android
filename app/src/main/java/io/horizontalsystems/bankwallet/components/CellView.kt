package io.horizontalsystems.bankwallet.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_cell.view.*

class CellView : ConstraintLayout {

    private var title: String? = null
    private var subtitle: String? = null
    private var rightTitle: String? = null
    private var bottomBorder: Boolean = false

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

    fun bind(checked: Boolean) {
        cellRight.checked = checked
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        cellLeft.bind(title, subtitle)
        cellRight.title = rightTitle

        if (bottomBorder) {
            bottomShade.visibility = View.VISIBLE
        }
    }
}
