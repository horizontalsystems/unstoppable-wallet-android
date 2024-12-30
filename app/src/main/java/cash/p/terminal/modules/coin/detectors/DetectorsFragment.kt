package cash.p.terminal.modules.coin.detectors

import android.os.Parcelable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
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
import cash.p.terminal.R
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.core.requireInput
import cash.p.terminal.modules.coin.detectors.DetectorsModule.DetectorsTab
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.InfoText
import io.horizontalsystems.core.RowUniversal
import cash.p.terminal.ui_compose.components.TabItem
import cash.p.terminal.ui_compose.components.Tabs
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.subhead1_grey
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.components.subhead2_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import kotlinx.parcelize.Parcelize

class DetectorsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.requireInput<Input>()
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

    @Parcelize
    data class Input(val title: String, val issues: List<IssueParcelable>) : Parcelable

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DetectorsScreen(
    viewModel: DetectorsViewModel,
    onBackClick: () -> Unit,
) {

    val uiState = viewModel.uiState

    Scaffold(
        backgroundColor = cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = uiState.title,
                navigationIcon = {
                    HsBackButton(onClick = onBackClick)
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            val tabs = DetectorsTab.values()
            var selectedTab by remember { mutableStateOf(DetectorsTab.Token) }
            val pagerState = rememberPagerState(initialPage = selectedTab.ordinal) { tabs.size }
            LaunchedEffect(key1 = selectedTab, block = {
                pagerState.scrollToPage(selectedTab.ordinal)
            })
            val tabItems = tabs.map {
                TabItem(stringResource(id = it.titleResId), it == selectedTab, it)
            }
            Tabs(tabItems, onClick = { selectedTab = it })

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
                Divider(
                    thickness = 1.dp,
                    color = ComposeAppTheme.colors.steel10,
                )
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
    var iconTint = cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.leah

    issues.firstOrNull()?.let {
        when (it.impact) {
            "Critical" -> {
                iconResource = R.drawable.ic_warning_24
                iconTint = cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.lucian
            }

            "High" -> {
                iconResource = R.drawable.ic_warning_24
                iconTint = cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.jacob
            }

            "Low" -> {
                iconResource = R.drawable.ic_warning_24
                iconTint = cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.remus
            }

            "Informational",
            "Optimization" -> {
                if (issues.isNotEmpty()) {
                    iconResource = R.drawable.ic_warning_24
                    iconTint = cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.laguna
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
                    body_leah(
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
                        Divider(
                            thickness = 1.dp,
                            color = ComposeAppTheme.colors.steel10,
                        )
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
