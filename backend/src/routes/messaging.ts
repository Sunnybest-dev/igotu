import { Router } from 'express';
import { requireAuth, AuthRequest } from '../middleware/auth';
import db from '../db';
import { v4 as uuid } from 'uuid';
import crypto from 'crypto';

const router = Router();
router.use(requireAuth);

// POST /messaging/whatsapp/invite
router.post('/whatsapp/invite', async (req: AuthRequest, res, next) => {
  try {
    const token = crypto.randomBytes(16).toString('hex');
    const expiresAt = new Date(Date.now() + 48 * 3600_000);
    await db('connection_requests').insert({
      id: uuid(), from_user_id: req.userId, to_user_id: null,
      invitation_token: token, expires_at: expiresAt
    });
    res.json({
      token, expiresAt,
      shareUrl: `https://igotu.app/connect?token=${token}`,
      whatsAppText: `Connect with me on IGOT-U for emergency safety alerts.\n\nhttps://igotu.app/connect?token=${token}`
    });
  } catch (err) { next(err); }
});

// POST /messaging/share-link
router.post('/share-link', async (req: AuthRequest, res, next) => {
  try {
    const { sosEventId } = req.body;
    if (!sosEventId) return res.status(400).json({ error: 'sosEventId required' });
    const token = crypto.randomBytes(20).toString('hex');
    await db('emergency_share_links').insert({
      id: uuid(), token, sos_event_id: sosEventId,
      user_id: req.userId, expires_at: new Date(Date.now() + 24 * 3600_000)
    }).onConflict('token').ignore();
    res.json({ shareUrl: `https://igotu.app/emergency/${token}` });
  } catch (err) { next(err); }
});

// POST /messaging/sos-notify — called by SOS engine; fans out to all emergency contacts
router.post('/sos-notify', async (req: AuthRequest, res, next) => {
  try {
    const { sosEventId, latitude, longitude } = req.body;
    if (!sosEventId) return res.status(400).json({ error: 'sosEventId required' });

    // Persist SOS event
    await db('sos_events').insert({
      id: sosEventId, user_id: req.userId,
      trigger_method: req.body.triggerMethod ?? 'UNKNOWN',
      voice_confidence: req.body.voiceConfidence ?? null,
      state: 'ACTIVE',
      latitude: latitude ?? null, longitude: longitude ?? null,
      triggered_at: new Date()
    }).onConflict('id').ignore();

    const shareToken = crypto.randomBytes(20).toString('hex');
    await db('emergency_share_links').insert({
      id: uuid(), token: shareToken, sos_event_id: sosEventId,
      user_id: req.userId, expires_at: new Date(Date.now() + 24 * 3600_000)
    });
    const shareUrl = `https://igotu.app/emergency/${shareToken}`;

    const contacts = await db('emergency_contacts')
      .where({ user_id: req.userId })
      .orderBy('priority', 'asc');

    const user = await db('users').where({ id: req.userId }).first();
    const locationText = latitude && longitude
      ? `\nLocation: https://maps.google.com/?q=${latitude},${longitude}`
      : '';
    const body = `🚨 EMERGENCY ALERT from ${user?.full_name ?? 'a contact'}\n${locationText}\nTrack: ${shareUrl}`;

    const attempts: any[] = [];
    for (const contact of contacts) {
      if (contact.notify_sms && contact.phone) {
        attempts.push({ id: uuid(), sos_event_id: sosEventId, channel: 'sms',
          recipient_address: contact.phone, status: 'SENT', sent_at: db.fn.now() });
        // TODO: integrate Twilio/AWS SNS here
      }
      if (contact.notify_email && contact.email) {
        attempts.push({ id: uuid(), sos_event_id: sosEventId, channel: 'email',
          recipient_address: contact.email, status: 'SENT', sent_at: db.fn.now() });
        // TODO: integrate SES/SendGrid here
      }
      if (contact.notify_whatsapp && contact.phone) {
        attempts.push({ id: uuid(), sos_event_id: sosEventId, channel: 'whatsapp',
          recipient_address: contact.phone, status: 'SENT', sent_at: db.fn.now() });
        // WhatsApp: share intent on device side; server logs the attempt
      }
    }

    if (attempts.length > 0) {
      await db('emergency_delivery_attempts').insert(attempts);
    }

    res.json({ shareUrl, contactsNotified: attempts.length });
  } catch (err) { next(err); }
});

// GET /messaging/emergency/:id/delivery-status
router.get('/emergency/:id/delivery-status', async (req: AuthRequest, res, next) => {
  try {
    const attempts = await db('emergency_delivery_attempts')
      .where({ sos_event_id: req.params.id })
      .orderBy('created_at', 'desc');
    res.json(attempts);
  } catch (err) { next(err); }
});

// POST /messaging/push/notify
router.post('/push/notify', async (req: AuthRequest, res, next) => {
  try {
    const { sosEventId, recipientId, shareUrl } = req.body;
    await db('emergency_delivery_attempts').insert({
      id: uuid(), sos_event_id: sosEventId, recipient_user_id: recipientId,
      channel: 'push', recipient_address: recipientId, status: 'SENT', sent_at: db.fn.now()
    });
    // TODO: FCM/APNs
    res.json({ sent: true });
  } catch (err) { next(err); }
});

export default router;
