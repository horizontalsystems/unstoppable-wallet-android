package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_sort_button.view.*

class SortButtonView : ConstraintLayout {

    interface Listener {
        fun onSortBtnTextClick()
        fun onSortBtnDirectionClick(direction: Direction)
    }

    private var listener: Listener? = null

    init {
        inflate(context, R.layout.view_sort_button, this)
        buttonText.setOnClickListener { listener?.onSortBtnTextClick() }
        buttonImage.setOnClickListener { listener?.onSortBtnDirectionClick(direction) }
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var direction = Direction.DOWN

    fun bindListener(listener: Listener){
        this.listener = listener
    }

    fun bind(text: String? = null, direction: Direction? = null) {
        direction?.let {
            this.direction = it
        }
        text?.let {
            buttonText.text = it
        }
        buttonImage.setImageResource(if (this.direction == Direction.UP) R.drawable.ic_up else R.drawable.ic_down)
        invalidate()
    }
}

enum class Direction{
    UP, DOWN
}
