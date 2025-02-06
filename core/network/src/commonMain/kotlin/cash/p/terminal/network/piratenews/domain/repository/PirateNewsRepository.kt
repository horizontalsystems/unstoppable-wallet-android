package cash.p.terminal.network.piratenews.domain.repository

import cash.p.terminal.network.piratenews.domain.entity.PiratePostItem

interface PirateNewsRepository {
    suspend fun getNews(): List<PiratePostItem>
}