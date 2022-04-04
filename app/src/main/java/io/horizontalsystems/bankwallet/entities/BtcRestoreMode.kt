package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.R

enum class BtcRestoreMode(val raw: String) {
    Api("api"),
    Blockchain("blockchain");

    val title: Int
        get() = when (this) {
            Api -> R.string.SettingsSecurity_SyncModeAPI
            Blockchain -> R.string.SettingsSecurity_SyncModeBlockchain
        }

    val description: Int
        get() = when (this) {
            Api -> R.string.SettingsSecurity_SyncModeAPIDescription
            Blockchain -> R.string.SettingsSecurity_SyncModeBlockchainDescription
        }

}
