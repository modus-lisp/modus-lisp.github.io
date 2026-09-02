# Zero divergence

**This Week in Modus №15** · 13–19 June 2026 · 227 commits · 3 repos

<https://modus-lisp.github.io/issues/2026-06-20/>

For fourteen issues this has been the story of one repository. This week there are three: a from-scratch Bitcoin consensus engine that agrees exactly with Bitcoin Core’s own compiled library, the curve implementation underneath it, and Modus itself building a collector that can let foreign code hold a pointer into the heap.

| Commits | Repos moved | Source files | Lines added | Faster verify |
|---|---|---|---|---|
| 227 | 3 | 221 | +20k | 11.6× |

---

*cl-consensus / 16–19 June / 27 commits*

## Agreeing with Core, exactly

A Bitcoin consensus engine is the one kind of program where being *nearly* right is worse than being obviously wrong. A node that disagrees with the network about whether a script is valid does not fail loudly; it follows a different chain. So the interesting question for a from-scratch implementation is not “do the tests pass” but “does it agree, on every input, with the implementation everyone else is running”.

The answer this week is a differential harness that calls into Core’s own compiled library over a foreign interface and compares verdicts — first against `libbitcoinconsensus`, which found two bugs immediately, then upgraded to the newer `libbitcoinkernel` so the comparison stays current. On top of that, a fuzzer for taproot witnesses and a harness driving Core’s own script test vectors.

> Full BIP-342 tapscript — zero divergence against Core on the `script_assets` vectors.

> — cl-consensus — 1212345

Getting there means implementing the parts of Bitcoin that exist only because of history. Legacy signature hashing has to delete the signature from the script it is signing, and has to truncate the script at the last executed `OP_CODESEPARATOR` — two rules that make no sense in isolation and that consensus depends on exactly. Both landed this week, along with a running log of which specific blocks in the chain forced which consensus fix, which is the most useful document a project like this can keep.

The rest is throughput, because a node that is correct and too slow to sync is not a node: script verification fanned across cores in the style Core itself uses, structural checks parallelised across a batch, UTXO lookups prefetched, verification moved off the critical path of the download, block download pipelined behind a background fetcher, and the UTXO set moved onto a disk-backed store with a memory-mapped slot table and an atomic marker.

---

*secp256k1-fast / 17 June / 14 commits*

## 11.6×, from two ideas

The curve arithmetic split into its own repository and got two textbook optimisations, both worth exactly what the textbook says. Working in Jacobian coordinates avoids a modular inverse on every point addition — 7.5 times faster verification. Shamir’s trick computes the two scalar multiplications a signature check needs simultaneously rather than separately, for another 1.55. Together, 11.6 times faster than where the week started, and cl-consensus migrated onto the shared implementation the day after.

A benchmark landed alongside them that estimates initial block download time from the measured cost of a verification, which is the honest way to justify optimisation work: not “this is faster” but “this is the difference between syncing and not”.

---

*modus / 16–18 June / mcgc stages 1–4*

## A collector that lets go

A copying collector moves objects, which is what makes it fast and what makes it impossible to hand a pointer to anything outside the Lisp. Modus wants to do input and output, and eventually to call foreign code, so it needs somewhere an object can sit still.

The answer is a mostly-copying collector in the Bartlett style: the heap is divided into four-kilobyte pages, most objects are copied as usual, and a page that something is holding a pointer into is *pinned* — left where it is and treated as part of the new space. It went in over four staged commits, each gated: a metadata region, then a bitmap marking where objects start, then validation of conservative root candidates against that bitmap, then a page-pool allocator and the collector itself, and finally pinning.

Stage four is a four-bug chain, and the commits name each link: a register clobbered during lazy initialisation, an instruction encoded with the wrong extension bit while rebuilding the free list, a free-run allocator picking the last available run instead of the largest so survivors overflowed it, and pin counts addressed at the wrong scale. There is also a revert in the middle — back to the last verified-working collector body — and a *fifth* bug found afterwards that this dispatch has met before under another name: the gray-object scan walked from object starts and skipped a stale page tail, which the commit labels, accurately, as layout fragility.

---

*modus / 15–16 June*

## And the standard, still

Underneath the collector, the conformance grind continued at its usual pace and in its usual places. `type-of` was a stub returning NIL and is now implemented. `fboundp` and `fdefinition` learned that macros and special operators count. `eval-when` respects `:execute`-only at non-top-level. `macrolet` got a correct expander with `&whole` and `&environment`. Complex numbers reached the functions that had been ignoring them — `signum`, `cis`, `phase`, `eql`. The pretty printer got real logical blocks. `setf` places stopped being evaluated twice by the modify macros. And the generic function protocol gained the argument checking the standard requires: call arity, keyword validation, method-to-generic-function congruence, and the rule that `call-next-method` with different arguments must still apply to the same methods.

One diagnosis note is worth quoting because it corrects an earlier one: a cluster of `warn` failures had been attributed to a compiler closure-capture bug, and turned out to be runtime signal state left dirty between tests.

---

**Method.** Commits by *author* date, 13 to 19 June 2026, across the git repositories in the modus-lisp workspace — at this point, three. Speedups and divergence results are the ones the commits report. Line counts exclude generated build artefacts. Produced with `bin/week 2026-06-20 --log`.

[← All issues](https://modus-lisp.github.io/issues/) · [crier](https://modus-lisp.github.io/)
