// Email + password auth: register, login, refresh (rotating), me.
import bcrypt from 'bcryptjs';
import { randomBytes } from 'node:crypto';
import type { FastifyInstance } from 'fastify';
import { z } from 'zod';
import { query } from '../db.js';
import {
  issueTokenPair,
  rotateRefreshToken,
  findUserById,
  verifyAccessToken,
  revokeAllUserTokens,
  type TokenPair,
  type UserRow,
} from '../tokens.js';

const registerSchema = z.object({
  email: z.string().trim().toLowerCase().email(),
  name: z.string().trim().max(64).optional(),
  password: z.string().min(8).max(72),
});

const loginSchema = z.object({
  email: z.string().trim().toLowerCase().email(),
  password: z.string().min(1).max(72),
});

const refreshSchema = z.object({ refreshToken: z.string().min(32).max(200) });

function bad(reply: { code(n: number): { send(b: unknown): unknown } }, message: string) {
  return reply.code(400).send({ message });
}

export default async function authRoutes(app: FastifyInstance) {
  app.post('/register', async (req, reply) => {
    const parsed = registerSchema.safeParse(req.body);
    if (!parsed.success) {
      return bad(reply, 'Invalid email, name or password (min 8 chars).');
    }
    const { email, password, name } = parsed.data;
    const exists = await query('SELECT 1 FROM users WHERE email = $1', [email]);
    if (exists.rowCount) {
      return reply.code(409).send({ message: 'An account with this email already exists.' });
    }
    const id = randomBytes(10).toString('hex'); // Appwrite-length (20 hex chars)
    const passwordHash = await bcrypt.hash(password, 10);
    const { rows } = await query<UserRow>(
      `INSERT INTO users(id, email, password_hash, name, provider)
       VALUES ($1,$2,$3,$4,'email')
       RETURNING id, email, name, provider, google_sub`,
      [id, email, passwordHash, name ?? ''],
    );
    const pair = await issueTokenPair(rows[0]);
    return pair;
  });

  app.post('/login', async (req, reply) => {
    const parsed = loginSchema.safeParse(req.body);
    if (!parsed.success) return bad(reply, 'Invalid email or password.');
    const { rows } = await query<UserRow & { password_hash: string | null }>(
      'SELECT id, email, name, provider, google_sub, password_hash FROM users WHERE email = $1',
      [parsed.data.email],
    );
    const user = rows[0];
    if (!user?.password_hash) {
      return reply.code(401).send({ message: 'Invalid email or password.' });
    }
    const ok = await bcrypt.compare(parsed.data.password, user.password_hash);
    if (!ok) {
      return reply.code(401).send({ message: 'Invalid email or password.' });
    }
    return issueTokenPair(user);
  });

  app.post('/refresh', async (req, reply) => {
    const parsed = refreshSchema.safeParse(req.body);
    if (!parsed.success) return bad(reply, 'Missing refresh token.');
    const rotated = await rotateRefreshToken(parsed.data.refreshToken);
    if (!rotated) {
      return reply.code(401).send({ message: 'Session expired. Please sign in again.' });
    }
    const user = await findUserById(rotated.userId);
    if (!user) {
      return reply.code(401).send({ message: 'Session expired. Please sign in again.' });
    }
    const pair: TokenPair = await issueTokenPair(user);
    return pair;
  });

  app.get('/me', async (req, reply) => {
    const header = req.headers.authorization ?? '';
    const token = header.startsWith('Bearer ') ? header.slice(7) : '';
    const claims = token ? await verifyAccessToken(token) : null;
    if (!claims) {
      return reply.code(401).send({ message: 'Unauthorized.' });
    }
    const user = await findUserById(claims.userId);
    if (!user) {
      return reply.code(401).send({ message: 'Account no longer exists.' });
    }
    return { userId: user.id, email: user.email, name: user.name, provider: user.provider };
  });

  // Sign-out hook: revoke every refresh token of the bearer (best-effort from
  // the client; access tokens expire on their own within minutes).
  app.delete('/refresh', async (req, reply) => {
    const header = req.headers.authorization ?? '';
    const token = header.startsWith('Bearer ') ? header.slice(7) : '';
    const claims = token ? await verifyAccessToken(token) : null;
    if (!claims) {
      return reply.code(401).send({ message: 'Unauthorized.' });
    }
    await revokeAllUserTokens(claims.userId);
    return reply.code(204).send();
  });
}
