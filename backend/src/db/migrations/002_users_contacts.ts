import { Knex } from 'knex';

export async function up(knex: Knex): Promise<void> {

  // ── Users ──────────────────────────────────────────────────────────────────
  await knex.schema.createTable('users', t => {
    t.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    t.string('email').notNullable().unique();
    t.string('password_hash').notNullable();
    t.string('full_name').notNullable();
    t.string('phone').nullable();
    t.string('push_token').nullable();
    t.boolean('email_verified').defaultTo(false);
    t.timestamp('created_at').defaultTo(knex.fn.now());
    t.timestamp('updated_at').defaultTo(knex.fn.now());
  });

  // ── Emergency Contacts ─────────────────────────────────────────────────────
  // These are phone/email contacts the user manually enters.
  // Separate from igotu_connections (which are app-to-app).
  await knex.schema.createTable('emergency_contacts', t => {
    t.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    t.uuid('user_id').notNullable().references('id').inTable('users').onDelete('CASCADE');
    t.string('name').notNullable();
    t.string('phone').nullable();
    t.string('email').nullable();
    t.string('relationship').nullable();   // e.g. "Mom", "Partner"
    t.boolean('notify_sms').defaultTo(true);
    t.boolean('notify_email').defaultTo(false);
    t.boolean('notify_whatsapp').defaultTo(false);
    t.integer('priority').defaultTo(1);    // 1 = highest
    t.timestamp('created_at').defaultTo(knex.fn.now());
    t.timestamp('updated_at').defaultTo(knex.fn.now());
  });

  // ── Emergency Share Links ──────────────────────────────────────────────────
  await knex.schema.createTable('emergency_share_links', t => {
    t.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    t.string('token').notNullable().unique();
    t.uuid('sos_event_id').notNullable();
    t.uuid('user_id').notNullable().references('id').inTable('users').onDelete('CASCADE');
    t.timestamp('expires_at').notNullable();
    t.timestamp('created_at').defaultTo(knex.fn.now());
  });

  // ── SOS Events ────────────────────────────────────────────────────────────
  await knex.schema.createTable('sos_events', t => {
    t.uuid('id').primary();
    t.uuid('user_id').notNullable().references('id').inTable('users').onDelete('CASCADE');
    t.string('trigger_method').notNullable();
    t.float('voice_confidence').nullable();
    t.string('state').notNullable().defaultTo('ACTIVE');
    t.decimal('latitude', 10, 7).nullable();
    t.decimal('longitude', 10, 7).nullable();
    t.timestamp('triggered_at').notNullable();
    t.timestamp('resolved_at').nullable();
    t.timestamp('cancelled_at').nullable();
    t.timestamp('created_at').defaultTo(knex.fn.now());
  });
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.dropTableIfExists('sos_events');
  await knex.schema.dropTableIfExists('emergency_share_links');
  await knex.schema.dropTableIfExists('emergency_contacts');
  await knex.schema.dropTableIfExists('users');
}
