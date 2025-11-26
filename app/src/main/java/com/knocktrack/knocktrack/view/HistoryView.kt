package com.knocktrack.knocktrack.view

import com.knocktrack.knocktrack.model.DoorbellEvent

/**
 * Contract for the History screen View in MVP.
 * Implemented by `HistoryActivity` and consumed by `HistoryPresenter`.
 */
interface HistoryView {
    fun showDoorbellEvents(events: List<DoorbellEvent>)
    fun showLoading(show: Boolean)
    fun showEmptyState(show: Boolean)
    fun showError(message: String)
    fun onHistoryCleared()
    fun navigateToHome()
    fun navigateToSettings()
}
