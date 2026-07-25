---
name: format-adapter-scaffolder
description: Scaffolds a new package format module adhering to Hexagonal DDD and ArchUnit boundary invariants.
---

# Format Adapter Scaffolding Skill (`/add-format`)

When invoked to scaffold a new package format module (e.g., `repo-format-pypi`, `repo-format-cargo`, `repo-format-helm`):

## 1. Module Layout Generation
Generate the Maven sub-module layout under `repo-format-<name>/`:

```text
repo-format-<name>/
├── pom.xml
└── src/
    ├── main/java/io/omnidepot/format/<name>/
    │   ├── package-info.java <-- JSpecify @NullMarked
    │   ├── adapter/         <-- Package-private REST/Vert.x routes
    │   ├── service/         <-- Package-private wire protocol mapping logic
    │   └── FormatProvider.java  <-- Public CDI SPI implementation
    └── test/java/io/omnidepot/format/<name>/
        └── ArchUnitTest.java   <-- Boundary verification test using AssertJ
```

## 2. Invariant Checklist
1. **POM Dependencies:** Include `repo-core-api` ONLY. Explicitly exclude `repo-storage-*` and `repo-infra-db-*`.
2. **Encapsulation:** Mark all classes in `adapter/` and `service/` `package-private`. Only the CDI `@ApplicationScoped` SPI implementation class may be `public`.
3. **SPI Selection:** Annotate the primary format provider with `@LookupIfProperty(name = "repo.format.<name>.enabled", stringValue = "true")`.
4. **Nullability:** Include `package-info.java` annotated with `@NullMarked`.
5. **Assertions:** Tests MUST use AssertJ (`assertThat`, `assertThatThrownBy`). `org.junit.jupiter.api.Assertions` is forbidden.
6. **ArchUnit Test:** Generate an ArchUnit test asserting that no class in `io.omnidepot.format.<name>**` directly accesses infrastructure or storage implementation packages.
