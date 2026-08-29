# crier

**The modus-lisp project's own dispatch — a static site, written by hand, served from this repo.**

[modus-lisp](https://github.com/modus-lisp) is forty-two repositories that add up to one
personal computer written from scratch in Common Lisp. That is a lot of work happening in
a lot of places, and none of it explains itself from the outside. crier is where it gets
explained.

## What's in it

| | |
|---|---|
| `index.html` | The front page: what the project is, the issue index, a map of the workspace. |
| `issues/YYYY-MM-DD/index.html` | One issue of **This Week in Modus** — what moved, across every repo, and why it was hard. |
| `assets/crier.css` | The whole design system. Tokens, then chrome, then prose. There is no second stylesheet. |
| `.nojekyll` | Tells Pages to serve the files as written rather than running Jekyll over them. |

No generator, no build step, no dependencies. A page is a file you can open with `file://`,
which is the same reason everything else in this org is written the way it is. If a
generator ever arrives it will be [weft](https://github.com/modus-lisp/weft) and
[scribe](https://github.com/modus-lisp/scribe) rendering it, not npm.

## Publishing

GitHub Pages, deployed from the `master` branch at the repository root — Settings → Pages →
*Deploy from a branch* → `master` / `/ (root)`. Every push is live within a minute; there is
nothing to build, so there is nothing to break in a build.

## Adding an issue

1. `cp -r issues/2026-08-29 issues/$(date +%F)` and rewrite it.
2. Change `<title>`, the `og:` tags, the kicker, the headline and the standfirst.
   The stylesheet needs no changes — every class an issue uses already exists.
3. Add one `<li>` to `ol.issues` in `index.html`, newest first.

## House rules for the writing

- **Every claim comes out of the commit log.** Counts are counted, not estimated; a
  quoted REPL transcript is one that actually ran. The method footnote at the bottom of an
  issue says what was measured and what was excluded, so a number can be checked.
- **A reverted commit is a fact too.** Issue №1 reports a 3× speedup and its revert two
  paragraphs later, because that is what the week contained.
- **Name the root cause, not the symptom.** The interesting sentence is almost never
  "fixed a crash"; it is which register the bootloader left set, or which comparison
  happens before the allocation.
- **The reader is a stranger.** Not everyone reading knows what a seat, a shard, or a
  constant vector is. Say it in the sentence rather than linking away.

## Licence

MIT, like everything else in the org.
