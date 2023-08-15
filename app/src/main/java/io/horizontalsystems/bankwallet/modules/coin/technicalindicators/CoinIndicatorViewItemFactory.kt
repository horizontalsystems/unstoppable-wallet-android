package io.horizontalsystems.bankwallet.modules.coin.technicalindicators

import io.horizontalsystems.bankwallet.modules.coin.technicalindicators.TechnicalIndicatorsDetailsModule.DetailViewItem
import io.horizontalsystems.bankwallet.modules.coin.technicalindicators.TechnicalIndicatorsDetailsModule.SectionViewItem

class CoinIndicatorViewItemFactory {

    private fun advice(items: List<TechnicalIndicatorService.Item>): AdviceViewItem {
        val rating = items.map { it.advice.rating }.sum()
        val adviceCount = items.count { it.advice != Advice.NODATA }
        val variations = 2 * adviceCount + 1

        val baseDelta = variations / 5
        val remainder = variations % 5
        val neutralAddict = remainder % 3 // how much variations will be added to the neutral zone
        val sideAddict = remainder / 3 // how much will be added to the sell/buy zone

        val deltas = listOf(baseDelta, baseDelta + sideAddict, baseDelta + sideAddict + neutralAddict, baseDelta + sideAddict, baseDelta)

        var current = -adviceCount
        val ranges = deltas.map { delta ->
            val range = current until (current + delta)
            current += delta
            range
        }
        val index = ranges.indexOfFirst { it.contains(rating) }

        return if (index != -1) AdviceViewItem.values()[index] else AdviceViewItem.NEUTRAL
    }

    fun viewItems(items: List<TechnicalIndicatorService.SectionItem>): List<TechnicalIndicatorData> {
        val viewItems = mutableListOf<TechnicalIndicatorData>()

        val allAdviceItems = mutableListOf<TechnicalIndicatorService.Item>()
        for (item in items) {
            allAdviceItems.addAll(item.items)
            val adviceViewItem = advice(item.items)
            val blocks = getBlocks(adviceViewItem)
            viewItems.add(TechnicalIndicatorData(item.name, adviceViewItem, blocks))
        }

        if (viewItems.size > 0) {
            val adviceViewItem = advice(allAdviceItems)
            val blocks = getBlocks(adviceViewItem)
            val viewItem = TechnicalIndicatorData("Summary", adviceViewItem, blocks)
            viewItems.add(0, viewItem)
        }
        return viewItems
    }

    private fun getBlocks(adviceViewItem: AdviceViewItem): List<AdviceBlock> {
        val blocks = mutableListOf<AdviceBlock>()
        var filled = true
        for (item in AdviceViewItem.values()) {
            blocks.add(AdviceBlock(item, filled))
            if (item == adviceViewItem) {
                filled = false
            }
        }
        return blocks
    }

    fun detailViewItems(items: List<TechnicalIndicatorService.SectionItem>): List<SectionViewItem> {
        return items.map { item ->
            SectionViewItem(
                item.name,
                item.items.map {
                    DetailViewItem(it.name, it.advice)
                }
            )
        }
    }
}
