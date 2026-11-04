import { Query } from './query.js';

export default function registerIndexAudit(app, getContext) {
  // Recommended indexes mirror appwrite/collections.json EXACTLY (verified
  // 2026-09-24). `key` values are attribute names; the audit matches them
  // against live index.attributes. `type` mirrors the collection's index
  // type (key|unique) for display.
  const COLLECTIONS_CONFIG = [
    {
      id: 'reciters',
      name: 'Reciters',
      recommended: [
        // Real: catalog_key_unique (unique on catalog_key) + slug_idx
        // (non-unique key on slug, added 2026-09-25 for the app's
        // Query.equal('slug', slug) history-row / avatar lookups).
        { key: 'catalog_key', type: 'unique' },
        { key: 'slug', type: 'key' }
      ]
    },
    {
      id: 'surahs',
      name: 'Surahs',
      recommended: [
        { key: 'surah_id', type: 'unique' }
      ]
    },
    {
      id: 'moods',
      name: 'Moods & Routine Modes',
      recommended: [
        { key: 'slug', type: 'unique' }
      ]
    },
    {
      id: 'history',
      name: 'Listening History',
      recommended: [
        // Real: user_played_idx (key on user_id + played_at_ms). The app
        // writes played_at_ms + position_ms per entry (SyncEngine HISTORY)
        // and pulls ordered by played_at_ms desc.
        { key: 'user_id', type: 'key' },
        { key: 'played_at_ms', type: 'key' }
      ]
    },
    {
      id: 'likes',
      name: 'Likes & Favorites',
      recommended: [
        // Real: user_like_idx (key on user_id + surah_id + ayah_no).
        // reciter_slug is an optional attribute, NOT part of the index.
        { key: 'user_id', type: 'key' },
        { key: 'surah_id', type: 'key' },
        { key: 'ayah_no', type: 'key' }
      ]
    },
    {
      id: 'follows',
      name: 'Reciter Follows',
      recommended: [
        // Real: two single-attribute key indexes (user_idx, reciter_slug_idx).
        { key: 'user_id', type: 'key' },
        { key: 'reciter_slug', type: 'key' }
      ]
    },
    {
      id: 'schedules',
      name: 'Recitation Schedules',
      recommended: [
        { key: 'user_id', type: 'key' }
      ]
    },
    {
      id: 'playlists',
      name: 'User Playlists',
      recommended: [
        // Real: owner_idx (owner_id) + public_idx (is_public).
        { key: 'owner_id', type: 'key' },
        { key: 'is_public', type: 'key' }
      ]
    },
    {
      id: 'playback_state',
      name: 'Playback State',
      recommended: [
        // Real: user_state_unique (unique on user_id).
        { key: 'user_id', type: 'unique' }
      ]
    },
    {
      id: 'listening_stats',
      name: 'Listening Stats',
      recommended: [
        // Real: user_idx (key on user_id). App fields: user_id,
        // total_seconds, days_streak, minutes_today, unique_surahs,
        // unique_reciters, updated_at, daily_json (string 8192), surah_plays
        // (string 2048). The two JSON series are not indexed — they are only
        // ever read off the user_id document.
        { key: 'user_id', type: 'key' }
      ]
    },
    {
      id: 'hifz_mastery',
      name: 'Hifz Mastery',
      recommended: [
        // Real: user_surah_ayah_idx (key on user_id + surah_id + ayah_no).
        { key: 'user_id', type: 'key' },
        { key: 'surah_id', type: 'key' },
        { key: 'ayah_no', type: 'key' }
      ]
    },
    {
      id: 'search_history',
      name: 'Search Query History',
      recommended: [
        // Real: user_ranked_idx (key on user_id + ranked_at_ms) for the
        // recency-ordered list, plus user_query_unique (unique on
        // user_id + query) so there is one document per query.
        { key: 'user_id', type: 'key' },
        { key: 'ranked_at_ms', type: 'key' },
        { key: 'query', type: 'unique' }
      ]
    },
    {
      id: 'playlist_items',
      name: 'Playlist Items',
      recommended: [
        // Real: playlist_pos_idx (key on playlist_id + position).
        // Attributes: playlist_id, position, reciter_slug, surah_id,
        // ayah_from, ayah_to — there is NO `order` attribute.
        { key: 'playlist_id', type: 'key' },
        { key: 'position', type: 'key' }
      ]
    },
    {
      id: 'khatma_plans',
      name: 'Khatma Completion Plans',
      recommended: [
        { key: 'user_id', type: 'key' }
      ]
    },
    { id: 'user_prefs', name: 'User Preferences', recommended: [] },
    { id: 'routine_backups', name: 'Routine Backups', recommended: [] },
    {
      id: 'reciter_stats',
      name: 'Reciter Stats',
      recommended: [
        // Real: slug_unique (unique on slug).
        { key: 'slug', type: 'unique' }
      ]
    },
  ];

  app.get('/api/settings/indexes', async (req, res) => {
    try {
      const { databases, databaseId, databaseAvailable } = getContext();
      const collectionsAudit = [];

      for (const col of COLLECTIONS_CONFIG) {
        let documentCount = 0;
        let existingIndexes = [];
        let isConnected = false;

        if (databaseAvailable && databases) {
          try {
            const countRes = await databases.listDocuments(databaseId, col.id, [Query.limit(1)]);
            documentCount = countRes.total ?? 0;
            isConnected = true;
          } catch (_) {}

          try {
            const idxRes = await databases.listIndexes(databaseId, col.id);
            existingIndexes = idxRes.indexes || [];
          } catch (_) {}
        }

        const recommended = col.recommended;
        let missingIndexes = [];

        if (recommended.length === 0) {
          missingIndexes = [];
        } else if (!isConnected) {
          missingIndexes = [];
        } else {
          const existingKeys = new Set();
          existingIndexes.forEach(idx => {
            if (idx.attributes) {
              idx.attributes.forEach(attr => existingKeys.add(attr));
            }
            if (idx.key) {
              existingKeys.add(idx.key);
            }
          });
          missingIndexes = recommended.filter(r => !existingKeys.has(r.key)).map(r => r.key);
        }

        const coverageStatus = !isConnected
          ? 'Optimal (Sandbox)'
          : missingIndexes.length === 0
          ? 'Optimal'
          : missingIndexes.length === recommended.length
          ? 'Unindexed'
          : 'Partial';

        collectionsAudit.push({
          collectionId: col.id,
          name: col.name,
          documentCount,
          recommendedIndexes: recommended,
          existingIndexes: existingIndexes.map(idx => ({
            key: idx.key || idx.name || 'index',
            type: idx.type || 'KEY',
            attributes: idx.attributes || []
          })),
          coverageStatus,
          missingIndexes
        });
      }

      res.json({ collectionsAudit });
    } catch (e) {
      console.error('Error in GET /api/settings/indexes:', e);
      res.status(500).json({ error: e.message });
    }
  });
}
