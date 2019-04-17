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
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.horizontalsystems.bankwallet.ui.extensions.AddressView
import io.horizontalsystems.bankwallet.ui.extensions.CoinIconView
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import io.horizontalsystems.bankwallet.viewHelpers.TextHelper

class ReceiveFragment : BottomSheetDialogFragment() {

    private var mDialog: Dialog? = null

    private lateinit var viewModel: ReceiveViewModel

    private var coinCode: CoinCode? = null

    private var itemIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(ReceiveViewModel::class.java)
        coinCode?.let { viewModel.init(it) } ?:  dismiss()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        mDialog = activity?.let { BottomSheetDialog(it, R.style.BottomDialog) }
        mDialog?.setContentView(R.layout.fragment_bottom_sheet_receive)

        mDialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        mDialog?.window?.setGravity(Gravity.BOTTOM)

        mDialog?.findViewById<Button>(R.id.btnShare)?.setOnClickListener { viewModel.delegate.onShareClick(itemIndex) }
        mDialog?.findViewById<AddressView>(R.id.addressView)?.setOnClickListener { viewModel.delegate.onAddressClick(itemIndex) }

        viewModel.showAddressesLiveData.observe(this, Observer { addresses ->
            addresses?.apply {
                if (addresses.isNotEmpty()) {
                    val address = addresses[itemIndex]
                    mDialog?.findViewById<CoinIconView>(R.id.coinIcon)?.bind(address.coin)
                    mDialog?.findViewById<TextView>(R.id.txtTitle)?.text = getString(R.string.Deposit_Title, address.coin.title)
                    mDialog?.findViewById<AddressView>(R.id.addressView)?.bind(address.address)
                    mDialog?.findViewById<ImageView>(R.id.imgQrCode)?.setImageBitmap(TextHelper.getQrCodeBitmapFromAddress(address.address))
                }
            }
        })

        viewModel.showErrorLiveData.observe(this, Observer { error ->
            error?.let {
                HudHelper.showErrorMessage(it)
            }
            dismiss()
        })

        viewModel.showCopiedLiveEvent.observe(this, Observer {
            HudHelper.showSuccessMessage(R.string.Hud_Text_Copied)
        })

        viewModel.shareAddressLiveEvent.observe(this, Observer { address ->
            address?.let {
                ShareCompat.IntentBuilder.from(activity)
                        .setType("text/plain")
                        .setText(it)
                        .startChooser()
            }
        })

        mDialog?.setOnShowListener(object : DialogInterface.OnShowListener {
            override fun onShow(dialog: DialogInterface?) {
                val bottomSheet = mDialog?.findViewById<View>(R.id.design_bottom_sheet)
                BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_EXPANDED
                BottomSheetBehavior.from(bottomSheet).isFitToContents = true
            }
        })

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
