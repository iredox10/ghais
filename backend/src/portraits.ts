import { createHash } from 'node:crypto';
import { createReadStream, readdirSync, readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import type { FastifyInstance } from 'fastify';
import { config } from './config.js';

const directory = fileURLToPath(new URL('../assets/reciters/', import.meta.url));
const portraits = new Set(readdirSync(directory).filter((file) => /^[a-z0-9-]+\.jpg$/.test(file)));
const portraitEtags = new Map<string, string>();
for (const file of portraits) {
  const hash = createHash('sha256').update(readFileSync(path.join(directory, file))).digest('hex').slice(0, 16);
  portraitEtags.set(file, `"${hash}"`);
}
const aliases: Record<string, string> = {
  ghamdi: 'saad-al-ghamdi',
  shuraym: 'saud-al-shuraim',
  muaiqly: 'maher-al-muaiqly',
  alafasy: 'mishary-rashid-alafasy',
  dossari: 'yasser-al-dossari',
};

export function portraitUrl(slug: unknown): string | null {
  if (typeof slug !== 'string' || !/^[a-z0-9-]+$/.test(slug)) return null;
  const canonical = Object.hasOwn(aliases, slug) ? aliases[slug] : slug;
  if (!portraits.has(`${canonical}.jpg`)) return null;
  return `${config.publicApiUrl}/v1/reciter-images/${canonical}.jpg`;
}

export async function portraitRoutes(app: FastifyInstance) {
  app.get<{ Params: { filename: string } }>('/:filename', async (req, reply) => {
    const { filename } = req.params;
    if (!portraits.has(filename)) return reply.code(404).send({ message: 'Portrait not found.' });
    const etag = portraitEtags.get(filename);
    const ifNoneMatch = req.headers['if-none-match'];
    reply.header('Cache-Control', 'public, max-age=604800');
    if (etag) reply.header('ETag', etag);
    if (etag && typeof ifNoneMatch === 'string' && ifNoneMatch.split(',').some((t) => t.trim() === etag)) {
      return reply.code(304).send();
    }
    return reply.type('image/jpeg').send(createReadStream(path.join(directory, filename)));
  });
}
