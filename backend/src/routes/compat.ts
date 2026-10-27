import type { FastifyInstance } from 'fastify';

// Unit 5 fills: Appwrite document REST compat shim —
// /v1/databases/{db}/collections/{collection}/documents[/{id}]
// (Appwrite list/create/read/patch/delete shapes over the Postgres tables;
// public collections readable with the project header, user-scoped ones
// require a Bearer access token).
export default async function compatRoutes(app: FastifyInstance) {}
