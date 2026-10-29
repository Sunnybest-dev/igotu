import { Router } from 'express';
import { requireAuth, AuthRequest } from '../middleware/auth';
import db from '../db';

const router = Router();
router.use(requireAuth);

router.get('/settings', async (req: AuthRequest, res) => {
  const settings = await db('privacy_lockdown_settings').where({ user_id: req.userId }).first();
  const apps = await db('protected_app_entries').where({ user_id: req.userId });
  res.json({ ...settings, protectedApps: apps });
});

router.patch('/settings', async (req: AuthRequest, res) => {
  const allowed = ['enabled', 'hide_sensitive_notifications', 'protect_igotu_content', 'activate_with_sos'];
  const update: Record<string, any> = { updated_at: db.fn.now() };
  for (const key of allowed) {
    if (req.body[key] !== undefined) update[key] = req.body[key];
  }
  await db('privacy_lockdown_settings')
    .insert({ user_id: req.userId, ...update })
    .onConflict('user_id').merge(update);

  if (Array.isArray(req.body.protectedApps)) {
    await db('protected_app_entries').where({ user_id: req.userId }).delete();
    if (req.body.protectedApps.length > 0) {
      await db('protected_app_entries').insert(
        req.body.protectedApps.map((app: any) => ({
          user_id: req.userId,
          package_name: app.packageName,
          display_name: app.displayName
        }))
      );
    }
  }

  const settings = await db('privacy_lockdown_settings').where({ user_id: req.userId }).first();
  res.json(settings);
});

router.post('/activate', async (req: AuthRequest, res) => {
  await db('privacy_lockdown_settings')
    .where({ user_id: req.userId })
    .update({ enabled: true, updated_at: db.fn.now() });
  res.json({ active: true });
});

router.post('/deactivate', async (req: AuthRequest, res) => {
  await db('privacy_lockdown_settings')
    .where({ user_id: req.userId })
    .update({ enabled: false, updated_at: db.fn.now() });
  res.json({ active: false });
});

export default router;
