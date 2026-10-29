import request from 'supertest';
import app from '../index';
import db from '../db';
import jwt from 'jsonwebtoken';

const makeToken = (userId: string) =>
  jwt.sign({ sub: userId }, process.env.JWT_SECRET ?? 'test-secret');

const user1 = 'user-uuid-0001';
const user2 = 'user-uuid-0002';
const token1 = makeToken(user1);
const token2 = makeToken(user2);

beforeAll(async () => {
  await db.migrate.latest();
  // Seed minimal user rows
  await db('users').insert([
    { id: user1, email: 'a@test.com', phone: '+10000000001' },
    { id: user2, email: 'b@test.com', phone: '+10000000002' }
  ]).onConflict('id').ignore();
});

afterAll(async () => {
  await db.destroy();
});

// ── Voice ──────────────────────────────────────────────────────────────────

describe('Voice enrollment', () => {
  it('records enrollment status', async () => {
    const res = await request(app)
      .post('/v1/voice/enrollment')
      .set('Authorization', `Bearer ${token1}`)
      .send({ sampleCount: 5 });
    expect(res.status).toBe(200);
    expect(res.body.enrolled).toBe(true);
  });

  it('returns settings after patch', async () => {
    await request(app)
      .patch('/v1/voice/settings')
      .set('Authorization', `Bearer ${token1}`)
      .send({ enabled: true, trigger_phrase: 'IGOT-U', sensitivity: 'HIGH' });

    const res = await request(app)
      .get('/v1/voice/settings')
      .set('Authorization', `Bearer ${token1}`);
    expect(res.body.enabled).toBe(true);
    expect(res.body.sensitivity).toBe('HIGH');
  });
});

// ── Connections ────────────────────────────────────────────────────────────

describe('Connections', () => {
  let requestId: string;

  it('user1 sends connection request to user2', async () => {
    const res = await request(app)
      .post('/v1/connections/request')
      .set('Authorization', `Bearer ${token1}`)
      .send({ toUserId: user2 });
    expect(res.status).toBe(201);
    requestId = res.body.id;
  });

  it('user2 accepts request', async () => {
    const res = await request(app)
      .post(`/v1/connections/request/${requestId}/accept`)
      .set('Authorization', `Bearer ${token2}`);
    expect(res.status).toBe(200);
  });

  it('default permissions do not include live location', async () => {
    const res = await request(app)
      .get('/v1/connections')
      .set('Authorization', `Bearer ${token1}`);
    expect(res.status).toBe(200);
    const conn = res.body[0];
    expect(conn.receive_live_location).toBe(false);
    expect(conn.receive_sos_alerts).toBe(true);
  });

  it('user1 can update permissions', async () => {
    const connections = await request(app)
      .get('/v1/connections')
      .set('Authorization', `Bearer ${token1}`);
    const connId = connections.body[0].id;

    const res = await request(app)
      .patch(`/v1/connections/${connId}/permissions`)
      .set('Authorization', `Bearer ${token1}`)
      .send({ receive_live_location: true });
    expect(res.body.receive_live_location).toBe(true);
  });

  it('user2 cannot modify user1 connection', async () => {
    const connections = await request(app)
      .get('/v1/connections')
      .set('Authorization', `Bearer ${token1}`);
    const connId = connections.body[0].id;

    const res = await request(app)
      .patch(`/v1/connections/${connId}/permissions`)
      .set('Authorization', `Bearer ${token2}`)
      .send({ receive_live_location: true });
    expect(res.status).toBe(403);
  });

  it('expired request cannot be accepted', async () => {
    // Insert an already-expired request
    const [expiredReq] = await db('connection_requests').insert({
      from_user_id: user1,
      to_user_id: user2,
      invitation_token: 'expired-token',
      expires_at: new Date(Date.now() - 1000)
    }).returning('*');

    const res = await request(app)
      .post(`/v1/connections/request/${expiredReq.id}/accept`)
      .set('Authorization', `Bearer ${token2}`);
    expect(res.status).toBe(410);
  });
});

// ── Recordings IDOR ────────────────────────────────────────────────────────

describe('Recordings IDOR protection', () => {
  let recordingId: string;

  it('user1 creates a recording', async () => {
    const res = await request(app)
      .post('/v1/recordings')
      .set('Authorization', `Bearer ${token1}`)
      .send({ sosEventId: 'sos-001', mediaType: 'VIDEO_AUDIO' });
    expect(res.status).toBe(201);
    recordingId = res.body.id;
  });

  it('user2 cannot access user1 recording without grant', async () => {
    const res = await request(app)
      .get(`/v1/recordings/${recordingId}`)
      .set('Authorization', `Bearer ${token2}`);
    expect(res.status).toBe(403);
  });

  it('user2 cannot delete user1 recording', async () => {
    const res = await request(app)
      .delete(`/v1/recordings/${recordingId}`)
      .set('Authorization', `Bearer ${token2}`);
    expect(res.status).toBe(403);
  });

  it('user1 can access their own recording', async () => {
    const res = await request(app)
      .get(`/v1/recordings/${recordingId}`)
      .set('Authorization', `Bearer ${token1}`);
    expect(res.status).toBe(200);
  });
});

// ── Privacy Lockdown ───────────────────────────────────────────────────────

describe('Privacy lockdown', () => {
  it('saves settings', async () => {
    const res = await request(app)
      .patch('/v1/privacy-lockdown/settings')
      .set('Authorization', `Bearer ${token1}`)
      .send({
        enabled: true,
        hide_sensitive_notifications: true,
        protectedApps: [{ packageName: 'com.whatsapp', displayName: 'WhatsApp' }]
      });
    expect(res.status).toBe(200);
  });

  it('activate returns active true', async () => {
    const res = await request(app)
      .post('/v1/privacy-lockdown/activate')
      .set('Authorization', `Bearer ${token1}`);
    expect(res.body.active).toBe(true);
  });

  it('deactivate returns active false', async () => {
    const res = await request(app)
      .post('/v1/privacy-lockdown/deactivate')
      .set('Authorization', `Bearer ${token1}`);
    expect(res.body.active).toBe(false);
  });
});

// ── WhatsApp invite ────────────────────────────────────────────────────────

describe('WhatsApp invite', () => {
  it('generates a short-lived token and share URL', async () => {
    const res = await request(app)
      .post('/v1/messaging/whatsapp/invite')
      .set('Authorization', `Bearer ${token1}`)
      .send({ phoneNumber: '+447700000000' });
    expect(res.status).toBe(200);
    expect(res.body.token).toBeDefined();
    expect(res.body.shareUrl).toMatch(/^https:\/\/igotu\.app\/connect\?token=/);
    expect(new Date(res.body.expiresAt).getTime()).toBeGreaterThan(Date.now());
  });
});

// ── Full emergency flow ────────────────────────────────────────────────────

describe('Full emergency flow', () => {
  it('creates recording, uploads chunk, completes, checks delivery status', async () => {
    // 1. Create recording
    const recRes = await request(app)
      .post('/v1/recordings')
      .set('Authorization', `Bearer ${token1}`)
      .send({ sosEventId: 'sos-full-001', mediaType: 'VIDEO_AUDIO' });
    expect(recRes.status).toBe(201);
    const recId = recRes.body.id;

    // 2. Upload chunk
    const chunkRes = await request(app)
      .post(`/v1/recordings/${recId}/upload`)
      .set('Authorization', `Bearer ${token1}`)
      .send({ chunkIndex: 0, sizeBytes: 512000 });
    expect(chunkRes.body.status).toBe('COMPLETE');

    // 3. Complete recording
    const completeRes = await request(app)
      .post(`/v1/recordings/${recId}/complete`)
      .set('Authorization', `Bearer ${token1}`);
    expect(completeRes.body.upload_status).toBe('COMPLETE');

    // 4. Push notification
    const pushRes = await request(app)
      .post('/v1/messaging/push/notify')
      .set('Authorization', `Bearer ${token1}`)
      .send({ sosEventId: 'sos-full-001', recipientId: user2, shareUrl: 'https://igotu.app/emergency/abc' });
    expect(pushRes.body.sent).toBe(true);

    // 5. Delivery status
    const statusRes = await request(app)
      .get('/v1/messaging/emergency/sos-full-001/delivery-status')
      .set('Authorization', `Bearer ${token1}`);
    expect(statusRes.status).toBe(200);
    expect(statusRes.body.length).toBeGreaterThan(0);
    expect(statusRes.body[0].status).toBe('SENT');
  });
});
