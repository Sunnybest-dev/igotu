import { Router } from 'express';
import { requireAuth, AuthRequest, assertOwnership } from '../middleware/auth';
import db from '../db';
import { v4 as uuid } from 'uuid';
import crypto from 'crypto';

const router = Router();
router.use(requireAuth);

// GET /connections
router.get('/', async (req: AuthRequest, res) => {
  const connections = await db('igotu_connections as c')
    .join('connection_permissions as p', 'p.connection_id', 'c.id')
    .where('c.user_id', req.userId)
    .where('c.status', 'CONNECTED')
    .select('c.*', 'p.*');
  res.json(connections);
});

// POST /connections/request
router.post('/request', async (req: AuthRequest, res) => {
  const { toUserId } = req.body;
  if (!toUserId) return res.status(400).json({ error: 'toUserId required' });

  const token = crypto.randomBytes(24).toString('hex');
  const expiresAt = new Date(Date.now() + 7 * 86400_000); // 7 days

  const [request] = await db('connection_requests')
    .insert({
      id: uuid(),
      from_user_id: req.userId,
      to_user_id: toUserId,
      invitation_token: token,
      expires_at: expiresAt
    })
    .returning('*');

  res.status(201).json(request);
});

// POST /connections/request/:id/accept
router.post('/request/:id/accept', async (req: AuthRequest, res) => {
  const request = await db('connection_requests').where({ id: req.params.id }).first();
  if (!request) return res.status(404).json({ error: 'Not found' });
  if (request.to_user_id !== req.userId) return res.status(403).json({ error: 'Forbidden' });
  if (request.accepted !== null) return res.status(409).json({ error: 'Already resolved' });
  if (new Date(request.expires_at) < new Date()) return res.status(410).json({ error: 'Expired' });

  await db('connection_requests').where({ id: req.params.id }).update({ accepted: true });

  const connectionId = uuid();
  // Create bidirectional connection rows
  await db('igotu_connections').insert([
    { id: connectionId, user_id: request.from_user_id, connected_user_id: request.to_user_id },
    { id: uuid(), user_id: request.to_user_id, connected_user_id: request.from_user_id }
  ]);

  // Default permissions — location NOT granted automatically
  await db('connection_permissions').insert({
    connection_id: connectionId,
    receive_sos_alerts: true,
    receive_live_location: false,
    receive_journey_alerts: false,
    receive_check_in_alerts: false,
    receive_emergency_recordings: false
  });

  const connection = await db('igotu_connections').where({ id: connectionId }).first();
  res.json(connection);
});

// POST /connections/request/:id/reject
router.post('/request/:id/reject', async (req: AuthRequest, res) => {
  const request = await db('connection_requests').where({ id: req.params.id }).first();
  if (!request) return res.status(404).json({ error: 'Not found' });
  if (request.to_user_id !== req.userId) return res.status(403).json({ error: 'Forbidden' });

  await db('connection_requests').where({ id: req.params.id }).update({ accepted: false });
  res.status(204).send();
});

// DELETE /connections/:id
router.delete('/:id', async (req: AuthRequest, res) => {
  const connection = await db('igotu_connections').where({ id: req.params.id }).first();
  if (!connection) return res.status(404).json({ error: 'Not found' });
  assertOwnership(connection.user_id, req.userId!);

  await db('igotu_connections').where({ id: req.params.id }).delete();
  res.status(204).send();
});

// PATCH /connections/:id/permissions
router.patch('/:id/permissions', async (req: AuthRequest, res) => {
  const connection = await db('igotu_connections').where({ id: req.params.id }).first();
  if (!connection) return res.status(404).json({ error: 'Not found' });
  assertOwnership(connection.user_id, req.userId!);

  const allowed = ['receive_sos_alerts', 'receive_live_location', 'receive_journey_alerts',
    'receive_check_in_alerts', 'receive_emergency_recordings'];
  const update: Record<string, any> = { updated_at: db.fn.now() };
  for (const key of allowed) {
    if (req.body[key] !== undefined) update[key] = req.body[key];
  }

  const [updated] = await db('connection_permissions')
    .where({ connection_id: req.params.id })
    .update(update)
    .returning('*');

  res.json(updated);
});

// POST /connections/:id/block
router.post('/:id/block', async (req: AuthRequest, res) => {
  const connection = await db('igotu_connections').where({ id: req.params.id }).first();
  if (!connection) return res.status(404).json({ error: 'Not found' });
  assertOwnership(connection.user_id, req.userId!);

  await db('igotu_connections').where({ id: req.params.id }).update({ status: 'BLOCKED' });
  res.status(204).send();
});

// POST /connections/invitation — generate QR/link invitation
router.post('/invitation', async (req: AuthRequest, res) => {
  const token = crypto.randomBytes(24).toString('hex');
  const expiresAt = new Date(Date.now() + 48 * 3600_000); // 48 hours

  await db('connection_requests').insert({
    id: uuid(),
    from_user_id: req.userId,
    to_user_id: null, // open invitation — any user can accept
    invitation_token: token,
    expires_at: expiresAt
  });

  res.json({
    token,
    expiresAt,
    qrCodeData: token,
    shareUrl: `https://igotu.app/connect?token=${token}`
  });
});

export default router;
