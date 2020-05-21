package io.horizontalsystems.bankwallet.modules.guides

import io.horizontalsystems.core.SingleLiveEvent


class GuidesView : GuidesModule.View {

    val openGuide = SingleLiveEvent<GuideItem>()

    override fun open(item: GuideItem) {
        openGuide.postValue(item)
    }
}