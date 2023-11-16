package cash.p.terminal.entities

import cash.p.terminal.R

enum class BtcRestoreMode(val raw: String) {
    Blockchair("blockchair"),
    Hybrid("hybrid"),
    Blockchain("blockchain");

    val title: Int
        get() = when (this) {
            Blockchair -> R.string.SettingsSecurity_SyncModeBlockchair
            Hybrid -> R.string.SettingsSecurity_SyncModeHybrid
            Blockchain -> R.string.SettingsSecurity_SyncModeBlockchain
        }

    val description: Int
        get() = when (this) {
            Blockchair -> R.string.SettingsSecurity_SyncModeBlockchairDescription
            Hybrid -> R.string.SettingsSecurity_SyncModeHybridDescription
            Blockchain -> R.string.SettingsSecurity_SyncModeBlockchainDescription
        }

}
