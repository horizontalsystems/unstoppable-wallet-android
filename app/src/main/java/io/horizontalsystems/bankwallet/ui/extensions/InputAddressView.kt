package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_address_input.view.*

class InputAddressView : ConstraintLayout {

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
        ConstraintLayout.inflate(context, R.layout.view_address_input, this)
    }

    fun bindAddressInputInitial(onAmpersandClick: (() -> (Unit))? = null,
                         onBarcodeClick: (() -> (Unit))? = null,
                         onPasteClick: (() -> (Unit))? = null,
                         onDeleteClick: (() -> (Unit))? = null
    ) {
        btnAmpersand.visibility = View.VISIBLE
        btnBarcodeScan.visibility =  View.VISIBLE
        btnPaste.visibility =  View.VISIBLE
        btnDeleteAddress.visibility =  View.GONE

        btnAmpersand?.setOnClickListener { onAmpersandClick?.invoke() }
        btnBarcodeScan?.setOnClickListener { onBarcodeClick?.invoke() }
        btnPaste?.setOnClickListener { onPasteClick?.invoke() }
        btnDeleteAddress?.setOnClickListener { onDeleteClick?.invoke() }

        invalidate()
    }

    fun updateInput(address: String = "", errorText: String? = null) {
        val empty = address.isEmpty()
        btnAmpersand.visibility = if (empty) View.VISIBLE else View.GONE
        btnBarcodeScan.visibility =  if (empty) View.VISIBLE else View.GONE
        btnPaste.visibility =  if (empty) View.VISIBLE else View.GONE

        btnDeleteAddress.visibility =  if (empty) View.GONE else View.VISIBLE

        errorText?.let {
            txtAddressError.visibility = View.VISIBLE
            txtAddressError.text = it
        } ?: run {
            txtAddressError.visibility = View.GONE
        }
    }

    fun enablePasteButton(enabled: Boolean) {
        btnPaste.isEnabled = enabled
    }

}
