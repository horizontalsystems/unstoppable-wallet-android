package io.horizontalsystems.marketkit.models

import java.util.*

data class AuditReport(
    val name: String,
    val date: Date?,
    val issues: Int,
    val link: String?
)
