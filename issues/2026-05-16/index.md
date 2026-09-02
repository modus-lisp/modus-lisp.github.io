# Every wedge root-caused

**This Week in Modus №10** · 9–15 May 2026 · 111 commits · 1 repo

<https://modus-lisp.github.io/issues/2026-05-16/>

Last week’s bare-metal harness bought its complete coverage with a promise: whole ranges of tests marked as failures in advance because they hung the machine. This week is that promise being paid, one bisection at a time, until there are none left.

| Commits | Source files | Passing, AArch64 | Pre-stamps left |
|---|---|---|---|
| 111 | 24 | 1,602→5,529 | 0 |

---

*modus / 10–14 May / ~40 commits*

## Taking the band-aids off

A pre-stamp is an admission. When a test wedged the machine, the fast fix was to mark the whole surrounding range as failed before the run started — which keeps the coverage honest, in that nothing goes missing, while quietly conceding that most of those tests were never tried. At the start of this week 85% of the suite was covered by such ranges: 15,110 of 17,692 tests marked failed in advance, and 1,602 passing.

The week is the systematic dismantling of that. Each umbrella range is bisected — build, run, split, repeat — until the actual wedging tests are isolated to a handful of identifiers and everything around them is released to run on its own merits. The commit subjects are the ledger, and they are relentless:

*modus — the pass count, commit by commit*

```
bisect speculative pre-stamps        P = 1,624 → 2,183   suite completes for the first time
remove 3 clean pre-stamp ranges      P = 2,183 → 2,586
remove 9 more clean ranges           P = 2,586 → 2,672
17072-18639 has only one wedge       P = 2,672 → 3,216
bisect the vector/array umbrella     +588
unstamp 4 big umbrellas              P = 4,612 → 4,964
sub-split the number-comparison tail +387
sub-split sequence + numcomp         P = 5,923 → 6,136
```

The pattern that repeats is worth naming: an umbrella covering 1,242 tests turns out to contain five genuine wedges, four of which were already known individually. The umbrella was never the diagnosis; it was the absence of one. And the discipline holds in both directions — three separate attempts to remove a stamp were reverted the same day, because the code shift from the change wedged something else.

On Thursday the last of them goes. Four fixes landing together — a trap for calling NIL as a function, a garbage collector that was scanning from the wrong place in its own trampoline frame, function-address bounds initialised so `functionp` can classify correctly on this architecture, and a `&key` supplied-p variable that was left unbound — let the whole suite run end to end with no bandage anywhere.

> After: P = 5,529, **zero** pre-stamps. Every FAIL record is now a real failure observed when the test actually ran, not a wedge papered over in advance.

> — modus — 02f3def

The remaining 12,163 failures are things that genuinely do not work yet, which is a much better problem to have than 12,163 things nobody has looked at.

---

*modus / 15 May / TAG-PLAN.md*

## Giving functions a tag

[№ 8](https://modus-lisp.github.io/issues/2026-05-02/) found that `functionp` could not tell a function from an integer because a raw function address *is* an integer. The structural answer is to stop storing raw addresses: tag them, the way every other value in the system is tagged. Function entries are sixteen-byte aligned, so the low bits are free — set them when the address is produced, subtract them before the indirect call, and `functionp` becomes a mask and a compare with no memory access at all.

It landed on Friday, was reverted the same day, had its findings written into a plan document, and landed again a few commits later. The reason for the round trip is the interesting part. Tagging worked — the dominant crash signature disappeared — but it exposed a latent bug underneath: something in the CLOS instance-allocation path was calling NIL as a function. Before the change, that fault landed on a mapped page and the crash handler recovered cleanly; after it, subtracting the tag moved the address to the end of an unmapped page and recovery stopped being reliable. So the second attempt shipped with an explicit guard that turns calling NIL into a proper undefined-function error before it can fault at all.

A latent bug made visible by a correct change, fixed rather than reverted around: that is the same move as [№ 7](https://modus-lisp.github.io/issues/2026-04-25/)’s arity check, which crashed everything until the call sites it was catching got fixed.

---

*modus / 11–15 May*

## AArch64 catches up, and the memory map gets safer

Much of the week’s remaining work is the second architecture being brought level with the first, feature by feature: the object layout migrated to match x64, closure-environment operations implemented, the callee-saved environment register preserved per the platform’s calling convention, tag-safe subtag reads and a tag-aware indirect call that traps on a tagged value rather than jumping into it, the collector reached through a native trampoline, and the handler stack wired through `setjmp`, `longjmp`, the boot exception vector and the deadline interrupt in five staged commits, each landing as dead code before anything called it.

The memory map got several long-overdue corrections at the same time. The stack moved out of the kernel image, with a build-time assertion that the two cannot overlap and a guard page below it so that overflowing the stack faults instead of quietly eating the image. Page tables and the interrupt descriptor table moved below the kernel load address, so growing the image can no longer land on them. And the image’s own pages are marked read-only. None of that is visible in a pass count; all of it is the difference between a crash that points at its cause and a crash that does not.

---

**Method.** Commits by *author* date, 9 to 15 May 2026, across the git repositories in the modus-lisp workspace — at this point, one. Pass counts are the ones the commits report, measured on the bare-metal AArch64 build under emulation. Line counts exclude generated build artefacts. Produced with `bin/week 2026-05-16 --log`.

[← All issues](https://modus-lisp.github.io/issues/) · [crier](https://modus-lisp.github.io/)
