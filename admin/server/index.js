import express from 'express';
import dotenv from 'dotenv';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { Query } from './query.js';
import { createDatabase } from './database.js';
import { createStorage } from './storage.js';
import { adminAuth } from './security.js';
import { registerPublicImages } from './publicImages.js';
import registerExportRoutes from './exportRoutes.js';
import registerIndexAudit from './indexAudit.js';
import registerBroadcastRoutes from './broadcastRoutes.js';
import { registerPlaylistEditing } from './playlistEditing.js';
import { getListeningAnalytics } from './listeningAnalytics.js';
import { registerScheduleDeletion } from './scheduleDeletion.js';
import registerPlaybackRoutes from './playbackRoutes.js';
import registerSurahsRoutes from './surahsRoutes.js';
import { registerTelemetry } from './telemetry.js';
import { setupRecitersSync } from './recitersSync.js';
import { registerCdnHealth } from './cdnHealth.js';
import registerSeedRoutes from './seedRoutes.js';
import {
  registerChartsAggregate,
  CHARTS_COLLECTION,
  CHARTS_DOCUMENT_ID,
} from './chartsAggregate.js';
import registerMoodsRoutes from './moodsRoutes.js';
import registerArchiveRoutes from './archiveRoutes.js';
import { registerUsersRoutes } from './usersRoutes.js';
import { registerActivityRoutes } from './activityRoutes.js';
import { RECITERS_CATALOG } from './recitersData.js';
import { FULL_SURAHS_LIST } from './surahsData.js';

// Canonical app schema (quranify/appwrite/collections.json):
//   playlists{owner_id,title,description,is_public,cover_url}
//   playlist_items{playlist_id,position(INTEGER),reciter_slug,surah_id,ayah_from,ayah_to}
// There is NO track_json / sequence / surah_name / reciter_name / audio_url /
// tracks_count / plays anywhere in the app schema — views below derive names
// from the local catalogs instead of reading phantom doc fields.
const RECITER_BY_SLUG = new Map(RECITERS_CATALOG.map(r => [r.slug, r]));
const SURAH_BY_ID = new Map(FULL_SURAHS_LIST.map(s => [s.id, s]));

function padSurah(n) {
  return String(n).padStart(3, '0');
}

// Build an auditionable track view from a canonical playlist_items doc.
// Only canonical fields are read: position, reciter_slug, surah_id,
// ayah_from, ayah_to. Names + audio URL are resolved from the catalogs.
// `sequence`, `ambientPairing` and `duration` are kept as UI-compat aliases:
// sequence mirrors position; ambient/duration have no app-schema source.
function buildPlaylistTrackView(doc, idx = 0) {
  const position = Number.isInteger(doc.position) ? doc.position : idx;
  const reciterSlug = doc.reciter_slug || 'mishary';
  const surahId = doc.surah_id || 1;
  const ayahFrom = doc.ayah_from ?? 1;
  const ayahTo = doc.ayah_to ?? ayahFrom;
  const reciter = RECITER_BY_SLUG.get(reciterSlug);
  const surah = SURAH_BY_ID.get(surahId);
  const reciterName = reciter?.nameEn || reciterSlug;
  const surahName = surah?.nameEn || `Surah ${surahId}`;
  const isFullSurah = (!ayahFrom && !ayahTo) || (ayahFrom <= 1 && ayahTo <= 1);
  const ayahRange = isFullSurah ? 'Full surah' : (ayahFrom === ayahTo ? `Ayah ${ayahFrom}` : `Ayahs ${ayahFrom}–${ayahTo}`);
  const baseUrl = reciter?.serverUrl || 'https://server8.mp3quran.net/afs/';
  const sep = baseUrl.endsWith('/') ? '' : '/';
  return {
    id: doc.$id || `track_${position + 1}`,
    position,
    sequence: position,
    playlistId: doc.playlist_id,
    reciterSlug,
    reciterName,
    surahId,
    surahName,
    ayahFrom,
    ayahTo,
    ayahRange,
    ambientPairing: '—',
    duration: '',
    audioUrl: `${baseUrl}${sep}${padSurah(surahId)}.mp3`
  };
}

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

function get242Reciters() {
  try {
    const mdPath = path.join(__dirname, '../../quranify/mp3quran_reciters_and_countries.md');
    if (fs.existsSync(mdPath)) {
      const content = fs.readFileSync(mdPath, 'utf8');
      const lines = content.split('\n');
      const parsed = [];
      for (const line of lines) {
        if (line.startsWith('|') && !line.includes('---') && !line.includes('Reciter (Arabic)')) {
          const parts = line.split('|').map(p => p.trim()).filter(Boolean);
          if (parts.length >= 4) {
            const nameAr = parts[1];
            const nameEn = parts[2];
            const country = parts[3];
            const slug = nameEn.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
            parsed.push({ slug, nameEn, nameAr, country });
          }
        }
      }

      if (parsed.length > 0) {
        const catalogMap = new Map();
        RECITERS_CATALOG.forEach(r => catalogMap.set(r.slug, r));

        return parsed.map(p => {
          const detailed = catalogMap.get(p.slug) || {};
          return {
            slug: p.slug,
            nameEn: detailed.nameEn || p.nameEn,
            nameAr: detailed.nameAr || p.nameAr,
            country: detailed.country || p.country,
            style: detailed.style || 'Murattal',
            riwayah: detailed.riwayah || 'Hafs',
            serverUrl: detailed.serverUrl || 'https://server8.mp3quran.net/afs/',
            audioFolder: detailed.audioFolder || '',
            availableSurahs: '',
            verified: detailed.verified ?? true
          };
        });
      }
    }
  } catch (e) {
    console.error('Error loading 242 reciters from markdown:', e.message);
  }
  return RECITERS_CATALOG.map(r => ({
    ...r,
    availableSurahs: '',
    serverUrl: r.serverUrl || 'https://server8.mp3quran.net/afs/'
  }));
}


dotenv.config();

const app = express();
const PORT = process.env.PORT || 5000;

// Only read-only portrait delivery is public; management stays authenticated.
const storage = createStorage();
registerPublicImages(app, storage);

app.use(adminAuth());
app.use(express.json({ limit: '1mb' }));

// The admin connects to the same PostgreSQL database as the mobile API.
const DATABASE_ID = 'ghais';
const { pool, database: databases, users: userStore } = createDatabase(process.env.DATABASE_URL);
const databaseAvailable = true;

// Fail closed instead of returning demo or fabricated data when the DB is unavailable.
app.use('/api', async (_req, res, next) => {
  try { await pool.query('SELECT 1'); next(); }
  catch { res.status(503).json({ error: 'Database unavailable.' }); }
});

// Verified Seed Reciters Catalog
const SEED_RECITERS = [
  { slug: "mishary", nameEn: "Mishary Rashid Alafasy", nameAr: "مشاري راشد العفاسي", country: "Kuwait 🇰🇼", style: "Murattal", riwayah: "Hafs", audioFolder: "Alafasy_64kbps", verified: true, serverUrl: "https://server8.mp3quran.net/afs/" },
  { slug: "al-sudais", nameEn: "Abdul Rahman Al-Sudais", nameAr: "عبد الرحمن السديس", country: "Saudi Arabia 🇸🇦", style: "Murattal", riwayah: "Hafs", audioFolder: "Sudais_192kbps", verified: true, serverUrl: "https://server11.mp3quran.net/sds/" },
  { slug: "al-muaiqly", nameEn: "Maher Al-Muaiqly", nameAr: "ماهر المعيقلي", country: "Saudi Arabia 🇸🇦", style: "Murattal", riwayah: "Hafs", audioFolder: "Maher_AlMuaiqly_64kbps", verified: true, serverUrl: "https://server12.mp3quran.net/maher/" },
  { slug: "al-dossari", nameEn: "Yasser Al-Dossari", nameAr: "ياسر الدوسري", country: "Saudi Arabia 🇸🇦", style: "Murattal", riwayah: "Hafs", audioFolder: "Yasser_Ad-Dussary_128kbps", verified: true, serverUrl: "https://server11.mp3quran.net/yasser/" },
  { slug: "abdul-basit", nameEn: "Abdul Basit Abdul Samad", nameAr: "عبد الباسط عبد الصمد", country: "Egypt 🇪🇬", style: "Mujawwad", riwayah: "Hafs", audioFolder: "Abdul_Basit_Mujawwad_128kbps", verified: true, serverUrl: "https://server7.mp3quran.net/basit/Almusshaf-Al-Mojawwad/" },
  { slug: "al-minshawi", nameEn: "Muhammad Siddiq Al-Minshawi", nameAr: "محمد صديق المنشاوي", country: "Egypt 🇪🇬", style: "Murattal", riwayah: "Hafs", audioFolder: "Minshawy_Murattal_128kbps", verified: true, serverUrl: "https://server10.mp3quran.net/minsh/" },
  { slug: "al-husary", nameEn: "Mahmoud Khalil Al-Husary", nameAr: "محمود خليل الحصري", country: "Egypt 🇪🇬", style: "Murattal", riwayah: "Hafs", audioFolder: "Husary_128kbps", verified: true, serverUrl: "https://server13.mp3quran.net/husr/" },
  { slug: "islam-sobhi", nameEn: "Islam Sobhi", nameAr: "إسلام صبحي", country: "Egypt 🇪🇬", style: "Murattal", riwayah: "Hafs", audioFolder: "Islam_Sobhi_128kbps", verified: true, serverUrl: "https://server14.mp3quran.net/islam/Rewayat-Hafs-A-n-Assem/" },
  { slug: "saad-alghamdi", nameEn: "Saad Al-Ghamdi", nameAr: "سعد الغامدي", country: "Saudi Arabia 🇸🇦", style: "Murattal", riwayah: "Hafs", audioFolder: "Ghamadi_40kbps", verified: true, serverUrl: "https://server7.mp3quran.net/s_gmd/" },
  { slug: "omar-hisham", nameEn: "Omar Hisham Al-Arabi", nameAr: "عمر هشام العربي", country: "Egypt 🇪🇬", style: "Murattal", riwayah: "Hafs", audioFolder: "Omar_Hisham_128kbps", verified: true, serverUrl: "https://archive.org/download/Omar-Hisham/" },
  { slug: "al-zain", nameEn: "Al-Zain Muhammad Ahmad", nameAr: "الزين محمد أحمد", country: "Sudan 🇸🇩", style: "Douri", riwayah: "Al-Douri", audioFolder: "Zain_128kbps", verified: true, serverUrl: "https://server9.mp3quran.net/alzain/" },
  { slug: "al-koushi", nameEn: "Al-Ouyoun Al-Koushi", nameAr: "العيون الكوشي", country: "Morocco 🇲🇦", style: "Warsh", riwayah: "Warsh", audioFolder: "Koushi_128kbps", verified: true, serverUrl: "https://server11.mp3quran.net/koshi/" },
  { slug: "al-dokali-muhammad-al-alem", nameEn: "Al-Dokali Muhammad Al-Alem", nameAr: "الدوكالي محمد العالم", country: "Libya 🇱🇾", style: "Qaloon", riwayah: "Qaloon", audioFolder: "Dokali_64kbps", verified: true, serverUrl: "https://server9.mp3quran.net/dokali/" },
  { slug: "muhammad-sayed", nameEn: "Muhammad Sayed", nameAr: "محمد سيد", country: "Egypt 🇪🇬", style: "Warsh", riwayah: "Warsh", audioFolder: "Sayed_64kbps", verified: true, serverUrl: "https://server16.mp3quran.net/m_sayed/" }
];

const SURAHS = [
  { id: 1, nameEn: "Al-Fatihah", nameAr: "الفاتحة", ayahs: 7 },
  { id: 2, nameEn: "Al-Baqarah", nameAr: "البقرة", ayahs: 286 },
  { id: 3, nameEn: "Ali 'Imran", nameAr: "آل عمران", ayahs: 200 },
  { id: 4, nameEn: "An-Nisa", nameAr: "النساء", ayahs: 176 },
  { id: 18, nameEn: "Al-Kahf", nameAr: "الكهف", ayahs: 110 },
  { id: 19, nameEn: "Maryam", nameAr: "مريم", ayahs: 98 },
  { id: 20, nameEn: "Taha", nameAr: "طه", ayahs: 135 },
  { id: 22, nameEn: "Al-Hajj", nameAr: "الحج", ayahs: 78 },
  { id: 36, nameEn: "Ya-Sin", nameAr: "يس", ayahs: 83 },
  { id: 55, nameEn: "Ar-Rahman", nameAr: "الرحمن", ayahs: 78 },
  { id: 56, nameEn: "Al-Waqi'ah", nameAr: "الواقعة", ayahs: 96 },
  { id: 67, nameEn: "Al-Mulk", nameAr: "الملك", ayahs: 30 },
  { id: 112, nameEn: "Al-Ikhlas", nameAr: "الإخلاص", ayahs: 4 },
  { id: 113, nameEn: "Al-Falaq", nameAr: "الفلق", ayahs: 5 },
  { id: 114, nameEn: "An-Nas", nameAr: "الناس", ayahs: 6 },
];

let editorialPlaylists = [
  { id: "pl_01", title: "Tahajjud Serenity", description: "Quiet late night recitations paired with rain and ocean waves.", tracksCount: 8, isPublic: true, plays: 12450, creator: "Ghais Editorial", coverUrl: "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=400&q=80" },
  { id: "pl_02", title: "Friday Surah Al-Kahf", description: "All-time favorite recitations of Surah Al-Kahf for Jumu'ah.", tracksCount: 6, isPublic: true, plays: 28900, creator: "Ghais Editorial", coverUrl: "https://images.unsplash.com/photo-1542838132-92c53300491e?w=400&q=80" },
  { id: "pl_03", title: "Deep Focus Quran", description: "Steady murattal pacing with subtle ambient birdsong for work & study.", tracksCount: 12, isPublic: true, plays: 18230, creator: "Ghais Editorial", coverUrl: "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=400&q=80" }
];

// ==========================================
// ROUTES
// ==========================================

// Every collection in APP_SCHEMA (19), in the same order as APP_SCHEMA and
// appwrite/collections.json. Order is the dashboard/Settings display order.
// Not order-critical (counts are keyed by id), but keep it in step with
// APP_SCHEMA so a new collection is never counted here but not displayed.
const COLLECTIONS_LIST = [
  'reciters',
  'surahs',
  'moods',
  'playlists',
  'playlist_items',
  'likes',
  'playback_state',
  'khatma_plans',
  'follows',
  'listening_stats',
  'schedules',
  'history',
  'user_prefs',
  'routine_backups',
  'reciter_stats',
  'broadcasts',
  'telemetry',
  'hifz_mastery',
  'search_history'
];

// PostgreSQL handles concurrent requests through its bounded connection pool.
async function awCall(factory) { return factory(); }
async function safeCall(factory) { return awCall(factory); }

async function fetchCollectionCounts() {
  let collectionCounts = {};
  let isConnected = false;
  let errorMsg = null;

  if (databaseAvailable && databases) {
    try {
      const results = await Promise.all(
        COLLECTIONS_LIST.map(async (colId) => {
          const res = await safeCall(
            () => databases.listDocuments(DATABASE_ID, colId, [Query.limit(1)]),
            { total: 0 }
          );
          return { colId, count: res.total ?? 0 };
        })
      );
      results.forEach(r => {
        collectionCounts[r.colId] = r.count;
      });
      isConnected = true;
    } catch (e) {
      errorMsg = e.message;
    }
  } else {
    COLLECTIONS_LIST.forEach(colId => {
      collectionCounts[colId] = 0;
    });
  }

  return { collectionCounts, isConnected, errorMsg };
}

// Health check with live Appwrite connection ping and document counts
app.get('/api/health', async (req, res) => {
  const { collectionCounts, isConnected, errorMsg } = await fetchCollectionCounts();

  res.json({
    status: 'online',
    timestamp: new Date().toISOString(),
    mode: isConnected ? 'live' : 'sandbox',
    endpoint: process.env.PUBLIC_API_URL || 'https://ghaith-c77c2426ed4d.herokuapp.com',
    projectId: 'ghais',
    databaseId: DATABASE_ID,
    hasApiKey: false,
    databaseConnected: isConnected,
    storageConfigured: Boolean(storage),
    totalCollections: COLLECTIONS_LIST.length,
    collectionCounts,
    errorMessage: errorMsg,
    totalIndexedReciters: collectionCounts.reciters ?? 0,
    totalSurahs: 114
  });
});

// Collections document counts endpoint
app.get('/api/settings/collections', async (req, res) => {
  const { collectionCounts, isConnected, errorMsg } = await fetchCollectionCounts();

  res.json({
    databaseId: DATABASE_ID,
    databaseConnected: isConnected,
    collections: COLLECTIONS_LIST.map(colId => ({
      id: colId,
      documentCount: collectionCounts[colId] ?? 0
    })),
    collectionCounts,
    errorMessage: errorMsg
  });
});

// Schema changes are applied explicitly by the release migration.
app.post('/api/settings/provision', (_req, res) => {
  res.status(410).json({ success: false, message: 'Run the PostgreSQL release migration to update schema.' });
});
app.post('/api/settings/test-connection', async (_req, res) => {
  try { await pool.query('SELECT 1'); res.json({ success: true, message: 'PostgreSQL is connected.' }); }
  catch { res.status(503).json({ success: false, message: 'PostgreSQL is unavailable.' }); }
});

// History schema helpers.
// Canonical (appwrite/collections.json): history = { user_id, track_json, played_at_ms, position_ms, updated_at } —
// it HAS played_at_ms and position_ms and has NO daily_json (daily_json lives on listening_stats).
// App writer (shared/.../SyncEngine.android.kt pushHistory + buildHistoryTrack) writes per doc:
//   { user_id, track_json (=Json.encodeToString(TrackItem)), played_at_ms, position_ms }
// TrackItem exact keys: reciterSlug, reciterName, surahId, surahNameEn, surahNameAr,
//   ayahNo (=0 for history), audioUrl, textUthmani (default ""), durationMs (often 0 = unknown).
// Admin therefore tolerates BOTH shapes and sorts by the canonical field.
function historyTs(h) {
  if (!h) return 0;
  const v = h.played_at_ms ?? h.updated_at ?? h.$updatedAt;
  if (typeof v === 'number' && v > 0) return v;
  const n = Number(v);
  if (Number.isFinite(n) && n > 0) return n;
  const c = h.$createdAt ? Date.parse(h.$createdAt) : NaN;
  return Number.isFinite(c) ? c : 0;
}

function parseTrack(s) {
  try { return JSON.parse(s || '{}') || {}; } catch (_) { return {}; }
}

function trackSurahId(tr) {
  return tr.surahId ?? tr.surah_id ?? tr.surahID ?? 1;
}

function trackSurahName(tr) {
  return tr.surahNameEn || tr.surahName || tr.surah_name || `Surah ${trackSurahId(tr)}`;
}

function trackReciterName(tr) {
  return tr.reciterName || tr.reciter_name || tr.reciter || 'Certified Reciter';
}

function trackReciterSlug(tr) {
  return tr.reciterSlug || tr.reciter_slug || tr.reciterSlugName || null;
}

// History list with ordering fallback. Live DBs carry played_at_ms (app
// writer); others may only have updated_at. Appwrite 400s on unknown
// attributes, so try each ordering in turn, then unordered + in-memory sort
// by historyTs() (which tolerates both shapes).
async function listHistoryDocs(extraQueries = []) {
  const orderings = [
    [Query.orderDesc('played_at_ms')],
    [Query.orderDesc('updated_at')],
    [],
  ];
  let lastErr = null;
  for (const order of orderings) {
    try {
      const res = await databases.listDocuments(
        DATABASE_ID, 'history', [...extraQueries, ...order]
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

// Executive Overview: Real database calculations.
// listening_stats fields read here (verified vs app SyncEngine STATS):
//   total_seconds (summed below). days_streak / minutes_today /
//   unique_surahs / unique_reciters / updated_at exist on the collection but
//   are consumed by /api/users, not this endpoint.
// Empty live DB degrades to zeros (no fabricated buffers); sandbox mode
// returns labeled demo values.
app.get('/api/overview', async (req, res) => {
  try {
    const isLive = true;
    let totalUsers = 0;
    let totalMinutesListened = 0;
    let totalLikes = 0;
    let totalFollows = 0;
    let followsBySlug = {};
    let activeSchedules = 0;
    let khatmaActive = 0;
    let realHistoryDocs = [];

    if (isLive) {
      // Fail visibly on DB errors instead of reporting zero users or activity.
      const history = [];
      for (let offset = 0;; offset += 500) {
        const page = await listHistoryDocs([Query.limit(500), Query.offset(offset)]);
        history.push(...page.documents);
        if (page.documents.length < 500) break;
      }
      const [uRes, secondsRes, lRes, fRes, followCounts, schRes, hRes, khRes] = await Promise.all([
        userStore.list([Query.limit(1)]),
        pool.query(`SELECT COALESCE(SUM(CASE WHEN doc->>'total_seconds' ~ '^[0-9]+$' THEN (doc->>'total_seconds')::numeric ELSE 0 END),0) AS total
          FROM (SELECT DISTINCT ON (owner) doc FROM listening_stats WHERE owner IS NOT NULL ORDER BY owner, updated_at DESC, id DESC) latest_stats`),
        databases.listDocuments(DATABASE_ID, 'likes', [Query.limit(1)]),
        databases.listDocuments(DATABASE_ID, 'follows', [Query.limit(1)]),
        pool.query(`WITH counts AS (
          SELECT doc->>'reciter_slug' AS slug, COUNT(DISTINCT owner)::int AS count
          FROM follows WHERE NULLIF(doc->>'reciter_slug','') IS NOT NULL GROUP BY 1
        ) SELECT counts.*, (SELECT doc FROM reciters WHERE doc->>'slug'=counts.slug ORDER BY id LIMIT 1) AS reciter FROM counts`),
        pool.query("SELECT COUNT(DISTINCT (owner, COALESCE(NULLIF(doc->>'schedule_id',''), id)))::int AS total FROM schedules WHERE owner IS NOT NULL AND COALESCE(doc->>'deleted','false') <> 'true' AND COALESCE(doc->>'enabled','true') = 'true'"),
        Promise.resolve({ documents: history }),
        databases.listDocuments(DATABASE_ID, 'khatma_plans', [Query.limit(1)]),
      ]);

      totalUsers = uRes.total ?? 0;
      totalLikes = lRes.total ?? 0;
      totalFollows = fRes.total ?? 0;
      followCounts.rows.forEach(f => { followsBySlug[f.slug] = f; });
      activeSchedules = schRes.rows[0]?.total ?? 0;
      khatmaActive = khRes.total ?? 0;
      realHistoryDocs = (hRes && hRes.documents) || [];

      // Sum all user seconds (real total_seconds only — no session buffer).
      totalMinutesListened = Math.round(Number(secondsRes.rows[0].total) / 60);
    }

    const totalHours = Math.round(totalMinutesListened / 60);

    // Compute top surahs from live history (TrackItem JSON: surahId,
    // surahNameEn, reciterName — verified vs app Models.kt TrackItem).
    const surahCounts = {};
    realHistoryDocs.forEach(h => {
      try {
        const tr = parseTrack(h.track_json);
        const sId = Number(tr.surahId ?? tr.surah_id ?? tr.surahID);
        if (Number.isInteger(sId) && sId >= 1 && sId <= 114) surahCounts[sId] = (surahCounts[sId] || 0) + 1;
      } catch (_) {}
    });

    // Live (incl. empty DB): raw counts, no inflation. Sandbox: demo plays.
    const topSurahs = FULL_SURAHS_LIST
      .map((s, i) => ({
        ...s,
        plays: isLive
          ? (surahCounts[s.id] || 0)
          : [320, 280, 240, 200, 160][i] ?? 120
      }))
      .filter(s => s.plays > 0)
      .sort((a, b) => b.plays - a.plays)
      .slice(0, 5);

    // Saved history is bounded and does not represent every listening session.
    const listeningTrends = Array.from({ length: 7 }, (_, i) => {
      const day = new Date(); day.setUTCHours(0, 0, 0, 0); day.setUTCDate(day.getUTCDate() - (6 - i));
      const start = day.getTime(), end = start + 86400000;
      const events = realHistoryDocs.filter(h => historyTs(h) >= start && historyTs(h) < end);
      return { day: day.toISOString().slice(0, 10), hours: 0, activeUsers: new Set(events.map(h => h.user_id).filter(Boolean)).size, sessions: events.length };
    });
    const activeToday = listeningTrends.at(-1)?.activeUsers ?? 0;

    const deltaSuffix = 'Synced from PostgreSQL';

    res.json({
      mode: 'live',
      kpis: {
        totalUsers: { value: totalUsers, delta: `${totalUsers} PostgreSQL users` },
        totalHoursListened: { value: totalHours, delta: deltaSuffix },
        activeListenersToday: { value: activeToday, delta: 'Users with saved history today (UTC)' },
        totalLikes: { value: isLive ? totalLikes : 0, delta: `${isLive ? totalLikes : 0} Ayah & Surah Likes` },
        totalFollows: { value: isLive ? totalFollows : 0, delta: `${isLive ? totalFollows : 0} Reciter Follows` },
        activeSchedules: { value: isLive ? activeSchedules : 0, delta: `${isLive ? activeSchedules : 0} Daily Alarms in DB` },
        khatmaActive: { value: khatmaActive, delta: `${khatmaActive} Khatma plans in DB` }
      },
      listeningTrends,
      routineDistribution: [],
      ambientUsage: [],
      topSurahs,
      topReciters: Object.values(followsBySlug).map(({slug, count, reciter}) => {
        const bundled = RECITER_BY_SLUG.get(slug);
        return { id: slug, slug, nameEn: reciter?.name_en || bundled?.nameEn || slug,
          nameAr: reciter?.name_ar || bundled?.nameAr || '', followers: count };
      }).filter(r => r.followers > 0).sort((a, b) => b.followers - a.followers).slice(0, 5)
    });
  } catch (e) {
    console.error('Error fetching overview:', e);
    res.status(500).json({ error: e.message });
  }
});

registerUsersRoutes(app, { pool, databases, databaseId: DATABASE_ID, userStore, listHistoryDocs, historyTs, parseTrack });

// Reciters Directory routes are handled by recitersSync.js


// Create a new playlist (writes directly to Appwrite Cloud if connected)
app.post('/api/playlists', async (req, res) => {
  try {
    const { owner_id = 'editorial', title, description = '', cover_url = '', is_public = true } = req.body || {};
    if (typeof title !== 'string' || !title.trim()) return res.status(400).json({ success: false, message: 'Playlist title is required.' });
    const doc = await databases.createDocument(DATABASE_ID, 'playlists', 'unique()', { owner_id, title: title.trim(), description, is_public, cover_url, publishedAt: Date.now(), qariSlugs: [] });
    return res.status(201).json({ success: true, playlist: { id: doc.$id, title: doc.title, description: doc.description, tracksCount: 0, isPublic: doc.is_public, creator: doc.owner_id === 'editorial' ? 'Ghais Editorial' : 'Community Listener', coverUrl: doc.cover_url }, document: doc });
  } catch (error) {
    return res.status(500).json({ success: false, message: 'Could not create playlist.' });
  }
});

// Real Playlists from Appwrite
app.get('/api/playlists', async (req, res) => {
  try {
    let livePlaylists = [];
    if (databaseAvailable && databases) {
      try {
        const [plRes, uRes, itemRes] = await Promise.all([
          databases.listDocuments(DATABASE_ID, 'playlists', [Query.limit(50)]),
          userStore.list([Query.limit(100)]).catch(() => ({ users: [] })),
          databases.listDocuments(DATABASE_ID, 'playlist_items', [Query.limit(200)]).catch(() => ({ documents: [] }))
        ]);

        const userMap = {};
        uRes.users.forEach(u => {
          userMap[u.$id] = u.name && u.name.trim() ? u.name : (u.email ? u.email.split('@')[0] : 'Community Listener');
        });

        const itemCountMap = {};
        itemRes.documents.forEach(item => {
          const plId = item.playlist_id;
          if (plId) {
            itemCountMap[plId] = (itemCountMap[plId] || 0) + 1;
          }
        });

        livePlaylists = plRes.documents.map(p => ({
          id: p.$id,
          ownerId: p.owner_id,
          title: p.title || 'Untitled Routine',
          description: p.description || 'Custom listener reflection sequence from Ghais',
          // Derived aggregate: count of playlist_items rows (no tracks_count in schema).
          tracksCount: itemCountMap[p.$id] || 0,
          isPublic: p.is_public ?? true,
          creator: userMap[p.owner_id] || (p.owner_id === 'editorial' ? 'Ghais Editorial' : 'Community Listener'),
          coverUrl: p.cover_url || 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=400&q=80'
        }));
      } catch (err) {
        throw err;
      }
    }

    res.json({
      curated: livePlaylists,
      userPlaylistsCount: livePlaylists.length,
      publicPlaylistsCount: livePlaylists.filter(p => p.isPublic).length
    });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Get tracks for a specific playlist
app.get('/api/playlists/:id/tracks', async (req, res) => {
  try {
    const { id } = req.params;
    let tracks = [];

    if (databaseAvailable && databases) {
      try {
        const response = await databases.listDocuments(
          DATABASE_ID,
          'playlist_items',
          [Query.equal('playlist_id', id), Query.orderAsc('position'), Query.limit(100)]
        );
        if (response.documents && response.documents.length > 0) {
          // Canonical item shape only: position, reciter_slug, surah_id,
          // ayah_from, ayah_to. Names/audio resolved via buildPlaylistTrackView.
          tracks = response.documents.map((doc, idx) => buildPlaylistTrackView(doc, idx));
          tracks.sort((a, b) => a.position - b.position);
        }
      } catch (e) {
        throw e;
      }
    }

    // Sandbox mocks keep their hand-written display values; normalize them
    // onto the canonical view shape (position + ayah range) for the UI.
    tracks = tracks.map((t, idx) => ({
      position: t.position ?? t.sequence ?? idx,
      sequence: t.sequence ?? t.position ?? idx,
      ayahFrom: t.ayahFrom ?? 1,
      ayahTo: t.ayahTo ?? t.ayahFrom ?? 1,
      ayahRange: t.ayahRange || 'Full surah',
      ...t,
    }));

    res.json({
      playlistId: id,
      total: tracks.length,
      tracks
    });
  } catch (e) {
    console.error('Error in GET /api/playlists/:id/tracks:', e);
    res.status(500).json({ error: e.message });
  }
});

// Delete a playlist by ID (plus its playlist_items children)
app.delete('/api/playlists/:id', async (req, res) => {
  let client;
  try {
    client = await pool.connect();
    const { id } = req.params;
    await client.query('BEGIN');
    await client.query("DELETE FROM playlist_items WHERE doc->>'playlist_id' = $1", [id]);
    await client.query('DELETE FROM playlists WHERE id = $1', [id]);
    await client.query('COMMIT');
    res.json({ success: true, id });
  } catch (e) {
    if (client) await client.query('ROLLBACK').catch(() => {});
    console.error('Error deleting playlist:', e);
    res.status(500).json({ success: false, error: e.message });
  } finally { client?.release(); }
});

registerScheduleDeletion(app, pool);


registerActivityRoutes(app, pool);

// Saved listening history is a partial snapshot, not a complete play log.
app.get('/api/analytics', async (req, res) => {
  try {
    res.json(await getListeningAnalytics(pool));
  } catch (e) {
    console.error('Listening analytics unavailable:', e.message);
    res.status(503).json({ error: 'Listening analytics are unavailable.' });
  }
});

registerExportRoutes(app, () => ({
  users: userStore,
  databases,
  pool,
  databaseId: DATABASE_ID,
  databaseAvailable
}));

registerIndexAudit(app, () => ({
  users: userStore,
  databases,
  databaseId: DATABASE_ID,
  databaseAvailable
}));

registerBroadcastRoutes(app, () => ({
  users: userStore,
  databases,
  databaseId: DATABASE_ID,
  databaseAvailable
}));

registerPlaybackRoutes(app, () => ({
  users: userStore,
  databases,
  databaseId: DATABASE_ID,
  databaseAvailable
}));

registerTelemetry(app, {
  getDatabases: () => databases,
  getDatabaseId: () => DATABASE_ID
});

registerSurahsRoutes(app, () => ({
  users: userStore,
  databases,
  databaseId: DATABASE_ID,
  databaseAvailable
}));
setupRecitersSync(app, databases, DATABASE_ID, storage);
registerPlaylistEditing(app, pool);
registerCdnHealth(app);

registerSeedRoutes(app, () => ({
  users: userStore,
  databases,
  databaseId: DATABASE_ID,
  databaseAvailable
}));

registerMoodsRoutes(app, () => ({
  users: userStore,
  databases,
  databaseId: DATABASE_ID,
  databaseAvailable
}));

registerArchiveRoutes(app, () => ({
  databases,
  storage,
  databaseId: DATABASE_ID,
  databaseAvailable,
}));

registerChartsAggregate(app, () => ({
  databases,
  databaseId: DATABASE_ID,
  databaseAvailable
}));

// GET /api/charts/week — read-through of the stored 7d window, mirroring the
// GET /api/charts/yesterday route in chartsAggregate.js (kept untouched for
// older clients). The consumer app's "Weekly most heard" reads the stored
// topQaris7d array; the response key topQarisWeek names the SURFACE, not the
// stored field. Same shape: { ok, topQarisWeek: [...], generatedAt }.
app.get('/api/charts/week', async (req, res) => {
  try {
    if (!databases) {
      return res.status(503).json({ ok: false, error: 'PostgreSQL is not configured.' });
    }
    const doc = await databases.getDocument(DATABASE_ID, CHARTS_COLLECTION, CHARTS_DOCUMENT_ID);
    return res.json({
      ok: true,
      topQarisWeek: Array.isArray(doc.topQaris7d) ? doc.topQaris7d : [],
      generatedAt: doc.generatedAt || null,
    });
  } catch (e) {
    if (e && (e.code === 404 || e.type === 'document_not_found')) {
      return res.json({ ok: true, topQarisWeek: [], generatedAt: null });
    }
    console.error('Error in GET /api/charts/week:', e?.message || e);
    return res.status(500).json({ ok: false, error: e?.message || 'lookup failed' });
  }
});

// Charts are refreshed by the mobile backend’s scheduled job.

app.use((error, req, res, next) => {
  if (/^\/api\/reciters\/[^/]+\/image$/.test(req.path)) {
    res.status(400).json({ error: error.message || 'Image upload failed.' });
    return;
  }
  next(error);
});

app.use('/api', (_req, res) => res.status(404).json({ error: 'API route not found.' }));
app.use(express.static(path.join(__dirname, '../dist')));
app.get('*', (_req, res) => res.sendFile(path.join(__dirname, '../dist/index.html')));

export { app };
if (process.argv[1] && path.resolve(process.argv[1]) === __filename) {
  app.listen(PORT, () => {
    console.log(`🚀 Ghais Admin API server running on http://localhost:${PORT}`);
  });
}
