import type { Knex } from 'knex';
import path from 'path';

const config: Knex.Config = {
  client: 'pg',
  connection: process.env.DATABASE_URL ?? 'postgresql://postgres:postgres@localhost:5432/igotu',
  pool: { min: 2, max: 10 },
  migrations: {
    directory: path.join(__dirname, 'src/db/migrations'),
    extension: 'ts',
  },
};

export default config;
