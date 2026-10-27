import type { FastifyInstance } from 'fastify';

// Units 6/8 fill: charts rollup + other periodic/server-side jobs.
// Guard with a service bearer token (ADMIN_TOKEN env) — never public.
export default async function jobRoutes(app: FastifyInstance) {}
