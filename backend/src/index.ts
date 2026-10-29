import express from 'express';
import helmet from 'helmet';
import rateLimit from 'express-rate-limit';
import authRouter from './routes/auth';
import voiceRouter from './routes/voice';
import recordingsRouter from './routes/recordings';
import connectionsRouter from './routes/connections';
import privacyRouter from './routes/privacyLockdown';
import messagingRouter from './routes/messaging';
import emergencyContactsRouter from './routes/emergencyContacts';

const app = express();

app.use(helmet());
app.use(express.json({ limit: '1mb' }));

const defaultLimiter = rateLimit({ windowMs: 60_000, max: 60 });
const strictLimiter  = rateLimit({ windowMs: 60_000, max: 10 });
const authLimiter    = rateLimit({ windowMs: 60_000, max: 5 });

app.use('/v1', defaultLimiter);
app.use('/v1/auth', authLimiter);
app.use('/v1/voice/enrollment', strictLimiter);
app.use('/v1/recordings', strictLimiter);

app.use('/v1/auth', authRouter);
app.use('/v1/voice', voiceRouter);
app.use('/v1/recordings', recordingsRouter);
app.use('/v1/connections', connectionsRouter);
app.use('/v1/privacy-lockdown', privacyRouter);
app.use('/v1/messaging', messagingRouter);
app.use('/v1/emergency-contacts', emergencyContactsRouter);

app.use((err: any, _req: express.Request, res: express.Response, _next: express.NextFunction) => {
  const status = err.status ?? 500;
  const message = status < 500 ? err.message : 'Internal server error';
  res.status(status).json({ error: message });
});

export default app;
