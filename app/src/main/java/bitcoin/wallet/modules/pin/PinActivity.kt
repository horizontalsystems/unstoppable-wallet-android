package bitcoin.wallet.modules.pin

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import android.os.Handler
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.UserNotAuthenticatedException
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import bitcoin.wallet.BaseActivity
import bitcoin.wallet.R
import bitcoin.wallet.core.App
import bitcoin.wallet.core.security.EncryptionManager
import bitcoin.wallet.core.security.FingerprintAuthenticationDialogFragment
import bitcoin.wallet.core.security.SecurityUtils
import bitcoin.wallet.modules.main.MainModule
import bitcoin.wallet.viewHelpers.HudHelper
import kotlinx.android.synthetic.main.activity_pin.*
import kotlinx.android.synthetic.main.custom_tall_toolbar.*
import java.security.UnrecoverableKeyException


class PinActivity : BaseActivity(), NumPadItemsAdapter.Listener, FingerprintAuthenticationDialogFragment.Callback {

    private lateinit var viewModel: PinViewModel

    private lateinit var imgPinMask1: ImageView
    private lateinit var imgPinMask2: ImageView
    private lateinit var imgPinMask3: ImageView
    private lateinit var imgPinMask4: ImageView
    private lateinit var imgPinMask5: ImageView
    private lateinit var imgPinMask6: ImageView

    private var isFingerprintEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        setContentView(R.layout.activity_pin)

        setSupportActionBar(toolbar)
        supportActionBar?.title = ""

        val interactionType = intent.getSerializableExtra(keyInteractionType) as PinInteractionType
        val enteredPin = intent.getStringExtra(keyEnteredPin)

        backButton.setOnClickListener { viewModel.delegate.onBackPressed() }

        viewModel = ViewModelProviders.of(this).get(PinViewModel::class.java)
        viewModel.init(interactionType, enteredPin)

        viewModel.title.observe(this, Observer { title ->
            title?.let {
                toolbarTitle.text = getString(it)
            }
        })

        viewModel.description.observe(this, Observer { description ->
            description?.let { txtPromptPin.setText(it) }
        })

        viewModel.highlightPinMask.observe(this, Observer { length ->
            length?.let { updatePinCircles(it) }
        })

        viewModel.showErrorInDialog.observe(this, Observer { error ->
            error?.let {
                HudHelper.showErrorMessage(error, this)
            }
        })

        viewModel.showErrorMessage.observe(this, Observer { error ->
            error?.let {
                errorMessage.setText(error)
            }
        })

        viewModel.goToPinConfirmation.observe(this, Observer { pin ->
            pin?.let {
                PinModule.startForSetPinConfirm(this, pin)
            }
        })

        viewModel.goToSetPin.observe(this, Observer {
            PinModule.startForSetPin(this)
        })

        viewModel.showSuccess.observe(this, Observer {
            HudHelper.showSuccessMessage(activity = this)
            MainModule.start(this)
            finish()
        })

        viewModel.hideToolbar.observe(this, Observer {
            supportActionBar?.hide()
        })

        viewModel.unlockWallet.observe(this, Observer {
            unlockWallet()
        })

        viewModel.clearPinMaskWithDelay.observe(this, Observer {
            Handler().postDelayed({
                updatePinCircles(0)
            }, 300)
        })

        viewModel.showFingerprintDialog.observe(this, Observer {
            showFingerprintDialog()
        })

        viewModel.minimizeApp.observe(this, Observer {
            moveTaskToBack(true)
        })

        viewModel.goBack.observe(this, Observer {
            super.onBackPressed()
        })

        viewModel.goToPinEdit.observe(this, Observer {
            PinModule.startForEditPin(this)
            finish()
        })

        viewModel.hideBackButton.observe(this, Observer {
            backButton.visibility = View.GONE
        })

        viewModel.keyStoreSafeExecute.observe(this, Observer { triple ->
            triple?.let { (action, onSuccess, onFailure) ->
                safeExecuteWithKeystore(action, onSuccess, onFailure)
            }
        })

        viewModel.showAttemptsLeftWarning.observe(this, Observer { pair ->
            pair?.let { (stringRes, attemptsCount) ->
                errorMessage.text = getString(stringRes, attemptsCount)
            }
        })

        viewModel.blockScreen.observe(this, Observer {
            pinUnlock.visibility = View.GONE
            pinUnlockBlocked.visibility = View.VISIBLE
        })

        viewModel.unblockScreen.observe(this, Observer {
            pinUnlock.visibility = View.VISIBLE
            pinUnlockBlocked.visibility = View.GONE
        })


        imgPinMask1 = findViewById(R.id.imgPinMaskOne)
        imgPinMask2 = findViewById(R.id.imgPinMaskTwo)
        imgPinMask3 = findViewById(R.id.imgPinMaskThree)
        imgPinMask4 = findViewById(R.id.imgPinMaskFour)
        imgPinMask5 = findViewById(R.id.imgPinMaskFive)
        imgPinMask6 = findViewById(R.id.imgPinMaskSix)

        numPadItems.adapter = NumPadItemsAdapter(listOf(
                NumPadItem(NumPadItemType.NUMBER, 1, ""),
                NumPadItem(NumPadItemType.NUMBER, 2, "abc"),
                NumPadItem(NumPadItemType.NUMBER, 3, "def"),
                NumPadItem(NumPadItemType.NUMBER, 4, "ghi"),
                NumPadItem(NumPadItemType.NUMBER, 5, "jkl"),
                NumPadItem(NumPadItemType.NUMBER, 6, "mno"),
                NumPadItem(NumPadItemType.NUMBER, 7, "pqrs"),
                NumPadItem(NumPadItemType.NUMBER, 8, "tuv"),
                NumPadItem(NumPadItemType.NUMBER, 9, "wxyz"),
                NumPadItem(NumPadItemType.FINGER, 0, "FINGER"),
                NumPadItem(NumPadItemType.NUMBER, 0, ""),
                NumPadItem(NumPadItemType.DELETE, 0, "DEL")
        ), this)

        numPadItems.layoutManager = GridLayoutManager(this, 3)
    }

    override fun onBackPressed() {
        viewModel.delegate.onBackPressed()
    }

    private fun showFingerprintDialog() {
        if (SecurityUtils.touchSensorCanBeUsed(this)) {
            try {
                val cryptoObject = App.encryptionManager.getCryptoObject()
                val fragment = FingerprintAuthenticationDialogFragment()
                fragment.setCryptoObject(cryptoObject)
                fragment.setCallback(this@PinActivity)
                fragment.isCancelable = true
                fragment.show(fragmentManager, "fingerprint_dialog")

                isFingerprintEnabled = true
                numPadItems.adapter?.notifyDataSetChanged()

            } catch (e: Exception) {
                when (e) {
                    is UserNotAuthenticatedException -> EncryptionManager.showAuthenticationScreen(this, AUTHENTICATE_TO_FINGERPRINT)
                    is KeyPermanentlyInvalidatedException,
                    is UnrecoverableKeyException -> EncryptionManager.showKeysInvalidatedAlert(this)
                }
            }
        }
    }

    override fun onFingerprintAuthSucceed(withFingerprint: Boolean, crypto: FingerprintManager.CryptoObject?) {
        unlockWallet()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == AUTHENTICATE_TO_FINGERPRINT) {
                showFingerprintDialog()
                return
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun unlockWallet() {
        App.promptPin = false
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        viewModel.delegate.onBackPressed()
        return true
    }

    override fun onItemClick(item: NumPadItem) {
        when (item.type) {
            NumPadItemType.NUMBER -> {
                errorMessage.text = ""
                viewModel.delegate.onEnterDigit(item.number)
            }
            NumPadItemType.DELETE -> {
                viewModel.delegate.onClickDelete()
            }
            NumPadItemType.FINGER -> {
                if (isFingerprintEnabled) {
                    showFingerprintDialog()
                }
            }
        }
    }

    override fun isFingerPrintEnabled() = isFingerprintEnabled

    private fun updatePinCircles(length: Int) {
        val filledCircle = R.drawable.pin_circle_filled
        val emptyCircle = R.drawable.pin_circle_empty

        imgPinMask1.setImageResource(if (length > 0) filledCircle else emptyCircle)
        imgPinMask2.setImageResource(if (length > 1) filledCircle else emptyCircle)
        imgPinMask3.setImageResource(if (length > 2) filledCircle else emptyCircle)
        imgPinMask4.setImageResource(if (length > 3) filledCircle else emptyCircle)
        imgPinMask5.setImageResource(if (length > 4) filledCircle else emptyCircle)
        imgPinMask6.setImageResource(if (length > 5) filledCircle else emptyCircle)
    }

    companion object {

        const val AUTHENTICATE_TO_FINGERPRINT = 1

        private const val keyInteractionType = "interaction_type"
        private const val keyEnteredPin = "entered_pin"

        fun start(context: Context, interactionType: PinInteractionType, enteredPin: String = "") {
            val intent = Intent(context, PinActivity::class.java)
            intent.putExtra(keyInteractionType, interactionType)
            intent.putExtra(keyEnteredPin, enteredPin)
            context.startActivity(intent)
        }
    }

}

enum class NumPadItemType {
    NUMBER, DELETE, FINGER
}

data class NumPadItem(val type: NumPadItemType, val number: Int, val letters: String)

class NumPadItemsAdapter(private val numPadItems: List<NumPadItem>, private val listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun onItemClick(item: NumPadItem)
        fun isFingerPrintEnabled(): Boolean
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return NumPadItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_numpad_button, parent, false))
    }

    override fun getItemCount() = numPadItems.count()

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is NumPadItemViewHolder) {
            holder.bind(numPadItems[position], { listener.onItemClick(numPadItems[position]) }, listener.isFingerPrintEnabled())
        }
    }
}

class NumPadItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private var txtNumber: TextView = itemView.findViewById(R.id.txtNumPadNumber)
    private var txtLetters: TextView = itemView.findViewById(R.id.txtNumPadText)
    private var imgBackSpace: ImageView = itemView.findViewById(R.id.imgBackSpace)
    private var imgFingerprint: ImageView = itemView.findViewById(R.id.imgFingerprint)


    fun bind(item: NumPadItem, onClick: () -> (Unit), isFingerprintEnabled: Boolean) {

        itemView.setOnClickListener { onClick.invoke() }

        txtNumber.visibility = View.GONE
        txtLetters.visibility = View.GONE
        imgBackSpace.visibility = View.GONE
        imgFingerprint.visibility = View.GONE
        itemView.background = null

        when (item.type) {
            NumPadItemType.DELETE -> {
                imgBackSpace.visibility = View.VISIBLE
            }

            NumPadItemType.NUMBER -> {
                txtNumber.visibility = View.VISIBLE
                txtLetters.visibility = if (item.number == 0) View.GONE else View.VISIBLE
                txtNumber.text = item.number.toString()
                txtLetters.text = item.letters
                itemView.setBackgroundResource(R.drawable.numpad_button_background)
            }

            NumPadItemType.FINGER -> {
                imgFingerprint.visibility = if (isFingerprintEnabled) View.VISIBLE else View.GONE
            }
        }
    }
}
