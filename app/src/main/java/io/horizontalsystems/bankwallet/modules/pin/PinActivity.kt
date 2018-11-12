package io.horizontalsystems.bankwallet.modules.pin

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
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PagerSnapHelper
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.security.EncryptionManager
import io.horizontalsystems.bankwallet.core.security.FingerprintAuthenticationDialogFragment
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.ui.extensions.SmoothLinearLayoutManager
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import kotlinx.android.synthetic.main.activity_new_pin.*
import kotlinx.android.synthetic.main.custom_tall_toolbar.*
import java.security.UnrecoverableKeyException

class PinActivity : BaseActivity(), NumPadItemsAdapter.Listener, FingerprintAuthenticationDialogFragment.Callback {

    private lateinit var viewModel: PinViewModel
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var pinPagesAdapter: PinPagesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        setContentView(R.layout.activity_new_pin)

        setSupportActionBar(toolbar)
        backButton.visibility = View.GONE
        backButton.setOnClickListener { viewModel.delegate.onBackPressed() }

        val interactionType = intent.getSerializableExtra(keyInteractionType) as PinInteractionType

        pinPagesAdapter = PinPagesAdapter()
        layoutManager = SmoothLinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        pinPagesRecyclerView.layoutManager = layoutManager
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(pinPagesRecyclerView)
        pinPagesRecyclerView.adapter = pinPagesAdapter

        pinPagesRecyclerView.setOnTouchListener { _, _ ->
            //disable recyclerview scroll
            true
        }


        viewModel = ViewModelProviders.of(this).get(PinViewModel::class.java)
        viewModel.init(interactionType)

        val numpadAdapter = NumPadItemsAdapter(listOf(
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

        numPadItems.adapter = numpadAdapter
        numPadItems.layoutManager = GridLayoutManager(this, 3)

        viewModel.titleLiveDate.observe(this, Observer { title ->
            title?.let { toolbarTitle.text = getString(it) }
        })

        viewModel.hideToolbar.observe(this, Observer {
            supportActionBar?.hide()
        })

        viewModel.showBackButton.observe(this, Observer {
            backButton.visibility = View.VISIBLE
        })

        viewModel.titleLiveDate.observe(this, Observer { title ->
            title?.let { supportActionBar?.title = getString(it) }
        })

        viewModel.addPagesEvent.observe(this, Observer { pinPages ->
            pinPages?.let {
                pinPagesAdapter.pinPages.addAll(it)
                pinPagesAdapter.notifyDataSetChanged()
            }
        })

        viewModel.showPageAtIndex.observe(this, Observer { index ->
            index?.let {
                Handler().postDelayed({
                    pinPagesAdapter.setEnteredPinLength(layoutManager.findFirstVisibleItemPosition(), 0)
                    pinPagesRecyclerView.smoothScrollToPosition(it)
                }, 300)
            }
        })

        viewModel.showErrorForPage.observe(this, Observer { errorForPage ->
            errorForPage?.let { (error, pageIndex) ->
                pinPagesAdapter.setErrorForPage(pageIndex, error)
            }
        })

        viewModel.showError.observe(this, Observer { error ->
            error?.let {
                HudHelper.showErrorMessage(it)
            }
        })

        viewModel.navigateToMainLiveEvent.observe(this, Observer {
            Handler().postDelayed({
                MainModule.startAsNewTask(this)
                finish()
            }, 300)
        })

        viewModel.keyStoreSafeExecute.observe(this, Observer { triple ->
            triple?.let {
                val (action, onSuccess, onFailure) = it
                safeExecuteWithKeystore(action, onSuccess, onFailure)
            }
        })

        viewModel.fillPinCircles.observe(this, Observer { pair ->
            pair?.let { (length, pageIndex) ->
                pinPagesAdapter.setEnteredPinLength(pageIndex, length)
            }
        })

        viewModel.dismissLiveEvent.observe(this, Observer {
            finish()
        })

        viewModel.showFingerprintInputLiveEvent.observe(this, Observer {
            showFingerprintDialog()
            numpadAdapter.showFingerPrintButton = true
        })

        viewModel.resetCirclesWithShakeAndDelayForPage.observe(this, Observer { pageIndex ->
            pageIndex?.let {
                pinPagesAdapter.shakePageIndex = it
                pinPagesAdapter.notifyDataSetChanged()
                Handler().postDelayed({
                    pinPagesAdapter.shakePageIndex = null
                    pinPagesAdapter.setEnteredPinLength(pageIndex, 0)
                    viewModel.delegate.resetPin()
                }, 300)
            }
        })

    }

    override fun onBackPressed() {
        viewModel.delegate.onBackPressed()
    }

    override fun onItemClick(item: NumPadItem) {
        when (item.type) {
            NumPadItemType.NUMBER -> {
                viewModel.delegate.onEnter(item.number.toString(), layoutManager.findFirstVisibleItemPosition())
            }
            NumPadItemType.DELETE -> {
                viewModel.delegate.onDelete(layoutManager.findFirstVisibleItemPosition())
            }
            NumPadItemType.FINGER -> {
                viewModel.delegate.showBiometricUnlock()
            }
        }
    }

    override fun onFingerprintAuthSucceed(withFingerprint: Boolean, crypto: FingerprintManager.CryptoObject?) {
        viewModel.delegate.onBiometricUnlock()
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

    private fun showFingerprintDialog() {
        if (App.systemInfoManager.touchSensorCanBeUsed()) {
            try {
                val cryptoObject = App.encryptionManager.getCryptoObject()
                val fragment = FingerprintAuthenticationDialogFragment()
                fragment.setCryptoObject(cryptoObject)
                fragment.setCallback(this@PinActivity)
                fragment.isCancelable = true
                fragment.show(fragmentManager, "fingerprint_dialog")

            } catch (e: Throwable) {
                when (e) {
                    is UserNotAuthenticatedException -> EncryptionManager.showAuthenticationScreen(this, AUTHENTICATE_TO_FINGERPRINT)
                    is KeyPermanentlyInvalidatedException,
                    is UnrecoverableKeyException -> EncryptionManager.showKeysInvalidatedAlert(this)
                }
            }
        }
    }

    companion object {
        const val AUTHENTICATE_TO_FINGERPRINT = 1

        private const val keyInteractionType = "interaction_type"

        fun start(context: Context, interactionType: PinInteractionType) {
            val intent = Intent(context, PinActivity::class.java)
            intent.putExtra(keyInteractionType, interactionType)
            context.startActivity(intent)
        }

        fun startForUnlock() {
            val intent = Intent(App.instance, PinActivity::class.java)
            intent.putExtra(keyInteractionType, PinInteractionType.UNLOCK)
            intent.flags = Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS and Intent.FLAG_ACTIVITY_NEW_TASK and Intent.FLAG_ACTIVITY_CLEAR_TOP
            App.instance.startActivity(intent)
        }
    }
}

enum class NumPadItemType {
    NUMBER, DELETE, FINGER
}

//PinPage part
class PinPage(val description: Int, var enteredDidgitsLength: Int = 0, var error: Int? = null)

class PinPagesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var pinPages = mutableListOf<PinPage>()
    var shakePageIndex: Int? = null

    fun setErrorForPage(pageIndex: Int, error: Int?) {
        pinPages[pageIndex].error = error
        notifyDataSetChanged()
    }

    fun setEnteredPinLength(pageIndex: Int, enteredLength: Int) {
        pinPages[pageIndex].enteredDidgitsLength = enteredLength
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return PinPageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_pin_page, parent, false))
    }

    override fun getItemCount() = pinPages.count()

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PinPageViewHolder) {
            holder.bind(pinPages[position], shakePageIndex == position)//, { listener.onItemClick(numPadItems[position]) }, listener.isFingerPrintEnabled())
        }
    }

}

class PinPageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private var txtDesc: TextView = itemView.findViewById(R.id.txtDescription)
    private var txtError: TextView = itemView.findViewById(R.id.errorMessage)
    private var pinCirclesWrapper = itemView.findViewById<ConstraintLayout>(R.id.pinCirclesWrapper)

    private var imgPinMask1: ImageView = itemView.findViewById(R.id.imgPinMaskOne)
    private var imgPinMask2: ImageView = itemView.findViewById(R.id.imgPinMaskTwo)
    private var imgPinMask3: ImageView = itemView.findViewById(R.id.imgPinMaskThree)
    private var imgPinMask4: ImageView = itemView.findViewById(R.id.imgPinMaskFour)
    private var imgPinMask5: ImageView = itemView.findViewById(R.id.imgPinMaskFive)
    private var imgPinMask6: ImageView = itemView.findViewById(R.id.imgPinMaskSix)

    fun bind(pinPage: PinPage, shake: Boolean) {
        txtDesc.text = itemView.resources.getString(pinPage.description)
        updatePinCircles(pinPage.enteredDidgitsLength)
        txtError.text = pinPage.error?.let { itemView.resources.getString(it) } ?: ""
        if (shake) {
            val shakeAnim = AnimationUtils.loadAnimation(itemView.context, R.anim.shake_pin_circles)
            pinCirclesWrapper.startAnimation(shakeAnim)
        }
    }

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
}

//NumPad part
data class NumPadItem(val type: NumPadItemType, val number: Int, val letters: String)

class NumPadItemsAdapter(private val numPadItems: List<NumPadItem>, private val listener: Listener)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun onItemClick(item: NumPadItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return NumPadItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_numpad_button, parent, false))
    }

    override fun getItemCount() = numPadItems.count()

    var showFingerPrintButton = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is NumPadItemViewHolder) {
            holder.bind(numPadItems[position], showFingerPrintButton) { listener.onItemClick(numPadItems[position]) }
        }
    }
}

class NumPadItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private var txtNumber: TextView = itemView.findViewById(R.id.txtNumPadNumber)
    private var txtLetters: TextView = itemView.findViewById(R.id.txtNumPadText)
    private var imgBackSpace: ImageView = itemView.findViewById(R.id.imgBackSpace)
    private var imgFingerprint: ImageView = itemView.findViewById(R.id.imgFingerprint)


    fun bind(item: NumPadItem, isFingerprintEnabled: Boolean, onClick: () -> (Unit)) {

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
