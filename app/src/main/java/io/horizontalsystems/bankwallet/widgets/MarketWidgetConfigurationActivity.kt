package io.horizontalsystems.bankwallet.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MarketWidgetConfigurationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val context = applicationContext
        val manufacturer = "xiaomi"

        val manager = GlanceAppWidgetManager(context)
        val currentGlanceId = manager.getGlanceIdBy(intent)

        setContent {
            var selectedType by remember { mutableStateOf<MarketWidgetType?>(null) }

            ComposeAppTheme {
                HSScaffold(
                    title = stringResource(R.string.WidgetList_Config_Title),
                    menuItems = listOf(
                        MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = { finish() }
                    ))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        VSpacer(12.dp)
                        CellSingleLineLawrenceSection(MarketWidgetManager.getMarketWidgetTypes()) { type ->
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
                        if (manufacturer.equals(Build.MANUFACTURER, ignoreCase = true)) {
                            VSpacer(24.dp)
                            TextImportantWarning(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                text = stringResource(R.string.Widget_EnableAutostartWarning)
                            )
                        }
                        VSpacer(24.dp)
                    }
                }
            }
        }
    }

    private fun finishActivity(
        selectedType: MarketWidgetType,
        appWidgetId: Int,
        glanceId: GlanceId?,
        context: Context
    ) {
        val scope = MainScope()
        scope.launch {
            glanceId?.let {
                updateAppWidgetState(context, MarketWidgetStateDefinition, glanceId) {
                    it.copy(widgetId = appWidgetId, type = selectedType)
                }
                MarketWidget().update(context, glanceId)
                MarketWidgetManager().refresh(glanceId)
                MarketWidgetWorker.enqueueWork(App.instance)
            }

            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(Activity.RESULT_OK, resultValue)
            finish()
        }
    }

}
