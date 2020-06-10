package io.horizontalsystems.bankwallet.modules.guideview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Guide

class GuideViewModel(private val guide: Guide?) : ViewModel() {

    val guideLiveData = MutableLiveData<Guide>(guide)

}
