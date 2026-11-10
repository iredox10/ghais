import {
  QueryClient,
  useQuery,
  useMutation,
  useQueryClient,
  type UseQueryOptions,
  type UseMutationOptions,
} from '@tanstack/react-query';
export { useQueryClient };

import {
  getOverview,
  getUsers,
  getUserDetails,
  getReciters,
  createReciter,
  updateReciter,
  deleteReciter,
  uploadReciterImage,
  syncRecitersCatalog,
  getPlaylists,
  createPlaylist,
  deletePlaylist,
  deleteSchedule,
  getActivity,
  getAnalytics,
  getHealth,
  testConnection,
  getCollections,
  provisionCollections,
  getPlaylistTracks,
  getTelemetry,
  getBroadcasts,
  sendBroadcast,
  getMoods,
  seedSurahs,
  seedMoods,
  deleteBroadcast,
  getPlayback,
  getSurahs,
  getSurahDetails,
  getCdnHealth,
  getIndexAudit,
  inspectArchive,
  importArchive,
  getArchiveHealth,
  ApiClientError,
} from '../api/adminClient';
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
  BroadcastsResponse,
  BroadcastResponse,
  MoodsResponse,
  SeedResponse,
  PlaybackResponse,
  SurahsResponse,
  SurahDetail,
  CdnHealthResponse,
  IndexAuditResponse,
  ArchiveInspectResponse,
  ArchiveImportResponse,
  ArchiveHealthResponse,
  ArchiveReciterDraft,
} from '../api/types';

// ============================================================================
// Query Client Configuration
// ============================================================================

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 30, // 30 seconds fresh window
      gcTime: 1000 * 60 * 10, // 10 minutes cache garbage collection
      refetchOnWindowFocus: false,
      retry: (failureCount, error) => {
        // Do not retry 4xx errors (client faults, not found, forbidden)
        if (error instanceof ApiClientError && error.status >= 400 && error.status < 500) {
          return false;
        }
        return failureCount < 2;
      },
    },
    mutations: {
      retry: false,
    },
  },
});

// ============================================================================
// Query Key Factory
// ============================================================================

export const adminQueryKeys = {
  all: ['admin'] as const,
  overview: () => [...adminQueryKeys.all, 'overview'] as const,
  users: () => [...adminQueryKeys.all, 'users'] as const,
  userDetails: (id: string) => [...adminQueryKeys.users(), id] as const,
  reciters: () => [...adminQueryKeys.all, 'reciters'] as const,
  playlists: () => [...adminQueryKeys.all, 'playlists'] as const,
  playlistTracks: (id: string) => [...adminQueryKeys.playlists(), id, 'tracks'] as const,
  activity: () => [...adminQueryKeys.all, 'activity'] as const,
  analytics: () => [...adminQueryKeys.all, 'analytics'] as const,
  health: () => [...adminQueryKeys.all, 'health'] as const,
  collections: () => [...adminQueryKeys.all, 'collections'] as const,
  telemetry: () => [...adminQueryKeys.all, 'telemetry'] as const,
  playback: () => [...adminQueryKeys.all, 'playback'] as const,
  cdnHealth: () => [...adminQueryKeys.all, 'cdnHealth'] as const,
  indexAudit: () => [...adminQueryKeys.all, 'indexAudit'] as const,
  surahs: () => [...adminQueryKeys.all, 'surahs'] as const,
  surahDetails: (id: number | string) => [...adminQueryKeys.surahs(), id] as const,
};

type QueryOptionsWithoutKeyFn<TData> = Omit<
  UseQueryOptions<TData, ApiClientError, TData, readonly unknown[]>,
  'queryKey' | 'queryFn'
>;

// ============================================================================
// Custom Query Hooks
// ============================================================================

/**
 * Hook to retrieve high-level dashboard metrics, KPIs, and charts.
 */
export function useOverview(options?: Partial<QueryOptionsWithoutKeyFn<OverviewData>>) {
  return useQuery({
    queryKey: adminQueryKeys.overview(),
    queryFn: getOverview,
    ...options,
  });
}

/**
 * Hook to retrieve registered users directory and summaries.
 */
export function useUsers(offset = 0, search = '', options?: Partial<QueryOptionsWithoutKeyFn<UsersResponse>>) {
  return useQuery({
    queryKey: [...adminQueryKeys.users(), 'page', offset, search],
    queryFn: () => getUsers(offset, search),
    ...options,
  });
}

/**
 * Hook to retrieve single user details, activity stats, schedules, and playlists.
 */
export function useUserDetails(
  id: string,
  options?: Partial<QueryOptionsWithoutKeyFn<UserDetails>>
) {
  return useQuery({
    queryKey: adminQueryKeys.userDetails(id),
    queryFn: () => getUserDetails(id),
    enabled: Boolean(id) && (options?.enabled ?? true),
    ...options,
  });
}

/**
 * Hook to retrieve verified reciters catalog and stats.
 */
export function useReciters(options?: Partial<QueryOptionsWithoutKeyFn<RecitersResponse>>) {
  return useQuery({
    queryKey: adminQueryKeys.reciters(),
    queryFn: getReciters,
    ...options,
  });
}

/**
 * Hook to retrieve the 114 Surahs catalog.
 */
export function useSurahs(options?: Partial<QueryOptionsWithoutKeyFn<SurahsResponse>>) {
  return useQuery({
    queryKey: adminQueryKeys.surahs(),
    queryFn: getSurahs,
    ...options,
  });
}

/**
 * Hook to retrieve Surah details with reciter stream URLs.
 */
export function useSurahDetails(
  id: number | string,
  options?: Partial<QueryOptionsWithoutKeyFn<SurahDetail>>
) {
  return useQuery({
    queryKey: adminQueryKeys.surahDetails(id),
    queryFn: () => getSurahDetails(id),
    enabled: Boolean(id) && (options?.enabled ?? true),
    ...options,
  });
}

/**
 * Hook to retrieve curated editorial and user playlists.
 */
export function usePlaylists(options?: Partial<QueryOptionsWithoutKeyFn<PlaylistsResponse>>) {
  return useQuery({
    queryKey: adminQueryKeys.playlists(),
    queryFn: getPlaylists,
    ...options,
  });
}

/**
 * Hook to retrieve tracks for a specific playlist.
 */
export function usePlaylistTracks(
  id: string,
  options?: Partial<QueryOptionsWithoutKeyFn<PlaylistTracksResponse>>
) {
  return useQuery({
    queryKey: adminQueryKeys.playlistTracks(id),
    queryFn: () => getPlaylistTracks(id),
    enabled: Boolean(id) && (options?.enabled ?? true),
    ...options,
  });
}

/**
 * Hook to retrieve live listener activity feed.
 * Default polling is set to 10 seconds to keep feed updated in real time.
 */
export function useActivity(options?: Partial<QueryOptionsWithoutKeyFn<ActivityResponse>>) {
  return useQuery({
    queryKey: adminQueryKeys.activity(),
    queryFn: getActivity,
    refetchInterval: 10000, // 10s auto-refresh for live activity stream
    ...options,
  });
}

/**
 * Hook to retrieve deep listening analytics, peak hours, and retention data.
 */
export function useAnalytics(options?: Partial<QueryOptionsWithoutKeyFn<AnalyticsData>>) {
  return useQuery({
    queryKey: adminQueryKeys.analytics(),
    queryFn: getAnalytics,
    ...options,
  });
}

/**
 * Hook to check server health, Appwrite connectivity status, and indexed reciter count.
 * Default polling is set to 15 seconds to monitor cluster status.
 */
export function useHealth(options?: Partial<QueryOptionsWithoutKeyFn<HealthStatus>>) {
  return useQuery({
    queryKey: adminQueryKeys.health(),
    queryFn: getHealth,
    refetchInterval: 15000, // 15s health pulse
    ...options,
  });
}

export function useArchiveHealth(enabled?: boolean) {
  return useQuery<ArchiveHealthResponse, ApiClientError>({
    queryKey: ['archive', 'health'],
    queryFn: getArchiveHealth,
    enabled: enabled ?? true,
    refetchInterval: 15000,
  });
}

// ============================================================================
// Custom Mutation Hooks
// ============================================================================

/**
 * Mutation hook to register or update a reciter with automatic cache invalidation.
 */
export function useCreateReciter(
  options?: Partial<
    UseMutationOptions<ReciterMutationResponse, ApiClientError, CreateReciterInput>
  >
) {
  const qc = useQueryClient();

  return useMutation({
    mutationFn: createReciter,
    onSuccess: (data, variables, onMutateResult, context) => {
      qc.invalidateQueries({ queryKey: adminQueryKeys.reciters() });
      qc.invalidateQueries({ queryKey: adminQueryKeys.overview() });
      options?.onSuccess?.(data, variables, onMutateResult, context);
    },
    ...options,
  });
}

export function useUpdateReciter(
  options?: Partial<
    UseMutationOptions<ReciterMutationResponse, ApiClientError, { id: string; data: UpdateReciterInput }>
  >
) {
  const qc = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateReciterInput }) => updateReciter(id, data),
    onSuccess: (data, variables, onMutateResult, context) => {
      qc.invalidateQueries({ queryKey: adminQueryKeys.reciters() });
      qc.invalidateQueries({ queryKey: adminQueryKeys.overview() });
      options?.onSuccess?.(data, variables, onMutateResult, context);
    },
    ...options,
  });
}

export function useDeleteReciter(
  options?: Partial<
    UseMutationOptions<{ success: boolean; id: string }, ApiClientError, string>
  >
) {
  const qc = useQueryClient();

  return useMutation({
    mutationFn: deleteReciter,
    onSuccess: (data, variables, onMutateResult, context) => {
      qc.invalidateQueries({ queryKey: adminQueryKeys.reciters() });
      options?.onSuccess?.(data, variables, onMutateResult, context);
    },
    ...options,
  });
}

export function useInspectArchive(
  options?: Partial<
    UseMutationOptions<
      ArchiveInspectResponse,
      ApiClientError,
      { url?: string; itemId?: string }
    >
  >
) {
  return useMutation({
    mutationFn: inspectArchive,
    ...options,
  });
}

export function useImportArchive(
  options?: Partial<
    UseMutationOptions<
      ArchiveImportResponse,
      ApiClientError,
      { itemId: string; reciter: ArchiveReciterDraft; image?: File }
    >
  >
) {
  const qc = useQueryClient();

  return useMutation({
    mutationFn: importArchive,
    onSuccess: (data, variables, onMutateResult, context) => {
      qc.invalidateQueries({ queryKey: adminQueryKeys.reciters() });
      options?.onSuccess?.(data, variables, onMutateResult, context);
    },
    ...options,
  });
}

export function useUploadReciterImage(
  options?: Partial<
    UseMutationOptions<ReciterImageResponse, ApiClientError, { id: string; file: File }>
  >
) {
  const qc = useQueryClient();

  return useMutation({
    mutationFn: ({ id, file }: { id: string; file: File }) => uploadReciterImage(id, file),
    onSuccess: (data, variables, onMutateResult, context) => {
      qc.invalidateQueries({ queryKey: adminQueryKeys.reciters() });
      options?.onSuccess?.(data, variables, onMutateResult, context);
    },
    ...options,
  });
}

export function useSyncRecitersCatalog(
  options?: Partial<
    UseMutationOptions<{ success: boolean; message: string; synced: number; total: number }, ApiClientError, void>
  >
) {
  const qc = useQueryClient();

  return useMutation({
    mutationFn: syncRecitersCatalog,
    onSuccess: (data, variables, onMutateResult, context) => {
      qc.invalidateQueries({ queryKey: adminQueryKeys.reciters() });
      qc.invalidateQueries({ queryKey: adminQueryKeys.overview() });
      options?.onSuccess?.(data, variables, onMutateResult, context);
    },
    ...options,
  });
}


/**
 * Mutation hook to create an editorial curated playlist.
 */
export function useCreatePlaylist(
  options?: Partial<
    UseMutationOptions<CreatePlaylistResponse, ApiClientError, CreatePlaylistInput>
  >
) {
  const qc = useQueryClient();

  return useMutation({
    mutationFn: createPlaylist,
    onSuccess: (data, variables, onMutateResult, context) => {
      qc.invalidateQueries({ queryKey: adminQueryKeys.playlists() });
      options?.onSuccess?.(data, variables, onMutateResult, context);
    },
    ...options,
  });
}

/**
 * Mutation hook to delete a playlist.
 */
export function useDeletePlaylist(
  options?: Partial<
    UseMutationOptions<{ success: boolean; id?: string }, ApiClientError, string>
  >
) {
  const qc = useQueryClient();

  return useMutation({
    mutationFn: deletePlaylist,
    onSuccess: (data, variables, onMutateResult, context) => {
      qc.invalidateQueries({ queryKey: adminQueryKeys.playlists() });
      options?.onSuccess?.(data, variables, onMutateResult, context);
    },
    ...options,
  });
}

/**
 * Mutation hook to delete a recitation schedule / alarm.
 */
export function useDeleteSchedule(
  options?: Partial<
    UseMutationOptions<{ success: boolean }, ApiClientError, string>
  >
) {
  const qc = useQueryClient();

  return useMutation({
    mutationFn: deleteSchedule,
    onSuccess: (data, variables, onMutateResult, context) => {
      qc.invalidateQueries({ queryKey: adminQueryKeys.users() });
      options?.onSuccess?.(data, variables, onMutateResult, context);
    },
    ...options,
  });
}


/**
 * Mutation hook to test Appwrite production connection credentials.
 */
export function useTestConnection(
  options?: Partial<
    UseMutationOptions<TestConnectionResponse, ApiClientError, TestConnectionRequest>
  >
) {
  const qc = useQueryClient();

  return useMutation({
    mutationFn: testConnection,
    onSuccess: (data, variables, onMutateResult, context) => {
      qc.invalidateQueries({ queryKey: adminQueryKeys.health() });
      options?.onSuccess?.(data, variables, onMutateResult, context);
    },
    ...options,
  });
}

/**
 * Hook to retrieve database collections document counts.
 */
export function useCollections(options?: Partial<QueryOptionsWithoutKeyFn<CollectionsResponse>>) {
  return useQuery({
    queryKey: adminQueryKeys.collections(),
    queryFn: getCollections,
    refetchInterval: 15000,
    ...options,
  });
}

/**
 * Mutation hook to provision missing database collections and attributes.
 */
export function useProvisionCollections(
  options?: Partial<
    UseMutationOptions<ProvisionResponse, ApiClientError, void>
  >
) {
  const qc = useQueryClient();

  return useMutation({
    mutationFn: provisionCollections,
    onSuccess: (data, variables, onMutateResult, context) => {
      qc.invalidateQueries({ queryKey: adminQueryKeys.collections() });
      qc.invalidateQueries({ queryKey: adminQueryKeys.health() });
      options?.onSuccess?.(data, variables, onMutateResult, context);
    },
    ...options,
  });
}

/**
 * Hook to retrieve system telemetry and cluster performance metrics.
 * Default polling is set to 5 seconds for real-time monitoring.
 */
export function useTelemetry(options?: Partial<QueryOptionsWithoutKeyFn<TelemetryData>>) {
  return useQuery({
    queryKey: adminQueryKeys.telemetry(),
    queryFn: getTelemetry,
    refetchInterval: 5000,
    ...options,
  });
}

/**
 * Hook to retrieve live active playback sessions radar data.
 * Default polling is set to 5 seconds for real-time radar monitoring.
 */
export function usePlayback(options?: Partial<QueryOptionsWithoutKeyFn<PlaybackResponse>>) {
  return useQuery({
    queryKey: adminQueryKeys.playback(),
    queryFn: getPlayback,
    refetchInterval: 5000,
    ...options,
  });
}

/**
 * Hook to retrieve audio CDN & edge cluster latency benchmark.
 * Default polling is set to 15 seconds.
 */
export function useCdnHealth(options?: Partial<QueryOptionsWithoutKeyFn<CdnHealthResponse>>) {
  return useQuery({
    queryKey: adminQueryKeys.cdnHealth(),
    queryFn: getCdnHealth,
    refetchInterval: 15000,
    ...options,
  });
}

/**
 * Hook to retrieve database index audit and query performance health.
 */
export function useIndexAudit(options?: Partial<QueryOptionsWithoutKeyFn<IndexAuditResponse>>) {
  return useQuery({
    queryKey: adminQueryKeys.indexAudit(),
    queryFn: getIndexAudit,
    ...options,
  });
}

/**
 * Hook to retrieve moods catalog.
 */
export function useMoods(options?: Partial<QueryOptionsWithoutKeyFn<MoodsResponse>>) {
  return useQuery({
    queryKey: ['moods'],
    queryFn: getMoods,
    ...options,
  });
}

/**
 * Mutation hook to seed the 114 Surahs catalog.
 */
export function useSeedSurahs(
  options?: Partial<UseMutationOptions<SeedResponse, ApiClientError, void>>
) {
  return useMutation({
    mutationFn: seedSurahs,
    ...options,
  });
}

/**
 * Mutation hook to seed the moods catalog.
 */
export function useSeedMoods(
  options?: Partial<UseMutationOptions<SeedResponse, ApiClientError, void>>
) {
  return useMutation({
    mutationFn: seedMoods,
    ...options,
  });
}

/**
 * Hook to retrieve broadcast history.
 */
export function useBroadcasts(
  enabled: boolean,
  options?: Partial<QueryOptionsWithoutKeyFn<BroadcastsResponse>>
) {
  return useQuery({
    queryKey: ['broadcasts'],
    queryFn: getBroadcasts,
    enabled,
    ...options,
  });
}

/**
 * Mutation hook to delete a broadcast with automatic cache invalidation.
 */
export function useDeleteBroadcast(
  options?: Partial<UseMutationOptions<{ success: boolean }, ApiClientError, string>>
) {
  const qc = useQueryClient();

  return useMutation({
    mutationFn: deleteBroadcast,
    onSuccess: (data, variables, onMutateResult, context) => {
      qc.invalidateQueries({ queryKey: ['broadcasts'] });
      options?.onSuccess?.(data, variables, onMutateResult, context);
    },
    ...options,
  });
}

