package net.veldor.flibusta_test.model.selections

class RestoreProgress {
    var state: Int = STATE_AWAITING
    var basePreferencesRestoreState = STATE_AWAITING
    var downloadedBooksRestoreState = STATE_AWAITING
    var readBooksRestoreState = STATE_AWAITING
    var searchAutofillRestoreState = STATE_AWAITING
    var bookmarksListRestoreState = STATE_AWAITING
    var subscribesRestoreState = STATE_AWAITING
    var filtersRestoreState = STATE_AWAITING
    var downloadScheduleRestoreState = STATE_AWAITING

    companion object{
        const val STATE_AWAITING = 0
        const val STATE_IN_PROGRESS = 1
        const val STATE_FINISHED = 2
        const val STATE_SKIPPED = 3
    }
}
