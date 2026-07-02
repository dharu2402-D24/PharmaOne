create database Pharma_db;
use pharma_db;
CREATE TABLE Medicine (
    medicine_id INT PRIMARY KEY,
    medicine_name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    dosage_form VARCHAR(50) NOT NULL,
    strength VARCHAR(50) NOT NULL,
    standard_price DECIMAL(10,2) CHECK (standard_price > 0),
    description VARCHAR(255)
);
CREATE TABLE Supplier (
    supplier_id INT PRIMARY KEY,
    supplier_name VARCHAR(100) NOT NULL,
    license_number VARCHAR(50) UNIQUE NOT NULL,
    contact_number VARCHAR(20),
    address VARCHAR(255)
);
CREATE TABLE Customer (
    customer_id INT PRIMARY KEY,
    customer_name VARCHAR(100) NOT NULL,
    customer_type VARCHAR(50)
        CHECK (customer_type IN ('Pharmacy', 'Hospital')),
    address VARCHAR(255),
    license_number VARCHAR(50) UNIQUE NOT NULL,
    contact_number VARCHAR(20)
);
CREATE TABLE Batch (
    batch_id INT PRIMARY KEY,
    batch_number VARCHAR(50) UNIQUE NOT NULL,
    manufacture_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    purchase_price DECIMAL(10,2) CHECK (purchase_price > 0),
    selling_price DECIMAL(10,2) CHECK (selling_price > 0),
    medicine_id INT NOT NULL,
    FOREIGN KEY (medicine_id) REFERENCES Medicine(medicine_id),
    CHECK (expiry_date > manufacture_date)
);
CREATE TABLE Inventory (
    inventory_id INT PRIMARY KEY,
    quantity_available INT NOT NULL CHECK (quantity_available >= 0),
    storage_location VARCHAR(100),
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    batch_id INT NOT NULL UNIQUE,
    FOREIGN KEY (batch_id) REFERENCES Batch(batch_id)
);
CREATE TABLE Purchase_Order (
    purchase_order_id INT PRIMARY KEY,
    order_date DATE NOT NULL,
    quantity_purchased INT NOT NULL CHECK (quantity_purchased > 0),
    unit_cost DECIMAL(10,2) NOT NULL CHECK (unit_cost > 0),
    total_amount DECIMAL(12,2),
    status VARCHAR(20),
    supplier_id INT NOT NULL,
    batch_id INT NOT NULL,
    FOREIGN KEY (supplier_id) REFERENCES Supplier(supplier_id),
    FOREIGN KEY (batch_id) REFERENCES Batch(batch_id)
);
CREATE TABLE Sales_Order (
    sales_order_id INT PRIMARY KEY,
    order_date DATE NOT NULL,
    quantity_sold INT NOT NULL CHECK (quantity_sold > 0),
    selling_price DECIMAL(10,2) NOT NULL CHECK (selling_price > 0),
    total_amount DECIMAL(12,2),
    status VARCHAR(20),
    customer_id INT NOT NULL,
    batch_id INT NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES Customer(customer_id),
    FOREIGN KEY (batch_id) REFERENCES Batch(batch_id)
);
CREATE INDEX idx_batch_medicine ON Batch(medicine_id);
CREATE INDEX idx_inventory_batch ON Inventory(batch_id);
CREATE INDEX idx_purchase_supplier ON Purchase_Order(supplier_id);
CREATE INDEX idx_sales_customer ON Sales_Order(customer_id);
INSERT INTO Medicine VALUES
(1, 'Dolo 650', 'Analgesic', 'Tablet', '650mg', 20.00, 'Pain reliever'),
(2, 'Augmentin 625 Duo', 'Antibiotic', 'Tablet', '625mg', 90.00, 'Bacterial infection');
INSERT INTO Batch VALUES
(101, 'BATCH-PARA-001', '2024-01-01', '2026-01-01', 15.00, 20.00, 1),
(102, 'BATCH-AMOX-001', '2024-02-01', '2025-12-01', 80.00, 90.00, 2);
INSERT INTO Supplier VALUES
(301, 'Micro Labs Ltd.', 'LIC12345', '9876543210', 'Delhi'),
(302, 'Glaxo SmithKline Pharmaceuticals Ltd.', 'LIC67890', '9123456780', 'Delhi');
INSERT INTO Customer VALUES
(401, 'Apollo Pharma', 'Pharmacy', 'Delhi', 'CUSTLIC001', '9000000001'),
(402, 'AIIMS', 'Hospital', 'Mumbai', 'CUSTLIC002', '9000000002');
INSERT INTO Medicine (medicine_id, medicine_name, category, dosage_form, strength, standard_price, description)
VALUES
(3, 'Limcee', 'Vitamin C Supplement', 'Chewable Tablet', '500 mg', 2.00, 'Vitamin C supplement for immunity'),
(4, 'Evion 600', 'Vitamin E Supplement', 'Capsule', '600 mg', 5.50, 'Vitamin E for skin and hair health'),
(5, 'Nexito 20', 'Antidepressant', 'Tablet', '20 mg', 12.00, 'Escitalopram for anxiety and depression'),
(6, 'Levoflox 500', 'Antibiotic', 'Tablet', '500 mg', 18.00, 'Broad-spectrum antibiotic'),
(7, 'Megalis', 'Erectile Dysfunction', 'Tablet', '20 mg', 25.00, 'Tadalafil for erectile dysfunction');
INSERT INTO Batch (batch_id, batch_number, manufacture_date, expiry_date, purchase_price, selling_price, medicine_id)
VALUES
(103, 'BATCH-LIM-001', '2024-01-10', '2026-01-10', 1.20, 2.00, 3),
(104, 'BATCH-EVI-001', '2024-02-15', '2026-02-15', 3.80, 5.50, 4),
(105, 'BATCH-NEX-001', '2024-03-01', '2026-03-01', 8.00, 12.00, 5),
(106, 'BATCH-LEV-001', '2024-01-25', '2025-12-25', 12.00, 18.00, 6),
(107, 'BATCH-MEG-001', '2024-04-01', '2026-04-01', 18.00, 25.00, 7);
INSERT INTO Inventory (inventory_id, quantity_available, storage_location, last_updated, batch_id)
VALUES
(203, 1500, 'Warehouse A', CURRENT_DATE(), 103),
(204, 800, 'Warehouse B', CURRENT_DATE(), 104),
(205, 600, 'Warehouse A', CURRENT_DATE(), 105),
(206, 400, 'Warehouse C', CURRENT_DATE(), 106),
(207, 300, 'Warehouse B', CURRENT_DATE(), 107);
INSERT INTO Inventory VALUES
(201, 1000, 'Warehouse A', CURRENT_DATE, 101),
(202, 500, 'Warehouse B', CURRENT_DATE, 102);
INSERT INTO Sales_Order VALUES
(601, '2024-04-01', 200, 500.00, 2.50, 'Completed', 401, 101),
(602, '2024-04-03', 100, 500.00, 5.00, 'Completed', 402, 102);
INSERT INTO Purchase_Order VALUES
(501, '2024-03-01', 1000, 1.80, 1800.00, 'Completed', 301, 101),
(502, '2024-03-05', 500, 3.50, 1750.00, 'Completed', 302, 102);

select * from Medicine;

select storage_location , sum(quantity_available) from inventory group by storage_location;

use pharma_db;

update inventory
set quantity_available = quantity_available + 500
where quantity_available < 500;

use pharma_db;

select count(inventory_id)
from inventory;

select medicine_id, medicine_name, strength from medicine
where category = 'Antibiotic';

select m.medicine_name, b.batch_number, b.expiry_date from medicine m
join batch b on m.medicine_id = b.medicine_id;

select m.medicine_name, i.quantity_available, i.storage_location from medicine m
join batch b on m.medicine_id = b.medicine_id
join inventory i on b.batch_id = i.batch_id;

select storage_location, sum(quantity_available) as Total_Stock
from inventory
group by storage_location
having sum(quantity_available) > 1000;

select s.supplier_name, m.medicine_name, p.quantity_purchased
from supplier s join purchase_order p on s.supplier_id = p.supplier_id
join batch b on p.batch_id = b.batch_id 
join medicine m on b.medicine_id = m.medicine_id;

select c.customer_name, m.medicine_name, s.quantity_sold
from customer c join sales_order s on c.customer_id = s.customer_id
join batch b on s.batch_id = b.batch_id 
join medicine m on b.medicine_id = m.medicine_id;

select medicine_name, standard_price from medicine
where standard_price > (select avg(standard_price) from medicine);

select batch_number, expiry_date from batch
where expiry_date < curdate();

select inventory_id, quantity_available,
case
	when quantity_available > 1000 then 'High Stock'
    when quantity_available between 500 and 1000 then 'Medium Stock'
    else 'Low Stock'
end as stock_status
from inventory;

select sum(quantity_sold * selling_price) as total_revenvue
from sales_order;

select batch_number, (selling_price - purchase_price) as profit_per_unit
from batch;

select m.medicine_name
from medicine m
where not exists(select 1
from batch b
join sales_order s on b.batch_id = s.batch_id
where b.medicine_id = m.medicine_id);

update batch b
join medicine m on b.medicine_id = m.medicine_id
set b.selling_price = b.selling_price * 1.10
where m.category = 'Antibiotic';
select * from batch;

delete from batch
where expiry_date < curdate();

select customer_type, count(*) as total_customers
from customer
group by customer_type;

ALTER TABLE Batch
ADD supplier_id INT,
ADD FOREIGN KEY (supplier_id) REFERENCES Supplier(supplier_id);

ALTER TABLE Inventory
DROP INDEX batch_id;

ALTER TABLE Inventory
ADD CONSTRAINT unique_batch_location UNIQUE (batch_id, storage_location);

ALTER TABLE Batch
ADD CONSTRAINT fk_batch_supplier
FOREIGN KEY (supplier_id) REFERENCES Supplier(supplier_id);