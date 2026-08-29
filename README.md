# crier

**The modus-lisp project's own dispatch — a static site, written by hand, served from this repo
at [modus-lisp.github.io](https://modus-lisp.github.io/).**

> The repository is called `modus-lisp.github.io` because GitHub will only serve an
> organisation's site at the root from a repo with exactly that name. The site is called
> **crier**, and that is the name that appears on it.

[modus-lisp](https://github.com/modus-lisp) is forty-two repositories that add up to one
personal computer written from scratch in Common Lisp. That is a lot of work happening in
a lot of places, and none of it explains itself from the outside. crier is where it gets
explained.

## What's in it

| | |
|---|---|
| `index.html` | The front page: what the project is, the issue index, a map of all 42 repositories. |
| `issues/YYYY-MM-DD/index.html` | One issue of **This Week in Modus** — what moved, across every repo, and why it was hard. The date is the Saturday that closes the week. |
| `bin/week` | Everything an issue is made of: the ledger, and every commit subject grouped by repo. |
| `assets/crier.css` | The whole design system. Tokens, then chrome, then prose. There is no second stylesheet. |
| `.nojekyll` | Tells Pages to serve the files as written rather than running Jekyll over them. |

No generator, no build step, no dependencies. A page is a file you can open with `file://`,
which is the same reason everything else in this org is written the way it is. If a
generator ever arrives it will be [weft](https://github.com/modus-lisp/weft) and
[scribe](https://github.com/modus-lisp/scribe) rendering it, not npm.

## Publishing

GitHub Pages, deployed from the `master` branch at the repository root — Settings → Pages →
*Deploy from a branch* → `master` / `/ (root)`. That puts the site at
<https://modus-lisp.github.io/>, the organisation's front door. Every push is live within a
minute; there is nothing to build, so there is nothing to break in a build.

## Weeks, and their numbers

An issue dated Saturday *S* covers the seven days `[S-7, S)`, by **author** date — when the work
was done, not when a rebase replayed it. `git log --since` reads the *committer* date, which a
history rewrite resets to the day of the rewrite, so a week's work would land in whatever week it
was replayed; `bin/week` filters on `%ad` instead.

Issues are numbered from the project's first working week, counting only weeks that have commits
in them. `bin/week --list` prints the whole numbering, and is the authority:

```
$ bin/week --list | tail -3
 23  2026-08-15  8 Aug–14 Aug 2026    273 commits
 24  2026-08-22  15 Aug–21 Aug 2026   136 commits
 25  2026-08-29  22 Aug–28 Aug 2026    88 commits
```

The number of an issue therefore never changes. №25 was published first; №1 is a single commit
in March 2025. **The archive is complete** — all 25 weeks of the project have an issue — so the
next one to write is simply the next Saturday.

## Adding an issue

1. `bin/week 2026-09-05 --log` — read the whole week, then read the bodies of the commits that
   turn out to matter. The subjects in this workspace are written to be read, but the *why* is
   almost always in the body.

   The backfill was written **forward**, oldest first, and any future one should be. An issue
   should only know what came before it, which is also the reader's position; written backwards
   you end up explaining early code in terms of what it later became, and inventing significance
   the week did not have yet. №17–№25 predate that rule and do contain forward references.
2. `cp -r issues/2026-08-29 issues/2026-09-05` and rewrite it. Change `<title>`, the `og:` tags,
   the kicker, the headline, the standfirst, the tally and the ledger. The stylesheet needs no
   changes — every class an issue uses already exists.
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
- **A gap in the numbers is not necessarily a gap in the work — and vice versa.** №1 sits
  eleven months before №2 because its *author* date is March 2025; its *committer* date is
  February 2026, because the repository was created on the way back from a break. The gap was
  real, and the log alone would not have shown it. Check `%cd` against `%ad` before drawing a
  conclusion from a date, and ask a human when the repository cannot answer.
- **Length follows the week.** №17 is 437 commits and runs six sections; №1 is one commit and
  runs three paragraphs. A single-repo week gets no ledger table, because a one-row table is
  furniture. Nothing here is printed and nothing is sold against it, so there is no length to
  hit.

## Licence

MIT, like everything else in the org.
