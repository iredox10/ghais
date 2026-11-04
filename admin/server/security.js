import { timingSafeEqual } from 'node:crypto';

function equal(a,b) { const x=Buffer.from(a),y=Buffer.from(b); return x.length===y.length && timingSafeEqual(x,y); }
export function adminAuth(env=process.env) {
  const username=env.ADMIN_USERNAME, password=env.ADMIN_PASSWORD;
  if (!username || !password || password.length < 16) throw new Error('Set ADMIN_USERNAME and an ADMIN_PASSWORD of at least 16 characters.');
  return (req,res,next) => {
    const header=req.headers.authorization || '';
    let provided=''; try { if(header.startsWith('Basic ')) provided=Buffer.from(header.slice(6),'base64').toString('utf8'); } catch {}
    const separator=provided.indexOf(':'), user=provided.slice(0,separator), pass=provided.slice(separator+1);
    if(separator<0 || !equal(user,username) || !equal(pass,password)) { res.set('WWW-Authenticate','Basic realm="Quranify Admin", charset="UTF-8"'); return res.status(401).json({error:'Authentication required.'}); }
    if (!['GET','HEAD','OPTIONS'].includes(req.method) && req.headers.origin) {
      let allowed=false; try { const origin=new URL(req.headers.origin); allowed=origin.host===req.get('host') || (env.ADMIN_DEV_ORIGIN && origin.origin===env.ADMIN_DEV_ORIGIN); } catch {}
      if(!allowed) return res.status(403).json({error:'Origin not allowed.'});
    }
    next();
  };
}
