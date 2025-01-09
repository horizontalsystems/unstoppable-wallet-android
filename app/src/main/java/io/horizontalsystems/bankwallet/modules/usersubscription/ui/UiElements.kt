package io.horizontalsystems.bankwallet.modules.usersubscription.ui

import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.usersubscription.BuySubscriptionChoosePlanViewModel
import io.horizontalsystems.bankwallet.modules.usersubscription.BuySubscriptionModel.descriptionStringRes
import io.horizontalsystems.bankwallet.modules.usersubscription.BuySubscriptionModel.iconRes
import io.horizontalsystems.bankwallet.modules.usersubscription.BuySubscriptionModel.stringRepresentation
import io.horizontalsystems.bankwallet.modules.usersubscription.BuySubscriptionModel.titleStringRes
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Steel20
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefaults
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryTransparent
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondary
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.SecondaryButtonDefaults.buttonColors
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.bankwallet.ui.compose.components.caption_grey
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_jacob
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_remus
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.subscriptions.core.HSPurchase
import io.horizontalsystems.subscriptions.core.IPaidAction
import io.horizontalsystems.subscriptions.core.Subscription
import io.horizontalsystems.subscriptions.core.VIPClub
import io.horizontalsystems.subscriptions.core.VIPSupport

enum class PremiumPlanType(@StringRes val titleResId: Int) {
    ProPlan(R.string.Premium_PlanPro),
    VipPlan(R.string.Premium_PlanVip);
}

val yellowGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFFFFD000),
        Color(0xFFFFA800),
    )
)

val steelBrush = Brush.horizontalGradient(
    colors = listOf(Steel20, Steel20)
)

@Composable
fun SelectSubscriptionBottomSheet(
    subscriptionId: String,
    type: PremiumPlanType,
    onDismiss: () -> Unit,
    viewModel: BuySubscriptionChoosePlanViewModel = viewModel(),
    onPurchase: (PremiumPlanType) -> Unit,
    onError: (Throwable) -> Unit,
) {
    val uiState = viewModel.uiState
    val activity = LocalActivity.current

    LaunchedEffect(Unit) {
        viewModel.getBasePlans(subscriptionId)
    }

    uiState.error?.let {
        onError(it)
    }

    uiState.purchase?.let {
        if(it.status == HSPurchase.Status.Purchased) {
            onPurchase(type)
        }
    }

    val selectedItemIndex = remember { mutableIntStateOf(0) }
    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.prem_star_yellow_16),
        iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
        title = stringResource(type.titleResId),
        onCloseClick = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                uiState.basePlans.forEachIndexed() { index, basePlan ->
                    SubscriptionOption(
                        title = basePlan.id,
                        price = basePlan.stringRepresentation(),
                        note = "",
                        isSelected = selectedItemIndex.intValue == index,
                        badgeText = null,
                        onClick = {
                            selectedItemIndex.intValue = index
                        }
                    )
                }
            }

            val bottomText = highlightText(
                text = stringResource(R.string.Premium_EnjoyFirst7DaysFree_Description),
                textColor = ComposeAppTheme.colors.leah,
                highlightPart = stringResource(R.string.Premium_EnjoyFirst7DaysFree),
                highlightColor = ComposeAppTheme.colors.remus
            )
            VSpacer(12.dp)
            Text(
                text = bottomText,
                color = ComposeAppTheme.colors.grey,
                style = ComposeAppTheme.typography.subhead2,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp)
            )
            VSpacer(24.dp)
            ButtonPrimaryYellow(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.Premium_Get7DaysFree),
                onClick = {
                    activity?.let {
                        viewModel.launchPurchaseFlow(
                            subscriptionId = subscriptionId,
                            planId = uiState.basePlans[selectedItemIndex.value].id,
                            activity = it
                        )
                    }
                }
            )
            VSpacer(12.dp)
            ButtonPrimaryTransparent(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                title = stringResource(R.string.Premium_AddPromoCode),
                onClick = {
                    //TODO
                }
            )
            VSpacer(36.dp)
        }
    }
}

@Composable
fun SubscriptionOption(
    title: String,
    price: String,
    note: String,
    isSelected: Boolean,
    badgeText: String?,
    onClick: () -> Unit
) {
    val borderColor =
        if (isSelected) ComposeAppTheme.colors.jacob else ComposeAppTheme.colors.steel20

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                headline1_leah(title)
                if (badgeText != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                ComposeAppTheme.colors.remus,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = ComposeAppTheme.colors.claude,
                            style = ComposeAppTheme.typography.microSB,
                        )
                    }
                }
            }

            Row() {
                subhead2_jacob(price)
                if (note.isNotEmpty()) {
                    HSpacer(4.dp)
                    subhead2_remus(note)
                }
            }
        }
    }
}

@Composable
fun PlanItems(
    items: List<IPaidAction>,
    onItemClick: (IPaidAction) -> Unit
) {
    Column {
        items.forEachIndexed { index, item ->
            PremiumFeatureItem(
                icon = item.iconRes,
                title = item.titleStringRes,
                subtitle = item.descriptionStringRes,
                tint = if (item is VIPClub || item is VIPSupport) ComposeAppTheme.colors.jacob else ComposeAppTheme.colors.leah,
                click = { onItemClick(item) }
            )
            if (index < items.size - 1) {
                Divider(color = ComposeAppTheme.colors.steel20)
            }
        }
    }
}

@Composable
fun PremiumFeatureItem(
    icon: Int,
    title: Int,
    subtitle: Int,
    tint: Color = ComposeAppTheme.colors.leah,
    click: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { click() }
            .background(ComposeAppTheme.colors.steel10)
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            modifier = Modifier.size(24.dp),
            tint = tint,
            contentDescription = null
        )
        HSpacer(16.dp)
        Column {
            subhead1_leah(stringResource(title))
            caption_grey(stringResource(subtitle))
        }
    }
}

@Composable
fun highlightText(
    text: String,
    textColor: Color,
    highlightPart: String,
    highlightColor: Color
): AnnotatedString {

    return buildAnnotatedString {
        withStyle(SpanStyle(color = textColor)) {
            val highlightIndex = text
                .lowercase()
                .indexOf(highlightPart.lowercase())

            if (highlightIndex != -1) {
                append(text.substring(0, highlightIndex))

                withStyle(
                    SpanStyle(color = highlightColor)
                ) {
                    append(
                        text.substring(
                            highlightIndex,
                            highlightIndex + highlightPart.length
                        )
                    )
                }

                append(
                    text.substring(highlightIndex + highlightPart.length)
                )
            } else {
                append(text)
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ButtonPrimaryCustomColor(
    modifier: Modifier = Modifier,
    title: String,
    brush: Brush,
    onClick: () -> Unit,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonPrimaryDefaults.ContentPadding,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(25.dp))
            .background(brush),
        shape = RoundedCornerShape(25.dp),
        color = Color.Transparent,
        contentColor = ComposeAppTheme.colors.dark,
        onClick = onClick,
        enabled = enabled,
    ) {
        ProvideTextStyle(
            value = ComposeAppTheme.typography.headline2
        ) {
            Row(
                Modifier
                    .defaultMinSize(
                        minWidth = ButtonPrimaryDefaults.MinWidth,
                        minHeight = ButtonPrimaryDefaults.MinHeight
                    )
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun TitleCenteredTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onCloseClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .fillMaxWidth(),
    ) {
        headline1_leah(
            text = title,
            modifier = Modifier.align(Alignment.Center)
        )
        HsIconButton(
            modifier = Modifier.align(Alignment.CenterEnd),
            onClick = onCloseClick
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = "close button",
                tint = ComposeAppTheme.colors.jacob,
            )
        }
    }
}

@Composable
fun ColoredTextSecondaryButton(
    title: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ButtonSecondary(
        modifier = modifier,
        onClick = onClick,
        buttonColors = buttonColors(
            backgroundColor = ComposeAppTheme.colors.transparent,
            contentColor = color,
            disabledBackgroundColor = ComposeAppTheme.colors.transparent,
            disabledContentColor = ComposeAppTheme.colors.grey50,
        ),
        content = {
            Text(
                text = title,
                maxLines = 1,
                style = ComposeAppTheme.typography.body,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@Composable
fun SubscriptionTabs(
    subscriptions: List<Subscription>,
    selectedTabIndex: MutableState<Int>,
    modifier: Modifier,
    onTabSelected: (Int) -> Unit = {}
) {
    if (subscriptions.isNotEmpty()) {
        TabRow(
            selectedTabIndex = selectedTabIndex.value,
            modifier = modifier,
            backgroundColor = ComposeAppTheme.colors.transparent, // Dark background
            contentColor = Color(0xFFEDD716),
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier
                        .tabIndicatorOffset(tabPositions[selectedTabIndex.value])
                        .height(0.dp), // No indicator line
                    color = Color.Transparent
                )
            }
        ) {
            subscriptions.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTabIndex.value == index,
                    onClick = {
                        selectedTabIndex.value = index
                        onTabSelected(index)
                    },
                    modifier = Modifier.background(
                        brush =
                        if (selectedTabIndex.value == index) yellowGradient else steelBrush,
                    ),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .height(44.dp)
                            .padding(vertical = 8.dp, horizontal = 16.dp)
                    ) {
                        Icon(
                            painter = painterResource(if (index == 0) R.drawable.prem_star_yellow_16 else R.drawable.prem_crown_yellow_16),
                            contentDescription = null,
                            tint = if (selectedTabIndex.value == index) ComposeAppTheme.colors.dark else ComposeAppTheme.colors.jacob,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tab.name,
                            color = if (selectedTabIndex.value == index) ComposeAppTheme.colors.dark else ComposeAppTheme.colors.grey,
                            style = ComposeAppTheme.typography.captionSB
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoBottomSheet(
    icon: Int,
    title: String,
    description: String,
    hideBottomSheet: () -> Unit,
    bottomSheetState: SheetState
) {
    ModalBottomSheet(
        onDismissRequest = hideBottomSheet,
        sheetState = bottomSheetState,
        containerColor = ComposeAppTheme.colors.transparent
    ) {
        BottomSheetHeader(
            iconPainter = painterResource(icon),
            title = title,
            titleColor = ComposeAppTheme.colors.jacob,
            iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
            onCloseClick = hideBottomSheet
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = 12.dp, horizontal = 32.dp)
                    .fillMaxWidth()
            ) {
                body_bran(
                    text = description,
                    modifier = Modifier.fillMaxWidth(),
                )
                VSpacer(52.dp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionBottomSheet(
    modalBottomSheetState: SheetState,
    subscriptions: List<Subscription>,
    selectedTabIndex: MutableState<Int>,
    navController: NavController,
    hideBottomSheet: () -> Unit,
    onError: (Throwable) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = hideBottomSheet,
        sheetState = modalBottomSheetState,
        containerColor = ComposeAppTheme.colors.transparent
    ) {
        if (subscriptions.isNotEmpty()) {
            SelectSubscriptionBottomSheet(
                subscriptionId = subscriptions[selectedTabIndex.value].id,
                type = PremiumPlanType.entries[selectedTabIndex.value],
                onDismiss = hideBottomSheet,
                onPurchase = { type ->
                    hideBottomSheet()
                    navController.navigate("premium_subscribed_page?type=${type.name}")
                },
                onError = onError
            )
        }
    }
}