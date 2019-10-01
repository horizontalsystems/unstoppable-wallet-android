package io.horizontalsystems.bankwallet.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_cell_right.view.*

class CellRightView : ConstraintLayout {

    init {
        inflate(context, R.layout.view_cell_right, this)
        enableIcon(null)
        cellTitle.visibility = View.GONE
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var title: String? = null
        set(value) {
            field = value

            cellTitle.text = value
            cellTitle.visibility = if (value == null) View.GONE else View.VISIBLE
        }

    var checked: Boolean = false
        set(value) {
            field = value
            enableIcon(if (value) checkIcon else null)
        }

    var downArrow: Boolean = false
        set(value) {
            field = value
            enableIcon(if (value) downIcon else null)
        }

    private fun enableIcon(icon: ImageView?) {
        listOf(lightModeIcon, downIcon, arrowIcon, checkIcon).forEach {
            it.visibility = View.GONE
        }

        icon?.visibility = View.VISIBLE
    }
}
