// Collection registry: which Appwrite collections are public/user-scoped/etc.
// The access classes:
// - 'public'  : anon read; client writes rejected (server/admin only).
// - 'user'    : Bearer required; EVERY op is forced to owner = token user,
//               regardless of the queried user_id / userId attribute.
// - 'catalog' : anon read (editorial pulls); writes need Bearer and are
//               owner-scoped (own playlist publishing only).
// - 'social'  : Bearer; read-any-authenticated; write when own or when the
//               token user is referenced by the document (pair/member rules).
export type AccessClass = 'public' | 'user' | 'catalog' | 'social';

export type CollectionDef = {
  access: AccessClass;
  /** Attribute names naming the user on this collection's documents. */
  ownerAttrs: string[];
  /** Attributes whose values are numbers — comparisons cast to numeric. */
  numeric: string[];
};

const userCollection = (ownerAttrs: string[], numeric: string[] = []): CollectionDef => ({
  access: 'user',
  ownerAttrs,
  numeric,
});

export const CATALOG: Record<string, CollectionDef> = {
  likes: userCollection(['user_id'], ['surah_id', 'ayah_no']),
  playback_state: userCollection(['user_id'], ['position_ms']),
  follows: userCollection(['user_id']),
  listening_stats: userCollection(
    ['user_id'],
    ['total_seconds', 'days_streak', 'minutes_today', 'unique_surahs', 'unique_reciters'],
  ),
  schedules: userCollection(['user_id']),
  history: userCollection(['user_id'], ['played_at_ms', 'position_ms']),
  reciter_stats: { access: 'public', ownerAttrs: [], numeric: ['followers_count', 'likes_count'] },
  routine_backups: userCollection(['user_id']),
  user_prefs: userCollection(['user_id'], ['daily_minutes']),
  khatma_plans: userCollection(
    ['user_id'],
    ['total_days', 'current_surah', 'current_ayah', 'percent', 'total_ayahs_read', 'start_date_ms', 'last_progress_ms'],
  ),
  broadcasts: { access: 'public', ownerAttrs: [], numeric: [] },
  play_events: userCollection(['ownerId'], ['ms']),
  hifz_mastery: userCollection(
    ['user_id'],
    ['surah_id', 'ayah_no', 'daily_goal_count', 'streak_days', 'today_reviewed_count'],
  ),
  search_history: userCollection(['user_id']),
  playlists: { access: 'catalog', ownerAttrs: ['owner_id'], numeric: ['publishedAt', 'published_at'] },
  playlist_items: { access: 'catalog', ownerAttrs: ['playlist_id'], numeric: ['position', 'surah_id', 'ayah_from', 'ayah_to'] },
  charts: { access: 'public', ownerAttrs: [], numeric: [] },
  reciters: { access: 'public', ownerAttrs: [], numeric: [] },
  devotion: { access: 'social', ownerAttrs: ['userId'], numeric: ['streakCount', 'longestStreak', 'graceDays'] },
  leagues: { access: 'social', ownerAttrs: ['leagueId'], numeric: [] },
  friendships: { access: 'social', ownerAttrs: ['pairKey', 'aId', 'bId'], numeric: ['sharedStreak'] },
  global_charts: { access: 'public', ownerAttrs: [], numeric: [] },
  achievements: { access: 'social', ownerAttrs: ['userId', 'achievementId'], numeric: ['unlockedMs'] },
};

/** Shared database id (strict). */
export const DATABASE_ID = 'ghais';

/** True when attr is a safe SQL identifier (whitelisted shape). */
export function isAttr(attr: string): boolean {
  return /^[A-Za-z0-9_]+$/.test(attr);
}
