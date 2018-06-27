package bitcoin.wallet.modules.pay

import android.app.Dialog
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import bitcoin.wallet.R
import bitcoin.wallet.entities.coins.Coin
import com.google.zxing.integration.android.IntentIntegrator

class PayFragment : DialogFragment() {

    private var mDialog: Dialog? = null

    private lateinit var address: EditText

    private lateinit var viewModel: PayViewModel

    private lateinit var coin: Coin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(PayViewModel::class.java)
        viewModel.init()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = activity?.let { AlertDialog.Builder(it, R.style.BottomDialog) }

        val rootView = View.inflate(context, R.layout.fragment_bottom_sheet_pay, null) as ViewGroup

        builder?.setView(rootView)

        mDialog = builder?.create()
        mDialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        mDialog?.window?.setGravity(Gravity.BOTTOM)
        mDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnScan = rootView.findViewById<View>(R.id.btnScan)
        btnScan.setOnClickListener {
            startScanner()
        }

        address = rootView.findViewById(R.id.address)

        return mDialog as Dialog
    }

    private fun startScanner() {
        val intentIntegrator = IntentIntegrator.forSupportFragment(this)
        intentIntegrator.captureActivity = PortraitCaptureActivity::class.java
        intentIntegrator.setOrientationLocked(true)
        intentIntegrator.setPrompt("")
        intentIntegrator.setBeepEnabled(false)
        intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        intentIntegrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        val scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent)
        if (scanResult != null) {
            // handle scan result
            address.setText(scanResult.contents)
        }
    }

    companion object {
        fun show(activity: FragmentActivity, coin: Coin) {
            val fragment = PayFragment()
            fragment.coin = coin
            val ft = activity.supportFragmentManager.beginTransaction()
            ft.add(fragment, "pay_fragment")
            ft.commitAllowingStateLoss()
        }
    }

}
