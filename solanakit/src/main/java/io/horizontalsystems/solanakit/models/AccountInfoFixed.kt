package io.horizontalsystems.solanakit.models

import kotlinx.serialization.Serializable

/***
 * The main difference with the original code is rentEpoch: String
 * The original code has rentEpoch: Long and we got overflows when converting to Long
 */
@Serializable
data class AccountInfoFixed<D>(
    val data: D?,
    val executable: Boolean,
    val lamports: Long,
    val owner: String?
)
