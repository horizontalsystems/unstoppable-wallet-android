package io.horizontalsystems.walletkit.ui.compose.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme

/**
 * Reachability of a single node/server, as measured by a chain's node ping.
 *
 * Latency thresholds are chain-specific — a gRPC-over-TLS round trip is not comparable to an HTTP
 * JSON-RPC one — so each chain's view model classifies its own [Level] before reaching the badge.
 */
sealed interface PingState {
    object Loading : PingState
    object Unreachable : PingState
    data class Reachable(val responseTimeMs: Int, val level: Level) : PingState

    enum class Level { Good, Medium, Slow }
}

@Composable
fun PingBadge(ping: PingState) {
    when (ping) {
        PingState.Loading -> {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = ComposeAppTheme.colors.grey,
                strokeWidth = 2.dp
            )
        }

        PingState.Unreachable -> {
            caption_lucian(text = stringResource(R.string.NetworkSettings_Unreachable))
        }

        is PingState.Reachable -> {
            val text = stringResource(R.string.NetworkSettings_Latency, ping.responseTimeMs)
            when (ping.level) {
                PingState.Level.Good -> caption_remus(text = text)
                PingState.Level.Medium -> caption_jacob(text = text)
                PingState.Level.Slow -> caption_grey(text = text)
            }
        }
    }
}
