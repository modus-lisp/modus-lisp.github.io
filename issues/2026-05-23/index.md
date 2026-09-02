# An object system, and a number system

**This Week in Modus №11** · 16–22 May 2026 · 87 commits · 1 repo

<https://modus-lisp.github.io/issues/2026-05-23/>

Two large pieces of Common Lisp that had been standing in as stubs became real in the same week: the object protocol, where the hooks the standard requires had been no-op functions, and the numeric tower, where the transcendental functions had been returning 0 or 1 regardless of what you asked them.

| Commits | Source files | Lines added | CLOS smoke tests |
|---|---|---|---|
| 87 | 23 | +6.8k | 54/54 |

---

*modus / 18–19 May / 0b0d6c8*

## The protocol, not the appearance

CLOS has been in Modus since [№ 6](https://modus-lisp.github.io/issues/2026-04-18/), in the sense that `defclass` and `defmethod` worked and dispatch happened. What was missing is the part that makes it a protocol rather than a feature: the standard specifies that `initialize-instance`, `print-object`, `no-applicable-method`, `slot-missing` and their relatives are *generic functions*, so that your own methods on them participate in how the system behaves. Modus had them as ordinary functions doing nothing much.

Each one now has three parts: a default holding the system-supplied behaviour, a dispatcher that consults the generic function registry for user methods and falls back to that default, and the user-facing name. They are registered at boot as real generic functions with methods on `standard-object` or `t`, and the dispatch closures are mirrored into a reverse lookup so that `#'print-object` finds the generic function it names — which is what `compute-applicable-methods` and `find-method` need in order to answer questions about it.

On top of that: `make-instance` routed through the initialization protocol rather than around it, `reinitialize-instance` actually re-initializing, `make-load-form` as a real generic function with default methods that signal, metaobject accessors for classes and methods, `with-slots` and `with-accessors` that can be written through, and `class-of` always returning a class object rather than sometimes a name.

The verification is a smoke test that grows as it goes — 28 checks, then 42 with `:around` methods, diamond inheritance and method combination, then 46 with `next-method-p` and the slot-access hooks, then 54 with `eql` specializers and `remove-method`. A hand-written end-to-end test alongside a conformance suite is not redundancy: the suite tells you what fraction of the standard works, and the smoke test tells you whether the thing works *at all* before you go looking.

---

*modus / 19 May / 22d125c, c530448*

## Numbers that mean what they say

IEEE floating point went in behind a per-architecture capability, which is the right shape for a system that targets nine machines: each target declares its float support as native, software or none, x86-64 declares native and its translator emits SSE2 instructions, and the compiler asks before emitting a fast path that a given target could not execute. Three phases — the instruction set and the translator, then the runtime arithmetic wiring, then the transcendentals and mixed-type comparison.

Complex numbers became real objects, with `#C(r i)` in the reader building one, arithmetic that contaminates correctly, `abs` computing a magnitude, and `typep` able to tell an IEEE float from an integer without tripping over bignums.

And then the transcendental functions, which is the commit worth reading twice. They had been stubs returning 0 or 1 whatever you passed them. They are now computed as *rationals*:

> Sine and cosine as ten-term Taylor series with the angle reduced into (−π, π]; tangent as their quotient; exponential by Taylor with halve-and-square reduction for large arguments; logarithm by Newton iteration on exp(*y*) − *x*. All in scaled integer arithmetic.

> — modus — c530448

That is how you get transcendental functions on a machine whose floating-point support you have not written yet, and on the six architectures where you may never write it. The exactness is not free — scaled rationals are slower and have their own precision story — but it is portable, and portability is the whole reason the virtual instruction set exists.

---

*modus / 22 May / aba4a28*

## Not layout, after all

Enabling a `&key` transformation for anonymous functions, rather than only for named ones, cost ninety tests. After the last two issues, the obvious suspect was layout — and the investigation says, explicitly, that it is not. It is a deterministic code-generation bug, reproduced by bisection and by running without forking so the debugger can watch.

The broken case is small enough to hold in your head: `(lambda (&key (a b) b) …)` written inside a `let` that binds `b`. Key `a`’s default refers to the outer `b`. But the transformation expands into a `let*` that also binds a `b`, later, and the free-variable analysis treats that later binding as covering the earlier reference — so the outer `b` is never captured, and the default reads whatever happens to be there.

Naming a regression as a real bug rather than as fragility is worth as much as the fix. [№ 9](https://modus-lisp.github.io/issues/2026-05-09/)’s docstring made everything look layout-shaped for a fortnight; the discipline afterwards is to keep asking, and to be willing to answer no. By the end of the week both the anonymous and the top-level `&key` paths were doing real keyword extraction, and immediately-applied capturing lambdas — whose closure environment was simply never being loaded — worked for the first time.

---

*modus / 18 May / cf6436c*

## One character of difference

The smallest bug of the week is the one most worth telling. `string=` was implemented by calling `string-equal`. Those are two different functions: `string-equal` ignores case, and `string=` must not. So `(string= "A" "a")` was true — and because `equal` on strings had just been correctly routed through `string=`, it inherited the same wrong answer. A one-word confusion at the bottom of a comparison chain, quietly making the entire implementation case-blind about strings.

The rest of the week is the ordinary grind, and it moved the hosted Linux build from 12,110 to 12,972 passing: `symbol-macrolet` for real, pathname matching and translation, `apropos` walking both symbol tables, a directory function, documentation strings, array constants, stricter compound type specifiers, and keyword-plist validation extended across the sequence and set operations — with leftmost-wins applied yet again, this time to `:allow-other-keys` itself.

---

**Method.** Commits by *author* date, 16 to 22 May 2026, across the git repositories in the modus-lisp workspace — at this point, one. The pass counts in this issue are from the hosted Linux x64 build and are not comparable with [№ 10](https://modus-lisp.github.io/issues/2026-05-16/)’s, which are bare-metal AArch64. Line counts exclude generated build artefacts. Produced with `bin/week 2026-05-23 --log`.

[← All issues](https://modus-lisp.github.io/issues/) · [crier](https://modus-lisp.github.io/)
