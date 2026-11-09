import Fastify from 'fastify';
import cors from '@fastify/cors';
import { config } from './config.js';
import authRoutes from './routes/auth.js';
import googleRoutes from './routes/google.js';
import compatRoutes from './routes/compat.js';
import jobRoutes from './routes/jobs.js';
import { portraitRoutes } from './portraits.js';
import { mountAdmin } from './admin.js';
import { editorialRoutes } from './routes/editorial.js';

const app = Fastify({ logger: true });
await app.register(cors, { origin: true });

if (process.env.ADMIN_ENABLED === 'true') {
  // Kept conditional so the mobile API can run independently in development.
  const adminPath = '../../admin/server/index.js';
  const { app: adminApp } = await import(adminPath);
  await mountAdmin(app, adminApp);
}

app.get('/healthz', async () => ({ status: 'ok' }));

await app.register(authRoutes, { prefix: '/v1/auth' });
await app.register(googleRoutes, { prefix: '/v1/auth/google' });
await app.register(compatRoutes, { prefix: '/v1/databases' });
await app.register(jobRoutes, { prefix: '/v1/jobs' });
await app.register(portraitRoutes, { prefix: '/v1/reciter-images' });
await app.register(editorialRoutes, { prefix: '/v1/editorial' });

const start = async (): Promise<void> => {
  try {
    await app.listen({ host: '0.0.0.0', port: config.port });
  } catch (err) {
    app.log.error(err);
    process.exit(1);
  }
};
start();
