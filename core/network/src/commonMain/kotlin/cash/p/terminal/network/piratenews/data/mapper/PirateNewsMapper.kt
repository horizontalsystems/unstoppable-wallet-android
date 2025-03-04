package cash.p.terminal.network.piratenews.data.mapper

import cash.p.terminal.network.piratenews.data.entity.PiratePostItemDto
import cash.p.terminal.network.piratenews.domain.entity.PiratePostItem
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

internal class PirateNewsMapper {

    fun mapNews(pirateNewsDto: List<PiratePostItemDto>): List<PiratePostItem> =
        pirateNewsDto.mapNotNull(::mapPiratePostItemDto)

    private fun mapPiratePostItemDto(piratePostItemDto: PiratePostItemDto): PiratePostItem? {
        val date = try {
            LocalDateTime.parse(piratePostItemDto.date, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .toInstant(
                    ZoneOffset.UTC
                )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        if (date == null) {
            return null
        }
        return PiratePostItem(
            id = piratePostItemDto.id,
            date = date.toEpochMilli()/1000,
            title = piratePostItemDto.title.rendered,
            link = piratePostItemDto.link,
            body = piratePostItemDto.excerpt.rendered
        )
    }
}