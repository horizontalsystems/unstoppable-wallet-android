package io.horizontalsystems.marketkit.providers

import io.horizontalsystems.marketkit.models.AuditReport
import io.horizontalsystems.marketkit.models.Auditor
import io.horizontalsystems.marketkit.providers.DefiYieldService.*
import io.reactivex.Single
import java.text.SimpleDateFormat
import java.util.*

class DefiYieldProvider(
    apiKey: String?
) {
    private val baseUrl = "https://api.safe.defiyield.app"
    private val authBearerToken = apiKey?.let { "Bearer $it" }
    private val fileBaseUrl = "https://files.safe.defiyield.app/"

    private val defiYieldService: DefiYieldService by lazy {
        RetrofitUtils.build(baseUrl).create(DefiYieldService::class.java)
    }

    fun auditReportsSingle(addresses: List<String>): Single<List<Auditor>> {
        if (addresses.isEmpty()) {
            return Single.just(listOf())
        }

        return defiYieldService.auditInfo(authBearerToken, Addresses(addresses))
            .map { auditInfos ->
                val auditors = mutableListOf<Auditor>()

                auditInfos.firstOrNull()?.let { info ->
                    val partners = mutableMapOf<Int, Partner>()
                    val audits = mutableMapOf<Int, MutableList<PartnerAudit>>()

                    info.partnerAudits.forEach { audit ->
                        audit.partner?.let{ partner ->
                            val partnerId = partner.id
                            partners[partnerId] = partner
                            val partnerAudits = audits[partnerId] ?: mutableListOf()
                            partnerAudits.add(audit)
                            audits[partnerId] = partnerAudits
                        }
                    }

                    partners.mapNotNull { (id, partner) ->
                        val partnerAudits = audits[id] ?: return@mapNotNull null

                        val reports = partnerAudits.map { audit ->
                            val date = try {
                                SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(audit.date)
                            } catch (exception: Exception) {
                                null
                            }
                            AuditReport(
                                name = audit.name,
                                date = date,
                                issues = audit.techIssues ?: 0,
                                link = audit.auditLink?.let { "$fileBaseUrl$it" }
                            )
                        }
                        auditors.add(Auditor(partner.name, reports))
                    }
                }
                auditors
            }
    }
}
