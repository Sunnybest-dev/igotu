import { Knex } from 'knex';

export async function up(knex: Knex): Promise<void> {

  // ── Voice ──────────────────────────────────────────────────────────────────
  await knex.schema.createTable('voice_profiles', t => {
    t.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    t.uuid('user_id').notNullable().references('id').inTable('users').onDelete('CASCADE');
    t.timestamp('created_at').defaultTo(knex.fn.now());
    t.timestamp('updated_at').defaultTo(knex.fn.now());
    // Raw embedding NOT stored here — kept on-device only.
    // This row tracks enrollment status and metadata only.
    t.boolean('enrolled').defaultTo(false);
    t.integer('sample_count').defaultTo(0);
    t.timestamp('deleted_at').nullable();
  });

  await knex.schema.createTable('voice_trigger_settings', t => {
    t.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    t.uuid('user_id').notNullable().unique().references('id').inTable('users').onDelete('CASCADE');
    t.boolean('enabled').defaultTo(false);
    t.string('trigger_phrase').defaultTo('IGOT-U');
    t.string('sensitivity').defaultTo('MEDIUM');
    t.string('activation_mode').defaultTo('COUNTDOWN');
    t.integer('countdown_seconds').defaultTo(5);
    t.integer('cooldown_seconds').defaultTo(30);
    t.timestamp('updated_at').defaultTo(knex.fn.now());
  });

  await knex.schema.createTable('voice_trigger_events', t => {
    t.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    t.uuid('user_id').notNullable().references('id').inTable('users');
    t.uuid('sos_event_id').nullable();
    t.float('phrase_confidence').notNullable();
    t.float('voice_match_confidence').nullable();
    t.boolean('triggered_sos').defaultTo(false);
    t.boolean('cancelled').defaultTo(false);
    t.timestamp('created_at').defaultTo(knex.fn.now());
  });

  // ── Emergency Recordings ───────────────────────────────────────────────────
  await knex.schema.createTable('emergency_recordings', t => {
    t.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    t.uuid('sos_event_id').notNullable();
    t.uuid('user_id').notNullable().references('id').inTable('users');
    t.timestamp('created_at').defaultTo(knex.fn.now());
    t.timestamp('started_at').nullable();
    t.timestamp('ended_at').nullable();
    t.integer('duration_seconds').nullable();
    t.string('media_type').notNullable(); // VIDEO_AUDIO, AUDIO_ONLY, etc.
    t.bigInteger('file_size_bytes').nullable();
    t.string('storage_location').nullable(); // S3 key — never public
    t.string('upload_status').defaultTo('PENDING');
    t.boolean('encryption_status').defaultTo(false);
    t.timestamp('retention_expiry').nullable();
  });

  await knex.schema.createTable('recording_chunks', t => {
    t.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    t.uuid('recording_id').notNullable().references('id').inTable('emergency_recordings').onDelete('CASCADE');
    t.integer('chunk_index').notNullable();
    t.bigInteger('size_bytes').notNullable();
    t.string('upload_status').defaultTo('PENDING');
    t.timestamp('uploaded_at').nullable();
  });

  await knex.schema.createTable('recording_access_grants', t => {
    t.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    t.uuid('recording_id').notNullable().references('id').inTable('emergency_recordings').onDelete('CASCADE');
    t.uuid('granted_to_user_id').notNullable().references('id').inTable('users');
    t.timestamp('granted_at').defaultTo(knex.fn.now());
    t.timestamp('expires_at').nullable();
    t.boolean('revoked').defaultTo(false);
  });

  await knex.schema.createTable('recording_access_logs', t => {
    t.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    t.uuid('recording_id').notNullable().references('id').inTable('emergency_recordings');
    t.uuid('accessed_by_user_id').notNullable().references('id').inTable('users');
    t.timestamp('accessed_at').defaultTo(knex.fn.now());
    t.string('access_method'); // signed_url, api
  });

  // ── Privacy Lockdown ───────────────────────────────────────────────────────
  await knex.schema.createTable('privacy_lockdown_settings', t => {
    t.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    t.uuid('user_id').notNullable().unique().references('id').inTable('users').onDelete('CASCADE');
    t.boolean('enabled').defaultTo(false);
    t.boolean('hide_sensitive_notifications').defaultTo(true);
    t.boolean('protect_igotu_content').defaultTo(true);
    t.boolean('activate_with_sos').defaultTo(true);
    t.timestamp('updated_at').defaultTo(knex.fn.now());
  });

  await knex.schema.createTable('protected_app_entries', t => {
    t.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    t.uuid('user_id').notNullable().references('id').inTable('users').onDelete('CASCADE');
    t.string('package_name').notNullable();
    t.string('display_name').notNullable();
    t.unique(['user_id', 'package_name']);
  });

  // ── Connections ────────────────────────────────────────────────────────────
  await knex.schema.createTable('igotu_connections', t => {
    t.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    t.uuid('user_id').notNullable().references('id').inTable('users');
    t.uuid('connected_user_id').notNullable().references('id').inTable('users');
    t.string('status').notNullable().defaultTo('CONNECTED'); // CONNECTED, BLOCKED
    t.timestamp('created_at').defaultTo(knex.fn.now());
    t.string('verification_method').nullable();
    t.unique(['user_id', 'connected_user_id']);
  });

  await knex.schema.createTable('connection_requests', t => {
    t.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    t.uuid('from_user_id').notNullable().references('id').inTable('users');
    t.uuid('to_user_id').nullable().references('id').inTable('users'); // nullable = open invitation
    t.string('invitation_token').notNullable().unique();
    t.timestamp('created_at').defaultTo(knex.fn.now());
    t.timestamp('expires_at').notNullable();
    t.boolean('accepted').nullable(); // null=pending, true=accepted, false=rejected
  });

  await knex.schema.createTable('connection_permissions', t => {
    t.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    t.uuid('connection_id').notNullable().unique().references('id').inTable('igotu_connections').onDelete('CASCADE');
    t.boolean('receive_sos_alerts').defaultTo(true);
    t.boolean('receive_live_location').defaultTo(false);
    t.boolean('receive_journey_alerts').defaultTo(false);
    t.boolean('receive_check_in_alerts').defaultTo(false);
    t.boolean('receive_emergency_recordings').defaultTo(false);
    t.timestamp('updated_at').defaultTo(knex.fn.now());
  });

  // ── Messaging ──────────────────────────────────────────────────────────────
  await knex.schema.createTable('messaging_contacts', t => {
    t.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    t.uuid('user_id').notNullable().references('id').inTable('users').onDelete('CASCADE');
    t.string('channel').notNullable(); // whatsapp, sms, email, push
    t.string('address').notNullable(); // phone, email, push token
    t.string('display_name').notNullable();
    t.boolean('enabled').defaultTo(true);
    t.unique(['user_id', 'channel', 'address']);
  });

  await knex.schema.createTable('emergency_notification_preferences', t => {
    t.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    t.uuid('user_id').notNullable().references('id').inTable('users').onDelete('CASCADE');
    t.uuid('contact_id').notNullable().references('id').inTable('messaging_contacts').onDelete('CASCADE');
    t.boolean('send_sos').defaultTo(true);
    t.boolean('send_location').defaultTo(false);
    t.boolean('send_recording').defaultTo(false);
    t.boolean('send_journey').defaultTo(false);
  });

  await knex.schema.createTable('emergency_delivery_attempts', t => {
    t.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    t.uuid('sos_event_id').notNullable();
    t.uuid('recipient_user_id').nullable();
    t.string('channel').notNullable();
    t.string('recipient_address').notNullable();
    t.string('status').notNullable().defaultTo('PENDING'); // SENT, DELIVERED, READ, FAILED
    t.string('provider_message_id').nullable();
    t.string('error').nullable();
    t.timestamp('sent_at').nullable();
    t.timestamp('delivered_at').nullable();
    t.timestamp('read_at').nullable();
    t.timestamp('created_at').defaultTo(knex.fn.now());
  });
}

export async function down(knex: Knex): Promise<void> {
  const tables = [
    'emergency_delivery_attempts', 'emergency_notification_preferences',
    'messaging_contacts', 'connection_permissions', 'connection_requests',
    'igotu_connections', 'protected_app_entries', 'privacy_lockdown_settings',
    'recording_access_logs', 'recording_access_grants', 'recording_chunks',
    'emergency_recordings', 'voice_trigger_events', 'voice_trigger_settings',
    'voice_profiles'
  ];
  for (const t of tables.reverse()) await knex.schema.dropTableIfExists(t);
}
