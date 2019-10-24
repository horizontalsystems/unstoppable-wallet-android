package io.horizontalsystems.bankwallet.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Coin
import kotlinx.android.synthetic.main.view_cell_left.view.*

class CellLeftView : ConstraintLayout {

    var title: String? = null
        set(value) {
            field = value
            cellTitle.text = value
        }

    var subtitle: String? = null
        set(value) {
            field = value
            cellSubtitle.text = value
            cellSubtitle.visibility = if (value == null) View.GONE else View.VISIBLE
        }

    var imageResource: Int? = null
        set(value) {
            field = value
            value?.let { cellIcon.bind(it) }
            cellIcon.visibility = if (value == null) View.GONE else View.VISIBLE
        }

    var imageDrawable: Drawable? = null
        set(value) {
            field = value
            value?.let { cellIcon.bind(it) }
            cellIcon.visibility = if (value == null) View.GONE else View.VISIBLE
        }

    var imageCoin: Coin? = null
        set(value) {
            field = value
            value?.let { cellIcon.bind(it) }
            cellIcon.visibility = if (value == null) View.GONE else View.VISIBLE
        }

    var imageCoinCode: String? = null
        set(value) {
            field = value
            value?.let { cellIcon.bind(it) }
            cellIcon.visibility = if (value == null) View.GONE else View.VISIBLE
        }

    var imageTint: ColorStateList? = null
        set(value) {
            field = value
            value?.let { cellIcon.setTint(it) }
        }

    init {
        inflate(context, R.layout.view_cell_left, this)
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onFinishInflate() {
        super.onFinishInflate()

        cellIcon.visibility = View.GONE
        cellSubtitle.visibility = View.GONE
    }
}
