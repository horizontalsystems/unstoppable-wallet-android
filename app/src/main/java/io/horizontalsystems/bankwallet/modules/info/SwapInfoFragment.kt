package io.horizontalsystems.bankwallet.modules.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.info.ui.InfoBody
import io.horizontalsystems.bankwallet.modules.info.ui.InfoHeader
import io.horizontalsystems.bankwallet.modules.info.ui.InfoSubHeader
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.core.findNavController

class SwapInfoFragment : BaseFragment() {

    companion object {
        const val DexKey = "dexKey"
        fun prepareParams(dex: SwapMainModule.Dex) = bundleOf(DexKey to dex)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            setContent {
                ComposeAppTheme {
                    SwapInfoScreen(
                        findNavController(),
                        arguments?.getParcelable(DexKey)!!
                    )
                }
            }
        }
    }

}

@Composable
private fun SwapInfoScreen(
    navController: NavController,
    dex: SwapMainModule.Dex
) {

    val context = LocalContext.current

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                title = TranslatableString.PlainString(dex.provider.title),
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = { navController.popBackStack() }
                    )
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                InfoTextBody(
                    stringResource(
                        R.string.SwapInfo_Description,
                        dex.provider.title,
                        dex.blockchain.name,
                        dex.provider.title
                    )
                )
                InfoH1(stringResource(R.string.SwapInfo_DexRelated, dex.provider.title))
                InfoSubHeader(R.string.SwapInfo_AllowanceTitle)
                InfoBody(R.string.SwapInfo_AllowanceDescription)
                InfoSubHeader(R.string.SwapInfo_PriceImpactTitle)
                InfoBody(R.string.SwapInfo_PriceImpactDescription)
                InfoSubHeader(R.string.SwapInfo_SwapFeeTitle)
                InfoBody(R.string.SwapInfo_SwapFeeDescription)
                InfoSubHeader(R.string.SwapInfo_GuaranteedAmountTitle)
                InfoBody(R.string.SwapInfo_GuaranteedAmountDescription)
                InfoSubHeader(R.string.SwapInfo_MaxSpendTitle)
                InfoBody(R.string.SwapInfo_MaxSpendDescription)

                InfoHeader(R.string.SwapInfo_Other)
                InfoSubHeader(R.string.SwapInfo_TransactionFeeTitle)
                InfoBody(R.string.SwapInfo_TransactionFeeDescription)
                InfoSubHeader(R.string.SwapInfo_TransactionSpeedTitle)
                InfoBody(R.string.SwapInfo_TransactionSpeedDescription)
                Spacer(Modifier.height(12.dp))
                ButtonSecondaryDefault(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    title = stringResource(R.string.SwapInfo_Site, dex.provider.title),
                    onClick = {
                        LinkHelper.openLinkInAppBrowser(context, dex.provider.url)
                    }
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
