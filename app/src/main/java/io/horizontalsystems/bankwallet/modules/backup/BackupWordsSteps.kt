package io.horizontalsystems.bankwallet.modules.backup

class BackupWordsSteps {
    private val pagesCount: Int = 3
    var currentPage: Int = 0

    fun canLoadNextPage(): Boolean {
        if (currentPage + 1 < pagesCount) {
            currentPage++
            return true
        }
        return false
    }

    fun canLoadPrevPage(): Boolean {
        if (currentPage > 0) {
            currentPage--
            return true
        }
        return false
    }
}
