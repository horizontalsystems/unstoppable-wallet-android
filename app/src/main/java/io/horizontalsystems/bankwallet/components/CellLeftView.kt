package io.horizontalsystems.bankwallet.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_cell_left.view.*

class CellLeftView : ConstraintLayout {

    init {
        inflate(context, R.layout.view_cell_left, this)
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun bind(title: String?, subtitle: String?) {
        cellTitle.text = title
        cellSubtitle.text = subtitle

        cellSubtitle.visibility = if (subtitle == null) View.GONE else View.VISIBLE
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        cellIcon.visibility = View.GONE
    }
}
