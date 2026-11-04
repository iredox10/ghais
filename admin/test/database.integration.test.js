import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createDatabase } from '../server/database.js';
import { Query } from '../server/query.js';
const url=process.env.TEST_DATABASE_URL;
test('PostgreSQL adapter persists documents, filters ownership data and rejects unsafe identifiers', {skip:!url}, async()=>{
  if (!['localhost','127.0.0.1','::1'].includes(new URL(url).hostname)) throw new Error('Integration database must be local.');
  const {pool,database}=createDatabase(url,'');
  try {
    await pool.query('CREATE TABLE IF NOT EXISTS reciters (id TEXT PRIMARY KEY,owner TEXT,doc JSONB NOT NULL,created_at TIMESTAMPTZ NOT NULL DEFAULT now(),updated_at TIMESTAMPTZ NOT NULL DEFAULT now())');
    await pool.query("DELETE FROM reciters WHERE id LIKE 'admin-test-%'");
    await database.createDocument('ghais','reciters','admin-test-a',{slug:'admin-test-a',name_en:'A',enabled:true});
    await database.createDocument('ghais','reciters','admin-test-b',{slug:'admin-test-b',name_en:'B',enabled:true});
    let result=await database.listDocuments('ghais','reciters',[Query.equal('slug','admin-test-b')]);assert.equal(result.total,1);assert.equal(result.documents[0].name_en,'B');
    await database.updateDocument('ghais','reciters','admin-test-a',{name_en:'Updated'});assert.equal((await database.getDocument('ghais','reciters','admin-test-a')).name_en,'Updated');
    await assert.rejects(database.listDocuments('ghais','reciters',[Query.equal("x') OR true --",'x')]));
    await assert.rejects(database.listDocuments('ghais','users; DROP TABLE users;--',[]));
    await database.deleteDocument('ghais','reciters','admin-test-a');await assert.rejects(database.getDocument('ghais','reciters','admin-test-a'),e=>e.code===404);
  } finally {await pool.query("DELETE FROM reciters WHERE id LIKE 'admin-test-%'").catch(()=>{});await pool.end();}
});
