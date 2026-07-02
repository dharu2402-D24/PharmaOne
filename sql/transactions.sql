-- ============================================================
-- TASK 6: DATABASE TRANSACTIONS — PharmaOne
-- ============================================================
-- This file demonstrates DB transactions including conflicting
-- scenarios and their effects on database consistency and
-- concurrency handling.
--
-- All scenarios use Batch 103 (Limcee - highest inventory)
-- Deadlock uses Batch 103 + 106 (two different rows)
--
-- Scenarios covered:
--   1. Basic COMMIT Transaction (Atomicity)
--   2. ROLLBACK on Error (Consistency)
--   3. Dirty Read Prevention (Isolation Levels)
--   4. Lost Update / Conflicting Write (Concurrency)
--   5. Deadlock Detection & Resolution
-- ============================================================

USE pharma_db;


-- ============================================================
-- SCENARIO 1: BASIC COMMIT TRANSACTION (Atomicity)
-- ============================================================
-- Demonstrates: A multi-step transaction that COMMITS atomically.
-- Business Case: A sales order must simultaneously insert the
--   order record AND deduct inventory. Both must succeed together.
-- ============================================================

-- BEFORE STATE
SELECT 'SCENARIO 1: BEFORE STATE' AS label;
SELECT i.batch_id, b.batch_number, i.quantity_available
FROM Inventory i JOIN Batch b ON i.batch_id = b.batch_id
WHERE b.batch_id = 103;

-- Begin atomic transaction
START TRANSACTION;

    -- Step 1: Insert a new sales order
    INSERT INTO Sales_Order (sales_order_id, order_date, quantity_sold, selling_price, total_amount, status, customer_id, batch_id)
    VALUES (9901, CURDATE(), 50, 2.00, 100.00, 'Completed', 401, 103);

    -- Step 2: Deduct inventory for the batch sold
    UPDATE Inventory
    SET quantity_available = quantity_available - 50,
        last_updated = NOW()
    WHERE batch_id = 103;

COMMIT;

-- AFTER STATE: Verify both changes persisted
SELECT 'SCENARIO 1: AFTER COMMIT' AS label;
SELECT i.batch_id, b.batch_number, i.quantity_available
FROM Inventory i JOIN Batch b ON i.batch_id = b.batch_id
WHERE b.batch_id = 103;

SELECT sales_order_id, quantity_sold, total_amount, status
FROM Sales_Order WHERE sales_order_id = 9901;

-- Cleanup
DELETE FROM Sales_Order WHERE sales_order_id = 9901;
UPDATE Inventory SET quantity_available = quantity_available + 50 WHERE batch_id = 103;


-- ============================================================
-- SCENARIO 2: ROLLBACK ON ERROR (Consistency)
-- ============================================================
-- Demonstrates: A transaction that encounters an error midway.
--   The CHECK constraint (quantity_available >= 0) prevents the
--   invalid UPDATE. We then ROLLBACK to undo the already-inserted
--   Sales Order from Step 1.
-- Business Case: Selling more units than available stock must
--   abort the entire transaction, leaving DB unchanged.
-- ============================================================

SELECT 'SCENARIO 2: BEFORE STATE' AS label;
SELECT i.batch_id, b.batch_number, i.quantity_available
FROM Inventory i JOIN Batch b ON i.batch_id = b.batch_id
WHERE b.batch_id = 103;

START TRANSACTION;

    -- Step 1: Insert a sales order for 99999 units (more than available)
    INSERT INTO Sales_Order (sales_order_id, order_date, quantity_sold, selling_price, total_amount, status, customer_id, batch_id)
    VALUES (9902, CURDATE(), 99999, 2.00, 199998.00, 'Completed', 401, 103);

    -- Step 2: Try to deduct from inventory
    -- This FAILS because of CHECK constraint: quantity_available >= 0
    -- ERROR: Check constraint 'inventory_chk_1' is violated.
    UPDATE Inventory
    SET quantity_available = quantity_available - 99999
    WHERE batch_id = 103;

    -- The UPDATE above fails! The Sales Order from Step 1 is still
    -- pending in the transaction. We must ROLLBACK to undo it.

ROLLBACK;

-- AFTER STATE: Verify nothing changed
SELECT 'SCENARIO 2: AFTER ROLLBACK — No Changes' AS label;
SELECT i.batch_id, b.batch_number, i.quantity_available
FROM Inventory i JOIN Batch b ON i.batch_id = b.batch_id
WHERE b.batch_id = 103;

-- Verify the sales order was NOT inserted
SELECT COUNT(*) AS 'Order 9902 exists (should be 0)'
FROM Sales_Order WHERE sales_order_id = 9902;


-- ============================================================
-- SCENARIO 3: DIRTY READ PREVENTION (Isolation Levels)
-- ============================================================
-- Demonstrates: How isolation levels prevent dirty reads.
-- This requires TWO separate MySQL sessions (connections).
--
-- SESSION A: Updates inventory but does NOT commit.
-- SESSION B: Tries to read the same row.
--
-- With READ COMMITTED (default):
--   Session B sees the OLD value (dirty read prevented).
-- With READ UNCOMMITTED:
--   Session B sees the UNCOMMITTED new value (dirty read!).
-- ============================================================

-- >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
-- >>> RUN THIS IN SESSION A (Connection 1):           <<<
-- >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

SELECT 'SCENARIO 3: SESSION A — Start' AS label;
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;

    -- Session A updates inventory but does NOT commit yet
    UPDATE Inventory SET quantity_available = 9999 WHERE batch_id = 103;
    SELECT 'Session A: Updated batch 103 qty to 9999 (NOT committed)' AS status;

    -- DO NOT COMMIT! Now switch to Session B...


-- >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
-- >>> RUN THIS IN SESSION B (Connection 2):           <<<
-- >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

-- Test 1: READ COMMITTED (default — safe)
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
    SELECT quantity_available FROM Inventory WHERE batch_id = 103;
    -- Result: Sees the ORIGINAL value (e.g., 1500), NOT 9999.
    -- Dirty read is PREVENTED. ✓
COMMIT;

-- Test 2: READ UNCOMMITTED (unsafe — allows dirty reads)
SET SESSION TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;
START TRANSACTION;
    SELECT quantity_available FROM Inventory WHERE batch_id = 103;
    -- Result: Sees 9999 — the UNCOMMITTED value! DIRTY READ! ✗
COMMIT;


-- >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
-- >>> BACK IN SESSION A — Rollback:                   <<<
-- >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

ROLLBACK;

SELECT 'SCENARIO 3: SESSION A — Rolled back' AS label;
SELECT batch_id, quantity_available FROM Inventory WHERE batch_id = 103;
-- Quantity is back to original. No changes persisted.


-- ============================================================
-- SCENARIO 4: LOST UPDATE / CONFLICTING WRITE (Concurrency)
-- ============================================================
-- Demonstrates: Two sessions read the same value, then both
--   write based on stale data — one update is "lost".
--   Then shows how SELECT ... FOR UPDATE prevents this.
-- ============================================================

-- ─── Part A: THE PROBLEM — Lost Update ───

SELECT 'SCENARIO 4A: LOST UPDATE PROBLEM' AS label;
SELECT quantity_available AS 'Original Qty (batch 103)'
FROM Inventory WHERE batch_id = 103;

-- >>> SESSION A reads qty (e.g., 1500):
-- START TRANSACTION;
-- SELECT quantity_available INTO @qty_a FROM Inventory WHERE batch_id = 103;
-- (reads 1500)

-- >>> SESSION B also reads qty (same stale value):
-- START TRANSACTION;
-- SELECT quantity_available INTO @qty_b FROM Inventory WHERE batch_id = 103;
-- (reads 1500)

-- >>> SESSION A subtracts 200 and commits:
-- UPDATE Inventory SET quantity_available = 1500 - 200 WHERE batch_id = 103;
-- COMMIT;
-- (writes 1300)

-- >>> SESSION B subtracts 300 and commits (overwrites A!):
-- UPDATE Inventory SET quantity_available = 1500 - 300 WHERE batch_id = 103;
-- COMMIT;
-- (writes 1200, OVERWRITING Session A's result!)

-- EXPECTED: 1500 - 200 - 300 = 1000
-- ACTUAL:   1200 (Session A's deduction of 200 was LOST!)

-- Simulate the lost update
START TRANSACTION;
    UPDATE Inventory SET quantity_available = 1300 WHERE batch_id = 103;
COMMIT;
START TRANSACTION;
    UPDATE Inventory SET quantity_available = 1200 WHERE batch_id = 103;
COMMIT;

SELECT 'SCENARIO 4A: After Lost Update' AS label;
SELECT quantity_available AS 'Qty after lost update (should be 1000, but is 1200)'
FROM Inventory WHERE batch_id = 103;

-- Restore original value
UPDATE Inventory SET quantity_available = 1500 WHERE batch_id = 103;


-- ─── Part B: THE FIX — SELECT ... FOR UPDATE (Pessimistic Locking) ───

SELECT 'SCENARIO 4B: FIX WITH SELECT ... FOR UPDATE' AS label;

-- >>> SESSION A:
START TRANSACTION;

    -- Lock the row! Other sessions must wait until we commit/rollback.
    SELECT quantity_available INTO @qty_a
    FROM Inventory WHERE batch_id = 103
    FOR UPDATE;

    -- (reads 1500, row is now LOCKED)
    UPDATE Inventory SET quantity_available = @qty_a - 200 WHERE batch_id = 103;
    -- (writes 1300)

COMMIT;

-- >>> SESSION B:
START TRANSACTION;

    -- This SELECT FOR UPDATE will WAIT until Session A's lock is released.
    SELECT quantity_available INTO @qty_b
    FROM Inventory WHERE batch_id = 103
    FOR UPDATE;

    -- (reads 1300 — the CORRECT post-Session-A value!)
    UPDATE Inventory SET quantity_available = @qty_b - 300 WHERE batch_id = 103;
    -- (writes 1000 — CORRECT!)

COMMIT;

SELECT 'SCENARIO 4B: After Proper Locking' AS label;
SELECT quantity_available AS 'Qty with FOR UPDATE fix (correctly 1000)'
FROM Inventory WHERE batch_id = 103;

-- Restore
UPDATE Inventory SET quantity_available = 1500 WHERE batch_id = 103;


-- ============================================================
-- SCENARIO 5: DEADLOCK DETECTION & RESOLUTION
-- ============================================================
-- Demonstrates: Two sessions lock rows in opposite order,
--   causing a circular wait (deadlock). MySQL detects this
--   and automatically kills one transaction.
--
-- Uses Batch 103 and Batch 106 (two different inventory rows).
-- This MUST be run in two separate sessions simultaneously.
-- ============================================================

-- >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
-- >>> RUN THIS IN SESSION A (Connection 1):           <<<
-- >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

START TRANSACTION;
    -- Step 1: Lock batch 103
    SELECT * FROM Inventory WHERE batch_id = 103 FOR UPDATE;
    -- (batch 103 is now locked by Session A)

    -- Wait 2-3 seconds for Session B to lock batch 106...

    -- Step 2: Try to lock batch 106 (Session B holds this!)
    SELECT * FROM Inventory WHERE batch_id = 106 FOR UPDATE;
    -- BLOCKED! Waiting for Session B...


-- >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
-- >>> RUN THIS IN SESSION B (Connection 2):           <<<
-- >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

START TRANSACTION;
    -- Step 1: Lock batch 106
    SELECT * FROM Inventory WHERE batch_id = 106 FOR UPDATE;
    -- (batch 106 is now locked by Session B)

    -- Wait 2-3 seconds for Session A to lock batch 103...

    -- Step 2: Try to lock batch 103 (Session A holds this!)
    SELECT * FROM Inventory WHERE batch_id = 103 FOR UPDATE;
    -- BLOCKED! Waiting for Session A...

-- RESULT:
--   MySQL detects the circular wait (deadlock).
--   ERROR 1213 (40001): Deadlock found when trying to get lock;
--                       try restarting transaction
--   One session is automatically rolled back (the "victim").
--   The other session's transaction completes successfully.
--   Neither session corrupts data — MySQL guarantees consistency.


-- ============================================================
-- FINAL VERIFICATION: Database Consistency Check
-- ============================================================

SELECT 'FINAL VERIFICATION' AS label;

-- Check no test sales orders remain
SELECT COUNT(*) AS 'Test orders remaining (should be 0)'
FROM Sales_Order WHERE sales_order_id IN (9901, 9902);

-- Check inventory values are reasonable
SELECT i.batch_id, b.batch_number, i.quantity_available,
    CASE
        WHEN i.quantity_available < 0 THEN 'ERROR: Negative stock!'
        WHEN i.quantity_available > 10000 THEN 'WARNING: Unusually high'
        ELSE 'OK'
    END AS consistency_check
FROM Inventory i
JOIN Batch b ON i.batch_id = b.batch_id
WHERE b.status = 'Active'
ORDER BY i.batch_id;

-- Show current isolation level
SELECT @@transaction_isolation AS 'Current Isolation Level';
