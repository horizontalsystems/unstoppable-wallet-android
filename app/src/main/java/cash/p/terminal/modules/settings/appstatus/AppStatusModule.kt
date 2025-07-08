package cash.p.terminal.modules.settings.appstatus

object AppStatusModule {
    sealed class BlockContent {
        data class Header(val title: String) : BlockContent()
        data class Text(val text: String) : BlockContent()
        data class TitleValue(val title: String, val value: String) : BlockContent()
    }

    data class BlockData(val title: String?, val content: List<BlockContent>)

    data class UiState(
        val appStatusAsText: String?,
        val blockViewItems: List<BlockData>,
    )
}
