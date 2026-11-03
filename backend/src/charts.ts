/** Preserve the original Appwrite function's UTC-day dedupe and velocity ranking. */
export type ChartEvent = { id: string; owner: string | null; doc: Record<string, unknown>; created_at: Date };
export function aggregateCharts(events: ChartEvent[], now: number) {
  const day = 86_400_000;
  const today = Math.floor(now / day) * day;
  const seen = new Set<string>();
  const week = new Map<string, number>();
  const month = new Map<string, number>();
  const yesterday = new Map<string, number>();
  const add = (counts: Map<string, number>, slug: string) => counts.set(slug, (counts.get(slug) ?? 0) + 1);
  for (const event of events) {
    const slug = event.doc.reciterSlug;
    const surah = event.doc.surahId;
    const time = event.created_at.getTime();
    if (typeof slug !== 'string' || !slug || typeof surah !== 'number' || !Number.isFinite(time) || time <= now - 30 * day || time > now) continue;
    const key = JSON.stringify([event.owner ?? event.doc.ownerId ?? event.id, slug, surah, Math.floor(time / day)]);
    if (seen.has(key)) continue;
    seen.add(key);
    add(month, slug);
    if (time >= now - 7 * day) add(week, slug);
    if (time >= today - day && time < today) add(yesterday, slug);
  }
  const top = (counts: Map<string, number>) => [...counts].sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0])).slice(0, 20).map(([slug]) => slug);
  const total7 = [...week.values()].reduce((a, b) => a + b, 0);
  const total30 = [...month.values()].reduce((a, b) => a + b, 0);
  const trending = !total7 || !total30 ? top(week) : [...week].map(([slug, count]) => ({ slug, velocity: count / total7 - (month.get(slug) ?? 0) / total30 }))
    .sort((a, b) => b.velocity - a.velocity || a.slug.localeCompare(b.slug)).slice(0, 20).map(x => x.slug);
  return { topQaris7d: top(week), topQaris30d: top(month), topQarisYesterday: top(yesterday), trending, generatedAt: new Date(now).toISOString() };
}
