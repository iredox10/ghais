import test from 'node:test';
import assert from 'node:assert/strict';
import { mergeListeningStats } from '../src/stats.js';
const update = (device: string, seconds: number, plays: number, total = 120 + seconds) => ({ total_seconds: total, surah_plays: JSON.stringify({1: 3 + plays}), device_counters: {device, seconds, plays: {1: plays}} });
test('device stats preserve legacy baseline and add independent deltas idempotently', () => {
 let doc = mergeListeningStats({ total_seconds: 120, surah_plays: '{"1":3}' }, update('a', 60, 1));
 assert.equal(doc.total_seconds, 180);
 doc = mergeListeningStats(doc, update('b', 40, 2));
 assert.equal(doc.total_seconds, 220);
 assert.equal(JSON.parse(doc.surah_plays)['1'], 6);
 const repeat = mergeListeningStats(doc, update('a', 60, 1));
 assert.equal(repeat.total_seconds, 220);
 assert.equal(repeat.surah_plays, doc.surah_plays);
 const stale = mergeListeningStats(doc, update('a', 10, 0));
 assert.equal(stale.total_seconds, 220);
});
test('new document separates current device deltas from its legacy totals', () => {
 const doc=mergeListeningStats({},update('a',60,1));
 assert.equal(doc.total_seconds,180);
 assert.equal(JSON.parse(doc.surah_plays)['1'],4);
 const legacy=mergeListeningStats(doc,{total_seconds:1,surah_plays:'{}'});
 assert.equal(legacy.total_seconds,180);
 assert.equal(JSON.parse(legacy.surah_plays)['1'],4);
});

test('legacy growth survives the next device retry and malformed play maps are safe', () => {
 let doc=mergeListeningStats({},update('a',60,1));
 doc=mergeListeningStats(doc,{total_seconds:240,surah_plays:'{"1":7}'});
 doc=mergeListeningStats(doc,update('a',60,1));
 assert.equal(doc.total_seconds,240);
 assert.equal(JSON.parse(doc.surah_plays)['1'],7);
 assert.doesNotThrow(()=>mergeListeningStats({}, {surah_plays:'null',device_counters:{device:'b',seconds:0,plays:null}}));
});
