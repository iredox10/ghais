/**
 * Live Playback State Radar Routes
 *
 * Canonical source: the `playback_state` collection
 * (see quranify/appwrite/collections.json):
 *   { user_id, current_ref "<reciterSlug>/<surahId>", position_ms INT,
 *     queue_json "[{reciter_slug,surah_id,ayah_no,audio_url}]", updated_at }
 * Single doc per user (docId = userId). The app writes position in
 * milliseconds and carries the ayah + audio URL inside queue_json; there is
 * NO track_json / surah_name / position_seconds / duration_seconds /
 * device_type in the model — those were radar-side guesses and are fixed
 * below. Duration is unknown until streaming (app keeps 0 = unknown), so the
 * radar reports durationSeconds: 0 and deviceType: 'Unknown' for live docs.
 * Legacy UI keys (surahName, positionSeconds, …) are kept for the admin UI.
 */

import { Query } from './query.js';
import { SURAHS_CATALOG } from './surahsData.js';

function parseCurrentRef(ref) {
  if (typeof ref !== 'string') return null;
  const idx = ref.lastIndexOf('/');
  if (idx <= 0 || idx >= ref.length - 1) return null;
  const slug = ref.slice(0, idx).trim();
  const surahId = parseInt(ref.slice(idx + 1).trim(), 10);
  if (!slug || !Number.isInteger(surahId) || surahId <= 0) return null;
  return { slug, surahId };
}

function parseQueueFirst(queueJson) {
  if (typeof queueJson !== 'string' || !queueJson.trim()) return null;
  try {
    const first = JSON.parse(queueJson)[0];
    if (!first || typeof first !== 'object') return null;
    const slug = typeof first.reciter_slug === 'string' ? first.reciter_slug.trim() : '';
    const surahId = Number(first.surah_id);
    if (!slug || !Number.isInteger(surahId) || surahId <= 0) return null;
    return {
      slug,
      surahId,
      ayahNo: Number.isInteger(first.ayah_no) ? first.ayah_no : 0,
      audioUrl: typeof first.audio_url === 'string' ? first.audio_url : ''
    };
  } catch (_) {
    return null;
  }
}

function surahNameEn(surahId) {
  const s = SURAHS_CATALOG.find(x => x.surah_id === surahId || x.id === surahId);
  return s ? (s.name_en || s.nameEn) : `Surah ${surahId}`;
}

function formatUpdatedAt(doc) {
  if (typeof doc.updated_at === 'number' && doc.updated_at > 0) {
    return new Date(doc.updated_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  }
  if (doc.$updatedAt) {
    return new Date(doc.$updatedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  }
  return 'Just now';
}

export default function registerPlaybackRoutes(app, getContext) {
  // GET /api/playback: Queries Appwrite's playback_state collection and maps active listening sessions
  app.get('/api/playback', async (req, res) => {
    try {
      const { users, databases, databaseId, databaseAvailable } = getContext();

      let sessions = [];

      if (databaseAvailable && databases) {
        try {
          const [pbRes, uRes] = await Promise.all([
            databases.listDocuments(databaseId, 'playback_state', [
              Query.limit(50),
              Query.orderDesc('$updatedAt')
            ]).catch(() => ({ documents: [] })),
            users.list([Query.limit(100)]).catch(() => ({ users: [] }))
          ]);

          const userMap = {};
          uRes.users.forEach(u => {
            userMap[u.$id] = {
              name: u.name && u.name.trim() ? u.name : (u.email ? u.email.split('@')[0] : 'Listener'),
              avatar: u.prefs?.avatar || `https://api.dicebear.com/7.x/avataaars/svg?seed=${u.$id}`
            };
          });

          if (pbRes.documents && pbRes.documents.length > 0) {
            sessions = pbRes.documents.map((doc, index) => {
              const userInfo = userMap[doc.user_id] || {
                name: 'Active Listener',
                avatar: `https://api.dicebear.com/7.x/avataaars/svg?seed=${doc.user_id || index}`
              };

              // Canonical parsing: queue_json first, current_ref fallback
              // (same precedence as the app's pullPlaybackIfEmpty).
              const queued = parseQueueFirst(doc.queue_json);
              const ref = parseCurrentRef(doc.current_ref);
              const reciterSlug = queued?.slug || ref?.slug || 'mishary';
              const surahId = queued?.surahId || ref?.surahId || 1;
              const ayahNo = queued?.ayahNo || 0;
              const positionMs = typeof doc.position_ms === 'number'
                ? Math.max(0, Math.floor(doc.position_ms))
                : 0;
              const positionSeconds = Math.floor(positionMs / 1000);

              return {
                id: doc.$id || `playback_${index + 1}`,
                userId: doc.user_id || `usr_${index + 1}`,
                userName: userInfo.name,
                userAvatar: userInfo.avatar,
                surahName: surahNameEn(surahId),
                surahId,
                reciter: reciterSlug,
                positionSeconds,
                durationSeconds: 0, // unknown until streaming (app: 0 = unknown)
                deviceType: 'Unknown', // not part of the playback_state model
                updatedAt: formatUpdatedAt(doc),
                // Canonical passthrough for debugging / future UI use
                current_ref: doc.current_ref || '',
                position_ms: positionMs,
                ayahNo,
                audioUrl: queued?.audioUrl || '',
                updated_at: typeof doc.updated_at === 'number' ? doc.updated_at : null
              };
            });
          }
        } catch (dbErr) {
          console.error('Error querying playback_state from PostgreSQL:', dbErr.message);
        }
      }

      // Default sample sessions if database is empty or sandbox mode


      res.json({
        total: sessions.length,
        sessions
      });
    } catch (e) {
      console.error('Error in GET /api/playback:', e);
      res.status(500).json({ error: e.message });
    }
  });
}
