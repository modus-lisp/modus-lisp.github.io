# The bug was a docstring

**This Week in Modus №9** · 2–8 May 2026 · 62 commits · 1 repo

<https://modus-lisp.github.io/issues/2026-05-09/>

The layout fragility that had been flipping tests for two weeks turned out to be documentation. Meanwhile the ANSI suite was ported to bare-metal AArch64, where there is no operating system to fork with — so isolation had to be rebuilt out of a timer, a bitmap, and eventually an actor.

| Commits | Source files | Lines added | Records, on bare metal |
|---|---|---|---|
| 62 | 21 | +8.6k | 17,692 |

---

*modus / 3 May / 45071a9*

## The bug was a docstring

The compiler strips declarations from the front of a function body before compiling it. It did not strip docstrings. So a string sitting there as the first form was compiled as a string — allocated, in the function’s prologue, every single time the function was called, at roughly twenty bytes of machine code per character.

> `string-equal`, with a 280-character docstring, compiled to 7,022 bytes instead of 1,822. The five-kilobyte shift pushed unrelated callers downstream across a function-address alignment edge, corrupting dispatch and crashing at boot.

> — modus — 45071a9, “root cause of the long-running layout fragility plague”

That is the answer to [№ 8](https://modus-lisp.github.io/issues/2026-05-02/)’s fuzzer. The tests that flipped when the code moved were not flipping because of anything mysterious about the code; something had been silently making functions four times larger than they should be, and the amount depended on how much prose their author had felt like writing. It also explains a separate note in the project’s own documentation about miscompiles past a “function size threshold”.

The fix is one line in the right place, and the commit immediately after it puts the full docstring back on `string-equal` — which is the nicest possible way to demonstrate that a bug is fixed.

---

*modus / 5–7 May / e3d4982, 9a4d21d, 6e22773*

## Isolation without a kernel

Everything that made the harness trustworthy in [№ 6](https://modus-lisp.github.io/issues/2026-04-18/) and [№ 7](https://modus-lisp.github.io/issues/2026-04-25/) was borrowed from Linux: `fork` for per-test isolation, shared memory for verdicts that outlive a crash, signal handlers to turn faults into errors. On bare metal none of those exist, and bare metal is where this implementation is supposed to live. So the suite was ported to run with no operating system underneath it, on both x64 and AArch64, and every borrowed mechanism had to be replaced.

- **For `fork`: nothing, at first.** Each test simply runs inside a single `handler-case`. Which means a test that hangs hangs the machine.

- **For the alarm: the virtual timer interrupt.** A per-test deadline, tuned down from five guest seconds — about ten minutes of wall clock under emulation — to fifty milliseconds, so a timeout can actually fire before anybody gives up.

- **For crash containment: a bitmap.** One byte per test at a fixed address, recording that a verdict has been produced. Without it, a stale non-local exit from a wedged test re-entered ten later tests about twenty times each, filling the output with duplicates. With it, every test produces at most one result.

- **For the tests that cannot be run at all: say so in advance.** Ranges of known-wedging tests are pre-stamped as failures before the run, so their absence is recorded rather than lost — the same principle as the `+` byte, applied to tests that never get the chance to write one.

The coverage climbs commit by commit — 229, then 2,058, then 7,898, then 13,211 at 75%, then 17,403 at 98% — and closes on Thursday:

*modus — 6e22773, bare-metal AArch64*

```
P = 1,602   F = 16,090   =  17,692 unique records
100% per-test coverage — missing = 0
```

Sixteen thousand of those are failures, and that is the point rather than an embarrassment. This week was not about passing the suite on bare metal; it was about being able to *see* the suite on bare metal. A number you can trust is the precondition for improving it, which is the whole argument of the last four issues.

---

*modus / 7–8 May / a2ae64d*

## An actor per test

The remaining hole is the one `fork` was really for: a test that wedges takes the machine with it. Modus already has the answer and has had it since [№ 2](https://modus-lisp.github.io/issues/2026-02-28/) — actors, each with its own stack and heap, which is what isolation looks like when the language is the operating system.

The mechanism is small enough to describe completely. The main actor writes a test identifier, a thunk and an expected value into a shared block at a fixed address and yields. A worker actor, looping on yield, picks the request up, runs the thunk under its own `handler-case`, writes back either a result or a crash status, and yields. The main actor reads the status: completed, hand the value to the comparison; crashed, record a failure; still running past the deadline, record a failure and *leak the actor* — abandon it where it stands and carry on, which is exactly what killing a forked child amounts to.

It went in against the single test that had started the whole investigation, then six, then eight, then fifteen. A proof of concept rather than a finished harness — but the shape is right, and it is the first time the actor system has been used for the thing it was designed for.

---

*modus / 3–4 May / 4b8c87e*

## Keywords become objects

A keyword had been compiled to a hash of its name, loaded as a tagged integer. Identity worked and nothing else did: `(symbolp :foo)` was false, `(integerp :foo)` was true, `symbol-name` and printing were broken, and — the expensive one — every keyword-argument validator that checked `symbolp` on an incoming `:test` or `:key` silently did the wrong thing.

Keywords are now real one-slot heap objects with their own subtag, distinct from symbols so that `keywordp` can answer without consulting a package, with a single intern table shared by the reader and the compiler. That last part matters more than it sounds: reading `:foo` from source and compiling a literal `:foo` now reach the same object, which is the property everything else depends on. This is the same family of fix as the closures and symbols in [№ 7](https://modus-lisp.github.io/issues/2026-04-25/) — a thing that had been represented as something cheaper becoming itself.

---

*modus / 2–4 May*

## CLOS learns some manners

The object system spent the week learning to fail properly rather than to fail. Reading an unbound slot signals a real condition carrying the slot name and the instance; an unknown slot dispatches through `slot-missing` instead of returning nonsense; a `defclass` with duplicate slots or duplicate options signals a program error; `ensure-generic-function` refuses to overwrite a function that is not a generic function. The nine standard method combinations are registered at boot rather than assembled on demand, `find-class` returns a proxy for the built-in types so they can be used where a class is expected, and `typep` recognises methods and generic functions as the types they are.

---

**Method.** Commits by *author* date, 2 to 8 May 2026, across the git repositories in the modus-lisp workspace — at this point, one. The pass and coverage figures are the ones the commits report, and are measured on bare-metal AArch64 under emulation, not on the hosted Linux build. Line counts exclude generated build artefacts. Produced with `bin/week 2026-05-09 --log`.

[← All issues](https://modus-lisp.github.io/issues/) · [crier](https://modus-lisp.github.io/)
