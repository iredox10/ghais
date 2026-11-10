/** Grow-only per-device counters; legacy aggregates form a preserved baseline. */
export function mergeListeningStats(previous: Record<string, any>, patch: Record<string, any>) {
  const incoming = patch.device_counters;
  if (!incoming || typeof incoming.device !== 'string' || !incoming.device || incoming.device.length > 128 || !/^[a-zA-Z0-9_-]+$/.test(incoming.device) || ['__proto__', 'constructor', 'prototype'].includes(incoming.device)) {
    const state = previous.counter_state;
    if (!state) return { ...previous, ...patch };
    const devices = state.devices || {};
    const delta = Object.values(devices).reduce((sum: number, value: any) => sum + number(value.seconds), 0);
    const devicePlays: Record<string, number> = {};
    for (const value of Object.values(devices) as any[]) for (const [id, count] of Object.entries(value.plays || {})) devicePlays[id] = number(devicePlays[id]) + number(count);
    const baselineSeconds = Math.max(number(state.baselineSeconds), number(patch.total_seconds) - delta);
    const baselinePlays = maxPlays(state.baselinePlays || {}, subtract(parse(patch.surah_plays), devicePlays));
    const totalPlays = { ...baselinePlays };
    for (const [id, count] of Object.entries(devicePlays)) totalPlays[id] = number(totalPlays[id]) + count;
    return { ...previous, ...patch, counter_state: { baselineSeconds, baselinePlays, devices }, total_seconds: baselineSeconds + delta, surah_plays: JSON.stringify(totalPlays) };
  }
  const state = previous.counter_state;
  const devices = { ...(state?.devices || {}) };
  const old = devices[incoming.device] || {};
  devices[incoming.device] = { seconds: Math.max(number(old.seconds), number(incoming.seconds)), plays: maxPlays(old.plays || {}, incoming.plays || {}) };
  const deltaSeconds = Object.values(devices).reduce((sum: number, value: any) => sum + number(value.seconds), 0);
  const baselineSeconds = state ? number(state.baselineSeconds) : Math.max(number(previous.total_seconds), number(patch.total_seconds) - number(incoming.seconds));
  const baselinePlays = state?.baselinePlays || maxPlays(parse(previous.surah_plays), subtract(parse(patch.surah_plays), incoming.plays || {}));
  const plays = { ...baselinePlays };
  for (const value of Object.values(devices) as any[]) for (const [id, count] of Object.entries(value.plays || {})) plays[id] = number(plays[id]) + number(count);
  const result: Record<string, any> = { ...previous, ...patch, counter_state: { baselineSeconds, baselinePlays, devices }, total_seconds: baselineSeconds + deltaSeconds, surah_plays: JSON.stringify(plays) };
  delete result.device_counters;
  return result;
}
const number = (value: unknown): number => typeof value === 'number' && Number.isSafeInteger(value) && value > 0 ? value : 0;
function parse(value: unknown): Record<string, number> { try { const parsed = typeof value === 'string' ? JSON.parse(value) : null; return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}; } catch { return {}; } }
function maxPlays(a: Record<string, any>, b: Record<string, any>) { const out: Record<string, number> = {}; for (let id = 1; id <= 114; id++) { const n = Math.max(number(a?.[id]), number(b?.[id])); if(n) out[id] = n; } return out; }
function subtract(a: Record<string, any>, b: Record<string, any>) { const out: Record<string, number> = {}; for (let id = 1; id <= 114; id++) { const n = Math.max(0,number(a?.[id])-number(b?.[id])); if(n) out[id] = n; } return out; }
