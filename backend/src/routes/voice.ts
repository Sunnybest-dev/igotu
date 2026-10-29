import { Router } from 'express';
import { requireAuth, AuthRequest } from '../middleware/auth';
import db from '../db';

const router = Router();
router.use(requireAuth);

router.post('/enrollment', async (req: AuthRequest, res) => {
  const { sampleCount } = req.body;
  await db('voice_profiles')
    .insert({ user_id: req.userId, enrolled: true, sample_count: sampleCount })
    .onConflict('user_id')
    .merge({ enrolled: true, sample_count: sampleCount, updated_at: db.fn.now() });
  res.json({ enrolled: true });
});

router.get('/settings', async (req: AuthRequest, res) => {
  const settings = await db('voice_trigger_settings').where({ user_id: req.userId }).first();
  res.json(settings ?? {});
});

router.patch('/settings', async (req: AuthRequest, res) => {
  const allowed = ['enabled', 'trigger_phrase', 'sensitivity', 'activation_mode',
    'countdown_seconds', 'cooldown_seconds'];
  const update: Record<string, any> = {};
  for (const key of allowed) {
    if (req.body[key] !== undefined) update[key] = req.body[key];
  }
  update.updated_at = db.fn.now();
  await db('voice_trigger_settings')
    .insert({ user_id: req.userId, ...update })
    .onConflict('user_id').merge(update);
  const settings = await db('voice_trigger_settings').where({ user_id: req.userId }).first();
  res.json(settings);
});

router.post('/trigger/test', async (req: AuthRequest, res) => {
  const { phraseConfidence, voiceMatchConfidence } = req.body;
  await db('voice_trigger_events').insert({
    user_id: req.userId,
    phrase_confidence: phraseConfidence ?? 0,
    voice_match_confidence: voiceMatchConfidence ?? null,
    triggered_sos: false
  });
  res.json({ tested: true });
});

export default router;
