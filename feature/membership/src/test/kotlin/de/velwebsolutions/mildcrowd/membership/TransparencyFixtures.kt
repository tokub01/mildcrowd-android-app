package de.velwebsolutions.mildcrowd.membership

import de.velwebsolutions.mildcrowd.core.network.DataEnvelope
import de.velwebsolutions.mildcrowd.core.network.MildcrowdJson
import de.velwebsolutions.mildcrowd.membership.api.FinanceReport
import de.velwebsolutions.mildcrowd.membership.api.Transparency

/** „Wohin dein Beitrag geht“ as `GET transparency` answers it (docs/mildcrowd-admin.md). */
object TransparencyFixtures {
    fun reportJson(id: Int, label: String, balance: Double = 157.44) = """
        {"id":$id,"period_type":"month","period_start":"2026-11-01","period_end":"2026-11-30","period_label":"$label",
        "income":1234.56,"costs":[{"key":"server","label":"Server & Technik","amount":400.0},
        {"key":"moderation","label":"Moderation","amount":600.0},{"key":"development","label":"Entwicklung","amount":77.12},
        {"key":"reserves","label":"Rücklagen","amount":0.0},{"key":"other","label":"Sonstiges","amount":0.0}],
        "costs_total":1077.12,"balance":$balance,"note":"Der Server wurde größer.","published_at":"2026-12-02T10:00:00Z"}
    """.trimIndent()

    fun pageJson(report: String? = reportJson(4, "November 2026")) = """
        {"data":{"operator":"Tobias Kubina - VelWebSolutions","figures":{"active_members":1234,"crowds":17,"ideas_done":3},
        "report":${report ?: "null"},"earlier":[{"id":3,"period_label":"Oktober 2026"}]}}
    """.trimIndent()

    fun page(json: String = pageJson()): Transparency =
        MildcrowdJson.snake.decodeFromString(DataEnvelope.serializer(Transparency.serializer()), json).data

    fun report(id: Int, label: String, balance: Double = 157.44): FinanceReport =
        MildcrowdJson.snake.decodeFromString(FinanceReport.serializer(), reportJson(id, label, balance))
}
