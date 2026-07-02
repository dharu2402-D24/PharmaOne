# 💊 PharmaOne — Pharmaceutical Inventory Management System

A full-featured **Java Swing** desktop application for managing pharmaceutical inventory, orders, suppliers, and customers. Built as a DBMS course project to demonstrate advanced MySQL concepts including transactions, triggers, concurrency control, and batch lifecycle management.

<p align="center">
  <img src="logo.png" alt="PharmaOne Logo" width="80"/>
</p>

> **Note:** This is an academic project demonstrating database concepts through a real-world pharmaceutical management scenario.

---

## 📸 Screenshots

<!-- Replace these with actual screenshots of your running application -->

| Dashboard | Inventory |
|:---------:|:---------:|
| ![Dashboard](docs/screenshots/dashboard.png) | ![Inventory](docs/screenshots/inventory.png) |

| Orders | SQL Console |
|:------:|:-----------:|
| ![Orders](docs/screenshots/orders.png) | ![SQL Console](docs/screenshots/sql_console.png) |

---

## 🎬 Demo Video

<!-- Add a link to your demo video here -->
> 🔗 [Watch the demo video](#) *(coming soon)*

---

## ✨ Features

- **Dashboard** — Real-time overview of inventory stats, stock levels, and recent activity
- **Inventory Catalog** — Full CRUD management of medicines, batches, and stock quantities
- **Orders & Shipments** — Create and track purchase orders (inbound) and sales orders (outbound)
- **Supplier Directory** — Manage pharmaceutical supplier records
- **Customer Directory** — Manage pharmacy and hospital customer records
- **Reports & Analytics** — Stock level reports, revenue summaries, and query-based analysis
- **SQL Console** — Execute raw SQL queries directly against the database
- **Trigger Demonstrations** — Interactive demos of MySQL database triggers
- **Transaction Demonstrations** — Live demos of ACID properties, isolation levels, deadlocks, and concurrency control using multiple database sessions
- **Discarded Batches** — Automated expired batch lifecycle management (cleanup runs on startup)
- **Settings** — Database connection configuration and system info

---

## 🛠️ Tech Stack

| Component | Technology |
|-----------|-----------|
| **Language** | Java 17+ |
| **UI Framework** | Java Swing |
| **Look & Feel** | [FlatLaf 3.4](https://www.formdev.com/flatlaf/) (Modern flat UI theme) |
| **Database** | MySQL 8.0+ |
| **JDBC Driver** | MySQL Connector/J 8.3.0 |
| **Build** | Plain Java (`javac` + `jar`) — no Maven/Gradle required |

---

## 📋 Prerequisites

- **Java JDK 17** or later — [Download](https://adoptium.net/)
- **MySQL Server 8.0** or later — [Download](https://dev.mysql.com/downloads/mysql/)
- A MySQL client (MySQL Workbench, command-line `mysql`, DBeaver, etc.)

Verify your installations:

```bash
java -version
javac -version
mysql --version
```

---

## 🗄️ Database Setup

1. **Start MySQL Server** and open a MySQL client.

2. **Run the schema + seed data script:**

   ```sql
   source sql/pharma_db.sql;
   ```

   This creates the `pharma_db` database with all tables (Medicine, Supplier, Customer, Batch, Inventory, Purchase_Order, Sales_Order) and sample data.

3. **Run the schema migration script** (adds Discarded_Batch table, batch status column):

   ```sql
   source sql/changes.sql;
   ```

4. *(Optional)* To explore the transaction demo scenarios independently:

   ```sql
   source sql/transactions.sql;
   ```

> **Default connection:** `localhost:3306`, database `pharma_db`, user `root`, no password.  
> You can change these at launch — the app shows a connection dialog on startup.

---

## 🚀 Build & Run

### Windows

```batch
build.bat
```

This compiles all source files, builds a standalone JAR, and optionally launches the app.

### Linux / macOS

```bash
chmod +x build.sh
./build.sh
```

### Run the standalone JAR

After building:

```bash
java -jar PharmaOne-Standalone.jar
```

### Run from compiled classes (development)

```bash
# Windows
java -cp "out;lib\*" Main

# Linux / macOS
java -cp "out:lib/*" Main
```

---

## 📁 Project Structure

```
PharmaOne/
├── src/
│   ├── Main.java                  # Application entry point
│   ├── db/
│   │   └── DatabaseConnection.java # MySQL singleton + connection dialog
│   ├── ui/
│   │   ├── MainFrame.java         # Main window (CardLayout navigation)
│   │   ├── HeaderPanel.java       # Top header bar
│   │   ├── Sidebar.java           # Navigation sidebar
│   │   ├── StatusBar.java         # Bottom status bar
│   │   ├── DashboardPanel.java    # Dashboard with stats
│   │   ├── InventoryPanel.java    # Inventory CRUD
│   │   ├── OrdersPanel.java       # Purchase & Sales orders
│   │   ├── SuppliersPanel.java    # Supplier management
│   │   ├── CustomersPanel.java    # Customer management
│   │   ├── ReportsPanel.java      # Reports & analytics
│   │   ├── SettingsPanel.java     # Settings & DB config
│   │   ├── SQLConsolePanel.java   # Raw SQL execution
│   │   ├── TriggerDemoPanel.java  # Trigger demonstrations
│   │   ├── TransactionDemoPanel.java # Transaction & concurrency demos
│   │   └── DiscardedBatchesPanel.java # Expired batch archive
│   └── util/
│       ├── ThemeColors.java       # Color palette constants
│       └── CustomIcons.java       # Programmatic SVG-style icons
├── lib/
│   ├── flatlaf-3.4.jar            # FlatLaf Look & Feel
│   └── mysql-connector-j-8.3.0.jar # MySQL JDBC driver
├── sql/
│   ├── pharma_db.sql              # Schema + seed data
│   ├── changes.sql                # Schema migrations
│   └── transactions.sql           # Transaction demo scripts
├── docs/
│   ├── ER Model - Frame 1.jpg     # Entity-Relationship diagram
│   ├── ER Model - Frame 2.jpg     # ER diagram (continued)
│   ├── transaction_demo_script.txt # Demo talking points
│   └── *.pdf                      # Project scope & relational model docs
├── build.bat                      # Windows build script
├── build.sh                       # Linux/macOS build script
├── logo.png                       # Application logo
├── logo.ico                       # Application icon
├── Manifest.txt                   # JAR manifest template
└── .gitignore
```

---

## 📊 ER Diagram

<p align="center">
  <img src="docs/ER Model - Frame 1.jpg" alt="ER Diagram - Part 1" width="700"/>
</p>

<p align="center">
  <img src="docs/ER Model - Frame 2.jpg" alt="ER Diagram - Part 2" width="700"/>
</p>

---

## 📝 License

This project was developed as an academic DBMS course project.

---

## 👥 Authors

<!-- Add your team members here -->
- **Dharu** — [GitHub](https://github.com/dharu2402-D24)
