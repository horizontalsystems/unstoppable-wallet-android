package io.horizontalsystems.marketkit.models

import java.util.*

data class CoinReport(
    val author: String,
    val title: String,
    val body: String,
    val date: Date,
    val url: String
)
