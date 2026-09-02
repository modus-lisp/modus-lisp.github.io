# The suite, unmodified

**This Week in Modus №12** · 23–29 May 2026 · 111 commits · 1 repo

<https://modus-lisp.github.io/issues/2026-05-30/>

Every ANSI test Modus had run so far had been preprocessed on the way in. This week it gained enough runtime macro machinery to load the files as they are — and on AArch64, discovered that NIL and the fixnum zero had been the same bit pattern all along.

| Commits | Source files | Lines added | Files loaded as written |
|---|---|---|---|
| 111 | 22 | +11k | ~150 |

---

*modus / 25 May / 032b2bc*

## Loading a file nobody rewrote

There has been an asterisk on every pass rate in this dispatch so far. The ANSI suite defines its tests with a macro, and Modus could not define macros at runtime, so the test files were being rewritten before Modus ever saw them — a bridge on the host side turning `deftest` forms into something the implementation could already compile. That grades a translation, not the original.

Removing the asterisk took three insights that arrived together, after three earlier attempts had regressed the suite badly enough to be reverted — by 1,749 tests, and then by 2,860.

- **The macro could not be written with backquote.** The host Lisp’s backquote expands into its own internal forms, which Modus’s evaluator has no reason to understand. Building the macro body out of explicit `list` and `quote` calls sidesteps a dependency nobody had noticed taking.

- **It had to be defined at runtime, not compile time.** A global compile-time `deftest` macro broke everything — that was all three failed attempts. Defining it by evaluating a `defmacro` during initialisation affects only later evaluation, leaving already-compiled call sites alone.

- **The reader stopped at the first line.** The suite’s files open with an Emacs mode line, and the whitespace skipper had no clause for a semicolon comment. The very first form of every file misparsed.

With those, one unmodified file loads. Then three more, then seventeen more cons files, then whole directories — symbols, strings, numbers, iteration, sequences, conditions — about 150 files of the real suite, read as written. The machinery underneath it is the interesting part: a runtime macro table that extracts all seventy of the compiler’s macros at boot, runtime `defun` callable from `eval`, runtime `loop`, `block`, `return-from`, `tagbody` and `go`, and `macrolet` in the evaluator.

---

*modus / 29 May / 7713045*

## NIL was zero

On the AArch64 builds, the register holding NIL had been initialised to raw zero. A fixnum zero is also zero. So `(eq 0 nil)` was true, `(null 0)` was true, and every place in the standard where zero is a legitimate value and NIL means “not supplied” became ambiguous.

> `:count 0` truncating early. `:end 0` returning the whole sequence. Slot index zero misrouting in CLOS. `documentation` returning 0 instead of NIL. One catalogue of symptoms, one cause.

> — modus — 7713045

Moving NIL to the same distinguished value x64 uses fixed the class. Two commits either side of it change internal sentinels from NIL to −1 for the same reason — in the sequence functions and in CLOS slot lookup — because an “absent” marker that can collide with a legitimate value is a bug waiting for the right input, and those two were worth 146 and 140 tests respectively.

That work sat on top of a new platform: a hosted Linux/AArch64 target, with its own ELF wrapper, entry stub, syscall helpers and cross-compiler dispatch. Being able to run the suite on ARM without emulating a whole machine took Linux/AArch64 from 61.4% to 67.4% over two days, with IEEE float opcodes for that architecture worth another 210 tests.

---

*modus / 25 May / 3aa4de9*

## Not layout either, for the third time

Adding `macrolet` lost 253 tests, in the pattern that by now everybody recognises. It was not a layout shift. Every shard was re-running the entire suite-loading probe block — about 150 files, five to ten seconds — and adding `macrolet` pushed the total past a ten-minute per-shard wall-clock cap, so the tests at the tail of each shard never ran.

The diagnosis is a nice piece of method: compare which test identifiers went missing before and after, notice they are all at the ends of shards rather than clustered by feature, run one of them directly, and watch it pass. Three times now — a docstring in [№ 9](https://modus-lisp.github.io/issues/2026-05-09/), a code-generation bug in [№ 11](https://modus-lisp.github.io/issues/2026-05-23/), a timeout here — something has looked like layout fragility and been something ordinary underneath.

---

*modus / 26–27 May*

## LOOP, and a class precedence list that was wrong

Roughly twenty commits went into `loop`, which is less a macro than a language: destructuring in `with` and in `being the hash-keys`, chained `with … and` bindings parsed as parallel rather than sequential, `of-type` honoured so a typed accumulator starts at 0.0 rather than 0, `loop-finish`, `finally` skipped when the body returns, a named loop suppressing the implicit block that would otherwise swallow the return, and program errors signalled for duplicate iteration variables and for accumulator clauses that cannot coexist.

And the class precedence list was being computed by depth-first search with duplicates removed, which gives the wrong order for diamond inheritance: with D inheriting from B and C, both inheriting A, it produced D B A C — putting A ahead of C, so C’s slot initialisers lost to A’s. The standard specifies C3 linearization, and that is now what it does. The commit notes that the shard score did not move, because an unrelated crash upstream was still pre-stamping the range that would have shown it — a fix landing correctly with no number to show for it, recorded anyway.

---

**Method.** Commits by *author* date, 23 to 29 May 2026, across the git repositories in the modus-lisp workspace — at this point, one. Percentages are the ones the commits report, on the hosted Linux/AArch64 build. Line counts exclude generated build artefacts. Produced with `bin/week 2026-05-30 --log`.

[← All issues](https://modus-lisp.github.io/issues/) · [crier](https://modus-lisp.github.io/)
