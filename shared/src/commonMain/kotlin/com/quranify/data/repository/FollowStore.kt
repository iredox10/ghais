package com.quranify.data.repository

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * FollowStore: persisted store of followed Qari (identified by reciter slug).
 *
 * Mirrors [FavoritesStore] persistence approach (multiplatform Settings +
 * JSON-encoded list) but tracks a set of reciter slug Strings.
 */
object FollowStore {
    private const val KEY_FOLLOWED_QARI = "quranify_followed_qari"

    private val settings: Settings by lazy { Settings() }
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _followedSlugs = MutableStateFlow<Set<String>>(emptySet())
    val followedSlugs: StateFlow<Set<String>> = _followedSlugs.asStateFlow()

    init {
        load()
    }

    fun isFollowing(slug: String): Boolean {
        return _followedSlugs.value.contains(slug)
    }

    fun toggle(slug: String) {
        _followedSlugs.update { set ->
            if (set.contains(slug)) {
                set - slug
            } else {
                set + slug
            }
        }
        save()
    }

    fun follow(slug: String) {
        _followedSlugs.update { set ->
            if (set.contains(slug)) set else set + slug
        }
        save()
    }

    fun unfollow(slug: String) {
        _followedSlugs.update { set ->
            set - slug
        }
        save()
    }

    fun clear() {
        _followedSlugs.value = emptySet()
        save()
    }

    private fun load() {
        try {
            val raw = settings.getString(KEY_FOLLOWED_QARI, "")
            if (raw.isBlank()) {
                _followedSlugs.value = emptySet()
                return
            }
            _followedSlugs.value = json.decodeFromString<List<String>>(raw).toSet()
        } catch (_: Exception) {
            _followedSlugs.value = emptySet()
        }
    }

    private fun save() {
        try {
            settings.putString(KEY_FOLLOWED_QARI, json.encodeToString(_followedSlugs.value.toList()))
        } catch (_: Exception) {
        }
    }
}
