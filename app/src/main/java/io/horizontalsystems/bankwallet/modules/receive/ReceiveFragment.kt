package io.horizontalsystems.bankwallet.modules.receive

import android.app.AlertDialog
import android.app.Dialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper
import io.horizontalsystems.bankwallet.viewHelpers.TextHelper

class ReceiveFragment : DialogFragment() {

    private var mDialog: Dialog? = null

    private lateinit var viewModel: ReceiveViewModel

    private lateinit var adapterId: String

    private var itemIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(ReceiveViewModel::class.java)
        viewModel.init(adapterId)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = activity?.let { AlertDialog.Builder(it, R.style.BottomDialog) }

        val rootView = View.inflate(context, R.layout.fragment_bottom_sheet_receive, null) as ViewGroup
        builder?.setView(rootView)

        mDialog = builder?.create()
        mDialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        mDialog?.window?.setGravity(Gravity.BOTTOM)

        rootView.findViewById<Button>(R.id.btnCopy)?.setOnClickListener { viewModel.delegate.onCopyClick(itemIndex) }

        viewModel.showAddressesLiveData.observe(this, Observer { addresses ->
            addresses?.apply {
                if (addresses.isNotEmpty()) {
                    val address = addresses[itemIndex]
                    context?.let {ctx ->
                        val coinDrawable = ContextCompat.getDrawable(ctx, LayoutHelper.getCoinDrawableResource(address.coin.code))
                        rootView.findViewById<ImageView>(R.id.coinImg)?.setImageDrawable(coinDrawable)
                    }
                    rootView.findViewById<TextView>(R.id.txtTitle)?.text = getString(R.string.receive_title, address.coin.code)
                    rootView.findViewById<TextView>(R.id.txtAddress)?.let { it.text = address.address }
                    rootView.findViewById<ImageView>(R.id.imgQrCode)?.setImageBitmap(TextHelper.getQrCodeBitmapFromAddress(address.address))
                }
            }
        })

        viewModel.showErrorLiveData.observe(this, Observer { error ->
            //todo remove after Wallet starts to work
            val someAddress = TextHelper.randomHashGenerator()
            rootView.findViewById<TextView>(R.id.txtAddress)?.let { it.text = someAddress }
            rootView.findViewById<ImageView>(R.id.imgQrCode)?.setImageBitmap(TextHelper.getQrCodeBitmapFromAddress(someAddress))
            //uncomment me
//            error?.let {
//                HudHelper.showErrorMessage(it, activity)
//            }
//            dismiss()
        })

        viewModel.showCopiedLiveEvent.observe(this, Observer {
            HudHelper.showSuccessMessage(R.string.hud_text_copied)
        })

        return mDialog as Dialog
    }

    companion object {
        fun show(activity: FragmentActivity, adapterId: String) {
            val fragment = ReceiveFragment()
            fragment.adapterId = adapterId
            fragment.show(activity.supportFragmentManager, "receive_fragment")
        }
    }

}
