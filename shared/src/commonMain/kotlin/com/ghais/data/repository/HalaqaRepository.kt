package com.ghais.data.repository

import com.ghais.data.backend.SocialContracts
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Family Halaqa weekly board (private by default, ≤8 members).
 *
 * Phase 1 of the social rollout: local group management works now (create /
 * join / leave / aliases in device Settings); per-member devotion data
 * flows through [HalaqaTransport] once the Android backend is provisioned
 * (`devotion` + `friendships` collections) and injected. Until then
 * [board] stays null and all UI stays behind `HookFlags.friendshipsEnabled`
 * (false). No `poolLeague`, no XP, no public discovery. Never throws.
 */
object HalaqaRepository {

    /** One board member row. */
    @Serializable
    data class Member(
        val userId: String,
        val alias: String = "",
        val streak: Int = 0,
        val minutes7d: Int = 0,
        val activeToday: Boolean = false,
    )

    /** Weekly board: members + pairwise shared streaks + week anchor. */
    data class HalaqaBoard(
        val name: String,
        val members: List<Member>,
        val pairStreaks: Map<String, Int> = emptyMap(),
        val weekStartMs: Long = 0L,
    )

    /**
     * Backend seam (Android injects the Appwrite implementation, same
     * precedent as FollowStore's `countFetcher`). Null = unprovisioned:
     * refresh/record degrade to local-only gracefully.
     */
    interface HalaqaTransport {
        suspend fun getDevotion(uid: String): SocialContracts.DevotionState?
        suspend fun getPairStreak(a: String, b: String): Int
        suspend fun upsertDevotion(state: SocialContracts.DevotionState)
    }

    var transport: HalaqaTransport? = null

    private const val KEY_GROUP = "ghais_halaqa_group"
    private const val KEY_NAME = "ghais_halaqa_name"
    private const val MAX_MEMBERS = 8

    private val settings: Settings by lazy { Settings() }
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _members = MutableStateFlow(loadGroup())
    val members: StateFlow<List<Member>> = _members.asStateFlow()

    private val _board = MutableStateFlow<HalaqaBoard?>(null)
    val board: StateFlow<HalaqaBoard?> = _board.asStateFlow()

    private var groupName: String = loadName()

    fun groupDisplayName(): String = groupName.ifBlank { "Family Halaqa" }

    /** Creates (or replaces) the local group; invite codes arrive later. */
    fun createHalaqa(name: String, selfUid: String): Boolean = try {
        if (selfUid.isBlank()) return false
        groupName = name.trim().take(40)
        _members.value = listOf(Member(userId = selfUid.trim()))
        save()
        true
    } catch (_: Exception) {
        false
    }

    /** Joins by member uid (invite-code resolution lands with transport). */
    fun joinHalaqa(memberUid: String): Boolean = try {
        val uid = memberUid.trim()
        if (uid.isBlank()) return false
        val current = _members.value
        if (current.any { it.userId == uid }) return true
        if (current.size >= MAX_MEMBERS) return false
        _members.value = current + Member(userId = uid)
        save()
        true
    } catch (_: Exception) {
        false
    }

    fun leaveHalaqa(uid: String): Boolean = try {
        _members.value = _members.value.filter { it.userId != uid.trim() }
        save()
        true
    } catch (_: Exception) {
        false
    }

    fun renameMember(uid: String, alias: String): Boolean = try {
        _members.value = _members.value.map {
            if (it.userId == uid.trim()) it.copy(alias = alias.trim().take(24)) else it
        }
        save()
        true
    } catch (_: Exception) {
        false
    }

    /**
     * Refreshes the board from the backend. Local-only until [transport] is
     * injected: returns false and leaves [board] untouched. Never throws.
     */
    suspend fun refreshBoard(): Boolean {
        val t = transport ?: return false
        return try {
            val memberIds = _members.value.map { it.userId }
            if (memberIds.isEmpty()) return false
            val states = memberIds.associateWith { uid ->
                try {
                    t.getDevotion(uid)
                } catch (_: Exception) {
                    null
                }
            }
            val pairs = mutableMapOf<String, Int>()
            for (i in memberIds.indices) {
                for (j in i + 1 until memberIds.size) {
                    val key = pairKeyOf(memberIds[i], memberIds[j])
                    pairs[key] = try {
                        t.getPairStreak(memberIds[i], memberIds[j])
                    } catch (_: Exception) {
                        0
                    }
                }
            }
            _members.value = _members.value.map { m ->
                val s = states[m.userId]
                if (s == null) m else m.copy(streak = s.streakCount)
            }
            save()
            _board.value = HalaqaBoard(
                name = groupDisplayName(),
                members = _members.value,
                pairStreaks = pairs,
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Stable pair key for two uids (sorted join — matches backend docId). */
    fun pairKeyOf(a: String, b: String): String = try {
        listOf(a.trim(), b.trim()).sorted().joinToString("|")
    } catch (_: Exception) {
        "$a|$b"
    }

    private fun loadGroup(): List<Member> = try {
        val raw = settings.getString(KEY_GROUP, "")
        if (raw.isBlank()) emptyList()
        else json.decodeFromString<List<Member>>(raw).take(MAX_MEMBERS)
    } catch (_: Exception) {
        emptyList()
    }

    private fun loadName(): String = try {
        settings.getString(KEY_NAME, "")
    } catch (_: Exception) {
        ""
    }

    private fun save() {
        try {
            settings.putString(KEY_GROUP, json.encodeToString(_members.value))
            settings.putString(KEY_NAME, groupName)
        } catch (_: Exception) {
        }
    }
}
