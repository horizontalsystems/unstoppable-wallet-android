package io.horizontalsystems.bankwallet.modules.coin.audits

import android.os.Parcelable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.ui.compose.components.CellFooter
import io.horizontalsystems.bankwallet.ui.compose.components.CellMultilineLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.HsImage
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import kotlinx.parcelize.Parcelize

class CoinAuditsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            val viewModel = viewModel<CoinAuditsViewModel>(
                factory = CoinAuditsModule.Factory(input.audits)
            )
            CoinAuditsScreen(
                viewModel = viewModel,
                onPressBack = {
                    navController.popBackStack()
                },
                onClickReportUrl = {
                    LinkHelper.openLinkInAppBrowser(requireContext(), it)
                }
            )
        }
    }

    @Parcelize
    data class Input(val audits: List<CoinAuditsModule.AuditParcelable>) : Parcelable
}

@Composable
private fun CoinAuditsScreen(
    viewModel: CoinAuditsViewModel,
    onPressBack: () -> Unit,
    onClickReportUrl: (url: String) -> Unit
) {
    val uiState = viewModel.uiState

    HSScaffold(
        title = stringResource(R.string.SendNft_Title),
        onBack = onPressBack,
    ) {
        Column(Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                uiState.auditors.forEach { viewItem ->
                    item {
                        CoinAuditHeader(viewItem.name, viewItem.logoUrl)
                    }
                    item {
                        CellMultilineLawrenceSection(viewItem.auditViewItems) { auditViewItem ->
                            CoinAudit(auditViewItem) {
                                auditViewItem.reportUrl?.let {
                                    onClickReportUrl(
                                        it
                                    )
                                }
                            }
                        }
                        VSpacer(24.dp)
                    }
                }
            }

            CellFooter(text = stringResource(id = R.string.CoinPage_Audits_PoweredBy))
        }
    }
}

@Composable
fun CoinAuditHeader(name: String, logoUrl: String) {
    HsDivider()
    VSpacer(height = 14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        HsImage(
            url = logoUrl,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(32.dp)
        )
        body_leah(text = name)
    }
    VSpacer(height = 14.dp)
}

@Composable
fun CoinAudit(auditViewItem: CoinAuditsModule.AuditViewItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick, enabled = auditViewItem.reportUrl != null)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            headline2_leah(text = auditViewItem.date ?: "")
            subhead2_grey(
                text = auditViewItem.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        subhead2_grey(text = auditViewItem.issues.getString())

        if (auditViewItem.reportUrl != null) {
            Image(painterResource(id = R.drawable.ic_arrow_right), contentDescription = "")
        }
    }
}
