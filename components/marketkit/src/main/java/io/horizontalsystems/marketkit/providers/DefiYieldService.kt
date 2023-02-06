package io.horizontalsystems.marketkit.providers

import com.google.gson.annotations.SerializedName
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface DefiYieldService {

    @Headers("Content-Type: application/json", "Accept: application/json")
    @POST("audit/address")
    fun auditInfo(
        @Header("Authorization") auth: String?,
        @Body body: Addresses
    ): Single<List<AuditInfo>>

    data class Addresses(val addresses: List<String>)

    data class AuditInfo(
        val partnerAudits: List<PartnerAudit>
    )

    data class PartnerAudit(
        val name: String,
        val date: String,
        @SerializedName("tech_issues")
        val techIssues: Int?,
        @SerializedName("audit_link")
        val auditLink: String?,
        val partner: Partner?
    )

    data class Partner(
        val id: Int,
        val name: String
    )
}
