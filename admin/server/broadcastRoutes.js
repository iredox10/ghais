import express from 'express';
import { Query } from './query.js';

export default function registerBroadcastRoutes(app, getContext) {
  const router=express.Router();
  router.get('/api/broadcast', async (_req,res) => {
    try {
      const {databases,databaseId}=getContext();
      const result=await databases.listDocuments(databaseId,'broadcasts',[Query.limit(50),Query.orderDesc('$createdAt')]);
      const broadcasts=result.documents.map(d=>({id:d.$id,title:d.title,body:d.body,audience:d.audience||'all',targetTopic:d.audience||'all',urgency:d.urgency||'Normal',timestamp:d.$createdAt}));
      res.json({success:true,broadcasts});
    } catch(error) { res.status(500).json({success:false,error:'Could not load broadcasts.'}); }
  });
  router.post('/api/broadcast', async (req,res) => {
    try {
      const {title,body,audience,targetTopic,targetGroup,urgency}=req.body||{};
      if(typeof title!=='string'||!title.trim()||typeof body!=='string'||!body.trim()) return res.status(400).json({success:false,error:'Title and message body are required.'});
      const {databases,databaseId}=getContext();
      const doc=await databases.createDocument(databaseId,'broadcasts','unique()',{title:title.trim(),body:body.trim(),audience:audience||targetTopic||targetGroup||'all',urgency:urgency||'Normal'});
      res.status(201).json({success:true,broadcastId:doc.$id,timestamp:doc.$createdAt});
    } catch(error) { res.status(500).json({success:false,error:'Could not save broadcast.'}); }
  });
  router.delete('/api/broadcast/:id', async (req,res) => {
    try { const {databases,databaseId}=getContext(); try { await databases.deleteDocument(databaseId,'broadcasts',req.params.id); } catch(error) { if(error.code!==404) throw error; } res.json({success:true,id:req.params.id,broadcastId:req.params.id}); }
    catch { res.status(500).json({success:false,error:'Could not delete broadcast.'}); }
  });
  app.use(router);
}
