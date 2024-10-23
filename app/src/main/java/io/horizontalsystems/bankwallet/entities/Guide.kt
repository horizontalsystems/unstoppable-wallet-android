package io.horizontalsystems.bankwallet.entities

data class Guide(
    val title: String,
    val markdown: String,
)

data class GuideCategory(
    val category: String,
    val sections: List<GuideSection>
)

data class GuideCategoryMultiLang(
    val category: Map<String, String>,
    val sections: List<GuideSectionMultiLang>
)

data class GuideSection(
    val title: String,
    val items: List<Guide>
)

data class GuideSectionMultiLang(
    val title: Map<String, String>,
    val items: List<Map<String, Guide>>
)
