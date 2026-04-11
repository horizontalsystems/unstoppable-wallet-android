package cash.p.terminal.modules.premium.settings

import cash.p.terminal.R

enum class PollingInterval(val minutes: Long, val titleResId: Int) {
    REALTIME(0, R.string.push_notification_interval_realtime),
    MIN_5(5, R.string.push_notification_interval_5min),
    MIN_10(10, R.string.push_notification_interval_10min),
    MIN_15(15, R.string.push_notification_interval_15min);
}
