package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.databinding.ViewSendFeeBinding
import io.horizontalsystems.bankwallet.modules.send.SendActivity
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.submodules.SendSubmoduleFragment
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.horizontalsystems.seekbar.FeeSeekBar

class SendFeeFragment(
    private val coin: PlatformCoin,
    private val feeModuleDelegate: SendFeeModule.IFeeModuleDelegate,
    private val sendHandler: SendModule.ISendHandler,
    private val customPriorityUnit: CustomPriorityUnit?
) : SendSubmoduleFragment() {

    private val presenter by activityViewModels<SendFeePresenter> {
        SendFeeModule.Factory(
            coin,
            sendHandler,
            feeModuleDelegate,
            customPriorityUnit
        )
    }

    private var _binding: ViewSendFeeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ViewSendFeeBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        val presenterView = presenter.view as SendFeeView

        binding.txError.isVisible = false

        binding.txSpeedMenuClickArea.setOnClickListener {
            presenter.onClickFeeRatePriority()
        }
        binding.feeInfoImageClickArea.setOnClickListener {
            (activity as? SendActivity)?.showFeeInfo()
        }
        binding.txFeeLoading.isVisible = false
        binding.txFeeLoading.text = getString(R.string.Alert_Loading)

        binding.customFeeSeekBar.setListener(object : FeeSeekBar.Listener {
            override fun onSelect(value: Int) {
                presenter.onChangeFeeRateValue(value)
            }
        })

        presenterView.showAdjustableFeeMenu.observe(viewLifecycleOwner, Observer { visible ->
            binding.speedViews.isVisible = visible
        })

        presenterView.primaryFee.observe(
            viewLifecycleOwner,
            Observer { binding.txFeePrimary.text = " $it" })

        presenterView.secondaryFee.observe(viewLifecycleOwner, Observer { fiatFee ->
            fiatFee?.let { binding.txFeeSecondary.text = " | $it" }
        })

        presenterView.feePriority.observe(viewLifecycleOwner, Observer { feePriority ->
            context?.let {
                binding.txSpeedMenu.text = TextHelper.getFeeRatePriorityString(it, feePriority)
            }
        })

        presenterView.showFeePriorityOptions.observe(viewLifecycleOwner, Observer { feeRates ->
            val selectorItems = feeRates.map { feeRateViewItem ->
                val caption = context?.let { context ->
                    TextHelper.getFeeRatePriorityString(context, feeRateViewItem.feeRatePriority)
                } ?: ""

                SelectorItem(caption, feeRateViewItem.selected)
            }

            SelectorDialog
                .newInstance(selectorItems, getString(R.string.Send_DialogSpeed)) { position ->
                    presenter.onChangeFeeRate(feeRates[position].feeRatePriority)
                }
                .show(parentFragmentManager, "fee_rate_priority_selector")
        })

        presenterView.showCustomFeePriority.observe(viewLifecycleOwner, Observer { isVisible ->
            binding.customFeeSeekBar.isVisible = isVisible
        })

        presenterView.setCustomFeeParams.observe(
            viewLifecycleOwner,
            Observer { (value, range, label) ->
                binding.customFeeSeekBar.progress = value
                binding.customFeeSeekBar.min = range.first
                binding.customFeeSeekBar.max = range.last
                label?.let {
                    binding.customFeeSeekBar.setBubbleHint(it)
                }
            })

        presenterView.insufficientFeeBalanceError.observe(viewLifecycleOwner, Observer { error ->
            binding.feeError.isVisible = error != null

            if (error != null) {
                val coinCode = error.coin.code
                val tokenProtocol = error.coinProtocol
                val feeCoinTitle = error.feeCoin.name
                val formattedFee =
                    App.numberFormatter.formatCoin(error.fee.value, error.fee.coin.code, 0, 8)

                binding.feeError.text = context?.getString(
                    R.string.Send_Token_InsufficientFeeAlert, coinCode, tokenProtocol,
                    feeCoinTitle, formattedFee
                )
            }
        })

        presenterView.setLoading.observe(viewLifecycleOwner, Observer { loading ->
            setLoading(loading)
        })

        presenterView.setError.observe(viewLifecycleOwner, Observer { error ->
            setError(error)
        })

        presenterView.showLowFeeWarningLiveData.observe(viewLifecycleOwner, { show ->
            binding.lowFeeWarning.isVisible = show
        })
    }

    override fun init() {
        presenter.onViewDidLoad()
    }

    private fun setLoading(loading: Boolean) {

        binding.txFeePrimary.isVisible = !loading
        binding.txFeeSecondary.isVisible = !loading
        binding.txFeeLoading.isVisible = loading

        binding.txSpeedMenu.alpha = if (loading) 0.5f else 1f
        binding.speedViews.isEnabled = (!loading)
    }

    private fun setError(error: Exception) {
        binding.txError.isVisible = true
        binding.txFeeTitle.isInvisible = true
        binding.txFeeLoading.isVisible = false
        binding.txFeePrimary.isVisible = false
        binding.txFeeSecondary.isVisible = false
    }
}

