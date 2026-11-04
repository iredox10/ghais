// Only server-generated portrait keys are publicly readable.
export function registerPublicImages(app, storage) {
  app.get('/media/reciters/:filename', async (req, res) => {
    if (!storage) return res.status(503).end();
    try {
      const object = await storage.get(`reciters/${req.params.filename}`);
      if (!object?.Body) return res.status(404).end();
      const extension = req.params.filename.split('.').pop();
      const mime = { jpg: 'image/jpeg', png: 'image/png', webp: 'image/webp', avif: 'image/avif' }[extension];
      if (!mime) return res.status(404).end();
      res.set({ 'Content-Type': mime, 'X-Content-Type-Options': 'nosniff', 'Cache-Control': 'public, max-age=86400' });
      object.Body.on('error', () => res.destroy());
      return object.Body.pipe(res);
    } catch { return res.status(502).end(); }
  });
}
