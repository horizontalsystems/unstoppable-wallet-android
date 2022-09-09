package io.horizontalsystems.bankwallet.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.core.CoreActivity
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MarketWidgetConfigurationActivity : CoreActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val context = applicationContext

        setContent {
            var selectedType by remember { mutableStateOf<MarketWidgetType?>(null) }
            var currentGlanceId by remember { mutableStateOf<GlanceId?>(null) }

            LaunchedEffect(Unit) {
                val manager = GlanceAppWidgetManager(context)
                for (glanceId in manager.getGlanceIds(MarketWidget::class.java)) {
                    val state = getAppWidgetState(context, MarketWidgetStateDefinition, glanceId)
                    if (state.widgetId == 0 || state.widgetId == appWidgetId) {
                        selectedType = if (state.widgetId != 0) state.type else null
                        currentGlanceId = glanceId
                        break
                    }
                }
            }

            ComposeAppTheme {
                Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
                    AppBar(
                        title = TranslatableString.ResString(R.string.WidgetList_Config_Title),
                        navigationIcon = null,
                        menuItems = listOf(MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Close),
                            icon = R.drawable.ic_close,
                            onClick = { finish() }
                        ))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CellSingleLineLawrenceSection(MarketWidgetType.values().toList()) { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(onClick = {
                                    selectedType = type
                                    finishActivity(type, appWidgetId, currentGlanceId, context)
                                })
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            body_leah(
                                text = stringResource(type.title),
                                modifier = Modifier.weight(1f)
                            )
                            if (selectedType == type) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_checkmark_20),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(ComposeAppTheme.colors.jacob)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun finishActivity(selectedType: MarketWidgetType, appWidgetId: Int, glanceId: GlanceId?, context: Context) {
        val scope = MainScope()
        scope.launch {
            glanceId?.let {
                updateAppWidgetState(context, MarketWidgetStateDefinition, glanceId) {
                    it.copy(widgetId = appWidgetId, type = selectedType)
                }
            }

            MarketWidgetWorker.enqueue(context = context, widgetId = appWidgetId)

            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(Activity.RESULT_OK, resultValue)
            finish()
        }
    }

}
