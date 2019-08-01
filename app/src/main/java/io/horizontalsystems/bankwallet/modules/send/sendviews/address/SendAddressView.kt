package io.horizontalsystems.bankwallet.modules.send.sendviews.address

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.send.SendViewModel
import kotlinx.android.synthetic.main.view_address_input.view.*

class SendAddressView : ConstraintLayout {

    init {
        inflate(context, R.layout.view_address_input, this)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private lateinit var viewModel: SendAddressViewModel
    private lateinit var lifecycleOwner: LifecycleOwner

    fun bindAddressInputInitial(viewModel: SendAddressViewModel,
                                mainViewModel: SendViewModel,
                                lifecycleOwner: LifecycleOwner,
                                onBarcodeClick: (() -> (Unit))? = null
    ) {
        this.viewModel = viewModel
        this.lifecycleOwner = lifecycleOwner

        btnBarcodeScan.visibility = View.VISIBLE
        btnPaste.visibility = View.VISIBLE
        btnDeleteAddress.visibility = View.GONE

        btnBarcodeScan?.setOnClickListener { onBarcodeClick?.invoke() }
        btnPaste?.setOnClickListener { viewModel.delegate.onPasteButtonClick() }
        btnDeleteAddress?.setOnClickListener { viewModel.delegate.onAddressDeleteClick() }

        invalidate()

        viewModel.addressTextLiveData.observe(lifecycleOwner, Observer { address ->
            txtAddress.text = address

            val empty = address?.isEmpty() ?: true
            btnBarcodeScan.visibility = if (empty) View.VISIBLE else View.GONE
            btnPaste.visibility = if (empty) View.VISIBLE else View.GONE
            btnDeleteAddress.visibility = if (empty) View.GONE else View.VISIBLE
        })

        viewModel.errorLiveData.observe(lifecycleOwner, Observer { error ->
            error?.let {
                val errorText = context.getString(R.string.Send_Error_IncorrectAddress)
                txtAddressError.visibility = View.VISIBLE
                txtAddressError.text = errorText
            } ?: run {
                txtAddressError.visibility = View.GONE
            }
        })

        viewModel.pasteButtonEnabledLiveData.observe(lifecycleOwner, Observer { enabled ->
            btnPaste.isEnabled = enabled
        })

        viewModel.amountLiveData.observe(lifecycleOwner, Observer { amount ->
            mainViewModel.delegate.onAmountChanged(amount)
        })

        viewModel.notifyMainViewModelOnAddressChangedLiveData.observe(lifecycleOwner, Observer {
            mainViewModel.delegate.onAddressChanged()
        })

        viewModel.mainViewModelParseAddressLiveData.observe(lifecycleOwner, Observer { address ->
            mainViewModel.delegate.parseAddress(address)
        })

        viewModel.delegate.onViewDidLoad()
    }

}
