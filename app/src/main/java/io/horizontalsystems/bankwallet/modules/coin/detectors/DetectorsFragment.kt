package io.horizontalsystems.bankwallet.modules.coin.detectors

import android.os.Parcelable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.coin.detectors.DetectorsModule.DetectorsTab
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.tabs.TabItem
import io.horizontalsystems.bankwallet.uiv3.components.tabs.TabsTop
import io.horizontalsystems.bankwallet.uiv3.components.tabs.TabsTopType
import kotlinx.parcelize.Parcelize

class DetectorsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            val viewModel = viewModel<DetectorsViewModel>(
                factory = DetectorsModule.Factory(input.title, input.issues)
            )
            DetectorsScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                },
            )
        }
    }

    @Parcelize
    data class Input(val title: String, val issues: List<IssueParcelable>) : Parcelable

}

@Composable
private fun DetectorsScreen(
    viewModel: DetectorsViewModel,
    onBackClick: () -> Unit,
) {

    val uiState = viewModel.uiState

    HSScaffold(
        title = uiState.title,
        onBack = onBackClick,
    ) {
        Column(Modifier.fillMaxSize()) {
            val tabs = DetectorsTab.values()
            var selectedTab by remember { mutableStateOf(DetectorsTab.Token) }
            val pagerState = rememberPagerState(initialPage = selectedTab.ordinal) { tabs.size }
            LaunchedEffect(key1 = selectedTab, block = {
                pagerState.scrollToPage(selectedTab.ordinal)
            })
            val tabItems = tabs.map {
                TabItem(stringResource(id = it.titleResId), it == selectedTab, it)
            }
            TabsTop(TabsTopType.Fitted, tabItems) {
                selectedTab = it
            }

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false
            ) { page ->
                when (tabs[page]) {
                    DetectorsTab.Token -> IssueList(uiState.coreIssues) { id ->
                        viewModel.toggleExpandCore(id)
                    }

                    DetectorsTab.General -> IssueList(uiState.generalIssues) { id ->
                        viewModel.toggleExpandGeneral(id)
                    }
                }
            }
        }
    }
}

@Composable
fun IssueList(
    issues: List<DetectorsModule.IssueViewItem>,
    toggleExpand: (Int) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            VSpacer(12.dp)
        }
        itemsIndexed(issues) { index, issue ->
            if (index > 0) {
                HsDivider()
            }
            DetectorCell(
                issueViewItem = issue,
            ) {
                toggleExpand(it)
            }
        }
    }
}

@Composable
fun DetectorCell(
    issueViewItem: DetectorsModule.IssueViewItem,
    toggleExpand: (Int) -> Unit
) {
    val issue = issueViewItem.issue
    val issues = issue.issues ?: emptyList()
    var iconResource = R.drawable.ic_check_24
    var iconTint = ComposeAppTheme.colors.leah

    issues.firstOrNull()?.let {
        when (it.impact) {
            "Critical" -> {
                iconResource = R.drawable.ic_warning_24
                iconTint = ComposeAppTheme.colors.lucian
            }

            "High" -> {
                iconResource = R.drawable.ic_warning_24
                iconTint = ComposeAppTheme.colors.jacob
            }

            "Low" -> {
                iconResource = R.drawable.ic_warning_24
                iconTint = ComposeAppTheme.colors.remus
            }

            "Informational",
            "Optimization" -> {
                if (issues.isNotEmpty()) {
                    iconResource = R.drawable.ic_warning_24
                    iconTint = ComposeAppTheme.colors.laguna
                }
            }

            else -> {}
        }
    }

    Column(
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            toggleExpand.invoke(issueViewItem.id)
        }
    ) {
        RowUniversal(
            modifier = Modifier
                .fillMaxWidth()
                .background(ComposeAppTheme.colors.lawrence)
                .padding(horizontal = 16.dp),
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(iconResource),
                contentDescription = null,
                tint = iconTint
            )
            HSpacer(width = 16.dp)
            if (issue.title != null) {
                Column(modifier = Modifier.weight(1f)) {
                    headline2_leah(
                        text = issue.title,
                    )
                    VSpacer(1.dp)
                    subhead2_grey(
                        text = issue.description,
                    )
                }
            } else {
                subhead2_leah(
                    text = issue.description,
                    modifier = Modifier.weight(1f)
                )
            }

            if (issues.isNotEmpty()) {
                val painter = if (issueViewItem.expanded) {
                    painterResource(R.drawable.ic_arrow_big_up_20)
                } else {
                    painterResource(R.drawable.ic_arrow_big_down_20)
                }

                HSpacer(width = 8.dp)
                subhead1_grey(
                    text = stringResource(
                        id = R.string.Detectors_IssuesCount,
                        issues.size
                    )
                )
                Icon(
                    modifier = Modifier.padding(start = 8.dp),
                    painter = painter,
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
            }
        }

        AnimatedVisibility(
            visible = issueViewItem.expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                issues.forEachIndexed { index, text ->
                    if (index > 0) {
                        HsDivider()
                    }
                    InfoText(
                        text = text.description,
                        paddingBottom = 32.dp
                    )
                }
            }
        }
    }
}
