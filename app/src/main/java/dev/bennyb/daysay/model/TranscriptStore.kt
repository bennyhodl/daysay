package dev.bennyb.daysay.model

import android.content.Context
import dev.bennyb.daysay.core.Delivery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Transcript(
    val id: Long,
    val createdAt: Long,
    val text: String,
    val engine: String,
    val durationSeconds: Float,
    val delivery: Delivery,
)

/** Keeps the last transcripts on disk as one JSON file. Newest first. */
object TranscriptStore {
    private const val MAX_ENTRIES = 300
    private lateinit var file: File
    private val _items = MutableStateFlow<List<Transcript>>(emptyList())
    val items: StateFlow<List<Transcript>> get() = _items

    fun init(context: Context) {
        file = File(context.applicationContext.filesDir, "transcripts.json")
        _items.value = load()
    }

    @Synchronized
    fun add(text: String, engine: String, durationSeconds: Float, delivery: Delivery): Transcript {
        val entry = Transcript(
            id = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            text = text,
            engine = engine,
            durationSeconds = durationSeconds,
            delivery = delivery,
        )
        _items.value = (listOf(entry) + _items.value).take(MAX_ENTRIES)
        save()
        return entry
    }

    @Synchronized
    fun delete(id: Long) {
        _items.value = _items.value.filterNot { it.id == id }
        save()
    }

    @Synchronized
    fun clear() {
        _items.value = emptyList()
        save()
    }

    private fun load(): List<Transcript> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(file.readText())
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                Transcript(
                    id = o.getLong("id"),
                    createdAt = o.getLong("createdAt"),
                    text = o.getString("text"),
                    engine = o.optString("engine"),
                    durationSeconds = o.optDouble("durationSeconds", 0.0).toFloat(),
                    delivery = runCatching { Delivery.valueOf(o.optString("delivery")) }.getOrDefault(Delivery.CLIPBOARD),
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun save() {
        val array = JSONArray()
        _items.value.forEach { t ->
            array.put(
                JSONObject()
                    .put("id", t.id)
                    .put("createdAt", t.createdAt)
                    .put("text", t.text)
                    .put("engine", t.engine)
                    .put("durationSeconds", t.durationSeconds.toDouble())
                    .put("delivery", t.delivery.name)
            )
        }
        val tmp = File(file.path + ".tmp")
        tmp.writeText(array.toString())
        tmp.renameTo(file)
    }
}
