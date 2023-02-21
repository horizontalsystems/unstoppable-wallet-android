package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.R

enum class TransactionDataSortMode(val raw: String) {
    Shuffle("shuffle"),
    Bip69("bip69");

    val title: Int
        get() = when (this) {
            Shuffle -> R.string.SettingsSecurity_SortingShuffle
            Bip69 -> R.string.SettingsSecurity_SortingBip69
        }

    val titleShort: Int
        get() = when (this) {
            Shuffle -> R.string.SettingsSecurity_SortingShuffle
            Bip69 -> R.string.SettingsSecurity_SortingBip69Short
        }

    val description: Int
        get() = when (this) {
            Shuffle -> R.string.SettingsSecurity_SortingShuffleDescription
            Bip69 -> R.string.SettingsSecurity_SortingBip69Description
        }

}
