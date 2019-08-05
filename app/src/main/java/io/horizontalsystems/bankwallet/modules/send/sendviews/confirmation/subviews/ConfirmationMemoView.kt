package io.horizontalsystems.bankwallet.modules.send.sendviews.confirmation.subviews

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_confirmation_memo_item_view.view.*

class ConfirmationMemoView : ConstraintLayout {

    init {
        inflate(context, R.layout.view_confirmation_memo_item_view, this)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun getMemo(): String? {
        return memoInput.text?.toString()
    }

}
