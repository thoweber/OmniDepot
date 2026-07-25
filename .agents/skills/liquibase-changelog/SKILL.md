---
name: liquibase-changelog
description: Generates unified Liquibase XML changelogs with dbms attribute isolation for PostgreSQL 16+ and embedded H2.
---

# Liquibase Migration Generator Skill (`/new-migration`)

When generating database schema migrations (ADR-023):

## 1. File Placement & Naming
Place new versioned changelogs under `repo-infra-db/src/main/resources/db/changelog/v1.0/` and register them in `db.changelog-master.xml`:

```text
repo-infra-db/src/main/resources/db/changelog/
├── db.changelog-master.xml
└── v1.0/
    ├── 01-initial-schema.xml
    └── 02-add-index.xml
```

## 2. Dialect Mapping & Qualification Rules
* Use standard Liquibase tags (`<createTable>`, `<addColumn>`) for ANSI SQL.
* Isolate PostgreSQL-specific syntax (`JSONB`, `USING GIN`, `FOR UPDATE SKIP LOCKED`) using `dbms="postgresql"`.
* Isolate H2-specific fallback syntax using `dbms="h2"`.
* Every `<changeSet>` MUST contain an explicit `<rollback>` definition.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <changeSet id="1.0-create-example-table" author="omnidepot">
        <createTable tableName="example_table">
            <column name="id" type="UUID"><constraints primaryKey="true" nullable="false"/></column>
            <column name="payload" type="JSON"><constraints nullable="false"/></column>
        </createTable>
        <rollback>
            <dropTable tableName="example_table"/>
        </rollback>
    </changeSet>

    <changeSet id="1.0-example-gin-index-pg" author="omnidepot" dbms="postgresql">
        <sql>CREATE INDEX idx_example_payload_gin ON example_table USING GIN (payload);</sql>
        <rollback><sql>DROP INDEX IF EXISTS idx_example_payload_gin;</sql></rollback>
    </changeSet>
</databaseChangeLog>
```
