package io.horizontalsystems.bankwallet.ui
//
//import android.content.Context
//import android.util.AttributeSet
//import android.view.LayoutInflater
//import android.view.View
//import androidx.constraintlayout.widget.ConstraintLayout
//import androidx.core.view.isVisible
//import androidx.fragment.app.FragmentManager
//import androidx.lifecycle.LifecycleOwner
//import androidx.lifecycle.Observer
//import io.horizontalsystems.bankwallet.core.ethereum.ISendFeePriorityViewModel
//import io.horizontalsystems.bankwallet.core.ethereum.ISendFeeViewModel
//import io.horizontalsystems.bankwallet.modules.sendevmtransaction.feesettings.SendFeeSliderViewItem
//import io.horizontalsystems.bankwallet.databinding.ViewFeeSelectorBinding
//import io.horizontalsystems.seekbar.FeeSeekBar
//
//class FeeSelectorView @JvmOverloads constructor(
//    context: Context,
//    attrs: AttributeSet? = null,
//    defStyleAttr: Int = 0
//) : ConstraintLayout(context, attrs, defStyleAttr) {
//
//    private val binding = ViewFeeSelectorBinding.inflate(LayoutInflater.from(context), this)
//
//    var onTxSpeedClickListener: (view: View) -> Unit = { }
//    var prioritySelectListener: (position: Int) -> Unit = { }
//    var customFeeSeekBarListener: (value: Int) -> Unit = { }
//
//    init {
//        binding.txSpeedMenuClickArea.setOnClickListener {
//            onTxSpeedClickListener(it)
//        }
//
//        binding.customFeeSeekBar.setListener(object : FeeSeekBar.Listener {
//            override fun onSelect(value: Int) {
//                customFeeSeekBarListener(value)
//            }
//        })
//
//        clipChildren = false
//    }
//
//    fun setEstimatedFeeText(value: String) {
//        binding.txEstimatedFeeValue.text = value
//    }
//
//    fun setFeeText(value: String) {
//        binding.txFeeValue.text = value
//    }
//
//    fun setPriorityText(value: String) {
//        binding.txSpeedMenu.text = value
//        invalidate()
//    }
//
//    fun setFeeSliderViewItem(viewItem: SendFeeSliderViewItem?) {
//        binding.customFeeSeekBar.isVisible = viewItem != null
//
//        viewItem?.let {
//            binding.customFeeSeekBar.min = it.range.first.toInt()
//            binding.customFeeSeekBar.max = it.range.last.toInt()
//            binding.customFeeSeekBar.progress = it.initialValue.toInt()
//            binding.customFeeSeekBar.setBubbleHint(it.unit)
//        }
//
//        invalidate()
//    }
//
////    fun openPrioritySelector(items: List<SendPriorityViewItem>, fragmentManager: FragmentManager) {
////        val selectorItems = items.map { feeRateViewItem ->
////            SelectorItem(feeRateViewItem.title, feeRateViewItem.selected)
////        }
////
////        SelectorDialog
////            .newInstance(selectorItems, context.getString(R.string.Send_DialogSpeed)) { position ->
////                prioritySelectListener(position)
////            }
////            .show(fragmentManager, "fee_rate_priority_selector")
////    }
//
//    fun setFeeSelectorViewInteractions(
//        sendFeeViewModel: ISendFeeViewModel,
//        sendFeePriorityViewModel: ISendFeePriorityViewModel,
//        viewLifecycleOwner: LifecycleOwner,
//        fragmentManager: FragmentManager,
//        showSpeedInfoListener: () -> Unit
//    ) {
//
//        binding.feeInfoImageClickArea.setOnClickListener {
//            showSpeedInfoListener.invoke()
//        }
//
////        if (sendFeeViewModel.hasEstimatedFee) {
////            sendFeeViewModel.estimatedFeeLiveData.observe(viewLifecycleOwner, Observer {
////                setEstimatedFeeText(it)
////            })
////        } else {
////            // hideEstimatedFeeBlock
////            binding.txEstimatedFeeTitle.isVisible = false
////            binding.txEstimatedFeeValue.isVisible = false
////            binding.txFeeTitle.text = context.getString(R.string.Swap_Fee)
////        }
//
//
//        sendFeeViewModel.feeLiveData.observe(viewLifecycleOwner, Observer {
//            setFeeText(it)
//        })
//
////        sendFeePriorityViewModel.priorityLiveData.observe(viewLifecycleOwner, Observer {
////            setPriorityText(it)
////        })
////
////        sendFeePriorityViewModel.openSelectPriorityLiveEvent.observe(viewLifecycleOwner, Observer {
////            openPrioritySelector(it, fragmentManager)
////        })
////
////        sendFeePriorityViewModel.feeSliderLiveData.observe(viewLifecycleOwner, {
////            setFeeSliderViewItem(it)
////        })
//
////        sendFeeViewModel.warningOfStuckLiveData.observe(viewLifecycleOwner, {
////            binding.warningOfStuck.isVisible = it
////        })
//
////        onTxSpeedClickListener = {
////            sendFeePriorityViewModel.openSelectPriority()
////        }
//
////        prioritySelectListener = { position ->
////            sendFeePriorityViewModel.selectPriority(position)
////        }
//
////        customFeeSeekBarListener = {
////            sendFeePriorityViewModel.changeCustomPriority(it.toLong())
////        }
//    }
//
//}
