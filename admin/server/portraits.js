import { readFileSync } from 'node:fs';
const portraits=new Set(JSON.parse(readFileSync(new URL('./portrait-manifest.json',import.meta.url),'utf8')));
const aliases={ghamdi:'saad-al-ghamdi',shuraym:'saud-al-shuraim',muaiqly:'maher-al-muaiqly',alafasy:'mishary-rashid-alafasy',dossari:'yasser-al-dossari'};
export function portraitFor(slug, explicit) {
  if (typeof explicit === 'string') { try { const u=new URL(explicit);if(['http:','https:'].includes(u.protocol) && !u.hostname.endsWith('appwrite.io'))return explicit; } catch {} }
  if (typeof slug !== 'string' || !/^[a-z0-9-]+$/.test(slug)) return '';
  const canonical=Object.hasOwn(aliases,slug)?aliases[slug]:slug;
  return portraits.has(canonical) ? `${(process.env.PUBLIC_API_URL || 'https://ghaith-c77c2426ed4d.herokuapp.com').replace(/\/+$/,'')}/v1/reciter-images/${canonical}.jpg` : '';
}
