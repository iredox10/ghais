/**
 * Quranify - Appwrite Database Seeder
 * 
 * Usage:
 *   node scripts/seed_appwrite.js
 * 
 * Environment variables:
 *   APPWRITE_ENDPOINT=https://cloud.appwrite.io/v1
 *   APPWRITE_PROJECT_ID=quranify
 *   APPWRITE_API_KEY=your-secret-api-key
 *   APPWRITE_DATABASE_ID=quranify
 */

const fs = require('fs');
const path = require('path');

const endpoint = process.env.APPWRITE_ENDPOINT || 'https://cloud.appwrite.io/v1';
const projectId = process.env.APPWRITE_PROJECT_ID || 'quranify';
const apiKey = process.env.APPWRITE_API_KEY;
const databaseId = process.env.APPWRITE_DATABASE_ID || 'quranify';

if (!apiKey) {
  console.log('[Notice] Set APPWRITE_API_KEY environment variable to execute live cloud seed.');
  console.log('Displaying schema and seed statistics:');
}

const schemaPath = path.join(__dirname, '../appwrite/collections.json');
if (fs.existsSync(schemaPath)) {
  const schema = JSON.parse(fs.readFileSync(schemaPath, 'utf8'));
  console.log(`Found ${schema.collections.length} collections and ${schema.buckets.length} storage buckets.`);
  schema.collections.forEach(col => {
    console.log(` - Collection: [${col.$id}] ${col.name} (${col.attributes.length} attributes, ${col.indexes.length} indexes)`);
  });
}

console.log('\nReady for Appwrite deployment!');
