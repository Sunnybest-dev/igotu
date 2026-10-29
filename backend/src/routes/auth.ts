import { Router } from 'express';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { v4 as uuid } from 'uuid';
import db from '../db';

const router = Router();
const SALT_ROUNDS = 12;

// POST /auth/register
router.post('/register', async (req, res, next) => {
  try {
    const { email, password, fullName, phone } = req.body;
    if (!email || !password || !fullName)
      return res.status(400).json({ error: 'email, password, fullName required' });
    if (password.length < 8)
      return res.status(400).json({ error: 'Password must be at least 8 characters' });

    const existing = await db('users').where({ email: email.toLowerCase() }).first();
    if (existing) return res.status(409).json({ error: 'Email already registered' });

    const passwordHash = await bcrypt.hash(password, SALT_ROUNDS);
    const [user] = await db('users').insert({
      id: uuid(),
      email: email.toLowerCase(),
      password_hash: passwordHash,
      full_name: fullName,
      phone: phone ?? null,
    }).returning(['id', 'email', 'full_name', 'phone', 'created_at']);

    const token = signToken(user.id);
    res.status(201).json({ token, user });
  } catch (err) { next(err); }
});

// POST /auth/login
router.post('/login', async (req, res, next) => {
  try {
    const { email, password } = req.body;
    if (!email || !password)
      return res.status(400).json({ error: 'email and password required' });

    const user = await db('users').where({ email: email.toLowerCase() }).first();
    if (!user) return res.status(401).json({ error: 'Invalid credentials' });

    const valid = await bcrypt.compare(password, user.password_hash);
    if (!valid) return res.status(401).json({ error: 'Invalid credentials' });

    const token = signToken(user.id);
    res.json({ token, user: { id: user.id, email: user.email, fullName: user.full_name, phone: user.phone } });
  } catch (err) { next(err); }
});

function signToken(userId: string): string {
  return jwt.sign({ sub: userId }, process.env.JWT_SECRET!, { expiresIn: '30d' });
}

export default router;
