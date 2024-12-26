package io.horizontalsystems.bankwallet.modules.usersubscription.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.usersubscription.BuySubscriptionViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.RadialBackground
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectSubscriptionScreen(
    navController: NavController,
    onCloseClick: () -> Unit
) {
    val viewModel = viewModel<BuySubscriptionViewModel> {
        BuySubscriptionViewModel()
    }

    val uiState = viewModel.uiState

    val subscriptions = uiState.subscriptions

    val coroutineScope = rememberCoroutineScope()
    val selectedTabIndex = remember { mutableStateOf(0) }
    val modalBottomSheetState =
        androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isBottomSheetVisible by remember { mutableStateOf(false) }

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            TitleCenteredTopBar(
                title = stringResource(R.string.Premium_Title),
                onCloseClick = onCloseClick
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            RadialBackground()
            Column {
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                )
                {
                    VSpacer(12.dp)
                    body_grey(
                        text = stringResource(R.string.Premium_ChoosePlanForYou),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    VSpacer(24.dp)

                    if (subscriptions.isNotEmpty()) {
                        PlanTabs(
                            items = subscriptions,
                            selectedTabIndex = selectedTabIndex.value,
                            onTabChange = { index ->
                                selectedTabIndex.value = index
                            }
                        )
                        VSpacer(32.dp)
                    }
                }

                ButtonsGroupWithShade {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ButtonPrimaryCustomColor(
                            modifier = Modifier.fillMaxWidth(),
                            title = stringResource(R.string.Premium_TryForFree),
                            brush = yellowGradient,
                            onClick = {
                                coroutineScope.launch {
                                    isBottomSheetVisible = true
                                    modalBottomSheetState.show()
                                }
                            },
                        )
                        VSpacer(12.dp)
                        ColoredTextSecondaryButton(
                            title = stringResource(R.string.Premium_Restore),
                            onClick = {
                                //
                            },
                            color = ComposeAppTheme.colors.leah
                        )
                    }
                }
            }
        }
        if (isBottomSheetVisible) {
            ModalBottomSheet(
                onDismissRequest = {
                    coroutineScope.launch {
                        modalBottomSheetState.hide()
                    }
                    isBottomSheetVisible = false
                },
                sheetState = modalBottomSheetState,
                containerColor = ComposeAppTheme.colors.transparent
            ) {
                if (subscriptions.isNotEmpty()) {
                    SelectSubscriptionBottomSheet(
                        subscriptionId = subscriptions[selectedTabIndex.value].id,
                        type = PremiumPlanType.entries[selectedTabIndex.value],
                        onDismiss = {
                            coroutineScope.launch {
                                modalBottomSheetState.hide()
                                isBottomSheetVisible = false
                            }
                        },
                        onSubscribeClick = { type ->
                            coroutineScope.launch {
                                modalBottomSheetState.hide()
                                isBottomSheetVisible = false
                                navController.navigate("premium_subscribed_page?type=${type.name}")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun SelectSubscriptionScreenPreview() {
    ComposeAppTheme {
        val ctx = LocalContext.current
        SelectSubscriptionScreen(navController = NavController(ctx)) {}
    }
}