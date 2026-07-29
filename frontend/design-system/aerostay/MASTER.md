# Design System Master File

> **LOGIC:** When building a specific page, first check `design-system/aerostay/pages/[page-name].md`.
> If that file exists, its rules **override** this Master file.
> If not, strictly follow the rules below.
>
> **Provenance note:** the `ui-ux-pro-max` skill's `--design-system` search
> scaffolded this file's structure, but its keyword-matched recommendation
> (editorial black/pink, Calistoga+Inter) didn't match Aerostay's actual,
> already-shipped brand — so the content below is Aerostay's real tokens
> (from `src/index.css`), not the skill's generic guess. Kept the file in
> this location so future sessions using the skill's hierarchical
> retrieval pattern find the truth here instead of a mismatch.

---

**Project:** Aerostay (travel-platform frontend)
**Updated:** 2026-07-29
**Category:** Travel booking (flights + hotels)

---

## Global Rules

### Color Palette

Warm sand background + deep pine primary + sunset coral accent, deliberately
not the default Tailwind indigo/purple "AI-generated SaaS" look. Defined as
Tailwind v4 `@theme` tokens in `src/index.css` (no separate CSS variable
file - Tailwind generates `bg-pine-600`, `text-ink-950`, etc. directly).

| Role                     | Hex       | Tailwind class                  | Notes                                            |
| ------------------------ | --------- | ------------------------------- | ------------------------------------------------ |
| Ink 950 (text/headings)  | `#0b1b1e` | `text-ink-950`                  | Primary text, near-black but warm                |
| Ink 900                  | `#10282c` | `text-ink-900`                  |                                                  |
| Ink 800                  | `#173a3f` | `text-ink-800`                  | Secondary text (often with `/60`, `/70` opacity) |
| Ink 700                  | `#204f56` | `text-ink-700`                  |                                                  |
| Pine 600 (primary/brand) | `#1f6f63` | `bg-pine-600` / `text-pine-600` | Links, success states, brand accents             |
| Pine 500                 | `#2c8577` | `bg-pine-500`                   |                                                  |
| Pine 400                 | `#4aa494` | `bg-pine-400`                   |                                                  |
| Sunset 600               | `#e85a3a` | `hover:bg-sunset-600`           | Primary CTA hover                                |
| Sunset 500 (accent/CTA)  | `#ff6b4a` | `bg-sunset-500`                 | Primary CTA (Book/Search buttons, logo mark)     |
| Sunset 400               | `#ff8a66` | `text-sunset-400`               |                                                  |
| Sand 50 (background)     | `#fbf7f1` | `bg-sand-50`                    | Page background                                  |
| Sand 100                 | `#f3ece1` | `bg-sand-100`                   |                                                  |
| Sand 200                 | `#e8ddc9` | `bg-sand-200`                   |                                                  |

No dark mode - intentional brand choice for this portfolio project, revisit
only if explicitly requested.

### Typography

- **Display/heading font:** Fraunces (serif) - `font-display`, applied to
  `h1`/`h2`/`h3` globally in `src/index.css` `@layer base`
- **Body font:** Manrope - `font-sans`, applied to `body`
- **Why not Inter everywhere:** a serif display face reads as editorial/
  human rather than generic SaaS - see `src/index.css`'s own comment
- **Loading:** via `index.html` `<link>`/`@import` (check current setup
  before adding new weights - avoid FOUC, keep weight count minimal)

### Spacing & Radius

No custom spacing scale - plain Tailwind defaults (4px increments:
`gap-2`, `p-4`, `px-6`, etc.) used consistently. Common radii observed
across components: `rounded-lg` (inputs, buttons, small cards),
`rounded-xl` (result cards, popovers), `rounded-2xl` (page sections,
modals), `rounded-full` (pills, avatar badges, CTA buttons on landing).

### Shadow Depths

Two custom utilities in `src/index.css` `@layer utilities` (not raw
Tailwind shadow classes - use these for anything meant to read as
"elevated" or "floating"):

| Utility            | Value                                                          | Usage                                                                        |
| ------------------ | -------------------------------------------------------------- | ---------------------------------------------------------------------------- |
| `.shadow-elevated` | `0 1px 2px rgba(11,27,30,.04), 0 12px 32px rgba(11,27,30,.1)`  | Cards, sections (flight/hotel result cards, search form container)           |
| `.shadow-popover`  | `0 2px 6px rgba(11,27,30,.06), 0 16px 40px rgba(11,27,30,.16)` | Dropdowns, modals, popovers (AirportAutocomplete, DatePicker, CheckoutModal) |

### Texture

`.bg-grain` utility (`src/index.css`) - a self-contained inline SVG
turbulence overlay at 3.5% opacity, `mix-blend-mode: overlay`. Used on the
landing hero and dark sections so flat color fills read as tactile/paper
rather than a flat digital gradient (dev-standards "Visual Styles &
Morphisms" guidance). Not yet applied to result cards/modals - candidate
for a future pass, apply subtly (same 2-4% opacity range) if extended.

---

## Motion & Animation

Two libraries in use, matching `dev-standards/standards/web-animations-pro.md`'s
decision table:

- **Framer Motion (`motion` package)** - micro-interactions: button
  hover/tap (`whileHover`/`whileTap`, spring `{ stiffness: 400, damping: 17 }`),
  list stagger (`initial`/`animate` with `delay: i * 0.05`), page
  cross-fade in `App.tsx` (`AnimatePresence`).
- **GSAP + ScrollTrigger** (`src/lib/gsap.ts`) - landing hero timeline,
  scroll-reveal sections (`ScrollReveal.tsx`), count-up stats
  (`CountUp.tsx`), confetti burst (`ConfettiBurst.tsx`).

### Hard-won rules (do not relearn these the hard way)

- **`<Routes>` inside an `AnimatePresence`-keyed wrapper MUST get an
  explicit `location` prop.** Without it, the exiting page's `<Routes>`
  stays live and re-renders against the _new_ location on any re-render
  tick, so the exiting and entering pages both show the new content
  instead of a real crossfade. See `App.tsx`'s own comment.
- **Never use `AnimatePresence mode="wait"` on a route that can redirect
  synchronously** (e.g. `ProtectedRoute`'s `<Navigate>` when logged out).
  A second, immediate navigation preempts the first exit animation and
  `mode="wait"` stalls forever waiting for an exit that never resolves.
  Use the default (overlapping) mode instead.
- **The GSAP-hero-stuck-at-opacity-0 "bug" seen during automated browser
  testing is not real** - it only reproduces when `document.visibilityState`
  never becomes `"visible"` in the testing tool (confirmed repeatedly this
  project), which starves `requestAnimationFrame`. A real user's focused
  tab never has this problem. Don't "fix" working animation code because
  an automated screenshot shows it frozen - check `document.visibilityState`
  first.
- Respect `prefers-reduced-motion` globally (already handled in
  `src/index.css`'s media query, and per-component in `ConfettiBurst.tsx`).

---

## Component Specs (Tailwind classes, not raw CSS)

### Buttons

Primary CTA:

```tsx
<motion.button
  whileHover={{ scale: 1.03, y: -1 }}
  whileTap={{ scale: 0.96 }}
  transition={{ type: 'spring', stiffness: 400, damping: 17 }}
  className="rounded-lg bg-sunset-500 px-4 py-1.5 text-sm font-medium text-white hover:bg-sunset-600"
>
```

Secondary/dark:

```tsx
className =
  "rounded-lg bg-ink-950 px-5 py-2 text-sm font-medium text-white hover:bg-ink-800 disabled:opacity-60";
```

### Result cards (flights/hotels)

```tsx
className =
  "shadow-elevated flex flex-col gap-3 rounded-xl border border-ink-950/10 bg-white px-4 py-3 transition-shadow hover:border-ink-950/20 sm:flex-row sm:items-center sm:justify-between";
```

with `whileHover={{ y: -2 }}` for a subtle lift.

### Popovers/dropdowns (autocomplete, date picker)

```tsx
className = "absolute z-20 mt-1 rounded-xl border border-ink-950/10 bg-white shadow-popover";
```

### Inputs

```tsx
className =
  "rounded-lg border border-ink-950/15 bg-white px-3 py-2 text-ink-950 outline-none transition focus:border-pine-500 focus:ring-2 focus:ring-pine-500/20";
```

---

## Anti-Patterns (Do NOT Use)

- ❌ Emojis as icons - Lucide only (`lucide-react`), already the project standard
- ❌ Native `<input type="date">` - use `components/DatePicker.tsx` (the
  browser default was flagged as the single biggest "generic/unfinished"
  element in an earlier design audit)
- ❌ Raw Tailwind `shadow-md`/`shadow-lg` on elevated surfaces - use
  `.shadow-elevated`/`.shadow-popover` for consistency
- ❌ Indigo/purple/blue as primary or accent - reserved-against, breaks
  the warm sand/pine/sunset identity
- ❌ Inter as the only font - keep the Fraunces/Manrope pairing
- ❌ Missing `cursor-pointer` - all clickable elements need it
- ❌ Instant state changes - always transition (150-300ms micro, ≤400ms
  section-level)
- ❌ Dark mode - not implemented, don't add without explicit request

---

## Pre-Delivery Checklist

- [ ] No emojis used as icons
- [ ] Icons are Lucide, consistent stroke width
- [ ] `cursor-pointer` on all clickable elements
- [ ] Hover states with smooth transitions (150-300ms)
- [ ] Text contrast ≥4.5:1 (light mode only, no dark mode to check)
- [ ] Focus states visible for keyboard navigation
- [ ] `prefers-reduced-motion` respected
- [ ] Responsive: 375px, 768px, 1024px, 1440px
- [ ] `<Routes>` inside `AnimatePresence` has explicit `location` prop
      if this touches routing
- [ ] Verified in a real focused browser tab, not just an automated
      screenshot (see Motion & Animation note above)
