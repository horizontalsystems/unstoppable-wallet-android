package bitcoin.wallet.modules.receive

import android.app.AlertDialog
import android.app.Dialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentActivity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import bitcoin.wallet.R
import bitcoin.wallet.viewHelpers.HudHelper
import bitcoin.wallet.viewHelpers.TextHelper

class ReceiveFragment : DialogFragment() {

    private var mDialog: Dialog? = null

    private lateinit var viewModel: ReceiveViewModel

    private lateinit var coinCode: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(ReceiveViewModel::class.java)
        viewModel.init(coinCode)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = activity?.let { AlertDialog.Builder(it, R.style.BottomDialog) }

        val rootView = View.inflate(context, R.layout.fragment_bottom_sheet_receive, null) as ViewGroup
        builder?.setView(rootView)

        mDialog = builder?.create()
        mDialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        mDialog?.window?.setGravity(Gravity.BOTTOM)

        rootView.findViewById<TextView>(R.id.txtTitle)?.text = getString(R.string.receive_bottom_sheet_title, coinCode)

        rootView.findViewById<Button>(R.id.btnCancel)?.setOnClickListener { viewModel.delegate.onCancelClick() }

        rootView.findViewById<Button>(R.id.btnShare)?.setOnClickListener { viewModel.delegate.onShareClick() }

        rootView.findViewById<TextView>(R.id.txtCopy)?.setOnClickListener { viewModel.delegate.onCopyClick() }

        viewModel.showAddressLiveData.observe(this, Observer { address ->
            address?.apply {
                rootView.findViewById<TextView>(R.id.txtAddress)?.let { it.text = address }
                rootView.findViewById<ImageView>(R.id.imgQrCode)?.setImageBitmap(TextHelper.getQrCodeBitmapFromAddress(address))
            }
        })

        viewModel.showErrorLiveData.observe(this, Observer { error ->
            error?.let {
                HudHelper.showErrorMessage(it, activity)
            }
            dismiss()
        })

        viewModel.showCopiedLiveEvent.observe(this, Observer {
            rootView.findViewById<TextView>(R.id.txtCopy)?.let {
                HudHelper.showSuccessMessage(R.string.hud_text_copied, activity)
            }
        })

        viewModel.closeViewLiveEvent.observe(this, Observer {
            dismiss()
        })

        viewModel.openShareViewLiveEvent.observe(this, Observer { address ->
            address?.let {
                context?.let { context -> TextHelper.shareExternalText(context, address, getString(R.string.receive_bottom_sheet_share_to)) }
            }
        })

        return mDialog as Dialog
    }

    companion object {
        fun show(activity: FragmentActivity, coinCode: String) {
            val fragment = ReceiveFragment()
            fragment.coinCode = coinCode
            fragment.show(activity.supportFragmentManager, "receive_fragment")
        }
    }

}
