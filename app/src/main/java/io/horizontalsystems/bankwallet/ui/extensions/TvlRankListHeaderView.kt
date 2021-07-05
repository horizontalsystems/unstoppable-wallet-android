package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import kotlinx.android.synthetic.main.view_tvlrank_list_header.view.*

class TvlRankListHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    interface Listener {
        fun onClickLeftMenu()
        fun onClickRightMenu()
    }

    var listener: Listener? = null

    init {
        inflate(context, R.layout.view_tvlrank_list_header, this)

        leftMenu.setOnSingleClickListener {
            listener?.onClickLeftMenu()
        }
        rightMenu.setOnSingleClickListener {
            listener?.onClickRightMenu()
        }
    }

    fun setLeftField(fieldName: Int) {
        val fieldText = context.getString(fieldName)
        if (leftMenu.text != fieldText) {
            leftMenu.text = fieldText
        }
    }

    fun setRightField(fieldName: Int) {
        val fieldText = context.getString(fieldName)
        if (rightMenu.text != fieldText) {
            rightMenu.text = fieldText
        }
    }

}
