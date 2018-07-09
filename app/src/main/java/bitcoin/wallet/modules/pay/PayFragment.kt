package bitcoin.wallet.modules.pay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
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
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import bitcoin.wallet.R
import bitcoin.wallet.entities.coins.Coin
import com.google.zxing.integration.android.IntentIntegrator

class PayFragment : DialogFragment() {

    private var mDialog: Dialog? = null

    private lateinit var addressTxt: EditText
    private lateinit var addressLayout: View

    private lateinit var amountTxt: EditText
    private lateinit var amountLayout: View

    private lateinit var paymentRefTxt: EditText
    private lateinit var moreTxt: TextView

    private lateinit var moreOptionsLayout: View

    private lateinit var scrollView: ScrollView

    private lateinit var btnCancel: Button

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

        addressTxt = rootView.findViewById(R.id.txtAddress)
        addressLayout = rootView.findViewById<View>(R.id.addressLayout)
        addressTxt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus)
                addressLayout.background = resources.getDrawable(R.drawable.border_yellow, null)
            else
                addressLayout.background = resources.getDrawable(R.drawable.border_grey, null)
        }

        amountTxt = rootView.findViewById(R.id.txtAmount)
        amountLayout = rootView.findViewById<View>(R.id.amountLayout)
        amountTxt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus)
                amountLayout.background = resources.getDrawable(R.drawable.border_yellow, null)
            else
                amountLayout.background = resources.getDrawable(R.drawable.border_grey, null)
        }

        paymentRefTxt = rootView.findViewById(R.id.txtPaymentRef)
        paymentRefTxt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus)
                paymentRefTxt.background = resources.getDrawable(R.drawable.border_yellow, null)
            else
                paymentRefTxt.background = resources.getDrawable(R.drawable.border_grey, null)
        }

        scrollView = rootView.findViewById(R.id.scrollView)


        moreTxt = rootView.findViewById(R.id.txtMore)
        moreOptionsLayout = rootView.findViewById(R.id.moreOptionsLayout)
        moreTxt.setOnClickListener {
            moreTxt.visibility = View.GONE

            // Prepare the View for the animation
            moreOptionsLayout.visibility = View.VISIBLE
            moreOptionsLayout.alpha = 0.0f

            // Start the animation
            moreOptionsLayout.animate()
                    .translationY(moreOptionsLayout.height * 1F)
                    .alpha(1.0f)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator?) {
                            super.onAnimationEnd(animation)
                            scrollView.post({
                                scrollView.fullScroll(View.FOCUS_DOWN)
                                paymentRefTxt.requestFocus()
                            })
                        }
                    })
        }

        btnCancel = rootView.findViewById(R.id.btnCancel)
        btnCancel.setOnClickListener { dismiss() }

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
            addressTxt.setText(scanResult.contents)
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
