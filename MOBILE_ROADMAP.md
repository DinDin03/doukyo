# Doukyo — Mobile App Roadmap

> **Companion to** `ROADMAP.md` (backend) and the design document. This file is the
> complete, ordered plan for the **Expo / React Native / TypeScript** app.
> **Rule of the project:** build one vertical slice fully before the next, and name
> *what each step teaches*. Where a feature exists mainly to learn a concept, it's
> tagged **[learning]**.
>
> **Legend:** ✅ done · 🔨 in progress · ⬜ not started

---

## The sequencing decision: interleave, not frontend-last

Frontend is built **feature-by-feature, right after each backend slice** — not all at the
end. The reasoning:

- **Each feature stays usable end-to-end** (design doc §9) — a flatmate can open the app
  and *use* households the day the households API ships, not months later.
- **The frontend is the backend's best reviewer.** Consuming your own API from a real
  client surfaces design flaws (awkward shapes, missing fields, "I need the current user")
  *before* you build the next domain on top of them.
- **Momentum + skill balance** — you alternate backend and frontend instead of a
  demoralizing UI marathon at the end.
- **De-risking** — discover model/UX mismatches with 3 tables, not 10.

**The one exception:** genuinely shared setup — navigation, design system, the auth
stand-in, typed GraphQL codegen — is built **once, up front** (M0). Everything after M0 is
a thin per-domain UI slice.

### How mobile milestones map to backend milestones

| Backend (see `ROADMAP.md`) | Mobile milestone | Order |
|---|---|---|
| — (foundation for the app itself) | **M0** Frontend foundation | now |
| Milestone 1a (Users/Households) ✅ | **M1** Households & accounts | now-ish |
| Milestone 1 (Expenses) | **M2** Expenses | after M1 |
| Milestone 2 (Chores) | **M3** Chores | after M2 |
| Milestone 3 (Shopping list) | **M4** Shopping list | after M3 |
| Milestone 4 (Meals & recipes) | **M5** Meals & recipes | after M4 |
| (cross-cutting) | **M6** Dashboard | after ≥2 domains exist |
| Milestone 5 (Kafka) + notifications | **M7** Real-time, activity feed, push | after M6 |
| Milestone 8/9 (ML + Agent) | **M8** AI assistant UI | after M7 |
| (quality) | **M9** Polish: offline, optimistic, a11y | ongoing/late |
| (delivery) | **M10** Release & distribution | when you want real installs |

---

## Cross-cutting tracks (started in M0, grow forever)

These aren't milestones; they're disciplines that begin at M0 and improve every slice:

- **Typed GraphQL (codegen).** GraphQL Code Generator turns your schema + queries into
  fully-typed Apollo hooks. Set up in M0, used in every feature after — the app and server
  can never disagree about data shapes. **[the single biggest DX win]**
- **Design system.** Theme tokens (colors, spacing, type), base components (`Screen`,
  `Button`, `TextInput`, `Card`, `ListItem`, state views). Grows with each screen; never a
  big rewrite.
- **State conventions.** Apollo cache is the source of truth for server data; light local
  state (React state / a small store) for UI. A written rule for *when to refetch vs update
  the cache vs use optimistic UI*.
- **UX states.** Every screen handles **loading / error / empty / success** — codified as
  reusable components so you never ship a screen that white-screens on an error.
- **Testing.** Component tests (React Native Testing Library) from M1; one end-to-end flow
  (Maestro) once there are multiple screens.

---

## M0 — Frontend foundation ⬜  *(build once, now)*

*The shared skeleton every feature screen plugs into. The only "big" frontend setup.*

**Depends on:** nothing new (Apollo already wired in Phase 0a mobile).

**Build:**
- ⬜ **Navigation** — add **Expo Router** (file-based routing). Establish the shell:
  a **bottom tab bar** (Home · Expenses · Chores · Shopping · Meals · More) with a
  **stack** inside each tab for drill-down. Stub every screen.
- ⬜ **GraphQL Codegen** — wire `@graphql-codegen` to the backend schema; generate typed
  query/mutation hooks. Replace the hand-typed `useQuery<{...}>` with generated types.
- ⬜ **Design system v0** — theme tokens (light/dark), and base components: `Screen`,
  `AppText`, `Button`, `TextInput`, `Card`, `ListItem`, `Loading`, `ErrorView`,
  `EmptyState`.
- ⬜ **Current-user stand-in (fake auth)** — a `CurrentUserProvider` (React context) that
  holds the selected user, persisted with `AsyncStorage`. A "pick or create a user" gate
  before the tabs. **Records ADR-007: defer real auth; fake current-user via local
  selection.**
- ⬜ **API config** — keep the LAN-IP-from-Metro trick; add an env layer so a real
  backend URL can be swapped in later (for device/EAS builds).

**Concepts:** file-based navigation, tab + stack composition, React context + providers,
local persistence, schema-driven codegen, a component library / design tokens, theming.

**Done when:** the app boots to a user-picker, you select a user, land on a tabbed shell,
and every tab shows a (stub) screen — all typed and themed.

---

## M1 — Households & accounts ⬜

*The first real domain UI. Wires the API you just built (Milestone 1a).*

**Depends on:** backend Users/Households/Membership (✅ done).

**Screens & features:**
- ⬜ **Pick/Create User** (the fake login from M0, fleshed out): list users, create one.
- ⬜ **Households list** — the current user's households; create a household.
- ⬜ **Household detail** — name, members list, **add member** (pick a user).
- ⬜ **Leave / remove member** (once backend supports it).
- ⬜ Switch active household (a household picker in the header).

**Concepts:** `useQuery` lists with `FlatList`, controlled **forms** + `TextInput`,
**mutations** (`createUser`/`createHousehold`/`addMember`), **cache updates after a
mutation** (refetch vs `cache.modify` vs optimistic — pick and learn each), navigation
**params** (passing a household id), loading/error/empty states for real.

**Done when:** you can create a user, create a household, add members, and see it all
update live — no terminal, no GraphiQL.

---

## M2 — Expenses ⬜  *(the big one)*

*The richest UI in the app, matching the richest backend domain.*

**Depends on:** backend Expenses (add/split/balances/settle).

**Screens & features:**
- ⬜ **Expenses list** — per household, grouped by date; amount, payer, description.
- ⬜ **Add expense** — amount input, payer, description, category, date; **split selector**
  (even / exact / % / shares) with a **live split preview** that updates as you type.
- ⬜ **Balances** — "who owes whom," the simplified settle-up view.
- ⬜ **Settle up** — record a payment; balances recompute.
- ⬜ **Expense detail / edit / delete** — with balances updating.
- ⬜ **Receipt photo** — image picker + upload (file handling on mobile).
- ⬜ Recurring expense setup.

**Concepts:** complex/derived forms (live computation in the UI), **money formatting**
(integer cents ↔ display), segmented controls, per-person dynamic inputs, image
picking/upload, cache updates that touch *multiple* queries (an expense changes balances),
optimistic UI where it shines.

**Done when:** a flatmate can add a split expense, see balances change, and settle — the
feature the house feels most.

---

## M3 — Chores ⬜

**Depends on:** backend Chores (rotation, assignments, fairness).

**Screens & features:**
- ⬜ **Chores list / "whose turn"** — today and upcoming, per person.
- ⬜ **Create chore** — one-off or recurring; assign or rotate.
- ⬜ **Mark done** (swipe action); **swap/cover**.
- ⬜ **Fairness view** — who's done how much.
- ⬜ **Overdue reminders** — local notifications.

**Concepts:** schedule/rotation UI, **swipe gestures** (mark done), **local
notifications** (Expo Notifications), segmented/timeline views, derived "fairness" display.

**Done when:** the roster shows whose turn it is, you can mark chores done, and overdue
ones nudge you.

---

## M4 — Shopping list ⬜

**Depends on:** backend Shopping list (+ the expense link).

**Screens & features:**
- ⬜ **List** — checkbox items, **quick-add** input, who requested each.
- ⬜ **Mark bought** — with (near) live updates as flatmates shop.
- ⬜ **Categorize** by aisle/store (sectioned list); recurring staples.
- ⬜ **Link a bought run to an expense** (grocery → split bill) — cross-domain UX.
- ⬜ Clear completed.

**Concepts:** fast optimistic check/uncheck, sectioned lists, quick-entry UX, **polling
for "live" now** (honest — true realtime arrives in M7), cross-domain navigation
(shopping → create expense).

**Done when:** the house shares one live-ish list and a grocery run can become a split
expense in a couple taps.

---

## M5 — Meals & recipes ⬜

**Depends on:** backend Meals/Recipes (+ plan → shopping-list generation).

**Screens & features:**
- ⬜ **Cookbook** — recipe list; **recipe detail** (ingredients, steps).
- ⬜ **Add/edit recipe** — a **dynamic ingredient list** (add/remove rows) — the hardest
  form in the app.
- ⬜ **Weekly meal calendar** — assign meals + who cooks.
- ⬜ **Generate shopping list from the plan** — one tap → items appear in M4.
- ⬜ Scale servings; tag recipes; rate/comment.

**Concepts:** **dynamic form arrays** (variable-length ingredient rows), a week
calendar/grid, an action that writes across domains (plan → shopping list), rating UI.

**Done when:** you can save a recipe, plan the week, and auto-build the shopping list.

---

## M6 — Dashboard ⬜  *(cross-cutting payoff)*

*The screen that ties everything together — and GraphQL's poster child.*

**Depends on:** ≥2 domains live (ideally after M2, better after M4).

**Screens & features:**
- ⬜ **Home dashboard** — balances + today's chores + tonight's dinner + shopping list, in
  **one screen** powered by **one nested GraphQL query**.
- ⬜ Pull-to-refresh; per-card navigation into each domain.

**Concepts:** a **single composed query** across domains (exactly why GraphQL was chosen —
one round trip for a multi-domain screen), card layouts, and the first place **caching**
(backend Redis, Milestone 6) visibly matters.

**Done when:** opening the app shows the state of the house at a glance.

---

## M7 — Real-time, activity feed & push ⬜  **[learning]**

*The frontend half of the Kafka/event work (backend Milestone 5).*

**Depends on:** backend event-driven layer + notifications.

**Screens & features:**
- ⬜ **Activity feed** — cross-domain events (expense added, chore done, item bought), with
  **pagination / infinite scroll**.
- ⬜ **Real-time updates** — **GraphQL subscriptions** (WebSocket) so lists update live
  without polling; replace M4's polling.
- ⬜ **Push notifications** — Expo Notifications; device token registration; deep links
  into the relevant screen.
- ⬜ Unread badges.

**Concepts:** **GraphQL subscriptions** over WebSocket, cursor **pagination**, **push
notifications** end-to-end (permissions, tokens, handling taps), **deep linking**.

**Done when:** something a flatmate does shows up on your phone in real time (and as a
push when the app is closed).

---

## M8 — AI assistant UI ⬜  **[learning]**

*The frontend for the RAG assistant + acting agent (backend Milestones 8–9).*

**Depends on:** backend RAG assistant + agent (MCP).

**Screens & features:**
- ⬜ **Assistant chat** — ask questions about the house ("who owes me?", "what can we
  cook?"); **streaming responses**.
- ⬜ **Agent actions** — when the agent proposes a change (plan meals, build the list),
  show a **confirmation UI** before it writes. (Human-in-the-loop — never let the agent act
  silently.)
- ⬜ Show sources/citations for RAG answers.

**Concepts:** chat UI, **streaming** token display, and the safety pattern of
**confirm-before-act** for agent mutations (mirrors the tool-permission model).

**Done when:** you can ask the house a question and let the assistant *safely* take an
approved action.

---

## M9 — Polish ⬜  *(ongoing, front-loaded lightly, finished late)*

*Quality passes. Some (optimistic UI, states) happen inline earlier; the rest here.*

- ⬜ **Optimistic updates** everywhere they improve feel (check a box → instant).
- ⬜ **Offline support** — Apollo cache persistence + a mutation queue that syncs on
  reconnect. **[learning]**
- ⬜ **Skeletons & animations** (Reanimated), **haptics**, refined dark mode.
- ⬜ **Accessibility** — labels, dynamic type, contrast, screen-reader passes.
- ⬜ **Error tracking** (Sentry) + light analytics.

**Concepts:** optimistic concurrency on the client, offline-first patterns, animation,
accessibility, observability on the client.

**Done when:** the app feels fast and intentional, works on a flaky train connection, and
is usable by everyone.

---

## M10 — Release & distribution ⬜

*Getting it onto flatmates' actual phones.*

- ⬜ **App identity** — real icon, splash, name, bundle IDs.
- ⬜ **EAS Build** — cloud builds for iOS + Android (moves beyond Expo Go).
- ⬜ **EAS Update** — over-the-air JS updates (ship fixes without a store round-trip).
- ⬜ **Distribution** — internal (TestFlight / an APK link) for the house; store submission
  only if you ever want it (non-goal per the design doc).
- ⬜ **Env management** — dev/prod API URLs, secrets handling.

**Concepts:** the real mobile build/release pipeline (EAS), OTA updates, code signing,
environments — the parts Expo Go hid from you.

**Done when:** your flatmates install Doukyo from a link and use it without your laptop
running Metro.

---

## Suggested immediate order (next few sessions)

1. **M0** foundation (navigation + codegen + design system + fake-auth). *One focused push.*
2. **M1** households UI (wire what's already built) — small, satisfying, validates M0.
3. Back to **backend Expenses**, then **M2** expenses UI.
4. …continue alternating: backend domain → its mobile slice.

Frontend and backend leapfrog each other from here on. **Dashboard (M6)** is the first
moment it all visibly comes together; **M7** makes it feel alive; **M8** makes it smart;
**M10** puts it in the house's hands.

---

_Living document — update ⬜/🔨/✅ as you go, and keep it in step with `ROADMAP.md` and
the design doc's ADR log (add ADR-007 for deferred auth when M0 lands)._
