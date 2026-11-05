import type { FastifyInstance } from 'fastify';
import fastifyExpress from '@fastify/express';

/** Mount the protected admin app while leaving the mobile API routes intact. */
export async function mountAdmin(app: FastifyInstance, adminApp: Parameters<FastifyInstance['use']>[1]) {
  await app.register(fastifyExpress);
  app.use('/admin', adminApp);
}
