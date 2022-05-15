package io.horizontalsystems.bankwallet.modules.settings.appstatus

import androidx.lifecycle.ViewModel
import io.horizontalsystems.core.helpers.DateHelper
import java.util.*

class AppStatusViewModel(
    service: AppStatusService
) : ViewModel() {

    val appStatus = formatMapToString(service.status)

    @Suppress("UNCHECKED_CAST")
    private fun formatMapToString(
        status: Map<String, Any>?,
        indentation: String = "",
        bullet: String = "",
        level: Int = 0
    ): String? {
        if (status == null)
            return null

        val sb = StringBuilder()
        status.toList().forEach { (key, value) ->
            val title = "$indentation$bullet$key"
            when (value) {
                is Date -> {
                    val date = DateHelper.formatDate(value, "MMM d, yyyy, HH:mm")
                    sb.appendLine("$title: $date")
                }
                is Map<*, *> -> {
                    val formattedValue = formatMapToString(
                        value as? Map<String, Any>,
                        "\t\t$indentation",
                        " - ",
                        level + 1
                    )
                    sb.append("$title:\n$formattedValue${if (level < 2) "\n" else ""}")
                }
                else -> {
                    sb.appendLine("$title: $value")
                }
            }
        }

        val statusString = sb.trimEnd()

        return if (statusString.isEmpty()) "" else "$statusString\n"
    }

}
