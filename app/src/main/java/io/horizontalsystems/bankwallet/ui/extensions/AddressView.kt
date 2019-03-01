package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_address.view.*

class AddressView : ConstraintLayout {

    constructor(context: Context) : super(context) {
        initializeViews()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initializeViews()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initializeViews()
    }

    private fun initializeViews() {
        ConstraintLayout.inflate(context, R.layout.view_address, this)
    }

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
