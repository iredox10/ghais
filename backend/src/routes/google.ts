import type { FastifyInstance } from 'fastify';

// Unit 4 fills: POST /v1/auth/google — verify on-device Google ID token
// via JWKS (audience = web + extra client ids), upsert/link user, issue tokens.
export default async function googleRoutes(app: FastifyInstance) {}
