import { Router } from 'express';
import { requireAuth, AuthRequest, assertOwnership } from '../middleware/auth';
import db from '../db';
import { v4 as uuid } from 'uuid';
import { generateSignedUrl } from '../services/storage';

const router = Router();
router.use(requireAuth);

// POST /recordings — create recording metadata record
router.post('/', async (req: AuthRequest, res) => {
  const id = uuid();
  const { sosEventId, mediaType } = req.body;
  const retentionDays = req.body.retentionDays ?? 7;
  const retentionExpiry = new Date(Date.now() + retentionDays * 86400_000);

  const [recording] = await db('emergency_recordings')
    .insert({
      id,
      sos_event_id: sosEventId,
      user_id: req.userId,
      media_type: mediaType,
      retention_expiry: retentionExpiry
    })
    .returning('*');

  res.status(201).json(recording);
});

// POST /recordings/:id/upload — accept a chunk, track progress
router.post('/:id/upload', async (req: AuthRequest, res) => {
  const recording = await db('emergency_recordings').where({ id: req.params.id }).first();
  if (!recording) return res.status(404).json({ error: 'Not found' });
  assertOwnership(recording.user_id, req.userId!);

  const { chunkIndex, sizeBytes } = req.body;
  await db('recording_chunks').insert({
    recording_id: req.params.id,
    chunk_index: chunkIndex,
    size_bytes: sizeBytes,
    upload_status: 'COMPLETE',
    uploaded_at: db.fn.now()
  }).onConflict(['recording_id', 'chunk_index']).merge({ upload_status: 'COMPLETE', uploaded_at: db.fn.now() });

  res.json({ chunkIndex, status: 'COMPLETE' });
});

// POST /recordings/:id/complete — mark recording finished
router.post('/:id/complete', async (req: AuthRequest, res) => {
  const recording = await db('emergency_recordings').where({ id: req.params.id }).first();
  if (!recording) return res.status(404).json({ error: 'Not found' });
  assertOwnership(recording.user_id, req.userId!);

  const [updated] = await db('emergency_recordings')
    .where({ id: req.params.id })
    .update({
      ended_at: db.fn.now(),
      upload_status: 'COMPLETE',
      encryption_status: true
    })
    .returning('*');

  res.json(updated);
});

// GET /recordings/:id — returns metadata + signed URL for authorized users
router.get('/:id', async (req: AuthRequest, res) => {
  const recording = await db('emergency_recordings').where({ id: req.params.id }).first();
  if (!recording) return res.status(404).json({ error: 'Not found' });

  const isOwner = recording.user_id === req.userId;
  const hasGrant = await db('recording_access_grants')
    .where({ recording_id: req.params.id, granted_to_user_id: req.userId, revoked: false })
    .where('expires_at', '>', new Date())
    .first();

  if (!isOwner && !hasGrant) return res.status(403).json({ error: 'Forbidden' });

  // Log access
  await db('recording_access_logs').insert({
    recording_id: req.params.id,
    accessed_by_user_id: req.userId,
    access_method: 'api'
  });

  const signedUrl = recording.storage_location
    ? await generateSignedUrl(recording.storage_location)
    : null;

  res.json({ ...recording, signedUrl });
});

// DELETE /recordings/:id
router.delete('/:id', async (req: AuthRequest, res) => {
  const recording = await db('emergency_recordings').where({ id: req.params.id }).first();
  if (!recording) return res.status(404).json({ error: 'Not found' });
  assertOwnership(recording.user_id, req.userId!);

  await db('emergency_recordings').where({ id: req.params.id }).delete();
  // TODO: delete from S3
  res.status(204).send();
});

export default router;
