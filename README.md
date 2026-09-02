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
| `index.html` | The front page: what the project is, the newest and oldest issues, a map of all 42 repositories. |
| `issues/index.html` | The archive — every issue, grouped by month. |
| `issues/YYYY-MM-DD/index.html` | One issue of **This Week in Modus** — what moved, across every repo, and why it was hard. The date is the Saturday that closes the week. |
| `bin/week` | Everything an issue is made of: the ledger, and every commit subject grouped by repo. |
| `bin/ci` | The last CI run for every repository in the org, worst first. `--bad` for only the problems. |
| `bin/mirror` | Regenerates the machine-readable half of the site from the site itself. `--check` fails if it is stale. |
| `assets/crier.css` | The whole design system. Tokens, then chrome, then prose. There is no second stylesheet. |
| `.nojekyll` | Tells Pages to serve the files as written rather than running Jekyll over them. |
| `issues/*/index.md`, `feed.xml`, `llms.txt`, `index.json`, `sitemap.xml`, `robots.txt` | **Derived.** Written by `bin/mirror`; never edit them by hand. |

Every page is written by hand, and a page is a file you can open with `file://` — which is
the same reason everything else in this org is written the way it is. There is no build step
between an issue and the web.

There is one generator, and it runs the other way. `bin/mirror` reads the finished HTML and
emits the machine-readable mirror of it: a Markdown copy of every issue, an Atom feed, an
llms.txt index, a JSON ledger, a sitemap. The HTML stays the source of truth, so the mirror
cannot drift from it — and if it does, `bin/mirror --check` says so and exits non-zero.
Delete every generated file and re-run; you get them all back.

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

   The archive was written **forward**, oldest first, and any future backfill should be. An issue
   should only know what came before it, which is also the reader's position; written backwards
   you end up explaining early code in terms of what it later became, and inventing significance
   the week did not have yet.

   №1 is the one deliberate exception, and it links forward twice. It covers a single commit that
   was filed eleven months after it was written, and whose meaning is that it was superseded ten
   days later — an issue about being superseded cannot be written without naming what superseded
   it. Everything from №2 on links backwards only.
2. `cp -r issues/2026-08-29 issues/2026-09-05` and rewrite it. Change `<title>`, the `og:` tags,
   the kicker, the headline, the standfirst, the tally and the ledger. The stylesheet needs no
   changes — every class an issue uses already exists.
3. Add one `<li>` to the top of the **Latest** list in `index.html`, and drop the oldest entry
   out of that list so it stays at five. The "From the beginning" list below it never changes.
4. Add the same `<li>` to `issues/index.html`, under its month heading.

## House rules for the writing

- **Every claim comes out of the commit log.** Counts are counted, not estimated; a
  quoted REPL transcript is one that actually ran. The method footnote at the bottom of an
  issue says what was measured and what was excluded, so a number can be checked.
- **A reverted commit is a fact too.** №7's best commit adds no code: three attempts at a
  runtime `car`/`cdr` check, all reverted, with the cost of each written down. A week that
  ended where it started still says something about the shape of the problem.
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

  The reverse case is sharper: a single dated commit can hold a fortnight. Everything Modus
  did in February 2026 on the Movitz base — SSH, a TCP stack, the long-mode experiments — is
  inside `abd9648`, which is dated March 2025. One point in a log, two weeks of work.
- **A second source is allowed, and must announce itself.** The archive is built from the
  commit log, and where a claim comes from anywhere else the issue says so in its own
  footnote. №1 uses the author's nostr notes (4,955 events for `576d23dc…`, pulled from nine
  relays, deduplicated by event id) because the log cannot show an eleven-month gap it does
  not record. Quote a note with its event id and its UTC timestamp so a reader can fetch it;
  store images in `assets/img/` rather than hotlinking them, and verify the hash when the
  host is content-addressed. A note is evidence of what was said and when — never silently
  evidence of what was built.
- **The archive should be as readable to a machine as to a person.** This project's argument
  is that legibility is a security property; an archive that can only be read by rendering it
  would be a small hypocrisy. Every issue has a Markdown mirror at `index.md` beside it, and
  the whole ledger is in `index.json`. Run `bin/mirror` after writing or editing an issue.
- **Length follows the week.** №17 is 437 commits and runs six sections. №1 is a single
  commit and would run three paragraphs on the log alone; it is long because the evidence for
  what that commit actually contains lies outside the log. A single-repo week gets no ledger
  table, because a one-row table is furniture. Nothing here is printed and nothing is sold
  against it, so there is no length to hit.

## Licence

MIT, like everything else in the org.
