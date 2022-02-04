package io.horizontalsystems.bankwallet.modules.sendevmtransaction.feesettings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItem
import io.horizontalsystems.bankwallet.core.ethereum.EvmFeeCellViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantError
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.marketkit.models.CoinType

class SendEvmFeeSettingsFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val feeViewModel by navGraphViewModels<EvmFeeCellViewModel>(requireArguments().getInt(NAV_GRAPH_ID))

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    SendEvmFeeSettingsScreen(
                        feeViewModel,
                        onClickNavigation = {
                            findNavController().popBackStack()
                        }
                    )
                }
            }
        }
    }

    companion object {
        private const val NAV_GRAPH_ID = "nav_graph_id"

        fun prepareParams(@IdRes navGraphId: Int) =
            bundleOf(NAV_GRAPH_ID to navGraphId)
    }

}

@Composable
fun SendEvmFeeSettingsScreen(
    feeViewModel: EvmFeeCellViewModel,
    onClickNavigation: () -> Unit
) {

    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            title = TranslatableString.ResString(R.string.FeeSettings_Title),
            navigationIcon = {
                IconButton(onClick = onClickNavigation) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = "back button",
                        tint = ComposeAppTheme.colors.jacob
                    )
                }
            }
        )

        val viewModel = viewModel<ViewModel>(
            factory = FeeSettingsModule.Factory(
                feeViewModel.feeService,
                feeViewModel.coinService
            )
        )

        Log.e("AAA", "SendEvmFeeSettingsScreen")

        if (viewModel is LegacyFeeSettingsViewModel) {
            LegacyFeeSettings(viewModel = viewModel, onSelectGasPrice = {
                viewModel.onSelectGasPrice(it)
            })
        }

//        Spacer(modifier = Modifier.height(16.dp))
//        FeeCell(title = stringResource(R.string.FeeSettings_Fee), fee)
//        Spacer(modifier = Modifier.height(8.dp))


//        when (val viewItem = viewItem) {
//            is FeeSettingsViewItem.Eip1559FeeSettingsViewItem -> {
//                Eip1559FeeSettings(
//                    viewItem = viewItem,
//                    onSelectMaxFee = { maxFee ->
//                        viewModel.onChangeMaxFee(maxFee)
//                    },
//                    onSelectMaxPriorityFee = { maxPriorityFee ->
//                        viewModel.onChangeMaxPriorityFee(maxPriorityFee)
//                    }
//                )
//            }
//            is FeeSettingsViewItem.LegacyFeeSettingsViewItem -> {
//                LegacyFeeSettings(viewItem) {
//                    Log.e("AAA", "onSelectGasPrice: $it")
//
//                    viewModel.changeCustomPriority(it)
//                }
//            }
//        }
//
//        viewModel.cautionsLiveData // TODO HANDLE

    }
}

sealed class FeeSettingsViewItem {

    data class LegacyFeeSettingsViewItem(
        val gasLimit: String,
        val gasPrice: Long,
        val gasPriceRange: LongRange,
        val unit: String
    ) : FeeSettingsViewItem()

    data class Eip1559FeeSettingsViewItem(
        val gasLimit: String,
        val baseFee: Long,
        val maxFee: Long,
        val maxFeeRange: LongRange,
        val maxPriorityFee: Long,
        val maxPriorityFeeRange: LongRange,
        val unit: String
    ) : FeeSettingsViewItem()

}

@Composable
fun Eip1559FeeSettings(
    viewItem: FeeSettingsViewItem.Eip1559FeeSettingsViewItem,
    onSelectMaxFee: (value: Long) -> Unit,
    onSelectMaxPriorityFee: (value: Long) -> Unit
) {

}


