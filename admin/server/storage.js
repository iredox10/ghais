import { randomUUID } from 'node:crypto';
import { S3Client, PutObjectCommand, DeleteObjectCommand, GetObjectCommand } from '@aws-sdk/client-s3';

const TYPES = new Map([['image/jpeg','jpg'],['image/png','png'],['image/webp','webp'],['image/avif','avif']]);
function matchesType(buffer, type) {
  if (type === 'image/jpeg') return buffer.subarray(0,3).equals(Buffer.from([0xff,0xd8,0xff]));
  if (type === 'image/png') return buffer.subarray(0,8).equals(Buffer.from([0x89,0x50,0x4e,0x47,0x0d,0x0a,0x1a,0x0a]));
  if (type === 'image/webp') return buffer.toString('ascii',0,4) === 'RIFF' && buffer.toString('ascii',8,12) === 'WEBP';
  if (type === 'image/avif') return buffer.toString('ascii',4,8) === 'ftyp' && /avif|avis/.test(buffer.toString('ascii',8,32));
  return false;
}
export function createStorage(env = process.env, clientOverride) {
  const { R2_ACCOUNT_ID, R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY, R2_BUCKET, R2_PUBLIC_URL, ADMIN_PUBLIC_URL } = env;
  if (![R2_ACCOUNT_ID,R2_ACCESS_KEY_ID,R2_SECRET_ACCESS_KEY,R2_BUCKET].every(Boolean)) return null;
  const imageOrigin = R2_PUBLIC_URL || ADMIN_PUBLIC_URL;
  if (!imageOrigin) throw new Error('Set ADMIN_PUBLIC_URL or R2_PUBLIC_URL for image access.');
  const publicUrl = new URL(imageOrigin);
  if (publicUrl.protocol !== 'https:' || publicUrl.username || publicUrl.password || publicUrl.search || publicUrl.hash) throw new Error('The public image URL must be a clean HTTPS URL.');
  const client = clientOverride || new S3Client({ region:'auto', endpoint:`https://${R2_ACCOUNT_ID}.r2.cloudflarestorage.com`, credentials:{accessKeyId:R2_ACCESS_KEY_ID,secretAccessKey:R2_SECRET_ACCESS_KEY} });
  return {
    async upload(buffer, contentType) {
      const extension=TYPES.get(contentType); if (!extension || !Buffer.isBuffer(buffer) || !buffer.length || buffer.length > 5*1024*1024 || !matchesType(buffer, contentType)) throw new Error('Invalid image.');
      const key=`reciters/${randomUUID()}.${extension}`;
      await client.send(new PutObjectCommand({Bucket:R2_BUCKET,Key:key,Body:buffer,ContentType:contentType,CacheControl:'public, max-age=31536000, immutable'}));
      const prefix = R2_PUBLIC_URL ? imageOrigin : `${imageOrigin.replace(/\/+$/,'')}/media`;
      return { key, url:`${prefix.replace(/\/+$/,'')}/${key}` };
    },
    async delete(key) {
      // Legacy Appwrite file IDs and unrelated bucket objects are never deleted.
      if (!/^reciters\/[a-f0-9-]{36}\.(jpg|png|webp|avif)$/.test(key || '')) return;
      await client.send(new DeleteObjectCommand({Bucket:R2_BUCKET,Key:key}));
    },
    async get(key) {
      if (!/^reciters\/[a-f0-9-]{36}\.(jpg|png|webp|avif)$/.test(key || '')) return null;
      try { return await client.send(new GetObjectCommand({Bucket:R2_BUCKET,Key:key})); }
      catch (error) { if (error.name === 'NoSuchKey' || error.$metadata?.httpStatusCode === 404) return null; throw error; }
    },
  };
}
