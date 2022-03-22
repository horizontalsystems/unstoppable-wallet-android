package io.horizontalsystems.pin

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.activity.addCallback
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.ERROR_LOCKOUT
import androidx.biometric.BiometricPrompt.ERROR_LOCKOUT_PERMANENT
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
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
import io.horizontalsystems.pin.databinding.FragmentPinBinding
import io.horizontalsystems.pin.databinding.ViewPinPageBinding
import io.horizontalsystems.pin.edit.EditPinModule
import io.horizontalsystems.pin.edit.EditPinPresenter
import io.horizontalsystems.pin.edit.EditPinRouter
import io.horizontalsystems.pin.set.SetPinModule
import io.horizontalsystems.pin.set.SetPinPresenter
import io.horizontalsystems.pin.set.SetPinRouter
import io.horizontalsystems.pin.unlock.UnlockPinModule
import io.horizontalsystems.pin.unlock.UnlockPinPresenter
import io.horizontalsystems.pin.unlock.UnlockPinRouter
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

    private var _binding: FragmentPinBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPinBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        attachedToLockScreen = arguments?.getBoolean(ATTACHED_TO_LOCKSCREEN, false) ?: false

        pinPagesAdapter = PinPagesAdapter(this)

        context?.let {
            layoutManager = SmoothLinearLayoutManager(it, LinearLayoutManager.HORIZONTAL, false)
            binding.pinPagesRecyclerView.layoutManager = layoutManager
        }

        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.pinPagesRecyclerView)
        binding.pinPagesRecyclerView.adapter = pinPagesAdapter

        binding.pinPagesRecyclerView.setOnTouchListener { _, _ -> true /*disable RecyclerView scroll*/ }

        when (interactionType) {
            PinInteractionType.UNLOCK -> {
                val unlockPresenter =
                    ViewModelProvider(this, UnlockPinModule.Factory(showCancelButton)).get(
                        UnlockPinPresenter::class.java
                    )
                val unlockRouter = unlockPresenter.router as UnlockPinRouter
                pinView = unlockPresenter.view as PinView
                viewDelegate = unlockPresenter

                unlockRouter.dismissWithSuccess.observe(
                    viewLifecycleOwner,
                    Observer { dismissWithSuccess() })
            }
            PinInteractionType.EDIT_PIN -> {
                val editPresenter = ViewModelProvider(
                    this,
                    EditPinModule.Factory()
                ).get(EditPinPresenter::class.java)
                val editRouter = editPresenter.router as EditPinRouter
                pinView = editPresenter.view as PinView
                viewDelegate = editPresenter

                editRouter.dismissWithSuccess.observe(
                    viewLifecycleOwner,
                    Observer { dismissWithSuccess() })
            }
            PinInteractionType.SET_PIN -> {
                val setPresenter =
                    ViewModelProvider(this, SetPinModule.Factory()).get(SetPinPresenter::class.java)
                val setRouter = setPresenter.router as SetPinRouter
                pinView = setPresenter.view as PinView
                viewDelegate = setPresenter

                setRouter.dismissWithSuccess.observe(
                    viewLifecycleOwner,
                    Observer { dismissWithSuccess() })
            }
        }

        viewDelegate.viewDidLoad()

        numpadAdapter = NumPadItemsAdapter(this, NumPadItemType.BIOMETRIC)

        binding.numPadItemsRecyclerView.adapter = numpadAdapter
        binding.numPadItemsRecyclerView.layoutManager = GridLayoutManager(context, 3)

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
            NumPadItemType.NUMBER -> viewDelegate.onEnter(
                item.number.toString(),
                layoutManager.findFirstVisibleItemPosition()
            )
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
                binding.pinPagesRecyclerView.smoothScrollToPosition(it)
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

        pinView.resetCirclesWithShakeAndDelayForPage.observe(
            viewLifecycleOwner,
            Observer { pageIndex ->
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
            pinPagesAdapter.pinLockedMessage =
                getString(R.string.UnlockPin_WalletDisabledUntil, time)
        })
    }

    private fun showToolbar(titleRes: Int) {
        binding.toolbar.isVisible = true
        binding.toolbar.title = getString(titleRes)

        binding.toolbar.setNavigationOnClickListener {
            onCancelClick()
        }
    }

    private fun showBiometricAuthDialog() {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.BiometricAuth_DialogTitle))
            .setNegativeButtonText(getString(R.string.Button_Cancel))
            .setConfirmationRequired(false)
            .build()

        val biometricPrompt =
            BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
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

class PinPagesAdapter(private val listener: Listener) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
        return PinPageViewHolder(
            ViewPinPageBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        ) { listener.onCancelClick() }
    }

    override fun getItemCount() = pinPages.count()

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PinPageViewHolder) {
            holder.bind(
                pinPages[position],
                shakePageIndex == position,
                pinLockedMessage,
                showCancelButton
            )
        }
    }
}

class PinPageViewHolder(private val binding: ViewPinPageBinding, onCancelClick: () -> (Unit)) :
    RecyclerView.ViewHolder(binding.root) {
    init {
        binding.cancelButton.setOnClickListener { onCancelClick.invoke() }
    }

    fun bind(pinPage: PinPage, shake: Boolean, pinLockedMessage: String, showCancel: Boolean) {
        binding.txtBigError.isVisible = false
        binding.txtDescription.isVisible = false
        binding.txtTitle.isVisible = false
        binding.txtSmallError.isVisible = false
        binding.cancelButton.isVisible = false

        binding.lockMessage.isVisible = pinLockedMessage.isNotEmpty()
        binding.lockImage.isVisible = pinLockedMessage.isNotEmpty()
        binding.pinCirclesWrapper.isVisible = pinLockedMessage.isEmpty()

        if (pinLockedMessage.isNotEmpty()) {
            binding.lockMessage.text = pinLockedMessage
            return
        }

        when (pinPage.topText) {
            is TopText.Title -> {
                binding.txtTitle.isVisible = true
                binding.txtTitle.setText(pinPage.topText.text)
            }
            is TopText.BigError -> {
                binding.txtBigError.isVisible = true
                binding.txtBigError.setText(pinPage.topText.text)
            }
            is TopText.Description -> {
                binding.txtDescription.isVisible = true
                binding.txtDescription.setText(pinPage.topText.text)
            }
            is TopText.SmallError -> {
                binding.txtSmallError.isVisible = true
                binding.txtSmallError.setText(pinPage.topText.text)
            }
        }

        if (showCancel) {
            binding.cancelButton.isVisible = true
        }

        updatePinCircles(pinPage.enteredDigitsLength)
        if (shake) {
            val shakeAnim = AnimationUtils.loadAnimation(itemView.context, R.anim.shake_pin_circles)
            binding.pinCirclesWrapper.startAnimation(shakeAnim)
        }
    }

    private fun updatePinCircles(length: Int) {
        val filledCircle = R.drawable.ic_pin_ellipse_yellow
        val emptyCircle = R.drawable.ic_pin_ellipse

        binding.imgPinMaskOne.setImageResource(if (length > 0) filledCircle else emptyCircle)
        binding.imgPinMaskTwo.setImageResource(if (length > 1) filledCircle else emptyCircle)
        binding.imgPinMaskThree.setImageResource(if (length > 2) filledCircle else emptyCircle)
        binding.imgPinMaskFour.setImageResource(if (length > 3) filledCircle else emptyCircle)
        binding.imgPinMaskFive.setImageResource(if (length > 4) filledCircle else emptyCircle)
        binding.imgPinMaskSix.setImageResource(if (length > 5) filledCircle else emptyCircle)
    }
}
