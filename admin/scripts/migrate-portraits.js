// Copies available local portraits into R2 and updates only records without a
// working HTTP image URL. Dry-run by default; safe to resume after failure.
import 'dotenv/config';
import { readFile, stat } from 'node:fs/promises';
import path from 'node:path';
import { createDatabase } from '../server/database.js';
import { createStorage } from '../server/storage.js';
import { Query } from '../server/query.js';

const args=process.argv.slice(2), execute=args.includes('--execute');
const sourceIndex=args.indexOf('--source');
if(sourceIndex<0 || !args[sourceIndex+1]) throw new Error('Usage: node scripts/migrate-portraits.js --source <portrait-directory> [--execute]');
const source=path.resolve(args[sourceIndex+1]);
const aliases={ghamdi:'saad-al-ghamdi',shuraym:'saud-al-shuraim',muaiqly:'maher-al-muaiqly',alafasy:'mishary-rashid-alafasy',dossari:'yasser-al-dossari'};
const {pool,database}=createDatabase(process.env.DATABASE_URL); const storage=execute?createStorage():null;
if(execute&&!storage) throw new Error('Configure R2 and a public image origin first.');
let candidates=0,uploaded=0,failed=0,offset=0;
try {
  for (;;) {
    const page=await database.listDocuments('ghais','reciters',[Query.orderAsc('$id'),Query.limit(500),Query.offset(offset)]);
    for (const reciter of page.documents) {
      const slug=reciter.slug;
      if(typeof slug!=='string'||!/^[a-z0-9-]+$/.test(slug)) continue;
      let valid=false;try { const u=new URL(reciter.image_url);valid=['http:','https:'].includes(u.protocol) && !u.hostname.endsWith('appwrite.io'); } catch {}
      if(valid) continue;
      const filename=path.join(source,`${Object.hasOwn(aliases,slug)?aliases[slug]:slug}.jpg`);
      try { if(!(await stat(filename)).isFile())continue; } catch {continue;}
      candidates++;
      if(!execute) continue;
      let uploadedFile;
      try {
        uploadedFile=await storage.upload(await readFile(filename),'image/jpeg');
        const result=await pool.query(
          "UPDATE reciters SET doc=doc || $3::jsonb, updated_at=now() WHERE id=$1 AND doc->>'image_url' IS NOT DISTINCT FROM $2",
          [reciter.$id,reciter.image_url ?? null,JSON.stringify({image_url:uploadedFile.url,image_file_id:uploadedFile.key})],
        );
        if(!result.rowCount) throw new Error('Image changed while uploading; leaving it unchanged.');
        uploaded++;
      } catch (error) {
        if(uploadedFile) await storage.delete(uploadedFile.key).catch(()=>{});
        failed++;console.error(`Portrait failed for ${slug}: ${error.message}`);
      }
    }
    if(page.documents.length<500) break;
    offset+=page.documents.length;
  }
  console.log(`${execute?'Completed':'Dry run'}: ${candidates} candidates, ${uploaded} uploaded, ${failed} failed.`);
  if(failed) process.exitCode=1;
} finally {await pool.end();}
