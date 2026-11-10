import express from 'express';
import { Query } from './query.js';
import { RECITERS_CATALOG } from './recitersData.js';
import { getListeningAnalytics } from './listeningAnalytics.js';

export default function registerExportRoutes(app, getContext) {
  const router = express.Router();

  // History doc timestamp: canonical field is `updated_at`; the app writer also
  // emits `played_at_ms` (+ `position_ms`). Tolerate both.
  function historyTs(h) {
    if (!h) return 0;
    const v = h.played_at_ms ?? h.updated_at ?? h.$updatedAt;
    if (typeof v === 'number' && v > 0) return v;
    const n = Number(v);
    if (Number.isFinite(n) && n > 0) return n;
    const c = h.$createdAt ? Date.parse(h.$createdAt) : NaN;
    return Number.isFinite(c) ? c : 0;
  }

  // History list with ordering fallback (played_at_ms -> updated_at ->
  // unordered + in-memory sort). Appwrite 400s on unknown attributes.
  async function listHistoryDocs(databases, databaseId, extraQueries = []) {
    const orderings = [
      [Query.orderDesc('played_at_ms')],
      [Query.orderDesc('updated_at')],
      [],
    ];
    let lastErr = null;
    for (const order of orderings) {
      try {
        const res = await databases.listDocuments(
          databaseId, 'history', [...extraQueries, ...order]
        );
        const docs = (res.documents || []).slice()
          .sort((a, b) => historyTs(b) - historyTs(a));
        return { documents: docs, total: res.total ?? docs.length };
      } catch (e) {
        lastErr = e;
      }
    }
    throw lastErr;
  }

  async function allDocuments(databases, databaseId, collection) {
    const documents=[];
    for (let offset=0;;offset+=500) {
      const page=await databases.listDocuments(databaseId,collection,[Query.orderAsc('$id'),Query.limit(500),Query.offset(offset)]);
      documents.push(...page.documents);
      if (page.documents.length < 500) break;
    }
    return { documents, total:documents.length };
  }
  async function allUsers(users) {
    const result=[];
    for (let offset=0;;offset+=500) {
      const page=await users.list([Query.orderAsc('$id'),Query.limit(500),Query.offset(offset)]);
      result.push(...page.users);
      if (page.users.length < 500) break;
    }
    return { users:result, total:result.length };
  }

  // GET /api/export/users - CSV streaming export
  router.get('/api/export/users', async (req, res) => {
    try {
      const { users, databases, databaseId, databaseAvailable } = getContext();

      let usersList = [];
      let statsMap = {};

      if (databaseAvailable && users) {
        try {
          const [uRes, stRes] = await Promise.all([
            allUsers(users),
            allDocuments(databases, databaseId, 'listening_stats')
          ]);

          stRes.documents.forEach(st => {
            statsMap[st.user_id] = st;
          });

          usersList = uRes.users.map(u => {
            const st = statsMap[u.$id] || {};
            // Real listening_stats fields (verified vs app SyncEngine STATS):
            // user_id, total_seconds, days_streak, minutes_today,
            // unique_surahs, unique_reciters, updated_at. A real doc with
            // zero seconds / zero streak exports as 0 — never inflated.
            const totalMinutes = Math.round((st.total_seconds ?? 0) / 60);
            const streak = st.days_streak ?? 0;
            const name = (u.name && u.name.trim()) ? u.name.replace(/"/g, '""') : (u.email ? u.email.split('@')[0] : 'Listener');
            const email = u.email ? u.email.replace(/"/g, '""') : 'no-email@quranify.app';
            const provider = u.provider || 'email';
            const createdAt = u.$createdAt || new Date().toISOString();

            return {
              id: u.$id,
              name: `"${name}"`,
              email: `"${email}"`,
              provider,
              createdAt,
              totalMinutes,
              streak
            };
          });
        } catch (err) {
          throw err;
        }
      }

      // Fallback sample data if sandbox mode or error


      res.setHeader('Content-Type', 'text/csv; charset=utf-8');
      res.setHeader('Content-Disposition', 'attachment; filename="quranify_users_export.csv"');

      const headers = ['id', 'name', 'email', 'provider', 'createdAt', 'totalMinutes', 'streak'];
      let csvContent = headers.join(',') + '\n';

      usersList.forEach(u => {
        csvContent += [
          u.id,
          u.name,
          u.email,
          u.provider,
          u.createdAt,
          u.totalMinutes,
          u.streak
        ].join(',') + '\n';
      });

      res.status(200).send(csvContent);
    } catch (e) {
      console.error('Error in GET /api/export/users:', e);
      res.status(500).json({ error: e.message });
    }
  });

  // GET /api/export/history - JSON pretty backup
  router.get('/api/export/history', async (req, res) => {
    try {
      const { databases, databaseId, databaseAvailable } = getContext();

      let documents = [];
      let total = 0;

      if (databaseAvailable && databases) {
        try {
          const resDoc = await allDocuments(databases, databaseId, 'history');
          documents = resDoc.documents;
          total = resDoc.total;
        } catch (err) {
          throw err;
        }
      }



      res.setHeader('Content-Type', 'application/json; charset=utf-8');
      res.setHeader('Content-Disposition', 'attachment; filename="quranify_listening_history_backup.json"');

      const backupPayload = {
        exportedAt: new Date().toISOString(),
        collection: 'history',
        databaseId,
        totalDocuments: total,
        documents
      };

      res.status(200).send(JSON.stringify(backupPayload, null, 2));
    } catch (e) {
      console.error('Error in GET /api/export/history:', e);
      res.status(500).json({ error: e.message });
    }
  });

  // GET /api/export/playlists - JSON playlists & items backup
  router.get('/api/export/playlists', async (req, res) => {
    try {
      const { databases, databaseId, databaseAvailable } = getContext();

      let playlists = [];
      let playlist_items = [];

      if (databaseAvailable && databases) {
        try {
          const [plRes, itemRes] = await Promise.all([
            allDocuments(databases, databaseId, 'playlists'),
            allDocuments(databases, databaseId, 'playlist_items')
          ]);
          playlists = plRes.documents;
          playlist_items = itemRes.documents;
        } catch (err) {
          throw err;
        }
      }



      res.setHeader('Content-Type', 'application/json; charset=utf-8');
      res.setHeader('Content-Disposition', 'attachment; filename="quranify_playlists_backup.json"');

      const backupPayload = {
        exportedAt: new Date().toISOString(),
        databaseId,
        totalPlaylists: playlists.length,
        totalPlaylistItems: playlist_items.length,
        playlists,
        playlist_items
      };

      res.status(200).send(JSON.stringify(backupPayload, null, 2));
    } catch (e) {
      console.error('Error in GET /api/export/playlists:', e);
      res.status(500).json({ error: e.message });
    }
  });

  // GET /api/export/reciters - Reciters CSV streaming export
  router.get('/api/export/reciters', async (req, res) => {
    try {
      const { databases, databaseId, databaseAvailable } = getContext();

      let followsBySlug = {};
      let likesBySlug = {};
      // Live mode reports REAL follows/likes counts (0 when the DB is
      // empty). Demo offsets apply ONLY to the sandbox fallback below, so
      // live exports are never inflated.
      let isLiveMetrics = false;

      if (databaseAvailable && databases) {
        try {
          const [fRes, lRes] = await Promise.all([
            allDocuments(databases, databaseId, 'follows'),
            allDocuments(databases, databaseId, 'likes'),
          ]);

          isLiveMetrics = true;
          fRes.documents.forEach(f => {
            if (f.reciter_slug) {
              followsBySlug[f.reciter_slug] = (followsBySlug[f.reciter_slug] || 0) + 1;
            }
          });

          lRes.documents.forEach(l => {
            if (l.reciter_slug) {
              likesBySlug[l.reciter_slug] = (likesBySlug[l.reciter_slug] || 0) + 1;
            }
          });
        } catch (dbErr) {
          throw dbErr;
        }
      }

      const catalog = await allDocuments(databases, databaseId, 'reciters');
      const recitersList = catalog.documents.map(r => ({
        slug: r.slug || '',
        nameEn: `"${(r.name_en || '').replace(/"/g, '""')}"`,
        nameAr: `"${(r.name_ar || '').replace(/"/g, '""')}"`,
        country: `"${(r.country || '').replace(/"/g, '""')}"`,
        style: `"${(r.style || '').replace(/"/g, '""')}"`,
        riwayah: `"${(r.riwayah || '').replace(/"/g, '""')}"`,
        audioFolder: r.audio_folder || '',
        followers: followsBySlug[r.slug] || 0,
        likes: likesBySlug[r.slug] || 0
      }));

      res.setHeader('Content-Type', 'text/csv; charset=utf-8');
      res.setHeader('Content-Disposition', 'attachment; filename="quranify_reciters_export.csv"');

      const headers = ['slug', 'nameEn', 'nameAr', 'country', 'style', 'riwayah', 'audioFolder', 'followers', 'likes'];
      let csvContent = headers.join(',') + '\n';

      recitersList.forEach(r => {
        csvContent += [
          r.slug,
          r.nameEn,
          r.nameAr,
          r.country,
          r.style,
          r.riwayah,
          r.audioFolder,
          r.followers,
          r.likes
        ].join(',') + '\n';
      });

      res.status(200).send(csvContent);
    } catch (e) {
      console.error('Error in GET /api/export/reciters:', e);
      res.status(500).json({ error: e.message });
    }
  });

  // GET /api/export/analytics - Analytics JSON export
  router.get('/api/export/analytics', async (req, res) => {
    try {
      const { pool, databaseId } = getContext();
      const analyticsPayload = { exportedAt: new Date().toISOString(), databaseId, ...await getListeningAnalytics(pool) };
      res.setHeader('Content-Type', 'application/json; charset=utf-8');
      res.setHeader('Content-Disposition', 'attachment; filename="quranify_analytics_export.json"');

      res.status(200).send(JSON.stringify(analyticsPayload, null, 2));
    } catch (e) {
      console.error('Error in GET /api/export/analytics:', e);
      res.status(500).json({ error: e.message });
    }
  });

  app.use(router);
}
