---
name: package_readme_generator
description: Generates standardized README.md files for Java packages in any codebase, including Mermaid class diagrams and design notes.
---

# Package README Generator Skill

A **project-agnostic** workflow for documenting Java packages. When activated, the agent analyzes the packages of whatever codebase it is run in and generates a descriptive `README.md` for each package, detailing its responsibilities, structure, class relationships, and design decisions.

This skill makes **no assumptions about a specific project**. It adapts to the host codebase's package layout, build tool, and architectural conventions (see [Adapt to the Project](#0-adapt-to-the-project-first)).

## Objectives

1. **Clear Package Responsibilities**: Document what the package does, who uses it, and what external dependencies it interacts with.
2. **Visual Class Relationships**: Generate correct, readable Mermaid class diagrams showing classes, records, interfaces, enums, their fields, methods, and associations (inheritance, composition, dependency).
3. **Architectural Alignment**: Capture key design notes, implementation details, and patterns used (e.g., Pipeline, Strategy, Clean Architecture ports/adapters).

---

## Scope

- **In scope**: production Java sources, typically under `src/main/java/**` (Maven/Gradle) or `app/src/main/java/**` (Android), grouped by package directory.
- **Out of scope**: build output (`target/`, `build/`, `out/`), test sources (`src/test/**`), and generated code. Read `package-info.java` for intent, but don't diagram it.
- **One README per package directory**, written as `README.md` alongside the `.java` files. Never aggregate multiple packages into one file.

---

## Step-by-Step Workflow

### 0. Adapt to the Project First
Before documenting anything, learn how *this* codebase is organized so the output matches its conventions:
- Read any `CLAUDE.md`, `AGENTS.md`, `README.md`, or `docs/architecture/**` at the repo root for the project's vocabulary, layering rules, and patterns.
- Detect the build tool and source roots: `pom.xml` → Maven (`src/main/java`, output `target/`); `build.gradle(.kts)` → Gradle (`src/main/java`, output `build/`); multi-module reactors have several source roots.
- Note the architectural style (layered, hexagonal/onion, modular monolith, plain MVC) — it determines what belongs in the "Design Notes" and how cross-package dependencies should be framed.
- Use the project's own terminology in the prose; do not import vocabulary from other projects.

### 1. Inventory & Map Packages
- Identify all packages containing production source files (apply the Scope exclusions).
- Group source files by package directory (one leaf directory = one package).
- Map dependencies between packages by scanning `import` statements — these become the cross-package relationships and the "Depends on" notes.
- Read each package's `package-info.java` first; it usually states intent in one place.

### 2. Document Package-by-Package
For each package directory:
- Parse all class, interface, record, and enum declarations.
- Identify core responsibilities (one or two sentences in the project's own terms).
- Map the fields and methods of key types to show in the diagram.
- Construct the Mermaid class diagram using the correct syntax (see guidelines below).
- Write specific design notes (why a pattern was used, thread-safety, transaction boundaries, framework constraints, side effects).

### 3. Generate `README.md`
Write a `README.md` inside the package directory containing:
- **Title**: `# Package: <fully.qualified.package.name>`
- **Responsibility**: A concise description of what the package does and who depends on it.
- **Class Diagram**: A `mermaid` block showing class details and relationships.
- **Design Notes**: Key decisions, patterns, constraints, and side effects.

### 4. Verify
Run the [Quality Checklist](#quality-checklist) before finishing. Re-running the skill on an already-documented package should **overwrite** the existing `README.md` (idempotent), not append to it.

---

## Mermaid Syntax Guidelines

To ensure the class diagrams compile correctly and look readable:
- Open the diagram with a ` ```mermaid ` fence and the first line `classDiagram`.
- Use `~` for generic parameters (e.g., `Pipeline~C, R~` or `Step~C, R~`). Angle brackets break Mermaid rendering.
- Tag types by kind: `<<interface>>`, `<<record>>`, `<<enum>>`, `<<abstract>>`.
- Field/method syntax inside a class body: `visibility name type` for fields, `visibility name(args) returnType` for methods, where visibility is `+` public, `-` private, `#` protected, `~` package-private.
- Show relationships clearly:
  - `ClassA ..|> InterfaceB` — implements (realization)
  - `ClassA --|> ParentClassB` — extends (inheritance)
  - `ClassA --> ClassB : uses` — association / dependency (label the edge)
  - `ClassA o-- ClassB` — aggregation; `ClassA *-- ClassB` — composition
- Keep diagrams clean. Focus on public/package-private API, core state, and main methods. Avoid dumping trivial getters/setters unless they matter to the model.

---

## Output Skeleton

This is the **shape** of the file to produce, with placeholders in `<…>`. It is a fill-in template, not a real implementation — replace every placeholder with the host project's actual package, types, relationships, and patterns. (Outer fence uses four backticks so the inner ` ```mermaid ` block renders verbatim.)

````markdown
# Package: `<fully.qualified.package.name>`

<One or two sentences: what this package is and where it sits in the system.>

## Responsibility

- <What the package does — bullet per responsibility.>
- <Who depends on it / what it depends on.>

## Class Diagram

```mermaid
classDiagram
    class <InterfaceName> {
        <<interface>>
        +<method>(<arg> <Type>) <ReturnType>
    }
    class <ImplName> {
        -<field> <Type>
        +<method>(<arg> <Type>) <ReturnType>
    }
    class <RecordName> {
        <<record>>
        +<field> <Type>
    }
    <ImplName> ..|> <InterfaceName>
    <ImplName> --> <RecordName> : <edge label>
```

## Design Notes

- **Pattern**: <pattern name(s) and how they apply here>.
- **Constraints**: <framework deps, thread-safety, transaction boundaries, side effects>.
````

---

## Quality Checklist

Before considering a package done, confirm:

- [ ] `README.md` exists in the package directory and starts with `# Package: <fqn>`.
- [ ] Every non-test, non-generated type in the package appears in the diagram (or is intentionally omitted with reason).
- [ ] The Mermaid block parses: `classDiagram` header, `~`-style generics, no raw `<...>`.
- [ ] Relationships reflect actual `import`s / field types, not guesses.
- [ ] Design Notes name the concrete pattern(s) and any constraints (framework, thread-safety, transactions, side effects).
- [ ] Prose uses the host project's own vocabulary — no leftover names from other codebases or from this skill's generic example.
- [ ] Cross-package dependencies match what the imports show.
