# Smart Home / IoT Device Monitoring System

An end-to-end IoT monitoring and automation platform backed by Oracle Database 21c XE, a Spring Boot REST API, and a modern React/Vite frontend.

---

## 📁 Repository Structure

```text
dbms_assignment/
│
├── backend/                  # Spring Boot 4.x / Java 21 REST API
│   ├── src/                  # Controllers, Entities, Repositories, Services
│   ├── pom.xml               # Maven configuration
│   └── mvnw / mvnw.cmd       # Maven wrapper
│
├── frontend/                 # React 19 / Vite / Tailwind CSS
│   ├── src/                  # Authentication, Landing, and Components
│   ├── package.json          # Node dependencies
│   └── vite.config.js        # Vite build configuration
│
├── database/                 # Oracle 21c SQL & PL/SQL Scripts
│   ├── 01_create_tables.sql  # Creates all 17 tables (Full EER model)
│   ├── 02_constraints.sql    # Primary, Foreign, Unique, and Check constraints
│   ├── 03_sequences.sql      # Surrogate primary key sequences
│   ├── 04_sample_data.sql    # Realistic sample dataset
│   ├── 05_queries.sql        # Core queries (joins, analytics, derived uptime)
│   └── 06_plsql.sql          # Stored procedures, functions, cursors, triggers
│
├── docs/                     # Architectural documentation
│   └── architecture.md
│
├── .gitignore
└── README.md
```

---

## 🗄️ Database Setup (Oracle XE 21c)

**Target Environment:**
* **Engine:** Oracle Database 21c Express Edition
* **PDB:** `XEPDB1`
* **Schema / User:** `SMART_HOME`
* **Hibernate Config:** `spring.jpa.hibernate.ddl-auto=none`

### Execution Order in SQL*Plus / SQL Developer:
1. `01_create_tables.sql` - Creates all 17 tables in strict dependency order.
2. `02_constraints.sql` - Applies foreign keys, checks, and unique constraints.
3. `03_sequences.sql` - Initializes surrogate key generators.
4. `04_sample_data.sql` - Populates demo dataset with multi-home access.
5. `05_queries.sql` - Runs verification and dashboard analytical queries.
6. `06_plsql.sql` - Compiles stored procedures, functions, cursors, and triggers.

---

## 🚀 Running the Project Locally

### Backend (Spring Boot):
```bash
cd backend
./mvnw spring-boot:run
```

### Frontend (React / Vite):
```bash
cd frontend
npm install
npm run dev
```