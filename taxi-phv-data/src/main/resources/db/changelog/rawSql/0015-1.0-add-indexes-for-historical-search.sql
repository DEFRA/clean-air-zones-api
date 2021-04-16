CREATE INDEX IF NOT EXISTS idx_btree_logged_actions_vrm_original ON audit.logged_actions USING BTREE ((original_data->>'vrm'));
CREATE INDEX IF NOT EXISTS idx_btree_logged_actions_vrm_new ON audit.logged_actions USING BTREE ((new_data->>'vrm'));
CREATE INDEX IF NOT EXISTS idx_logged_actions_table_name ON audit.logged_actions (table_name);