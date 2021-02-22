package io.horizontalsystems.pin

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.addCallback
import androidx.biometric.BiometricPrompt
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.biometric.BiometricPrompt.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.setNavigationResult
import io.horizontalsystems.pin.core.NumPadItem
import io.horizontalsystems.pin.core.NumPadItemType
import io.horizontalsystems.pin.core.NumPadItemsAdapter
import io.horizontalsystems.pin.core.SmoothLinearLayoutManager
import io.horizontalsystems.pin.edit.EditPinModule
import io.horizontalsystems.pin.edit.EditPinPresenter
import io.horizontalsystems.pin.edit.EditPinRouter
import io.horizontalsystems.pin.set.SetPinModule
import io.horizontalsystems.pin.set.SetPinPresenter
import io.horizontalsystems.pin.set.SetPinRouter
import io.horizontalsystems.pin.unlock.UnlockPinModule
import io.horizontalsystems.pin.unlock.UnlockPinPresenter
import io.horizontalsystems.pin.unlock.UnlockPinRouter
import kotlinx.android.synthetic.main.fragment_pin.*
import java.util.concurrent.Executor

class PinFragment : Fragment(), NumPadItemsAdapter.Listener, PinPagesAdapter.Listener {

    companion object {
        const val ATTACHED_TO_LOCKSCREEN = "attached_to_lock_screen"
    }

    private var attachedToLockScreen = false

    private val interactionType: PinInteractionType by lazy {
        arguments?.getParcelable(PinModule.keyInteractionType) ?: PinInteractionType.UNLOCK
    }

    private val showCancelButton: Boolean by lazy {
        arguments?.getBoolean(PinModule.keyShowCancel) ?: false
    }

    private lateinit var pinView: PinView
    private lateinit var viewDelegate: PinModule.IViewDelegate
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var pinPagesAdapter: PinPagesAdapter
    private lateinit var numpadAdapter: NumPadItemsAdapter
    private val executor = Executor { command -> command.run() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_pin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        attachedToLockScreen = arguments?.getBoolean(ATTACHED_TO_LOCKSCREEN, false) ?: false

        pinPagesAdapter = PinPagesAdapter(this)

        context?.let {
            layoutManager = SmoothLinearLayoutManager(it, LinearLayoutManager.HORIZONTAL, false)
            pinPagesRecyclerView.layoutManager = layoutManager
        }

        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(pinPagesRecyclerView)
        pinPagesRecyclerView.adapter = pinPagesAdapter

        pinPagesRecyclerView.setOnTouchListener { _, _ -> true /*disable RecyclerView scroll*/ }

        when (interactionType) {
            PinInteractionType.UNLOCK -> {
                val unlockPresenter = ViewModelProvider(this, UnlockPinModule.Factory(showCancelButton)).get(UnlockPinPresenter::class.java)
                val unlockRouter = unlockPresenter.router as UnlockPinRouter
                pinView = unlockPresenter.view as PinView
                viewDelegate = unlockPresenter

                unlockRouter.dismissWithSuccess.observe(viewLifecycleOwner, Observer { dismissWithSuccess() })
            }
            PinInteractionType.EDIT_PIN -> {
                val editPresenter = ViewModelProvider(this, EditPinModule.Factory()).get(EditPinPresenter::class.java)
                val editRouter = editPresenter.router as EditPinRouter
                pinView = editPresenter.view as PinView
                viewDelegate = editPresenter

                editRouter.dismissWithSuccess.observe(viewLifecycleOwner, Observer { dismissWithSuccess() })
            }
            PinInteractionType.SET_PIN -> {
                val setPresenter = ViewModelProvider(this, SetPinModule.Factory()).get(SetPinPresenter::class.java)
                val setRouter = setPresenter.router as SetPinRouter
                pinView = setPresenter.view as PinView
                viewDelegate = setPresenter

                setRouter.dismissWithSuccess.observe(viewLifecycleOwner, Observer { dismissWithSuccess() })
            }
        }

        viewDelegate.viewDidLoad()

        numpadAdapter = NumPadItemsAdapter(this, NumPadItemType.BIOMETRIC)

        numPadItemsRecyclerView.adapter = numpadAdapter
        numPadItemsRecyclerView.layoutManager = GridLayoutManager(context, 3)

        observeData()

        activity?.onBackPressedDispatcher?.addCallback(this) {
            onCancelClick()
        }
    }

    override fun onCancelClick() {
        val bundle = bundleOf(
                PinModule.requestType to interactionType,
                PinModule.requestResult to PinModule.RESULT_CANCELLED
        )

        if (attachedToLockScreen) {
            setFragmentResult(PinModule.requestKey, bundle)
            return
        }

        setNavigationResult(PinModule.requestKey, bundle)
        findNavController().popBackStack()
    }

    override fun onItemClick(item: NumPadItem) {
        when (item.type) {
            NumPadItemType.NUMBER -> viewDelegate.onEnter(item.number.toString(), layoutManager.findFirstVisibleItemPosition())
            NumPadItemType.DELETE -> viewDelegate.onDelete(layoutManager.findFirstVisibleItemPosition())
            NumPadItemType.BIOMETRIC -> viewDelegate.showBiometricAuthInput()
        }
    }

    private fun dismissWithSuccess() {
        val bundle = bundleOf(
                PinModule.requestType to interactionType,
                PinModule.requestResult to PinModule.RESULT_OK
        )

        if (attachedToLockScreen) {
            setFragmentResult(PinModule.requestKey, bundle)
            return
        }

        setNavigationResult(PinModule.requestKey, bundle)
        findNavController().popBackStack()
    }

    private fun observeData() {
        pinView.showCancelButton.observe(viewLifecycleOwner, Observer { showCancelButton ->
            pinPagesAdapter.showCancelButton = showCancelButton
        })

        pinView.toolbar.observe(viewLifecycleOwner, Observer { titleRes ->
            showToolbar(titleRes)
        })

        pinView.addPages.observe(viewLifecycleOwner, Observer {
            pinPagesAdapter.pinPages.addAll(it)
            pinPagesAdapter.notifyDataSetChanged()
        })

        pinView.showPageAtIndex.observe(viewLifecycleOwner, Observer {
            Handler(Looper.getMainLooper()).postDelayed({
                pinPagesRecyclerView.smoothScrollToPosition(it)
                viewDelegate.resetPin()
                pinPagesAdapter.setEnteredPinLength(layoutManager.findFirstVisibleItemPosition(), 0)
            }, 300)
        })

        pinView.updateTopTextForPage.observe(viewLifecycleOwner, Observer { (error, pageIndex) ->
            pinPagesAdapter.updateTopTextForPage(error, pageIndex)
        })

        pinView.showError.observe(viewLifecycleOwner, Observer {
            HudHelper.showErrorMessage(this.requireView(), it)
        })

        pinView.fillPinCircles.observe(viewLifecycleOwner, Observer { (length, pageIndex) ->
            pinPagesAdapter.setEnteredPinLength(pageIndex, length)
        })

        pinView.showBiometricAuthButton.observe(viewLifecycleOwner, Observer {
            numpadAdapter.showBiometricAuthButton = true
        })

        pinView.showBiometricAuthInput.observe(viewLifecycleOwner, Observer {
            showBiometricAuthDialog()
        })

        pinView.resetCirclesWithShakeAndDelayForPage.observe(viewLifecycleOwner, Observer { pageIndex ->
            pinPagesAdapter.shakePageIndex = pageIndex
            pinPagesAdapter.notifyDataSetChanged()
            Handler(Looper.getMainLooper()).postDelayed({
                pinPagesAdapter.shakePageIndex = null
                viewDelegate.resetPin()
                pinPagesAdapter.setEnteredPinLength(pageIndex, 0)
            }, 300)
        })

        pinView.enablePinInput.observe(viewLifecycleOwner, Observer {
            numpadAdapter.numpadEnabled = true
            pinPagesAdapter.pinLockedMessage = ""
        })

        pinView.showLockedView.observe(viewLifecycleOwner, Observer {
            numpadAdapter.numpadEnabled = false
            val time = DateHelper.getOnlyTime(it)
            pinPagesAdapter.pinLockedMessage = getString(R.string.UnlockPin_WalletDisabledUntil, time)
        })
    }

    private fun showToolbar(titleRes: Int) {
        toolbar.isVisible = true
        toolbar.title = getString(titleRes)

        toolbar.setNavigationOnClickListener {
            onCancelClick()
        }
    }

    private fun showBiometricAuthDialog() {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.BiometricAuth_DialogTitle))
                .setNegativeButtonText(getString(R.string.Button_Cancel))
                .setConfirmationRequired(false)
                .build()

        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                activity?.runOnUiThread {
                    viewDelegate.onBiometricsUnlock()
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == ERROR_LOCKOUT || errorCode == ERROR_LOCKOUT_PERMANENT) {
                    BiometricScannerDisabledDialogFragment.newInstance()
                            .show(childFragmentManager, "alert_dialog")
                }
            }
        })

        biometricPrompt.authenticate(promptInfo)
    }
}

class PinPage(var topText: TopText, var enteredDigitsLength: Int = 0)

class PinPagesAdapter(private val listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun onCancelClick()
    }

    var pinPages = mutableListOf<PinPage>()
    var shakePageIndex: Int? = null

    var showCancelButton = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var pinLockedMessage = ""
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun updateTopTextForPage(text: TopText, pageIndex: Int) {
        pinPages[pageIndex].topText = text
        notifyDataSetChanged()
    }

    fun setEnteredPinLength(pageIndex: Int, enteredLength: Int) {
        pinPages[pageIndex].enteredDigitsLength = enteredLength
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return PinPageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_pin_page, parent, false)) { listener.onCancelClick() }
    }

    override fun getItemCount() = pinPages.count()

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PinPageViewHolder) {
            holder.bind(pinPages[position], shakePageIndex == position, pinLockedMessage, showCancelButton)
        }
    }
}

class PinPageViewHolder(itemView: View, onCancelClick: () -> (Unit)) : RecyclerView.ViewHolder(itemView) {
    private var txtTitle: TextView = itemView.findViewById(R.id.txtTitle)
    private var bigError: TextView = itemView.findViewById(R.id.txtBigError)
    private var txtDesc: TextView = itemView.findViewById(R.id.txtDescription)
    private var smallError: TextView = itemView.findViewById(R.id.txtSmallError)
    private var lockMessage: TextView = itemView.findViewById(R.id.lockMessage)
    private var lockImage = itemView.findViewById<ImageView>(R.id.lockImage)
    private var pinCirclesWrapper = itemView.findViewById<LinearLayout>(R.id.pinCirclesWrapper)
    private var cancelButton = itemView.findViewById<TextView>(R.id.cancelButton)

    private var imgPinMask1: ImageView = itemView.findViewById(R.id.imgPinMaskOne)
    private var imgPinMask2: ImageView = itemView.findViewById(R.id.imgPinMaskTwo)
    private var imgPinMask3: ImageView = itemView.findViewById(R.id.imgPinMaskThree)
    private var imgPinMask4: ImageView = itemView.findViewById(R.id.imgPinMaskFour)
    private var imgPinMask5: ImageView = itemView.findViewById(R.id.imgPinMaskFive)
    private var imgPinMask6: ImageView = itemView.findViewById(R.id.imgPinMaskSix)

    init {
        cancelButton.setOnClickListener { onCancelClick.invoke() }
    }

    fun bind(pinPage: PinPage, shake: Boolean, pinLockedMessage: String, showCancel: Boolean) {
        bigError.isVisible = false
        txtDesc.isVisible = false
        txtTitle.isVisible = false
        smallError.isVisible = false
        cancelButton.isVisible = false

        lockMessage.isVisible = pinLockedMessage.isNotEmpty()
        lockImage.isVisible = pinLockedMessage.isNotEmpty()
        pinCirclesWrapper.isVisible = pinLockedMessage.isEmpty()

        if (pinLockedMessage.isNotEmpty()) {
            lockMessage.text = pinLockedMessage
            return
        }

        when (pinPage.topText) {
            is TopText.Title -> {
                txtTitle.isVisible = true
                txtTitle.setText(pinPage.topText.text)
            }
            is TopText.BigError -> {
                bigError.isVisible = true
                bigError.setText(pinPage.topText.text)
            }
            is TopText.Description -> {
                txtDesc.isVisible = true
                txtDesc.setText(pinPage.topText.text)
            }
            is TopText.SmallError -> {
                smallError.isVisible = true
                smallError.setText(pinPage.topText.text)
            }
        }

        if (showCancel) {
            cancelButton.isVisible = true
        }

        updatePinCircles(pinPage.enteredDigitsLength)
        if (shake) {
            val shakeAnim = AnimationUtils.loadAnimation(itemView.context, R.anim.shake_pin_circles)
            pinCirclesWrapper.startAnimation(shakeAnim)
        }
    }

    private fun updatePinCircles(length: Int) {
        val filledCircle = R.drawable.ic_pin_ellipse_yellow
        val emptyCircle = R.drawable.ic_pin_ellipse

        imgPinMask1.setImageResource(if (length > 0) filledCircle else emptyCircle)
        imgPinMask2.setImageResource(if (length > 1) filledCircle else emptyCircle)
        imgPinMask3.setImageResource(if (length > 2) filledCircle else emptyCircle)
        imgPinMask4.setImageResource(if (length > 3) filledCircle else emptyCircle)
        imgPinMask5.setImageResource(if (length > 4) filledCircle else emptyCircle)
        imgPinMask6.setImageResource(if (length > 5) filledCircle else emptyCircle)
    }
}
