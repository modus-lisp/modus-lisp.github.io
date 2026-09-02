# The frontier is a form number

**This Week in Modus №14** · 6–12 June 2026 · 113 commits · 1 repo

<https://modus-lisp.github.io/issues/2026-06-13/>

A conformance suite tells you what fraction of a standard you have. It does not tell you whether real software will load. So Modus started reading actual ASDF, one top-level form at a time, and treating the number of the form it dies on as the thing to improve.

| Commits | Source files | Lines added | ANSI, by Friday |
|---|---|---|---|
| 113 | 26 | +20k | 88.08% |

---

*modus / 10–12 June / the gauntlet*

## The frontier is a form number

ASDF is the system definition facility every Common Lisp library expects to exist. It is also a large, old, deliberately portable piece of Lisp that leans on the standard in places nobody writing a fresh implementation would think to visit. Vendoring it and feeding it to `load` gives a single number: how many top-level forms go in before something breaks.

The week starts at form 11 and ends with the file reading end to end — a frontier of 243 — and the intermediate stops are a tour of the standard’s corners. Form 11 was a re-export problem: the standard symbols were not marked external at the tail of boot, so `cl:&rest` could not be referred to from another package. Forms 34, 36 and 40 needed `define-condition` in the evaluator, not just in the compiler. Forms 43 and 44 were masking errors rather than reporting them. Form 54 was a reader bug. Form 55 was a garbage collection fault, narrowed to a missing root for a lexical local.

What makes the gauntlet worth running alongside a test suite is exactly that shape. A test suite is thousands of small independent questions and gives you a percentage; a real file is one long dependent chain, and it fails at the first thing you got wrong. The two measure different properties, and this week the project ran both, side by side, with a README in the vendored directory tracking the frontier as it moved.

---

*modus / 10–12 June / e0ea20c, edabf05, a3b11e9, c9c6278*

## Five things the collector could not see

The gauntlet found garbage collection bugs faster than the test suite ever had, and the reason is structural: loading a real file keeps a great deal of live state across many allocations, which is precisely the condition under which a missing root shows up. Four landed in three days.

- **The keyword and package tables were not roots.** Every keyword and every package lived in tables the collector never scanned, so a collection during runtime evaluation could move them out from under everything. Described in the commit as the number one cause of faults in that path.

- **The multiple-value area was not a root.** Values beyond the first, in flight between a producer and its consumer, were invisible.

- **The conservative stack scan ran off the end.** A missing bound on where from-space stops meant handler-heavy code could corrupt the heap.

- **Object copying had no bounds guard**, and allocation could overshoot the end of the mapped heap, so a sixteen-megabyte guard band now sits between the allocation check and the edge.

Each is a one-line entry in a status file with an ANSI figure beside it — 15,251, then 15,262, then 15,323, then 15,350 — which is what makes them findable at all. A collector bug that costs a hundred tests looks like a hundred unrelated feature gaps until something narrows it.

---

*modus / 10–12 June / bands 2, 3 and 4*

## 86.66% to 88.08%

The suite work ran in bands, and each commit records where the number stood. The largest single jump is `defgeneric`, which went from 13 of 55 tests in its file to 52 — a proper implementation of the standard’s semantics for it, plus the dispatch fixes that fell out. Adjustable arrays got reshaping at rank two and above, displacement offsets honoured when copying, and multi-dimensional array literals in the reader. The transcendental functions from [№ 11](https://modus-lisp.github.io/issues/2026-05-23/), which had been computed as scaled rationals for portability, gained native IEEE implementations on the architectures that have floating point — with the rational path still there for the ones that do not.

CLOS kept filling in: initialisation forms evaluated in the right lexical environment, initargs feeding several slots at once, `with-slots` and `with-accessors` writing back through `setf`, `slot-missing` honouring `eql` specializers, and a class keeping its identity when redefined rather than becoming a new one.

---

*modus / 11–12 June / fa197aa, 616bbb9*

## Two reader bugs worth naming

- **Interning collided on case.** Two names that hash the same but differ in case were being treated as the same symbol. The symbol table is keyed by a hash, and a hash is not an identity — the same lesson as [№ 7](https://modus-lisp.github.io/issues/2026-04-25/)’s subtags, arriving from a different direction.

- **Comments desynchronised dotted lists.** The whitespace scan inside a list had no clause for a semicolon comment, so a comment sitting between a dot and the tail threw the parse off. That was the ASDF gauntlet’s form 54 — a real file, containing a comment in a place no hand-written test would have put one.

Which is the argument for the gauntlet in one paragraph.

---

**Method.** Commits by *author* date, 6 to 12 June 2026, across the git repositories in the modus-lisp workspace — at this point, one. ANSI figures and percentages are the ones the commits report. Line counts exclude generated build artefacts. Produced with `bin/week 2026-06-13 --log`.

[← All issues](https://modus-lisp.github.io/issues/) · [crier](https://modus-lisp.github.io/)
