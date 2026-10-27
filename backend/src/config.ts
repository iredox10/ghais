import 'dotenv/config';

/** Server/env settings with dev-safe defaults. */
export const config = {
  dbUrl: process.env.DATABASE_URL ?? 'postgres://localhost:5432/ghais',
  port: Number(process.env.PORT ?? 8080),
  jwtSecret: process.env.JWT_SECRET ?? 'dev-insecure-jwt-secret',
  /** Google Cloud WEB client id — the audience claimed by on-device ID tokens. */
  googleWebClientId: process.env.GOOGLE_WEB_CLIENT_ID ?? '',
  /** Optional extra audiences (Android/iOS native client ids). */
  extraGoogleClientIds: (process.env.GOOGLE_EXTRA_CLIENT_IDS ?? '')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean),
  isProd: (process.env.NODE_ENV ?? 'production') === 'production',
  /** Bearer token guarding admin/job routes (e.g. Render CronJob hits). */
  adminToken: process.env.ADMIN_TOKEN ?? '',
};
