# A Lisp that loads

**This Week in Modus №13** · 30 May – 5 June 2026 · 173 commits · 1 repo

<https://modus-lisp.github.io/issues/2026-06-06/>

Modus stopped being a compiler that produces a program and became an image that reads Lisp: `load`, real backquote, macros defined at runtime, compilation firing inside the running image. Symbols learned which package they belong to, integers stopped having a size limit, and the layout cascade stopped reproducing.

| Commits | Source files | Lines added | To build an image |
|---|---|---|---|
| 173 | 39 | +15k | ~30s |

---

*modus / 3–5 June / 97a66b6, 8c7202d*

## An image that reads Lisp

Last week Modus could load the ANSI files as written. This week it went further and stopped treating “the program” and “the image” as the same thing. A new build produces an 8.8 MB image in about thirty seconds that runs Common Lisp at runtime rather than baking a fixed program in, and a layer above it puts the compiler *inside* that image, so compilation fires in-process.

What had to exist first is a long list, and it is most of the week’s commits. Real backquote in runtime `defmacro`, so a macro can be written the way anyone would write it rather than out of explicit `list` calls. `funcall` and `apply` as special forms in the evaluator. Runtime `catch` and `throw`, `restart-case`, `handler-bind`, `with-simple-restart`, `prog`, `progv`, `flet` and `labels` with their implicit blocks, dynamic `let` of special variables, `with-output-to-string`, the whole extended `loop` vocabulary at evaluation time, and `defvar` initialisation thunks that actually fire at boot. Underneath, the arithmetic and sequence functions had to exist as ordinary functions the evaluator could call, not only as things the compiler knows how to inline.

Alongside it, the test infrastructure was rebuilt around the same idea: a per-file runner generating one isolated binary per subdirectory, so sweeps run in parallel and a crash costs one file. Which is where a week’s worth of accumulated wall-clock problems — including the shard cap that had masqueraded as fragility in [№ 12](https://modus-lisp.github.io/issues/2026-05-30/) — stop being a factor at all.

---

*modus / 31 May – 3 June / a1327d6, daa0763*

## Symbols learn where they live

A symbol in Common Lisp is not just a name. It belongs to a package, and two symbols with the same name in different packages are different objects — that is the point of having packages at all. Modus had been interning by name, so they were not.

The fix arrives as a plan document and then a first phase: distinct interning per package, symbols carrying their home package, and quoting made package-aware in the compiler so that a literal symbol in source resolves to the symbol in the package the source was read in. It is the kind of change that touches everything and shows up in the pass rate only indirectly — and the commit that lands it also strikes a long-standing entry from the project’s own list of known limitations, which is the honest way to close one.

The same week integers stopped being bounded. Bignums had been fixed-width; they are now arbitrary precision, stored as sign and magnitude across as many limbs as needed, with literals materialised at runtime, bit operations that know about them, and a printing bug fixed that had been scrambling large values — worth 109 tests on its own. Two of the oldest simplifications in the implementation, replaced within a few days of each other.

---

*modus / 31 May / 19e62e9*

## The cascade stops reproducing

One line in the project’s notes closes a story that has run through five issues: the layout-dependence cascade is no longer reproducible. The docstring in [№ 9](https://modus-lisp.github.io/issues/2026-05-09/) was the bulk of it, function tagging in [№ 10](https://modus-lisp.github.io/issues/2026-05-16/) removed the mechanism that turned a shift into a wrong answer, and the two remaining suspects turned out to be a code-generation bug and a timeout. What looked for a month like a deep instability in the compiler was four ordinary bugs standing close together.

In its place, something better: `DIVERGENCE.md`, a living list of the ways this implementation knowingly differs from the standard. A project with a conformance suite accumulates two kinds of failure — things that are broken, and things that are deliberate — and writing the second kind down is what stops it quietly becoming the first.

---

*modus / 1–3 June*

## The printer, the reader, and 250 signalled errors

Two components got the treatment the sequence functions got in [№ 8](https://modus-lisp.github.io/issues/2026-05-02/). The printer implemented the full matrix of `*print-case*` against `*readtable-case*`, which is a genuinely fiddly cross-product and worth 40 tests, plus the vertical-bar escaping rule for symbols whose names need it, and correct handling of `*print-length*` and `*print-level*` against dotted tails and arrays. The reader gained read-time evaluation with `#.`, shared-structure labels and references, mid-token escapes, ratios in other radices, and `*features*` with the recursion its conditional forms require.

And the long tail of the week is one instruction repeated across dozens of functions: *complain*. Keyword plists validated for odd length and unknown keys in `map`, `merge`, `concatenate`, `replace`, `make-array`, `make-sequence`, `read-from-string` and `write`; arity errors on the stream operations and `maphash`; type errors from `fill-pointer`, `character`, and the array introspection functions; leftmost-wins applied yet again to duplicate keywords. Almost none of it is interesting on its own. All of it is the difference between a Lisp that mostly works and one that tells you when you are wrong.

---

*a note on the log*

## Five commits, landed and reverted and landed

On 2 June a batch of work across five subsystems — sequences, conditions, the reader, packages and CLOS — is committed, reverted commit-for-commit within minutes, and then committed again. Nothing in the tree records why, and this dispatch is not going to invent a reason. It is worth pointing at only because the alternative — quietly rewriting the history so the shelf looks tidy — is the thing that makes a log useless later.

---

**Method.** Commits by *author* date, 30 May to 5 June 2026, across the git repositories in the modus-lisp workspace — at this point, one. Line counts exclude generated build artefacts. Produced with `bin/week 2026-06-06 --log`.

[← All issues](https://modus-lisp.github.io/issues/) · [crier](https://modus-lisp.github.io/)
