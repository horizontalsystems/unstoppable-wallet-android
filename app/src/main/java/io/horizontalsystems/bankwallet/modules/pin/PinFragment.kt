package io.horizontalsystems.bankwallet.modules.pin

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.biometric.BiometricPrompt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.ui.extensions.*
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import kotlinx.android.synthetic.main.fragment_pin.*
import java.util.concurrent.Executor


class PinFragment: Fragment(), NumPadItemsAdapter.Listener {

    private val interactionType: PinInteractionType by lazy {
        //todo default parameter?
        arguments?.getSerializable(PinActivity.keyInteractionType) as? PinInteractionType ?: PinInteractionType.UNLOCK
    }

    private val showCancelButton: Boolean by lazy {
        arguments?.getBoolean(PinActivity.keyShowCancel) ?: true
    }

    private lateinit var viewModel: PinViewModel
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var pinPagesAdapter: PinPagesAdapter
    private val executor = Executor { command -> command.run() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (container == null){
            return null
        }
        return inflater.inflate(R.layout.fragment_pin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pinPagesAdapter = PinPagesAdapter()
        context?.let {
            layoutManager = SmoothLinearLayoutManager(it, LinearLayoutManager.HORIZONTAL, false)
            pinPagesRecyclerView.layoutManager = layoutManager
        }
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(pinPagesRecyclerView)
        pinPagesRecyclerView.adapter = pinPagesAdapter

        pinPagesRecyclerView.setOnTouchListener {_, _ ->  true /*disable RecyclerView scroll*/ }

        viewModel = ViewModelProviders.of(this).get(PinViewModel::class.java)
        viewModel.init(interactionType, showCancelButton)

        val numpadAdapter = NumPadItemsAdapter(this, NumPadItemType.FINGER)

        numPadItemsRecyclerView.adapter = numpadAdapter
        numPadItemsRecyclerView.layoutManager = GridLayoutManager(context, 3)

        viewModel.hideToolbar.observe(viewLifecycleOwner, Observer {
            shadowlessToolbar.visibility = View.GONE
        })

        viewModel.showBackButton.observe(viewLifecycleOwner, Observer {
            shadowlessToolbar.bind(null, TopMenuItem(R.drawable.back, onClick = { viewModel.delegate.onBackPressed() }))
        })

        viewModel.titleLiveDate.observe(viewLifecycleOwner, Observer { title ->
            title?.let {
                shadowlessToolbar.bindTitle(getString(it))
            }
        })

        viewModel.addPagesEvent.observe(viewLifecycleOwner, Observer { pinPages ->
            pinPages?.let {
                pinPagesAdapter.pinPages.addAll(it)
                pinPagesAdapter.notifyDataSetChanged()
            }
        })

        viewModel.showPageAtIndex.observe(viewLifecycleOwner, Observer { index ->
            index?.let {
                Handler().postDelayed({
                    pinPagesAdapter.setEnteredPinLength(layoutManager.findFirstVisibleItemPosition(), 0)
                    pinPagesRecyclerView.smoothScrollToPosition(it)
                }, 300)
            }
        })

        viewModel.updateTopTextForPage.observe(viewLifecycleOwner, Observer { (error, pageIndex) ->
            pinPagesAdapter.updateTopTextForPage(error, pageIndex)
        })

        viewModel.showError.observe(viewLifecycleOwner, Observer { error ->
            error?.let {
                HudHelper.showErrorMessage(it)
            }
        })

        viewModel.navigateToMainLiveEvent.observe(viewLifecycleOwner, Observer {
            context?.let { ctx -> MainModule.start(ctx) }
            activity?.finish()
        })

        viewModel.fillPinCircles.observe(viewLifecycleOwner, Observer { pair ->
            pair?.let { (length, pageIndex) ->
                pinPagesAdapter.setEnteredPinLength(pageIndex, length)
            }
        })

        viewModel.dismissWithCancelLiveEvent.observe(viewLifecycleOwner, Observer {
            activity?.setResult(PinModule.RESULT_CANCELLED)
            activity?.finish()
        })

        viewModel.dismissWithSuccessLiveEvent.observe(viewLifecycleOwner, Observer {
            activity?.setResult(PinModule.RESULT_OK)
            activity?.finish()
        })

        viewModel.showFingerprintInputLiveEvent.observe(viewLifecycleOwner, Observer { cryptoObject ->
            cryptoObject?.let {
                showFingerprintDialog(it)
                numpadAdapter.showFingerPrintButton = true
            }
        })

        viewModel.resetCirclesWithShakeAndDelayForPage.observe(viewLifecycleOwner, Observer { pageIndex ->
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

        viewModel.showPinInput.observe(viewLifecycleOwner, Observer {
            pinUnlock.visibility = View.VISIBLE
            pinUnlockBlocked.visibility = View.GONE
        })

        viewModel.showLockedView.observe(viewLifecycleOwner, Observer { untilDate ->
            untilDate?.let {
                pinUnlock.visibility = View.GONE
                pinUnlockBlocked.visibility = View.VISIBLE
                val time = DateHelper.formatDate(it, "HH:mm:ss")
                blockedScreenMessage.text = getString(R.string.UnlockPin_WalletDisabledUntil, time)
            }
        })

        viewModel.closeApplicationLiveEvent.observe(viewLifecycleOwner, Observer {
            activity?.finishAffinity()
        })
    }

    override fun onItemClick(item: NumPadItem) {
        when (item.type) {
            NumPadItemType.NUMBER -> viewModel.delegate.onEnter(item.number.toString(), layoutManager.findFirstVisibleItemPosition())
            NumPadItemType.DELETE -> viewModel.delegate.onDelete(layoutManager.findFirstVisibleItemPosition())
            NumPadItemType.FINGER -> viewModel.delegate.showFingerprintUnlock()
        }
    }

    private fun showFingerprintDialog(cryptoObject: BiometricPrompt.CryptoObject) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.Fingerprint_DialogTitle))
                .setNegativeButtonText(getString(R.string.Button_Cancel))
                .build()

        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                viewModel.delegate.onFingerprintUnlock()
            }
        })

        biometricPrompt.authenticate(promptInfo, cryptoObject)
    }

    companion object {

        fun newInstance(interactionType: PinInteractionType, showCancel: Boolean = true): PinFragment {
            val pinFragment = PinFragment()

            val args = Bundle().also {
                it.putSerializable(PinActivity.keyInteractionType, interactionType)
                it.putBoolean(PinActivity.keyShowCancel, showCancel)
            }
            pinFragment.arguments = args

            return pinFragment
        }
    }

}


//PinPage part
class PinPage(var topText: TopText, var enteredDigitsLength: Int = 0)

class PinPagesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var pinPages = mutableListOf<PinPage>()
    var shakePageIndex: Int? = null

    fun updateTopTextForPage(text: TopText, pageIndex: Int) {
        pinPages[pageIndex].topText = text
        notifyDataSetChanged()
    }

    fun setEnteredPinLength(pageIndex: Int, enteredLength: Int) {
        pinPages[pageIndex].enteredDigitsLength = enteredLength
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return PinPageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_pin_page, parent, false))
    }

    override fun getItemCount() = pinPages.count()

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PinPageViewHolder) {
            holder.bind(pinPages[position], shakePageIndex == position)//, { listener.onChangeProvider(numPadItems[position]) }, listener.isBiometricEnabled())
        }
    }

}

class PinPageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private var txtTitle: TextView = itemView.findViewById(R.id.txtTitle)
    private var bigError: TextView = itemView.findViewById(R.id.txtBigError)
    private var txtDesc: TextView = itemView.findViewById(R.id.txtDescription)
    private var smallError: TextView = itemView.findViewById(R.id.txtSmallError)
    private var pinCirclesWrapper = itemView.findViewById<ConstraintLayout>(R.id.pinCirclesWrapper)

    private var imgPinMask1: ImageView = itemView.findViewById(R.id.imgPinMaskOne)
    private var imgPinMask2: ImageView = itemView.findViewById(R.id.imgPinMaskTwo)
    private var imgPinMask3: ImageView = itemView.findViewById(R.id.imgPinMaskThree)
    private var imgPinMask4: ImageView = itemView.findViewById(R.id.imgPinMaskFour)
    private var imgPinMask5: ImageView = itemView.findViewById(R.id.imgPinMaskFive)
    private var imgPinMask6: ImageView = itemView.findViewById(R.id.imgPinMaskSix)

    fun bind(pinPage: PinPage, shake: Boolean) {
        bigError.visibility = View.GONE
        txtDesc.visibility = View.GONE
        txtTitle.visibility = View.GONE
        smallError.visibility = View.GONE

        when(pinPage.topText) {
            is TopText.Title -> {
                txtTitle.visibility = View.VISIBLE
                txtTitle.setText(pinPage.topText.text)
            }
            is TopText.BigError -> {
                bigError.visibility = View.VISIBLE
                bigError.setText(pinPage.topText.text)
            }
            is TopText.Description -> {
                txtDesc.visibility = View.VISIBLE
                txtDesc.setText(pinPage.topText.text)
            }
            is TopText.SmallError -> {
                smallError.visibility = View.VISIBLE
                smallError.setText(pinPage.topText.text)
            }
        }

        updatePinCircles(pinPage.enteredDigitsLength)
        if (shake) {
            val shakeAnim = AnimationUtils.loadAnimation(itemView.context, R.anim.shake_pin_circles)
            pinCirclesWrapper.startAnimation(shakeAnim)
        }
    }

    private fun updatePinCircles(length: Int) {
        val filledCircle = R.drawable.pin_circle_filled
        val emptyCircle = R.drawable.ic_circle_steel_20_with_border

        imgPinMask1.setImageResource(if (length > 0) filledCircle else emptyCircle)
        imgPinMask2.setImageResource(if (length > 1) filledCircle else emptyCircle)
        imgPinMask3.setImageResource(if (length > 2) filledCircle else emptyCircle)
        imgPinMask4.setImageResource(if (length > 3) filledCircle else emptyCircle)
        imgPinMask5.setImageResource(if (length > 4) filledCircle else emptyCircle)
        imgPinMask6.setImageResource(if (length > 5) filledCircle else emptyCircle)
    }
}
