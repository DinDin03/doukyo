# Doukyo (Sharehouse Companion) — Roadmap & Functional Breakdown

> **Companion to** the design document. This file expands §3 (functional requirements) and
> §9 (build roadmap) into a sequenced, buildable backlog.
> **Rule of the project:** build one vertical slice fully before starting the next, and for every
> milestone name *what it teaches*, not just what it ships. Where a feature is added purely to learn
> a concept (not because a real share house needs it), it's tagged **[learning]**.
>
> **Legend:** ✅ done · 🔨 in progress · ⬜ not started · **[learning]** added for learning, not product need

---

## How to read this

Each milestone has four parts:

- **User-facing features** — what a flatmate can now do, with acceptance criteria ("done when").
- **Data model** — the tables/relationships introduced or changed.
- **Engineering / infra** — non-feature work (endpoints, migrations, tests, infra).
- **Concepts learned** — the transferable ideas the milestone is a vehicle for.

A milestone is **done** only when its slice is usable end-to-end (API + persistence + tests), not when the code merely compiles.

---

## Phase 0 — Project foundation 🔨

*Before any domain work. This is the "ceremony" ADR-001 accepts as the cost of learning the real stack.*

**Sub-phases:** 0a = Spring Boot + GraphQL `ping`, no DB (✅ done). 0b = add Postgres + Flyway + JPA with the first real entities.

### Engineering / infra
- ✅ Gradle (Kotlin DSL) + Kotlin + Spring Boot project skeleton (`backend/`, single module).
- ✅ `docker-compose.yml` running PostgreSQL 16 locally (created; run in Phase 0b).
- ✅ **GraphQL API chosen** (Spring for GraphQL) with a working `ping` query + slice test.
- ✅ `.gitignore` (excludes `.idea/`, `build/`, `node_modules/`, env files).
- ✅ Database wired: Postgres 16 (Docker, host port **5433** to avoid a Homebrew Postgres on 5432) + Spring Data JPA (`ddl-auto: validate`) + **Flyway**; `V1__init.sql` creates `users`/`households`/`memberships`.
- ⬜ Spring profiles: `local` (Docker Postgres), `test` (Testcontainers). *(0b)*
- 🔨 Package structure reflecting the modular monolith: `household`, `expense`, `chore`, `shopping`, `meal`, `common` (only `health` exists so far).
- 🔨 Baseline test setup: JUnit 5 ✅, `GraphQlTester` ✅, Testcontainers for real Postgres ⬜ *(0b)*.
- ✅ `mobile/` Expo (SDK 54) + React Native + TypeScript app with Apollo Client calling `ping`/`greeting` live.

### Concepts learned
- The request/response loop made concrete (§8): controller → service → repository → DB.
- JVM build tooling (Gradle), dependency management, Spring context/bean wiring.
- Schema-as-code and repeatable environments (migrations + Docker Compose).
- The test pyramid: unit (service logic) vs integration (real DB via Testcontainers).

### Decisions locked
- ✅ **ADR-005 — Client stack: React Native + Expo (TypeScript).** One codebase for iOS + Android; Expo minimizes setup friction to preserve momentum; Apollo Client is best-in-class GraphQL tooling. Trade-off accepted: learning TypeScript alongside Kotlin.
- ✅ **ADR-006 — GraphQL over REST (Spring for GraphQL).** One client-shaped endpoint fits the dashboard's multi-domain aggregation; high transfer value (Atlassian runs GraphQL heavily). Supersedes the "REST-first" hint. Trade-offs owned: HTTP-caching lost, N+1 needs DataLoader, query-depth limits needed — all treated as deliberate learning.

---

## Milestone 1 — Foundation + Expenses (v1) ⬜  *← current focus*

*The richest self-contained domain and the one flatmates feel most. Build the whole vertical slice.*

### 1a. Households & accounts

**User-facing features**
- ⬜ Create a user profile (name, email). *Done when:* a user can be created and fetched.
- ⬜ Create a household. *Done when:* a household exists with a creator as its first member.
- ⬜ Invite flatmates by link/code; join a household via that code. *Done when:* a second user joins and both appear as members.
- ⬜ Leave a household. *Done when:* membership is removed and the user no longer sees household data.
- ⬜ A user may belong to multiple households; switching context returns only that household's data. *Done when:* data is correctly scoped per household (tenant isolation).
- ⬜ Remove a member and handle leftover data (outstanding debts, assigned chores) — for now: block removal if debts are unsettled, or reassign. *Done when:* removing a member with open shares is handled explicitly, not silently.

**Data model** (*Designed* in doc §6.1)
- `User(id, name, email)`
- `Household(id, name, created_at)`
- `Membership(id, user_id → User, household_id → Household, joined_at)` — the many-to-many join table.

**Engineering / infra**
- ⬜ CRUD endpoints for user, household, membership.
- ⬜ Invite-code generation + join flow.
- ⬜ Tenant scoping: every household-owned query filtered by `household_id` (foundation for multi-tenancy).
- ⬜ Migrations for the three tables.

**Concepts learned**
- Many-to-many via a join table; tenant isolation ("every row belongs to a household").
- No-roles / ownership model (ADR-002): equal members, ownership as the lighter future mechanism.
- Referential integrity and what "handle leftover data" means (FK constraints, cascade vs restrict).

### 1b. Expenses & splitting

**User-facing features**
- ⬜ Add an expense (payer, amount, description, date, category). *Done when:* an expense + its shares persist and sum correctly.
- ⬜ Split **evenly**. *Done when:* N shares each = amount/N, remainder handled (no lost cents).
- ⬜ Split by **exact amounts**. *Done when:* shares must sum to the total or the request is rejected.
- ⬜ Split by **percentage**. *Done when:* percentages sum to 100 and convert to cent-exact amounts.
- ⬜ Split by **shares/weights**. *Done when:* weighted split distributes cents exactly.
- ⬜ View per-person balances (who owes whom). *Done when:* a net balance per member is computed from unpaid shares.
- ⬜ Settle up / record a payment. *Done when:* settling flips share(s) `is_paid = true` and balances update.
- ⬜ Simplify debts to the fewest transactions **[learning]** *(nice for users, but the real reason is the graph/greedy algorithm exercise)*. *Done when:* a settle-up plan minimizes transaction count for a known example.
- ⬜ Recurring expenses (e.g. rent). *Done when:* a recurring rule generates expenses on schedule.
- ⬜ Attach a receipt photo. *Done when:* an image is stored (local/object storage) and linked to the expense.
- ⬜ Edit/delete an expense with balances recomputing. *Done when:* editing amount/split recomputes shares atomically.
- ⬜ Activity log of expense changes. *Done when:* create/edit/delete/settle are recorded and queryable.

**Data model** (*Designed* in doc §6.2)
- `Expense(id, household_id, amount, paid_by → User, description, category, created_at)`
- `ExpenseShare(id, expense_id → Expense, user_id → User, amount_owed, is_paid)`
- **Invariant:** shares always sum to `Expense.amount` (payer keeps a share row marked paid).
- Later: `RecurringExpense` rule; `Receipt` (or a URL column); `ExpenseActivity` log.

**Engineering / infra**
- ⬜ Money handling: store integer cents (never floats); a single split-calculation service with the remainder-distribution rule.
- ⬜ Transactional writes: expense + all shares committed atomically.
- ⬜ Validation: shares sum to total; percentages sum to 100; payer is a household member.
- ⬜ Balance query: aggregate unpaid shares into net per-person positions.
- ⬜ Debt-simplification algorithm (greedy min-cash-flow) with unit tests on worked examples.

**Concepts learned**
- One-to-many modelling (`Expense` → `ExpenseShare`); normalization / anomaly prevention (ADR-003).
- Financial correctness: integer money, cent-exact splits, invariants enforced in code + schema.
- Transactions and atomicity; recompute-on-edit as a consistency exercise.
- A real algorithm (debt simplification) with a test-first proof of correctness.

**Definition of done for the milestone:** a real household can add expenses, split them every way, see balances, and settle — via the API, backed by Postgres, covered by tests.

---

## Milestone 2 — Chores ⬜

**User-facing features**
- ⬜ Create chores, one-off or recurring.
- ⬜ Assign directly to a member.
- ⬜ Rotate fairly on a schedule (round-robin over members).
- ⬜ Mark a chore done; see whose turn it is.
- ⬜ Overdue reminders (surfaced in-app for now; real notifications arrive in Milestone 5).
- ⬜ Fairness view (who has done how much).
- ⬜ Swap or cover a chore for someone.

**Data model** (*To be designed* — doc §6.3)
- Likely `Chore(id, household_id, name, cadence, ...)` plus a **`ChoreAssignment`/`ChoreOccurrence`** table representing instances over time (rotation and "whose turn" imply a schedule of instances, not a static chore).
- **Modelling rep:** decide one-vs-two tables and how rotation is represented (materialize occurrences vs compute the next turn on the fly).

**Concepts learned**
- Modelling time/recurrence and rotation (state over a schedule).
- Fairness as a computed view over historical assignments.
- Reusing the recurrence machinery first met in recurring expenses.

---

## Milestone 3 — Shopping list ⬜

**User-facing features**
- ⬜ Add items; mark bought; live updates as flatmates shop.
- ⬜ Track who requested each item.
- ⬜ Recurring staples.
- ⬜ Link a bought item to an expense (grocery run → split bill) — ties back to Milestone 1.
- ⬜ Categorize by aisle/store.
- ⬜ Clear completed items.

**Data model** (*To be designed* — doc §6.4)
- **Active open question:** *one table or two?* Apply the duplication test — does any fact repeat across rows in a single table? Likely a `ShoppingItem` plus a link to the requester/buyer and an optional FK to `Expense`.

**Concepts learned**
- The duplication test as the decision procedure for normalization.
- Cross-domain foreign keys (shopping item → expense) and optional relationships.
- "Live updates" framed honestly: polling now; real push is the Kafka milestone.

---

## Milestone 4 — Meals & recipes ⬜

*The richest modelling problem in the project.*

**User-facing features**
- ⬜ Save recipes (ingredients, steps); a shared household cookbook.
- ⬜ Weekly meal calendar; assign who cooks.
- ⬜ Auto-generate a shopping list from planned meals (plan → required ingredients → dedup → list).
- ⬜ Scale recipes to servings.
- ⬜ Tag recipes.
- ⬜ "What can we make with what's in the pantry" (needs the pantry from the advanced layer).
- ⬜ Rate and comment on meals.

**Data model** (*To be designed* — doc §6.5)
- `Recipe` ↔ `Ingredient`: **one-to-many vs a normalized many-to-many** (shared ingredient catalogue). Decide and record.
- `MealPlan` referencing recipes + assigning cooks.
- The `plan → required-ingredients → dedup-against-pantry → shopping-list` chain is genuine logic, not just schema.

**Concepts learned**
- Many-to-many with attributes (recipe–ingredient with quantity/unit).
- A multi-step data pipeline expressed as domain logic (plan to list).
- When to introduce a shared catalogue vs inline data.

---

## Milestone 5 — Event-driven layer (Kafka) ⬜ **[learning]**

*The product does not need a broker for correctness (doc §4). Added deliberately to learn event-driven design.*

**Features it powers**
- ⬜ A unified **activity feed** across all domains (expense added, chore done, item bought).
- ⬜ **Notifications** (overdue chores, new expenses, settle reminders).

**Engineering / infra**
- ⬜ Every domain action emits a domain event.
- ⬜ Kafka in Docker Compose; producers in each module; consumers for feed + notifications.
- ⬜ Outbox pattern so events and DB writes stay consistent.

**Concepts learned**
- Event-driven architecture, producers/consumers, topics, ordering.
- The transactional outbox; at-least-once delivery and idempotent consumers.
- Decoupling side effects (feed, notifications) from the write path.

---

## Milestone 6 — Caching, resilience & observability ⬜ **[learning]**

**Engineering / infra**
- ⬜ **Redis** cache-aside for the dashboard and hot list reads; invalidation on writes.
- ⬜ **Resilience:** rate limiting, retries with backoff, circuit breakers, graceful degradation.
- ⬜ **Observability:** structured logging, metrics, distributed tracing; latency percentiles.
- ⬜ The **forward-decay reservoir** for recent-weighted p99 latency (doc §8: "store the birth date, not the age").

**Concepts learned**
- Cache-aside and the hard part (invalidation); read-vs-write trade-offs.
- Failure handling patterns; degrading instead of falling over.
- Percentile latency measurement and why averages lie.

---

## Milestone 7 — Service split ⬜ **[learning]**

- ⬜ Extract an **ML inference service** and an **assistant service** from the monolith.
- ⬜ Introduce inter-service contracts; apply the resilience patterns from Milestone 6 where they now matter (network between services).

**Concepts learned**
- When and how to break a monolith; the cost of a network boundary.
- Service contracts, versioning, and where resilience patterns actually earn their keep.

---

## Milestone 8 — ML ⬜ **[learning]**

- ⬜ **Expense auto-categorization** (classify an expense from its description).
- ⬜ **Shopping-list duplicate detection** (near-duplicate item matching).
- ⬜ A per-household **usage analytics** view.

**Concepts learned**
- A supervised classification pipeline: features, training, inference, serving.
- Fuzzy/near-duplicate matching (embeddings or string similarity).
- Serving a model behind the inference service from Milestone 7.

---

## Milestone 9 — Agentic AI layer ⬜ **[learning]**

- ⬜ **RAG assistant** over household data (ask questions about balances, chores, meals).
- ⬜ **Acting agent** that plans meals and builds the shopping list.
- ⬜ Expose the agent's capabilities via **MCP**.

**Concepts learned**
- Retrieval-augmented generation over your own structured data.
- Tool-using / acting agents and safe action boundaries.
- MCP as an integration surface.

---

## Milestone 10 — Evaluation & chaos ⬜ **[learning]**

- ⬜ An **evaluation harness** for both ML outputs (accuracy/precision/recall) and LLM outputs (task-level evals).
- ⬜ A **chaos-testing** pass (inject failures, kill dependencies, verify graceful degradation from Milestone 6).

**Concepts learned**
- Evaluating non-deterministic systems; offline metrics vs task evals.
- Resilience validated by deliberate failure, not hope.

---

## Cross-cutting features (span multiple milestones)

These aren't a milestone of their own; they land incrementally as the domains they tie together come online.

- ⬜ **Dashboard** — balances + chores + tonight's dinner + shopping list in one view. First useful after Milestone 1; grows each milestone. (Prime Redis cache target in Milestone 6.)
- ⬜ **Activity feed** across domains — stub in each milestone, unified properly in Milestone 5 (Kafka).
- ⬜ **Notifications** — in-app surfacing early; real push in Milestone 5.
- ⬜ **Search** across expenses/chores/items/recipes — added once there's enough data to search.
- ⬜ **Pantry / inventory tracker** — advanced-layer feature that feeds meal planning ("what can we make"); lands alongside Milestone 4/8.

---

## Sequencing rules (the discipline this project is really about)

1. **One slice at a time.** Finish a milestone end-to-end before the next; no half-built domains.
2. **Every [learning] item states the honest trigger.** Before adding infra, write down "here is the scale at which this would genuinely be needed" — and admit you're below it.
3. **Normalize by default** (ADR-003); denormalize only with a measurement in hand.
4. **Migrations for every schema change** — the schema is code from day one.
5. **A milestone isn't done without tests** — service-level for logic, integration for the DB path.
6. **Update the design doc's Designed / To-be-designed markers and ADR log as decisions land.**

---

_Living document — update the ⬜/🔨/✅ markers as work progresses, and keep it in sync with the design document's ADR log and open-questions section._
