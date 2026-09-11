package dev.bennyb.daysay.trigger

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dev.bennyb.daysay.core.Dictation
import dev.bennyb.daysay.core.DictationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Quick settings tile: a second trigger for devices or builds without a usable button. */
class DictationTileService : TileService() {
    private var scope: CoroutineScope? = null
    private var job: Job? = null

    override fun onStartListening() {
        val s = CoroutineScope(SupervisorJob() + Dispatchers.Main).also { scope = it }
        job = s.launch { Dictation.state.collect { render(it) } }
    }

    override fun onStopListening() {
        job?.cancel()
        scope?.cancel()
        scope = null
    }

    override fun onClick() {
        Dictation.toggle()
    }

    private fun render(state: DictationState) {
        val tile = qsTile ?: return
        tile.state = when (state) {
            is DictationState.Listening, DictationState.Transcribing, DictationState.Cleaning -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        tile.subtitle = when (state) {
            is DictationState.Listening -> "Listening"
            DictationState.Transcribing -> "Transcribing"
            DictationState.Cleaning -> "Cleaning up"
            else -> null
        }
        tile.updateTile()
    }
}
