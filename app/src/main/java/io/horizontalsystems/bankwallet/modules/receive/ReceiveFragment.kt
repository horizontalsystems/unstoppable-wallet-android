package io.horizontalsystems.bankwallet.modules.receive

import android.app.Dialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.content.DialogInterface
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.core.app.ShareCompat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.reObserve
import io.horizontalsystems.bankwallet.modules.receive.viewitems.AddressItem
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.horizontalsystems.bankwallet.ui.extensions.AddressView
import io.horizontalsystems.bankwallet.ui.extensions.CoinIconView
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import io.horizontalsystems.bankwallet.viewHelpers.TextHelper

class ReceiveFragment : BottomSheetDialogFragment() {

    private lateinit var viewModel: ReceiveViewModel
    private var coinCode: CoinCode? = null
    private var itemIndex = 0

    private val showAddressesObserver = Observer<List<AddressItem>?> { addresses ->
        addresses?.apply {
            if (addresses.isNotEmpty()) {
                val address = addresses[itemIndex]
                dialog?.findViewById<CoinIconView>(R.id.coinIcon)?.bind(address.coin)
                dialog?.findViewById<TextView>(R.id.txtTitle)?.text = getString(R.string.Deposit_Title, address.coin.title)
                dialog?.findViewById<AddressView>(R.id.addressView)?.bind(address.address)
                dialog?.findViewById<ImageView>(R.id.imgQrCode)?.setImageBitmap(TextHelper.getQrCodeBitmapFromAddress(address.address))
            }
        }
    }

    private val showErrorObserver = Observer<Int?> { error ->
        error?.let {
            HudHelper.showErrorMessage(it)
        }
        dismiss()
    }

    private val showCopiedObserver = Observer<Unit> { HudHelper.showSuccessMessage(R.string.Hud_Text_Copied) }

    private val shareAddressObserver = Observer<String?> { address ->
        address?.let {
            ShareCompat.IntentBuilder.from(activity)
                    .setType("text/plain")
                    .setText(it)
                    .startChooser()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(ReceiveViewModel::class.java)
        coinCode?.let { viewModel.init(it) } ?: dismiss()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.showAddressesLiveData.reObserve(this, showAddressesObserver)
        viewModel.showErrorLiveData.reObserve(this, showErrorObserver)
        viewModel.showCopiedLiveEvent.reObserve(this, showCopiedObserver)
        viewModel.shareAddressLiveEvent.reObserve(this, shareAddressObserver)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val mDialog = activity?.let { BottomSheetDialog(it, R.style.BottomDialog) }
        mDialog?.setContentView(R.layout.fragment_bottom_sheet_receive)

        mDialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        mDialog?.window?.setGravity(Gravity.BOTTOM)

        mDialog?.findViewById<Button>(R.id.btnShare)?.setOnClickListener { viewModel.delegate.onShareClick(itemIndex) }
        mDialog?.findViewById<AddressView>(R.id.addressView)?.setOnClickListener { viewModel.delegate.onAddressClick(itemIndex) }

        mDialog?.setOnShowListener {
            val bottomSheet = mDialog.findViewById<View>(R.id.design_bottom_sheet)
            BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_EXPANDED
            BottomSheetBehavior.from(bottomSheet).isFitToContents = true
        }

        return mDialog as Dialog
    }

    companion object {
        fun show(activity: FragmentActivity, coinCode: CoinCode) {
            val fragment = ReceiveFragment()
            fragment.coinCode = coinCode
            fragment.show(activity.supportFragmentManager, "receive_fragment")
        }
    }

}
