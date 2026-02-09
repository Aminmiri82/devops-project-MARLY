-- Add eco_mode_enabled column to journey table
ALTER TABLE journey ADD COLUMN eco_mode_enabled BOOLEAN NOT NULL DEFAULT FALSE;
