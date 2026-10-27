// Google sign-in without any client secret: the app requests a Google ID
// token ON DEVICE (native flow) and posts it here; the server verifies the
// signature against Google's JWKS, checks iss/aud/exp, then upserts or links
// the user and issues the standard token pair.
import { randomBytes } from 'node:crypto';
import { createRemoteJWKSet, jwtVerify } from 'jose';
import type { FastifyInstance } from 'fastify';
import { z } from 'zod';
import { config } from '../config.js';
import { query } from '../db.js';
import { issueTokenPair, type UserRow } from '../tokens.js';

const bodySchema = z.object({ idToken: z.string().min(20).max(4000) });

const GOOGLE_JWKS = createRemoteJWKSet(new URL('https://www.googleapis.com/oauth2/v3/certs'));
const VALID_ISSUERS = ['https://accounts.google.com', 'accounts.google.com'];

type GoogleClaims = {
  sub: string;
  email?: string;
  email_verified?: boolean;
  name?: string;
};

export default async function googleRoutes(app: FastifyInstance) {
  app.post('/', async (req, reply) => {
    const parsed = bodySchema.safeParse(req.body);
    if (!parsed.success) {
      return reply.code(400).send({ message: 'Missing Google id token.' });
    }

    const audiences = [config.googleWebClientId, ...config.extraGoogleClientIds].filter(Boolean);
    if (audiences.length === 0) {
      return reply.code(500).send({ message: 'Server not configured for Google sign-in.' });
    }

    let claims: GoogleClaims;
    try {
      const { payload } = await jwtVerify(parsed.data.idToken, GOOGLE_JWKS, {
        issuer: VALID_ISSUERS,
        audience: audiences,
      });
      claims = payload as unknown as GoogleClaims;
      if (!claims.sub) throw new Error('no sub');
    } catch {
      return reply.code(401).send({ message: 'Google sign-in failed (invalid token).' });
    }

    // Wrong-audience tokens are rejected above; this should not happen.
    if (claims.email_verified === false) {
      return reply.code(401).send({ message: 'Google account email is not verified.' });
    }
    const email = (claims.email ?? '').toLowerCase();

    // 1. Known Google subject -> straight to a pair.
    let rows = (await query<UserRow>(
      'SELECT id, email, name, provider, google_sub FROM users WHERE google_sub = $1',
      [claims.sub],
    )).rows;
    let user = rows[0];

    // 2. Same email booked by an email/password account -> LINK the Google
    //    subject onto it (keeps the liked-/follows-… document bodies intact).
    if (!user && email) {
      rows = (await query<UserRow>(
        'SELECT id, email, name, provider, google_sub FROM users WHERE email = $1',
        [email],
      )).rows;
      user = rows[0];
      if (user) {
        await query('UPDATE users SET google_sub = $1 WHERE id = $2', [claims.sub, user.id]);
      }
    }

    // 3. Fresh Google user.
    if (!user) {
      const id = randomBytes(10).toString('hex');
      rows = (await query<UserRow>(
        `INSERT INTO users(id, email, password_hash, name, provider, google_sub)
         VALUES ($1,$2,NULL,$3,'google',$4)
         RETURNING id, email, name, provider, google_sub`,
        [id, email || `${claims.sub}@users.noreply.google`, claims.name ?? '', claims.sub],
      )).rows;
      user = rows[0];
    }

    return issueTokenPair(user);
  });
}
