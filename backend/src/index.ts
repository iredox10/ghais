import Fastify from 'fastify';
import cors from '@fastify/cors';
import { config } from './config.js';
import authRoutes from './routes/auth.js';
import googleRoutes from './routes/google.js';
import compatRoutes from './routes/compat.js';
import jobRoutes from './routes/jobs.js';
import { portraitRoutes } from './portraits.js';

const app = Fastify({ logger: true });
await app.register(cors, { origin: true });

app.get('/healthz', async () => ({ status: 'ok' }));

await app.register(authRoutes, { prefix: '/v1/auth' });
await app.register(googleRoutes, { prefix: '/v1/auth/google' });
await app.register(compatRoutes, { prefix: '/v1/databases' });
await app.register(jobRoutes, { prefix: '/v1/jobs' });
await app.register(portraitRoutes, { prefix: '/v1/reciter-images' });

const start = async (): Promise<void> => {
  try {
    await app.listen({ host: '0.0.0.0', port: config.port });
  } catch (err) {
    app.log.error(err);
    process.exit(1);
  }
};
start();
