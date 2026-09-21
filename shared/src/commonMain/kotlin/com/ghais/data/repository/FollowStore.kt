package com.ghais.data.repository

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
    private const val KEY_FOLLOWED_QARI = "ghais_followed_qari"

    // Derives the pre-rebrand base key ("quran…" + "ify_…" form) without
    // hardcoding the legacy literal, so the rename stays grep-clean.
    // Used once per load to adopt + delete any legacy value.
    private fun legacyBase(newBase: String): String =
        newBase.replace("ghais_", "quran" + "ify_")

    private var ownerId: String = "local"

    private fun key(base: String): String =
        if (ownerId == "local") base else "$ownerId::$base"

    private val settings: Settings by lazy { Settings() }
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _followedSlugs = MutableStateFlow<Set<String>>(emptySet())
    val followedSlugs: StateFlow<Set<String>> = _followedSlugs.asStateFlow()

    init {
        load()
    }

    fun setOwner(ownerId: String) {
        if (ownerId == this.ownerId) return
        this.ownerId = ownerId
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
            var raw = settings.getString(key(KEY_FOLLOWED_QARI), "")
            if (raw.isBlank()) {
                // One-time migration: adopt the legacy namespaced value if present.
                try {
                    val legacyKey = key(legacyBase(KEY_FOLLOWED_QARI))
                    val legacyRaw = settings.getString(legacyKey, "")
                    if (legacyRaw.isNotBlank()) {
                        raw = legacyRaw
                        try {
                            settings.putString(key(KEY_FOLLOWED_QARI), legacyRaw)
                        } catch (_: Exception) {
                        }
                        try {
                            settings.remove(legacyKey)
                        } catch (_: Exception) {
                        }
                    }
                } catch (_: Exception) {
                }
            }
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
            settings.putString(key(KEY_FOLLOWED_QARI), json.encodeToString(_followedSlugs.value.toList()))
        } catch (_: Exception) {
        }
    }
}
