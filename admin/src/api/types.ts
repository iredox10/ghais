/**
 * TypeScript definitions for Quranify / Ghais Admin Portal
 */

// ============================================================================
// Overview & KPIs
// ============================================================================

export interface KPIItem {
  value: number;
  delta: string;
}

export interface OverviewKPIs {
  totalUsers: KPIItem;
  totalHoursListened: KPIItem;
  activeListenersToday: KPIItem;
  totalLikes: KPIItem;
  totalFollows: KPIItem;
  activeSchedules: KPIItem;
  khatmaActive: KPIItem;
}

export interface ListeningTrend {
  day: string;
  hours: number;
  activeUsers: number;
  sessions: number;
}

export interface RoutineDistribution {
  name: string;
  value: number;
  color: string;
}

export interface AmbientUsage {
  name: string;
  percentage: number;
}

export interface TopSurah {
  id: number;
  nameEn: string;
  nameAr: string;
  plays: number;
  ayahs: number;
}

export interface TopReciter {
  id: string;
  slug: string;
  nameEn: string;
  nameAr: string;
  country?: string;
  style?: string;
  riwayah?: string;
  serverUrl?: string;
  audioFolder?: string;
  followers: number;
  likes?: number;
  verified?: boolean;
}

export interface OverviewData {
  kpis: OverviewKPIs;
  listeningTrends: ListeningTrend[];
  routineDistribution: RoutineDistribution[];
  ambientUsage: AmbientUsage[];
  topSurahs: TopSurah[];
  topReciters: TopReciter[];
}

// ============================================================================
// Users & User Details
// ============================================================================

export type AuthProvider = 'google' | 'email' | string;

export interface User {
  id: string;
  name: string;
  email: string;
  provider: AuthProvider;
  createdAt: string;
  totalMinutes: number;
  daysStreak: number;
  uniqueSurahs: number;
  schedulesCount: number;
  playlistsCount: number;
  lastActive: string;
}

export interface UsersResponse {
  total: number;
  users: User[];
}

export interface UserStats {
  totalHours: number;
  streak: number;
  topReciter: string;
  favoriteSurah: string;
  preferredAmbient: string;
}

export interface UserSchedule {
  id: string;
  time: string;
  label: string;
  reciter: string;
  surahs: string;
  enabled: boolean;
}

export interface UserPlaylistSummary {
  id: string;
  title: string;
  tracks: number;
  createdAt: string;
}

export interface UserHistoryItem {
  id: string;
  surahName: string;
  reciterName: string;
  duration: string;
  playedTime: string;
  surahNumber?: number | null;
}

export interface UserDetails {
  user: User;
  stats: UserStats;
  schedules: UserSchedule[];
  playlists: UserPlaylistSummary[];
  history: UserHistoryItem[];
}

// ============================================================================
// Reciters
// ============================================================================

export interface Reciter {
  id: string;
  slug: string;
  nameEn: string;
  nameAr: string;
  country: string;
  style: string;
  riwayah: string;
  tempo: string;
  audioFolder: string;
  serverUrl: string;
  availableSurahs: string;
  description: string;
  isTeacher: boolean;
  imageUrl: string;
  imageFileId: string;
  followers: number;
  likes: number;
  verified: boolean;
  enabled: boolean;
}

export interface RecitersResponse {
  total: number;
  reciters: Reciter[];
}

export interface CreateReciterInput {
  slug: string;
  nameEn: string;
  nameAr: string;
  country?: string;
  style?: string;
  riwayah?: string;
  tempo?: string;
  audioFolder?: string;
  serverUrl?: string;
  availableSurahs?: string;
  description?: string;
  isTeacher?: boolean;
  imageUrl?: string;
  verified?: boolean;
  enabled?: boolean;
}

export type UpdateReciterInput = Partial<CreateReciterInput>;

export interface ReciterMutationResponse {
  success: boolean;
  reciter: Reciter;
}

export interface ReciterImageResponse {
  success: boolean;
  imageUrl: string;
  imageFileId: string;
  reciter: Reciter;
}

export type ArchiveScheme = 'canonical' | 'numbered-name' | 'name-only' | 'unknown';

export interface ArchiveEntry {
  surahId: number;
  surahNameEn: string;
  surahNameAr: string;
  fileName: string;
  filePath: string;
  url: string;
  sizeBytes: number;
  matchedBy: 'number' | 'name' | 'folder-number';
}

export interface ArchiveMetadata {
  title?: string;
  creator?: string;
  description?: string;
  subject?: string;
  language?: string;
  mediatype?: string;
  uploader?: string;
  addeddate?: string;
  publicdate?: string;
}

export interface ArchiveReciterDraft {
  slug: string;
  catalog: string;
  catalog_key: string;
  name_en: string;
  name_ar: string;
  riwayah: string;
  style: string;
  tempo: string;
  audio_folder: string;
  country: string;
  server_url: string;
  available_surahs: string;
  description: string;
  is_teacher: boolean;
  enabled: boolean;
  verified: boolean;
}

export interface ArchiveInspectResponse {
  success: true;
  itemId: string;
  metadata: ArchiveMetadata;
  scheme: ArchiveScheme;
  entries: ArchiveEntry[];
  matched: number;
  total: number;
  missingIds: number[];
  warnings: string[];
  suggested: ArchiveReciterDraft;
}

export interface ArchiveImportResponse {
  success: true;
  reciter: Reciter;
}

export interface ArchiveHealthResponse {
  success: boolean;
  reachable: boolean;
  checkedAt: string;
}

// ============================================================================
// Surahs
// ============================================================================

export interface SurahItem {
  id: number;
  number: number;
  nameEn: string;
  nameAr: string;
  translation: string;
  ayahs: number;
  revelationType: 'Meccan' | 'Medinan' | string;
}

// Backwards-compatible alias (Surahs page imports `Surah`)
export type Surah = SurahItem;

export interface SurahsResponse {
  total: number;
  surahs: SurahItem[];
}

export interface ReciterStream {
  slug: string;
  nameEn: string;
  nameAr: string;
  country: string;
  style: string;
  riwayah: string;
  streamUrl: string;
}

export interface SurahDetail extends SurahItem {
  reciters: ReciterStream[];
}

// ============================================================================
// Moods
// ============================================================================

export interface Mood {
  slug: string;
  title: string;
  subtitle: string;
  category: string;
  default_reciter_slug: string;
  default_ambient: string;
  cover_url: string;
}

export interface MoodsResponse {
  total: number;
  moods: Mood[];
  source?: string;
}

// ============================================================================
// Seed
// ============================================================================

export interface SeedResponse {
  success: boolean;
  seeded: number;
  skipped: number;
  total: number;
}

// ============================================================================
// Playlists
// ============================================================================

export interface Playlist {
  ownerId?: string;
  id: string;
  title: string;
  description: string;
  tracksCount: number;
  isPublic: boolean;
  plays: number;
  creator: string;
  coverUrl: string;
}

export interface PlaylistsResponse {
  curated: Playlist[];
  userPlaylistsCount: number;
  publicPlaylistsCount: number;
}

export interface CreatePlaylistInput {
  title: string;
  description: string;
  coverUrl?: string;
}

export interface CreatePlaylistResponse {
  success: boolean;
  playlist: Playlist;
}

export interface EditablePlaylistTrack {
  reciterSlug: string;
  surahId: number;
  ayahFrom: number;
  ayahTo: number;
}

export interface PlaylistTrack {
  id: string;
  sequence: number;
  surahId: number;
  surahName: string;
  reciterName: string;
  reciterSlug: string;
  ambientPairing: string;
  duration: string;
  audioUrl?: string;
}

export interface PlaylistTracksResponse {
  playlistId: string;
  total: number;
  tracks: PlaylistTrack[];
}

// ============================================================================
// Activity Events
// ============================================================================

export type ActivityType =
  | 'listen'
  | 'like'
  | 'schedule'
  | 'playlist'
  | 'streak'
  | 'follow'
  | string;

export interface ActivityEvent {
  id: string;
  user: string;
  action: string;
  reciter: string;
  surah: string;
  ambient: string;
  time: string;
  type: ActivityType;
}

export interface ActivityResponse {
  activities: ActivityEvent[];
}

// ============================================================================
// Analytics
// ============================================================================

export interface PeakHour {
  hour: string;
  index: number;
}

export interface HourlyCyclePoint {
  hour: string;
  hourNumber: number;
  volume: number;
  listeners: number;
  annotation?: string | null;
}

export interface SurahCompletionMetric {
  surahNumber: number;
  nameEn: string;
  nameAr: string;
  totalVerses: number;
  completionRate: number;
  totalPlays: number;
  avgDurationMins: number;
  contextTag: string;
}

export interface AyahRepetitionMetric {
  surah: string;
  ayah: string;
  nameEn: string;
  nameAr: string;
  repeatCount: number;
  repeatRatePct: number;
  primaryMode: string;
  avgRepeatsPerSession: number;
}

export interface RoutineModeMetric {
  mode: string;
  displayName: string;
  totalHours: number;
  percentage: number;
  avgSessionMinutes: number;
  dailyActiveSessions: number;
  color: string;
  primaryAmbient: string;
  description: string;
}

export interface DeviceDistribution {
  name: string;
  value: number;
}

export interface RetentionMetrics {
  day1: number;
  day7: number;
  day30: number;
}

export interface ReciterStyleDistribution {
  name: string;
  percentage: number;
}

export interface AnalyticsData {
  peakHours: PeakHour[];
  source: string;
  timezone: string;
  sampleSize: number;
  skipped: number;
  hourlyCycle?: HourlyCyclePoint[];
  surahCompletion?: SurahCompletionMetric[];
  ayahRepetitions?: AyahRepetitionMetric[];
  routineAnalytics?: RoutineModeMetric[];
  deviceDistribution: DeviceDistribution[];
  retention: RetentionMetrics | null;
  reciterStyleDistribution: ReciterStyleDistribution[];
}

// ============================================================================
// Health & Settings
// ============================================================================

export interface HealthStatus {
  status: string;
  timestamp?: string;
  mode: 'live' | 'sandbox' | string;
  endpoint: string;
  projectId: string;
  databaseId: string;
  hasApiKey?: boolean;
  databaseConnected: boolean;
  storageConfigured: boolean;
  errorMessage?: string | null;
  totalIndexedReciters: number;
  totalSurahs?: number;
}

export interface TestConnectionRequest {
  apiKey: string;
  endpoint?: string;
  projectId?: string;
  databaseId?: string;
}

export interface TestConnectionResponse {
  success: boolean;
  message: string;
}

export interface CollectionsResponse {
  databaseId: string;
  databaseConnected: boolean;
  collections: Array<{ id: string; documentCount: number }>;
  collectionCounts: Record<string, number>;
  errorMessage?: string | null;
}

export interface ProvisionResponse {
  success: boolean;
  message: string;
  collectionCounts?: Record<string, number>;
  databaseConnected?: boolean;
}

export interface TelemetryData {
  pingMs: number | null;
  uptimeSeconds: number;
  memoryMb: number;
  nodeVersion: string;
  platform: string;
  activeConnections: number;
  deviceTelemetry?: { available: boolean; devices: number; byPlatform: Record<string, number> };
}

export interface PlaybackSession {
  id: string;
  userId: string;
  userName: string;
  userAvatar: string;
  surahName: string;
  surahId: number;
  reciter: string;
  positionSeconds: number;
  durationSeconds: number;
  deviceType: string;
  updatedAt: string;
}

export interface PlaybackResponse {
  total: number;
  sessions: PlaybackSession[];
}

export interface BroadcastItem {
  id: string;
  title: string;
  body: string;
  urgency: string;
  targetTopic: string;
  timestamp: string;
}

export interface BroadcastsResponse {
  success: boolean;
  broadcasts: BroadcastItem[];
}

export interface BroadcastInput {
  title: string;
  body: string;
  urgency: string;
  targetTopic: string;
}

export interface BroadcastResponse {
  success: boolean;
  broadcastId: string;
  timestamp: string;
}

export interface CdnServerStatus {
  name: string;
  url: string;
  pingMs: number;
  status: 'healthy' | 'degraded' | 'down';
  httpStatus: number;
  reciters?: number;
}

export interface CdnHealthResponse {
  servers: CdnServerStatus[];
}

export interface IndexAuditItem {
  collectionId: string;
  name: string;
  documentCount: number;
  recommendedIndexes: Array<{ key: string; type: string; order?: string }>;
  existingIndexes: Array<{ key: string; type: string; attributes?: string[] }>;
  coverageStatus: string;
  missingIndexes: string[];
}

export interface IndexAuditResponse {
  collectionsAudit: IndexAuditItem[];
}




