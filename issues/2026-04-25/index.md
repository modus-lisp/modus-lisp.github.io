# Lost: none

**This Week in Modus №7** · 18–24 April 2026 · 82 commits · 1 repo

<https://modus-lisp.github.io/issues/2026-04-25/>

Last week’s reckoning left 17,587 tests unaccounted for. This week closes that to zero — and finds out why so many were crashing: closures and symbols had been the same shape, so calling one could call the other.

| Commits | Source files | Lines added | Tests lost |
|---|---|---|---|
| 82 | 21 | +4.5k | 0 |

---

*modus / 24 April / a65b0cf*

## Lost: none

Running each test in its own forked child stops one crash from taking nineteen files with it, but it does not solve the underlying problem: a child that dies from an uncatchable fault cannot report anything, and the parent only knows which test the child had been about to start. Across roughly twelve hundred chunks that still added up to 1,215 tests whose fate nobody knew.

The fix is to give parent and child somewhere to write that survives the child. A new trap emits an `mmap` call inline — anonymous, shared, read-write — and hands back a tagged address; writes to that page are visible on the other side of a fork. The parent allocates a four-kilobyte page at boot, the child stamps its verdict there before it does anything dangerous, and a child that dies still leaves behind what it knew.

> 1,215 lost tests, then none. Every test in the suite now produces a verdict — pass, fail, or crash — and nothing disappears silently.

> — modus — a65b0cf

The rest of the harness work is the same instinct applied in smaller places. A file’s outer handler records a failure rather than swallowing it. The per-file alarm is cancelled before the process exits, so a timeout cannot fire during shutdown. Nested `handler-case` gets a real per-fork handler stack, pushed on `setjmp` and cleared on the way out. A dedicated crash-failure path stops one bad test from cascading. And `write-object` is bounded, because printing an object that is not what you think it is should not be able to run forever.

---

*modus / 21–24 April / 398deb7, fd550a9, 9aff875*

## Two things wearing the same shape

With the harness no longer hiding its casualties, the reason for the casualties became visible, and it is a single design decision made early and never revisited: too many things were cons cells.

A closure was `(cons function-address environment)`. A Common Lisp symbol was also a cons cell. So `funcall`, which tested for a cons to decide whether it was looking at a closure, treated a symbol as one — took its `car`, which is a large integer, called it as a function address, and died.

> This was the root cause of the 974-lost-tests baseline. The signal handler caught them as failures, so they had been showing up as a wall of unrelated wrong answers.

> — modus — 398deb7

Closures moved to their own heap object with their own subtag, and `funcall` began checking a tag rather than a shape. Symbols followed three commits later, and together the two migrations were worth about 460 additional passes. Once they were separated, a null-check short-circuit in `car` and `cdr` that had been unsafe became safe.

The sequel, on Friday, is the same bug one level finer — and a good demonstration of why “give it a distinct tag” is only half an answer. Native symbols and Common Lisp symbols now shared a subtag but not a layout: one slot for the first, three for the second. A predicate that checked only the subtag said yes to both, and the code that trusted it read slot two of a one-slot object — landing in the next object’s data and returning whatever was there. In one file, 978 of 1,002 tests were crashing before they could even announce themselves. Teaching the predicate to check the layout as well as the tag was worth 970 passes on its own.

---

*modus / 18–21 April / 22ee9a9*

## Learning to complain

A large part of the ANSI suite checks that an implementation *rejects* things: call a function with too few arguments and you should get a program error, not a wrong answer. So the week put arity checking in — for too many arguments and then for too few, across the accessors (`first` through `caddr` and `rest`), the numeric macro wrappers, `ldb`, and the list functions — plus type-error signalling for the sequence builtins and an upfront type check on `member-if`.

Enabling it broadly crashed everything, because internal call sites had been quietly passing fewer arguments than their functions declared, and nothing had ever objected. Reading the resulting error log surfaced four real bugs. The best of them: the compiler’s generic `setf` expansion was dropping every place argument but the first, so `(setf (char s i) ch)` compiled to `(set-char s ch)` — storing the character at index NIL. Nobody had noticed, because nothing was checking.

---

*modus / 21 April / b2f6bbe*

## Three attempts, written down

The nicest commit of the week adds no code. Making `car` and `cdr` check their argument at runtime was tried three ways, and all three were reverted:

*modus — b2f6bbe, all three reverted*

```
gensym-let wrapping                       16,000 lost
inline null + consp + error, temp reg     16,000 lost
inline NULL CHECK ONLY, no consp, no error  245 lost, 122 regressions
```

The third is the interesting one, and the commit says so. Inserting a branch-and-label between evaluating the argument and reading the cell is semantically identical to the original for every value except NIL — and it still broke 245 test forks. That is not a correctness bug in the check; it is evidence that something about a label-branch in that position interacts badly with the compiler, which is a much more useful thing to know than a working `car`. A second commit went back and expanded the note after the second attempt, rather than deleting the record of the first.

The same week added `ansi-notes.md`, a handoff document: the fork, handler and alarm architecture, how tests were being lost, which diagnostic pipelines were tried and reverted, the specific mysteries still open, and what to do next. A project that is going to be picked up again by somebody without yesterday’s context — which describes almost everyone, eventually — needs the failures written down as carefully as the successes.

---

*modus / 18–19 April / ~40 commits*

## The keyword grind

Half the week is the least quotable work there is, and it is most of what standing up a Common Lisp actually consists of: making the sequence functions honour the arguments the standard says they take. `:test`, `:test-not`, `:key`, `:start`, `:end`, `:count` and `:from-end` threaded through `position`, `find`, `count`, `mismatch`, `fill`, `remove`, `delete`, `assoc`, `reduce` and the substitution family; result-type designators respected by `map`, `merge` and `concatenate`; `remove-duplicates` corrected to keep the *last* occurrence, which is the default the standard specifies and the opposite of what it had been doing; and the whole set made polymorphic over lists, strings and arrays, including presenting a string’s slots as characters rather than as the numbers underneath. LOOP got `into`, multiple accumulators, `initially`, `upto`, `maximize`, `minimize`, `of-type`, keyword clauses in any order, and numeric bounds that are floats or ratios without hanging.

---

**Method.** Commits by *author* date, 18 to 24 April 2026, across the git repositories in the modus-lisp workspace — at this point, one. Pass and loss counts are the ones the commits report. Line counts exclude generated build artefacts. Produced with `bin/week 2026-04-25 --log`.

[← All issues](https://modus-lisp.github.io/issues/) · [crier](https://modus-lisp.github.io/)
