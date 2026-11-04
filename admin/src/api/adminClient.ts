import type {
  OverviewData,
  UsersResponse,
  UserDetails,
  RecitersResponse,
  CreateReciterInput,
  UpdateReciterInput,
  ReciterMutationResponse,
  ReciterImageResponse,
  PlaylistsResponse,
  CreatePlaylistInput,
  CreatePlaylistResponse,
  ActivityResponse,
  AnalyticsData,
  HealthStatus,
  TestConnectionRequest,
  TestConnectionResponse,
  CollectionsResponse,
  ProvisionResponse,
  PlaylistTracksResponse,
  TelemetryData,
  BroadcastInput,
  BroadcastResponse,
  BroadcastsResponse,
  PlaybackResponse,
  SurahsResponse,
  SurahDetail,
  MoodsResponse,
  SeedResponse,
  CdnHealthResponse,
  IndexAuditResponse,
  ArchiveInspectResponse,
  ArchiveImportResponse,
  ArchiveHealthResponse,
  ArchiveReciterDraft,
} from './types';

export class ApiClientError extends Error {
  public status: number;
  public statusText: string;
  public data?: unknown;

  constructor(message: string, status: number, statusText: string, data?: unknown) {
    super(message);
    this.name = 'ApiClientError';
    this.status = status;
    this.statusText = statusText;
    this.data = data;
  }
}

export const adminBasePath = import.meta.env.BASE_URL.replace(/\/$/, '');
const API_BASE_URL =
  (typeof import.meta !== 'undefined' &&
    (import.meta as unknown as { env?: Record<string, string | undefined> })?.env?.VITE_API_BASE_URL) ||
  adminBasePath;

/**
 * Universal JSON fetch helper with standard error normalization
 */
async function apiRequest<T>(endpoint: string, options?: RequestInit): Promise<T> {
  const url = `${API_BASE_URL}${endpoint}`;
  const isFormData = typeof FormData !== 'undefined' && options?.body instanceof FormData;
  const config: RequestInit = {
    headers: isFormData
      ? options?.headers
      : {
          'Content-Type': 'application/json',
          ...options?.headers,
        },
    ...options,
  };

  try {
    const res = await fetch(url, config);

    if (!res.ok) {
      let errorData: any = null;
      try {
        errorData = await res.json();
      } catch {
        try {
          errorData = await res.text();
        } catch {
          errorData = null;
        }
      }

      const message =
        typeof errorData === 'object' && errorData !== null && errorData.message
          ? errorData.message
          : typeof errorData === 'object' && errorData !== null && errorData.error
          ? errorData.error
          : `Request to ${endpoint} failed with status ${res.status} (${res.statusText})`;

      throw new ApiClientError(message, res.status, res.statusText, errorData);
    }

    return (await res.json()) as T;
  } catch (err) {
    if (err instanceof ApiClientError) {
      throw err;
    }
    throw new ApiClientError(
      (err as Error).message || 'Network request failed',
      0,
      'NETWORK_ERROR',
      err
    );
  }
}

// ============================================================================
// Endpoint API Functions
// ============================================================================

/**
 * GET /api/overview
 * Overview KPI cards, listening trends, routine distribution, and top surahs
 */
export async function getOverview(): Promise<OverviewData> {
  return apiRequest<OverviewData>('/api/overview');
}

/**
 * GET /api/users
 * Directory of registered users and engagement summaries
 */
export async function getUsers(): Promise<UsersResponse> {
  return apiRequest<UsersResponse>('/api/users');
}

/**
 * GET /api/users/:id
 * In-depth listener profile, stats, listening schedules, and custom playlists
 */
export async function getUserDetails(id: string): Promise<UserDetails> {
  return apiRequest<UserDetails>(`/api/users/${encodeURIComponent(id)}`);
}

/**
 * GET /api/reciters
 * Directory of reciters, follower metrics, audio paths, and verified status
 */
export async function getReciters(): Promise<RecitersResponse> {
  return apiRequest<RecitersResponse>('/api/reciters');
}

/**
 * POST /api/reciters
 * Index a new reciter or update metadata
 */
export async function createReciter(data: CreateReciterInput): Promise<ReciterMutationResponse> {
  return apiRequest<ReciterMutationResponse>('/api/reciters', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function updateReciter(id: string, data: UpdateReciterInput): Promise<ReciterMutationResponse> {
  return apiRequest<ReciterMutationResponse>(`/api/reciters/${encodeURIComponent(id)}`, {
    method: 'PATCH',
    body: JSON.stringify(data),
  });
}

export async function deleteReciter(id: string): Promise<{ success: boolean; id: string }> {
  return apiRequest<{ success: boolean; id: string }>(`/api/reciters/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  });
}

export async function uploadReciterImage(id: string, file: File): Promise<ReciterImageResponse> {
  const body = new FormData();
  body.append('image', file);
  return apiRequest<ReciterImageResponse>(`/api/reciters/${encodeURIComponent(id)}/image`, {
    method: 'POST',
    body,
  });
}

export async function syncRecitersCatalog(): Promise<{ success: boolean; message: string; synced: number; total: number }> {
  return apiRequest<{ success: boolean; message: string; synced: number; total: number }>('/api/reciters/sync-catalog', {
    method: 'POST',
  });
}

/**
 * POST /api/archive/inspect
 * Inspect an Internet Archive item and suggest a reciter draft
 */
export async function inspectArchive(input: {
  url?: string;
  itemId?: string;
}): Promise<ArchiveInspectResponse> {
  return apiRequest<ArchiveInspectResponse>('/api/archive/inspect', {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

/**
 * POST /api/archive/import
 * Import an inspected archive item as a reciter
 */
export async function importArchive(input: {
  itemId: string;
  reciter: ArchiveReciterDraft;
  image?: File | null;
}): Promise<ArchiveImportResponse> {
  const body = new FormData();
  body.append('itemId', input.itemId);
  body.append('reciter', JSON.stringify(input.reciter));
  if (input.image) {
    body.append('image', input.image);
  }
  return apiRequest<ArchiveImportResponse>('/api/archive/import', {
    method: 'POST',
    body,
  });
}

/**
 * GET /api/archive/health
 * Check Internet Archive connectivity
 */
export async function getArchiveHealth(): Promise<ArchiveHealthResponse> {
  return apiRequest<ArchiveHealthResponse>('/api/archive/health');
}

/**
 * GET /api/surahs
 * Catalog of all 114 Surahs
 */
export async function getSurahs(): Promise<SurahsResponse> {
  return apiRequest<SurahsResponse>('/api/surahs');
}

/**
 * GET /api/surahs/:id
 * Surah details with stream URLs for top reciters
 */
export async function getSurahDetails(id: number | string): Promise<SurahDetail> {
  return apiRequest<SurahDetail>(`/api/surahs/${encodeURIComponent(id)}`);
}

/**
 * GET /api/moods
 * Catalog of mood presets with default reciter and ambient pairings
 */
export async function getMoods(): Promise<MoodsResponse> {
  return apiRequest<MoodsResponse>('/api/moods');
}

/**
 * POST /api/seed/surahs
 * Seed the surahs catalog
 */
export async function seedSurahs(): Promise<SeedResponse> {
  return apiRequest<SeedResponse>('/api/seed/surahs', {
    method: 'POST',
  });
}

/**
 * POST /api/seed/moods
 * Seed the moods catalog
 */
export async function seedMoods(): Promise<SeedResponse> {
  return apiRequest<SeedResponse>('/api/seed/moods', {
    method: 'POST',
  });
}


/**
 * GET /api/playlists
 * Curated and user playlists with aggregate counts
 */
export async function getPlaylists(): Promise<PlaylistsResponse> {
  return apiRequest<PlaylistsResponse>('/api/playlists');
}

/**
 * POST /api/playlists
 * Create a new curated editorial playlist
 * NOTE: server expects snake_case (cover_url / owner_id / is_public)
 */
export async function createPlaylist(data: CreatePlaylistInput): Promise<CreatePlaylistResponse> {
  return apiRequest<CreatePlaylistResponse>('/api/playlists', {
    method: 'POST',
    body: JSON.stringify({
      owner_id: 'editorial',
      title: data.title,
      description: data.description,
      cover_url: data.coverUrl,
      is_public: true,
    }),
  });
}

/**
 * DELETE /api/playlists/:id
 * Delete a playlist by ID
 * NOTE: server returns { success: true } with no id
 */
export async function deletePlaylist(id: string): Promise<{ success: boolean; id?: string }> {
  return apiRequest<{ success: boolean; id?: string }>(`/api/playlists/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  });
}

/**
 * GET /api/playlists/:id/tracks
 * Fetch tracklist for a specific playlist
 */
export async function getPlaylistTracks(id: string): Promise<PlaylistTracksResponse> {
  return apiRequest<PlaylistTracksResponse>(`/api/playlists/${encodeURIComponent(id)}/tracks`);
}

/**
 * DELETE /api/schedules/:id
 * Delete a recitation schedule / alarm by ID
 */
export async function deleteSchedule(id: string): Promise<{ success: boolean }> {
  return apiRequest<{ success: boolean }>(`/api/schedules/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  });
}


/**
 * GET /api/activity
 * Real-time event stream of recent listener interactions
 */
export async function getActivity(): Promise<ActivityResponse> {
  return apiRequest<ActivityResponse>('/api/activity');
}

/**
 * GET /api/analytics
 * In-depth analytics: peak hours, retention metrics, and reciter styles
 */
export async function getAnalytics(): Promise<AnalyticsData> {
  return apiRequest<AnalyticsData>('/api/analytics');
}

/**
 * GET /api/health
 * Appwrite connection check, cluster mode, and index statistics
 */
export async function getHealth(): Promise<HealthStatus> {
  return apiRequest<HealthStatus>('/api/health');
}

/**
 * POST /api/settings/test-connection
 * Verify Appwrite API key credentials against the production endpoint
 */
export async function testConnection(payload: TestConnectionRequest): Promise<TestConnectionResponse> {
  return apiRequest<TestConnectionResponse>('/api/settings/test-connection', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

/**
 * GET /api/settings/collections
 * Fetch document counts across all database collections
 */
export async function getCollections(): Promise<CollectionsResponse> {
  return apiRequest<CollectionsResponse>('/api/settings/collections');
}

/**
 * POST /api/settings/provision
 * Provision missing collections and schema attributes
 */
export async function provisionCollections(): Promise<ProvisionResponse> {
  return apiRequest<ProvisionResponse>('/api/settings/provision', {
    method: 'POST',
  });
}

/**
 * GET /api/telemetry
 * Retrieve system telemetry and cluster performance metrics
 */
export async function getTelemetry(): Promise<TelemetryData> {
  return apiRequest<TelemetryData>('/api/telemetry');
}

/**
 * GET /api/broadcast
 * Retrieve broadcast history
 */
export async function getBroadcasts(): Promise<BroadcastsResponse> {
  return apiRequest<BroadcastsResponse>('/api/broadcast');
}

/**
 * POST /api/broadcast
 * Dispatch a new in-app broadcast & push notification
 */
export async function sendBroadcast(data: BroadcastInput): Promise<BroadcastResponse> {
  return apiRequest<BroadcastResponse>('/api/broadcast', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

/**
 * DELETE /api/broadcast/:id
 * Delete a broadcast by ID
 */
export async function deleteBroadcast(id: string): Promise<{ success: boolean }> {
  return apiRequest<{ success: boolean }>(`/api/broadcast/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  });
}

/**
 * GET /api/playback
 * Retrieve live active playback sessions radar data
 */
export async function getPlayback(): Promise<PlaybackResponse> {
  return apiRequest<PlaybackResponse>('/api/playback');
}

/**
 * GET /api/health/cdn
 * Benchmark audio CDN latency and status
 */
export async function getCdnHealth(): Promise<CdnHealthResponse> {
  return apiRequest<CdnHealthResponse>('/api/health/cdn');
}

/**
 * GET /api/settings/indexes
 * Audit database collections and index coverage status
 */
export async function getIndexAudit(): Promise<IndexAuditResponse> {
  return apiRequest<IndexAuditResponse>('/api/settings/indexes');
}

/**
 * Aggregated client object for convenience
 */
export const adminClient = {
  getOverview,
  getUsers,
  getUserDetails,
  getReciters,
  createReciter,
  updateReciter,
  deleteReciter,
  uploadReciterImage,
  syncRecitersCatalog,
  inspectArchive,
  importArchive,
  getArchiveHealth,
  getSurahs,
  getSurahDetails,
  getMoods,
  seedSurahs,
  seedMoods,
  getPlaylists,
  createPlaylist,
  deletePlaylist,
  getPlaylistTracks,
  deleteSchedule,
  getActivity,
  getAnalytics,
  getHealth,
  testConnection,
  getCollections,
  provisionCollections,
  getTelemetry,
  getBroadcasts,
  sendBroadcast,
  deleteBroadcast,
  getPlayback,
  getCdnHealth,
  getIndexAudit,
};

