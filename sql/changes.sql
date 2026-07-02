CREATE TABLE Discarded_Batch (
    discarded_id INT PRIMARY KEY AUTO_INCREMENT,
    batch_id INT,
    batch_number VARCHAR(50),
    manufacture_date DATE,
    expiry_date DATE,
    purchase_price DECIMAL(10,2),
    selling_price DECIMAL(10,2),
    medicine_id INT,
    discarded_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reason VARCHAR(100) DEFAULT 'Expired',

    FOREIGN KEY (medicine_id) REFERENCES Medicine(medicine_id)
);
use pharma_db;
INSERT INTO Discarded_Batch 
(batch_id, batch_number, manufacture_date, expiry_date, purchase_price, selling_price, medicine_id, quantity_discarded)
SELECT 
b.batch_id,
b.batch_number,
b.manufacture_date,
b.expiry_date,
b.purchase_price,
b.selling_price,
b.medicine_id,
COALESCE(SUM(i.quantity_available), 0) AS quantity_discarded
FROM Batch b
LEFT JOIN Inventory i ON b.batch_id = i.batch_id
WHERE b.expiry_date < CURDATE()
GROUP BY 
b.batch_id, b.batch_number, b.manufacture_date, b.expiry_date,
b.purchase_price, b.selling_price, b.medicine_id;
ALTER TABLE Discarded_Batch
ADD quantity_discarded INT NOT NULL;
ALTER TABLE Batch
ADD status VARCHAR(20) NOT NULL DEFAULT 'Active';
UPDATE Batch
SET status = 'Discarded'
WHERE expiry_date < CURDATE();
DELETE FROM Inventory
WHERE batch_id IN (
    SELECT batch_id FROM Batch WHERE status = 'Discarded'
);