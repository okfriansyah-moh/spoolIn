ALTER TABLE credit ADD COLUMN billing_order integer CHECK (billing_order IS NULL OR billing_order >= 0);
