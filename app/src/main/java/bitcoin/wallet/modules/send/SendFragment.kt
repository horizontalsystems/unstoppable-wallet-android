package bitcoin.wallet.modules.send

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.AlertDialog
import android.app.Dialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentActivity
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import bitcoin.wallet.R
import bitcoin.wallet.entities.coins.Coin
import bitcoin.wallet.lib.ErrorDialog
import com.google.zxing.integration.android.IntentIntegrator

class SendFragment : DialogFragment() {

    private var mDialog: Dialog? = null

    private lateinit var addressTxt: EditText
    private lateinit var addressLayout: View

    private lateinit var amountTxt: EditText
    private lateinit var hintTxt: TextView
    private lateinit var amountLayout: View

    private lateinit var btnCurrency: Button

    private lateinit var paymentRefTxt: EditText
    private lateinit var moreTxt: TextView

    private lateinit var moreOptionsLayout: View

    private lateinit var scrollView: ScrollView

    private lateinit var btnCancel: Button

    private lateinit var viewModel: SendViewModel

    private lateinit var coin: Coin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(SendViewModel::class.java)
        viewModel.init(coin.code)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = activity?.let { AlertDialog.Builder(it, R.style.BottomDialog) }

        val rootView = View.inflate(context, R.layout.fragment_bottom_sheet_pay, null) as ViewGroup

        builder?.setView(rootView)

        mDialog = builder?.create()
        mDialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        mDialog?.window?.setGravity(Gravity.BOTTOM)

        rootView.findViewById<View>(R.id.btnScan)?.setOnClickListener {
            viewModel.delegate.onScanClick()
        }

        rootView.findViewById<TextView>(R.id.txtTitle)?.let { txtTitle ->
            txtTitle.text = getString(R.string.send_bottom_sheet_title, coin.code)
        }

        addressTxt = rootView.findViewById(R.id.txtAddress)
        addressLayout = rootView.findViewById<View>(R.id.addressLayout)
        addressTxt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus)
                addressLayout.background = resources.getDrawable(R.drawable.border_yellow, null)
            else
                addressLayout.background = resources.getDrawable(R.drawable.border_grey, null)
        }

        hintTxt = rootView.findViewById(R.id.txtAmountEquivalent)
        amountTxt = rootView.findViewById(R.id.txtAmount)
        amountLayout = rootView.findViewById<View>(R.id.amountLayout)
        amountTxt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus)
                amountLayout.background = resources.getDrawable(R.drawable.border_yellow, null)
            else
                amountLayout.background = resources.getDrawable(R.drawable.border_grey, null)
        }

        amountTxt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val enteredDouble = s?.toString()?.toDoubleOrNull()
                viewModel.delegate.onAmountEntered(enteredDouble ?: 0.0)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        rootView.findViewById<Button>(R.id.btnCurrency)?.apply {
            btnCurrency = this
            setOnClickListener {
                viewModel.delegate.onCurrencyButtonClick()
            }
        }

        rootView.findViewById<Button>(R.id.btnPaste)?.setOnClickListener {
            viewModel.delegate.onPasteClick()
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
        btnCancel.setOnClickListener { viewModel.delegate.onCancelClick() }

        rootView.findViewById<Button>(R.id.btnSend)?.setOnClickListener {
            viewModel.delegate.onSendClick(addressTxt.text.toString())
        }

        viewModel.startScanLiveEvent.observe(this, Observer {
            startScanner()
        })

        viewModel.addressLiveData.observe(this, Observer { address ->
            address?.let {
                addressTxt.setText(it)
            }
        })

        viewModel.primaryAmountLiveData.observe(this, Observer { primaryAmount ->
            primaryAmount?.let {
                amountTxt.setText(it.toString())
            }
        })

        viewModel.primaryCurrencyLiveData.observe(this, Observer { primaryCurrency ->
            primaryCurrency?.let {
                btnCurrency.text = primaryCurrency
            }
        })

        viewModel.secondaryAmountHintLiveData.observe(this, Observer { hint ->
            hint?.let {
                hintTxt.text = hint
            }
        })

        viewModel.closeViewLiveEvent.observe(this, Observer {
            dismiss()
        })

        viewModel.showErrorLiveData.observe(this, Observer { error ->
            error?.apply {
                activity?.let { context ->
                    ErrorDialog(context, error).show()
                }
            }
        })

        viewModel.showSuccessLiveEvent.observe(this, Observer {
            dismiss()
            Toast.makeText(context, R.string.send_bottom_sheet_success, Toast.LENGTH_LONG).show()
        })

        return mDialog as Dialog
    }


    private fun startScanner() {
        val intentIntegrator = IntentIntegrator.forSupportFragment(this)
        intentIntegrator.captureActivity = QRScannerActivity::class.java
        intentIntegrator.setOrientationLocked(true)
        intentIntegrator.setPrompt("")
        intentIntegrator.setBeepEnabled(false)
        intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        intentIntegrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        val scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent)
        if (scanResult != null && !TextUtils.isEmpty(scanResult.contents)) {
            // handle scan result
            addressTxt.setText(scanResult.contents)
        }
    }

    companion object {
        fun show(activity: FragmentActivity, coin: Coin) {
            val fragment = SendFragment()
            fragment.coin = coin
            fragment.show(activity.supportFragmentManager, "pay_fragment")
        }
    }

}
