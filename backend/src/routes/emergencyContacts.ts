import { Router } from 'express';
import { requireAuth, AuthRequest, assertOwnership } from '../middleware/auth';
import db from '../db';
import { v4 as uuid } from 'uuid';

const router = Router();
router.use(requireAuth);

// GET /emergency-contacts
router.get('/', async (req: AuthRequest, res, next) => {
  try {
    const contacts = await db('emergency_contacts')
      .where({ user_id: req.userId })
      .orderBy('priority', 'asc')
      .orderBy('created_at', 'asc');
    res.json(contacts);
  } catch (err) { next(err); }
});

// POST /emergency-contacts
router.post('/', async (req: AuthRequest, res, next) => {
  try {
    const { name, phone, email, relationship, notifySms, notifyEmail, notifyWhatsapp, priority } = req.body;
    if (!name) return res.status(400).json({ error: 'name required' });
    if (!phone && !email) return res.status(400).json({ error: 'phone or email required' });

    const [contact] = await db('emergency_contacts').insert({
      id: uuid(),
      user_id: req.userId,
      name,
      phone: phone ?? null,
      email: email ?? null,
      relationship: relationship ?? null,
      notify_sms: notifySms ?? true,
      notify_email: notifyEmail ?? false,
      notify_whatsapp: notifyWhatsapp ?? false,
      priority: priority ?? 1,
    }).returning('*');

    res.status(201).json(contact);
  } catch (err) { next(err); }
});

// PATCH /emergency-contacts/:id
router.patch('/:id', async (req: AuthRequest, res, next) => {
  try {
    const contact = await db('emergency_contacts').where({ id: req.params.id }).first();
    if (!contact) return res.status(404).json({ error: 'Not found' });
    assertOwnership(contact.user_id, req.userId!);

    const allowed = ['name', 'phone', 'email', 'relationship',
      'notify_sms', 'notify_email', 'notify_whatsapp', 'priority'];
    const update: Record<string, any> = { updated_at: db.fn.now() };
    for (const key of allowed) {
      const camel = key.replace(/_([a-z])/g, (_, c) => c.toUpperCase());
      if (req.body[camel] !== undefined) update[key] = req.body[camel];
      else if (req.body[key] !== undefined) update[key] = req.body[key];
    }

    const [updated] = await db('emergency_contacts')
      .where({ id: req.params.id })
      .update(update)
      .returning('*');
    res.json(updated);
  } catch (err) { next(err); }
});

// DELETE /emergency-contacts/:id
router.delete('/:id', async (req: AuthRequest, res, next) => {
  try {
    const contact = await db('emergency_contacts').where({ id: req.params.id }).first();
    if (!contact) return res.status(404).json({ error: 'Not found' });
    assertOwnership(contact.user_id, req.userId!);

    await db('emergency_contacts').where({ id: req.params.id }).delete();
    res.status(204).send();
  } catch (err) { next(err); }
});

export default router;
