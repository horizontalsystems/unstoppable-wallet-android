package io.horizontalsystems.bankwallet.widgets

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.*
import androidx.glance.layout.Alignment.Vertical.Companion.CenterVertically
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.market.Value
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

class MarketWidget : GlanceAppWidget() {

    companion object {
        private val smallMode = DpSize(140.dp, 120.dp)
        private val mediumMode = DpSize(220.dp, 200.dp)
        private val largeMode = DpSize(260.dp, 280.dp)
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(smallMode, mediumMode, largeMode)
    )

    override val stateDefinition = MarketWidgetStateDefinition

    @Composable
    override fun Content() {
        val state = currentState<MarketWidgetState>()
        val context = LocalContext.current

        AppWidgetTheme {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ImageProvider(R.drawable.widget_background))
                    .padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .background(ImageProvider(R.drawable.widget_list_background))
                ) {
                    Row(
                        modifier = GlanceModifier
                            .height(44.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = GlanceModifier.defaultWeight().padding(start = 16.dp),
                            text = context.getString(state.type.title),
                            style = AppWidgetTheme.textStyles.d1()
                        )
                        /*
                        Image(
                            modifier = GlanceModifier
                                .size(20.dp)
                                .clickable(
                                    actionStartActivity<MarketWidgetConfigurationActivity>(
                                        actionParametersOf(
                                            ActionParameters.Key<Int>(AppWidgetManager.EXTRA_APPWIDGET_ID) to state.widgetId
                                        )
                                    )
                                ),
                            provider = ImageProvider(R.drawable.ic_edit_20),
                            contentDescription = null
                        )
                        Spacer(modifier = GlanceModifier.width(16.dp))
                        */
                        Box(
                            modifier = GlanceModifier
                                .fillMaxHeight()
                                .padding(horizontal = 16.dp)
                                .clickable(actionRunCallback<UpdateMarketAction>()),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.loading) {
                                CircularProgressIndicator(modifier = GlanceModifier.size(20.dp))
                            } else {
                                Image(
                                    modifier = GlanceModifier.size(20.dp),
                                    provider = ImageProvider(R.drawable.ic_refresh),
                                    contentDescription = null
                                )
                            }
                        }
                    }

                    if (!state.loading && state.type == MarketWidgetType.Watchlist && state.items.isEmpty()) {
                        FullScreenMessage(
                            icon = R.drawable.ic_rate_24,
                            text = context.getString(R.string.Market_Tab_Watchlist_EmptyList),
                        )
                    } else if (state.error != null) {
                        FullScreenMessage(
                            icon = R.drawable.ic_sync_error,
                            text = state.error,
                        )
                    } else {
                        state.items.forEach { item ->
                            Box(
                                modifier = GlanceModifier
                                    .height(60.dp)
                                    .background(ImageProvider(R.drawable.widget_list_item_background))
                                    .clickable(
                                        actionStartActivity(Intent(Intent.ACTION_VIEW, getDeeplinkUri(item.uid, item.title, state.type)))
                                    )
                            ) {
                                Item(item = item)
                            }
                        }
                    }
                }
                Column {
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Text(
                        text = "Updated: " + SimpleDateFormat("HH:mm:ss, dd-MM-yyyy", Locale.US).format(Date(state.updateTimestampMillis)),
                        style = AppWidgetTheme.textStyles.micro()
                    )
                }
            }
        }
    }

    private fun getDeeplinkUri(itemUid: String, title: String, type: MarketWidgetType): Uri = when (type) {
        MarketWidgetType.Watchlist,
        MarketWidgetType.TopGainers -> {
            "unstoppable://coin-page?uid=${itemUid}".toUri()
        }
        MarketWidgetType.TopNfts -> {
            "unstoppable://nft-collection?uid=${itemUid}".toUri()
        }
        MarketWidgetType.TopPlatforms -> {
            "unstoppable://top-platforms?uid=${itemUid}&title=${title}".toUri()
        }
    }

    @Composable
    private fun Item(item: MarketWidgetItem) {
        Row(
            modifier = GlanceModifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp),
            verticalAlignment = CenterVertically
        ) {
            Image(
                provider = imageProvider(item.imageLocalPath),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = GlanceModifier.size(24.dp)
            )
            Spacer(modifier = GlanceModifier.width(16.dp))
            Column {
                ItemFirstRow(title = item.title, value = item.value)
                Spacer(modifier = GlanceModifier.height(3.dp))
                ItemSecondRow(
                    subtitle = item.subtitle,
                    label = item.label,
                    diff = item.diff,
                    marketCap = item.marketCap,
                    volume = item.volume
                )
            }
        }
    }

    private fun imageProvider(path: String?) = if (path == null) {
        ImageProvider(R.drawable.coin_placeholder)
    } else {
        ImageProvider(BitmapFactory.decodeFile(path))
    }

    @Composable
    private fun ItemFirstRow(title: String, value: String?) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = CenterVertically
        ) {

            Text(
                text = if (title.length > 20) title.take(17) + "..." else title,
                maxLines = 1,
                style = TextStyle(AppWidgetTheme.colors.leah, fontSize = 16.sp)
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = value ?: "",
                maxLines = 1,
                style = TextStyle(color = AppWidgetTheme.colors.leah, fontSize = 16.sp)
            )
        }
    }

    @Composable
    private fun ItemSecondRow(
        subtitle: String,
        label: String?,
        diff: BigDecimal?,
        marketCap: String?,
        volume: String?
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = CenterVertically
        ) {
            label?.let {
                Badge(text = it)
                Spacer(modifier = GlanceModifier.width(8.dp))
            }
            Text(
                text = subtitle,
                maxLines = 1,
                style = AppWidgetTheme.textStyles.d1()
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            MarketDataValueComponent(diff, marketCap, volume)
        }
    }

    @Composable
    private fun diffColor(value: BigDecimal) =
        if (value.signum() >= 0) {
            AppWidgetTheme.colors.remus
        } else {
            AppWidgetTheme.colors.lucian
        }

    @Composable
    private fun MarketDataValueComponent(
        diff: BigDecimal?,
        marketCap: String?,
        volume: String?
    ) {

        when {
            diff != null -> {
                Text(
                    text = App.numberFormatter.formatValueAsDiff(Value.Percent(diff)),
                    style = TextStyle(color = diffColor(diff), fontSize = 14.sp, fontWeight = FontWeight.Normal),
                    maxLines = 1
                )
            }
            marketCap != null -> {
                Row {
                    Text(
                        text = "MCap",
                        style = AppWidgetTheme.textStyles.c3(),
                        maxLines = 1
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = marketCap,
                        style = AppWidgetTheme.textStyles.d1(),
                        maxLines = 1
                    )
                }
            }
            volume != null -> {
                Row {
                    Text(
                        text = "Vol",
                        style = AppWidgetTheme.textStyles.d3(),
                        maxLines = 1
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = volume,
                        style = AppWidgetTheme.textStyles.d1(),
                        maxLines = 1
                    )
                }
            }
        }
    }

    @Composable
    private fun Badge(text: String) {
        Text(
            modifier = GlanceModifier
                .background(ImageProvider(R.drawable.widget_list_item_badge_background))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            text = text,
            style = TextStyle(color = AppWidgetTheme.colors.bran, fontSize = 10.sp, fontWeight = FontWeight.Medium),
        )
    }

    @Composable
    private fun FullScreenMessage(icon: Int, text: String) {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .size(100.dp)
                    .background(ImageProvider(R.drawable.widget_screen_message_icon_background)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    modifier = GlanceModifier.size(48.dp),
                    provider = ImageProvider(icon),
                    contentDescription = null,
                )
            }
            Spacer(modifier = GlanceModifier.height(32.dp))
            Text(
                text = text,
                style = AppWidgetTheme.textStyles.d1(textAlign = TextAlign.Center)
            )
            Spacer(modifier = GlanceModifier.height(32.dp))
        }
    }

}

class UpdateMarketAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, MarketWidgetStateDefinition, glanceId) { state ->
            state.copy(loading = true)
        }
        MarketWidget().update(context, glanceId)

        val state = getAppWidgetState(context, MarketWidgetStateDefinition, glanceId)
        MarketWidgetWorker.enqueue(context = context, widgetId = state.widgetId)
    }
}
