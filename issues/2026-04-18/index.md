# Counting what actually ran

**This Week in Modus №6** · 11–17 April 2026 · 107 commits · 1 repo

<https://modus-lisp.github.io/issues/2026-04-18/>

Modus spent four days building a Common Lisp runtime in layers — reader, printer, conditions, CLOS, streams, packages — and a copying garbage collector in x64 assembly, and reported 17,568 ANSI tests with one failure. On Friday it found out that 17,587 of those tests had never run.

| Commits | Source files | Lines added | Tests found missing |
|---|---|---|---|
| 107 | 39 | +43k | 17,587 |

---

*modus / 13–15 April / layers 2 to 8*

## A Common Lisp, in layers

The week’s middle is a plan and its execution. `PLAN.md` arrives on Monday and states the destination plainly — a self-hosting bare-metal Common Lisp that replaces SBCL — along with the architecture to get there: the portable bytecode, hosted and bare-metal targets, an image format that embeds its own source for hash verification and lazy compilation, and a roadmap for the language itself. Design notes for the collector, for concurrency (actors rather than threads, with a compatibility layer over them), and for a performance roadmap follow the same day.

Then the layers go in, each one a real piece of the standard rather than a stub:

- **The reader** — a readtable, `#` dispatch, backquote, package qualifiers.

- **Streams** — nine types, with no failures.

- **Packages** — twenty-four functions, and 303 package tests passing.

- **The printer**, and `format`.

- **Conditions** — twenty-four condition types in their real hierarchy, `define-condition`, `signal`, `warn`, `cerror`, `handler-bind`, the restart machinery, and the five standard restarts.

- **CLOS** — first minimally, then a full generic function system with `:before`, `:after` and `:around` methods, the short form of `define-method-combination`, `call-next-method`, and class precedence lists for built-in as well as user-defined types.

- **Eval, compile and load** — as a tree-walking interpreter.

Alongside them, the compiler kept paying for its own history. A frame-slot clobber in `let` turned out to be the root cause of a nested-expression bug that had been worked around; the workaround came out. So did workarounds for `string-capitalize`, for `equal` mis-dispatching to `eq`, and for a push-and-pop shape in multiple values. An x64 shift instruction had been encoding the wrong REX bit — `REX.B` is the one that extends a register-or-memory operand, not `REX.R`. Bignums gained working carry and overflow. And a heap that started 256 bytes in was overlapping the multiple-value storage area, which is the kind of bug that presents as anything at all.

---

*modus / 16 April / 2cc98bb*

## A copying collector

A Cheney semi-space collector, written in native x64 assembly: two 469 MB semispaces inside an 896 MB heap, collection triggered when the allocation pointer reaches the midpoint, roots scanned from the stack plus the globals list and the symbol table, cons cells and header-sized objects copied, forwarding pointers left behind in from-space, and a check emitted at every allocation site.

All 389 of the project’s own tests pass, and twelve method-combination tests regress, for a reason the commit states rather than hides: **the collector moves the CLOS dispatch tables and does not update every reference to them**, because the generic function registry is not yet a root. Two related bugs go with it — the scanning routine clobbering a register it had not saved, and a note correcting an earlier diagnosis from “translator bug” to “root scanning”.

---

*modus / 17 April / e666fca*

## Counting what actually ran

By Thursday evening the reported figure was 17,568 tests with a single failure — 99.994%. On Friday somebody asked what the harness was actually counting, and the answer was: not much.

Test files were being run in chunks, twenty files to a forked child. When a child died mid-chunk — and children were dying, on the first unimplemented thing each chunk reached — every test after that point simply vanished. It was not recorded as a pass. It was not recorded as a failure. The summary saw only the one test that had managed to print the word FAIL before the process went away.

> Honest counting: 109 of 17,708 pass. Twelve visible failures. **17,587 lost to fork crashes.**

> — modus — e666fca

The fix for the counting is almost crude, and it is exactly right. Each passing test writes a single `+` byte straight to standard output, unbuffered, with no trailing newline, so the byte is already gone before anything can crash; the summary counts bytes. The total expected is printed before the run begins, so lost tests are the difference. A harness that cannot lie to you about how many tests it ran.

Then the crashing itself was attacked. Each individual test now runs in its own forked child, so one crash costs one test rather than nineteen unrelated files. Signals — segmentation faults, bus errors, floating-point and illegal-instruction traps — are caught and converted into recoverable Lisp errors. Both parent and child wrap their work so that a child’s death cannot longjmp into the parent’s handler and fork-bomb the rest of the run, which is what had been happening.

The result, measured on a thousand-test sample in full isolation: crashes down from about 20% to about 1%, **99.1% of tests now producing a clean verdict**, and an honest pass rate of 25.6% with a confidence interval reported beside it. The working baseline for the files wired up at the time settles at 353 of 660.

Which is to say the week ended at a quarter of where it thought it was on Thursday, and that is the most valuable thing in it. [№ 5](https://modus-lisp.github.io/issues/2026-04-11/) adopted somebody else’s tests so the implementation could not grade itself; this week found out that adopting the tests is not enough if the thing counting them is your own.

---

*modus / 17 April / 0b857c9, ce53eb8*

## Three bugs it was hiding

Crashes at twenty per cent were not bad luck. Three real defects were underneath them, and each had been invisible precisely because the harness swallowed its own casualties.

- **The saved instruction pointer was twelve bytes wrong.** The trap that implements `setjmp` computed its return address for a two-byte jump that no longer existed, so the saved address landed in the middle of an instruction and every `longjmp` jumped into garbage. Latent for a long time, because `handler-case` was rarely exercised until conditions arrived this week.

- **Two things were using the same address.** `handler-case` saved its stack pointer at an address that collided with the closure environment slot, so any closure call inside a protected body silently destroyed the handler state guarding it.

- **Signals were never being delivered.** Installing a handler with the raw system call requires a restorer trampoline, and without it Linux on x86-64 fails to deliver the signal at all — quietly.

---

**Method.** Commits by *author* date, 11 to 17 April 2026, across the git repositories in the modus-lisp workspace — at this point, one. The pass rates quoted are the ones the commits themselves report, including the ones they later withdraw. Line counts exclude generated build artefacts. Produced with `bin/week 2026-04-18 --log`.

[← All issues](https://modus-lisp.github.io/issues/) · [crier](https://modus-lisp.github.io/)
