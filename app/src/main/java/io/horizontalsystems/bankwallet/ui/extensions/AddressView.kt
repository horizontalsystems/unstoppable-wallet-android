package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_address.view.*

class AddressView : ConstraintLayout {

    init {
        inflate(context, R.layout.view_address, this)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun bind(address: String, icon: Int? = null) {
        txtAddress.text = address
        icon?.let { iconImage.setImageResource(icon) }
        invalidate()
    }

    fun bindTransactionId(hash: String, withIcon: Boolean = true) {
        if (withIcon)
            iconImage.setImageResource(R.drawable.hash)
        else
            iconImage.visibility = View.GONE

        txtAddress.text = hash
        invalidate()
    }

}
