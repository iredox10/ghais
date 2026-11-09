package com.ghais.ui.screens.mood

import com.ghais.data.auth.BackendConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Curated display copy overlays only moods whose playback is bundled. */
object MoodCatalog {
    private val client by lazy { runCatching { HttpClient() }.getOrNull() }
    private val current = MutableStateFlow(AVAILABLE_MOODS)
    val moods = current.asStateFlow()

    internal fun overlay(body: String, base: List<MoodItem> = AVAILABLE_MOODS): List<MoodItem> {
        val entries = Json.parseToJsonElement(body).jsonObject["moods"]?.jsonArray ?: return base
        val byId = entries.mapNotNull { entry ->
            runCatching {
                val fields = entry.jsonObject
                fun text(key: String) = fields[key]?.jsonPrimitive?.content?.trim().orEmpty()
                val slug = text("slug")
                if (slug.isEmpty()) null else slug to Triple(text("title"), text("subtitle"), text("category"))
            }.getOrNull()
        }.toMap()
        return base.map { item ->
            val (title, subtitle, category) = byId[item.id] ?: return@map item
            item.copy(
                title = title.takeIf { it.isNotBlank() && it.length <= 120 } ?: item.title,
                subtitle = subtitle.takeIf { it.isNotBlank() && it.length <= 240 } ?: item.subtitle,
                category = category.takeIf { it in setOf("Reflection", "Comfort", "Focus", "Night") } ?: item.category,
            )
        }
    }

    suspend fun refresh() {
        try {
            val response = client?.get("${BackendConfig.ENDPOINT}/editorial/moods") ?: return
            if (response.status.isSuccess()) current.value = overlay(response.bodyAsText())
        } catch (_: Exception) {
            // Keep bundled or last successfully loaded copy when offline.
        }
    }
}
