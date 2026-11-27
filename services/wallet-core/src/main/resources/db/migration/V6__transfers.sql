-- T8: optional description of a transfer, shown to both sides.
ALTER TABLE transactions ADD COLUMN description VARCHAR(140);

-- R1: a transfer from a wallet that does not exist (or is not the customer's) has no known currency.
ALTER TABLE rejected_operations ALTER COLUMN currency DROP NOT NULL;
