# Data Model & Liquibase Dialect Isolation

omnidepot manages relational database schemas using **Liquibase** (`quarkus-liquibase`), supporting both production-grade PostgreSQL 16+ and embedded H2 test databases.

---

## Database Schema & Dialect Isolation

To handle JSON metadata across PostgreSQL and H2 without syntax errors, change sets use dynamic `dbms` attribute filtering:

```xml
<!-- PostgreSQL JSONB Column Definition -->
<changeSet id="1.0.0-blobs-postgresql" author="omnidepot" dbms="postgresql">
    <createTable tableName="blobs">
        <column name="id" type="UUID">
            <constraints nullable="false" primaryKey="true"/>
        </column>
        <column name="digest_sha256" type="VARCHAR(64)">
            <constraints nullable="false" unique="true"/>
        </column>
        <column name="size_bytes" type="BIGINT">
            <constraints nullable="false"/>
        </column>
        <column name="metadata" type="JSONB"/>
        <column name="created_at" type="TIMESTAMP WITH TIME ZONE">
            <constraints nullable="false"/>
        </column>
    </createTable>
    <rollback>
        <dropTable tableName="blobs"/>
    </rollback>
</changeSet>

<!-- H2 Fallback CLOB Definition -->
<changeSet id="1.0.0-blobs-h2" author="omnidepot" dbms="h2">
    <createTable tableName="blobs">
        <column name="id" type="UUID">
            <constraints nullable="false" primaryKey="true"/>
        </column>
        <column name="digest_sha256" type="VARCHAR(64)">
            <constraints nullable="false" unique="true"/>
        </column>
        <column name="size_bytes" type="BIGINT">
            <constraints nullable="false"/>
        </column>
        <column name="metadata" type="CLOB"/>
        <column name="created_at" type="TIMESTAMP WITH TIME ZONE">
            <constraints nullable="false"/>
        </column>
    </createTable>
    <rollback>
        <dropTable tableName="blobs"/>
    </rollback>
</changeSet>
```

---

## 🔒 Mandatory Rollback Policy

Every Liquibase change set MUST specify an explicit `<rollback>` block. Pull requests failing to supply `<rollback>` directives are rejected by CI.
