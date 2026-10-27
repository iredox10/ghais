// Access JWT (HS256, short-lived) + revocable refresh tokens (hashed rows).
import { createHash, randomBytes } from 'node:crypto';
import { SignJWT, jwtVerify } from 'jose';
import { config } from './config.js';
import { query } from './db.js';

export const ACCESS_TTL_SECONDS = 15 * 60;
export const REFRESH_TTL_MS = 30 * 24 * 60 * 60 * 1000;

export type AccessClaims = { userId: string; email: string; name: string };

export type UserRow = {
  id: string;
  email: string;
  name: string;
  provider: string;
  google_sub: string | null;
};

const secretKey = () => new TextEncoder().encode(config.jwtSecret);
const sha256 = (value: string) => createHash('sha256').update(value).digest('hex');

export async function signAccessToken(claims: AccessClaims): Promise<string> {
  return new SignJWT({ email: claims.email, name: claims.name })
    .setProtectedHeader({ alg: 'HS256' })
    .setSubject(claims.userId)
    .setIssuedAt()
    .setExpirationTime(`${ACCESS_TTL_SECONDS}s`)
    .sign(secretKey());
}

export async function verifyAccessToken(token: string): Promise<AccessClaims | null> {
  try {
    const { payload } = await jwtVerify(token, secretKey());
    if (!payload.sub) return null;
    return {
      userId: payload.sub,
      email: String(payload.email ?? ''),
      name: String(payload.name ?? ''),
    };
  } catch {
    return null;
  }
}

export async function issueRefreshToken(userId: string): Promise<string> {
  const raw = randomBytes(32).toString('hex');
  const expires = new Date(Date.now() + REFRESH_TTL_MS);
  await query(
    'INSERT INTO refresh_tokens(token_hash, user_id, expires_at) VALUES ($1,$2,$3)',
    [sha256(raw), userId, expires],
  );
  return raw;
}

/** Validates the presented refresh token, consumes it (rotation). */
export async function rotateRefreshToken(raw: string): Promise<{ userId: string } | null> {
  const hash = sha256(raw);
  const { rows } = await query<{ user_id: string; expires_at: Date }>(
    'SELECT user_id, expires_at FROM refresh_tokens WHERE token_hash = $1',
    [hash],
  );
  if (rows.length === 0) return null;
  const row = rows[0];
  await query('DELETE FROM refresh_tokens WHERE token_hash = $1', [hash]);
  if (row.expires_at.getTime() < Date.now()) return null;
  return { userId: row.user_id };
}

export async function revokeAllUserTokens(userId: string): Promise<void> {
  await query('DELETE FROM refresh_tokens WHERE user_id = $1', [userId]);
}

export async function findUserById(userId: string): Promise<UserRow | null> {
  const { rows } = await query<UserRow>(
    'SELECT id, email, name, provider, google_sub FROM users WHERE id = $1',
    [userId],
  );
  return rows[0] ?? null;
}

export type TokenPair = {
  userId: string;
  email: string;
  name: string;
  accessToken: string;
  refreshToken: string;
  /** Access token lifetime in seconds. */
  expiresIn: number;
};

/** Issues a fresh access+refresh pair for a user row. */
export async function issueTokenPair(user: UserRow): Promise<TokenPair> {
  const [accessToken, refreshToken] = await Promise.all([
    signAccessToken({ userId: user.id, email: user.email, name: user.name }),
    issueRefreshToken(user.id),
  ]);
  return {
    userId: user.id,
    email: user.email,
    name: user.name,
    accessToken,
    refreshToken,
    expiresIn: ACCESS_TTL_SECONDS,
  };
}
