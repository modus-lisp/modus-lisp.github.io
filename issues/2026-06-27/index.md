# A node, and something to keep it in

**This Week in Modus №16** · 20–26 June 2026 · 124 commits · 4 repos

<https://modus-lisp.github.io/issues/2026-06-27/>

The consensus engine became a whole node — reorgs, peer discovery, relay, a mempool with real policy, and a wallet that can spend to taproot. A new repository appeared holding a copy-on-write B-tree, and withnine hours later it was the node’s UTXO store. This is the first week the workspace behaves like a workspace.

| Commits | Repos moved | Source files | Lines added | Faster sorted writes |
|---|---|---|---|---|
| 124 | 4 | 89 | +18k | 7–20× |

---

*pagetree cl-consensus / 21–25 June*

## Four days from scaffold to production

On Sunday a repository appears containing a design and a skeleton for a copy-on-write B+tree key-value store in pure Common Lisp. On Monday it is a working store with commit-atomic free-page reclamation. On Tuesday it gets a compact overflow encoding, a page cache with a clock eviction policy, allocation-free lookups — about thirteen times faster reads — and batched sorted writes, seven to twenty times faster than applying them one at a time. On Wednesday, a bottom-up bulk loader that builds a tree in linear time from sorted input.

And nine hours after that first commit — late Sunday night to Monday morning — cl-consensus put its UTXO set behind an interface with two implementations and made pagetree one of them — digest-equivalent to the existing store, checked against a real block download rather than a unit test, with a migration tool for converting an existing database and a streaming build path for deep backlogs.

That is the first time in this dispatch that a library has been written for one project and consumed by another overnight, and it is the pattern that will define the workspace from here. A B+tree is a general thing. A UTXO set is a specific thing that needs one. Keeping them apart means the tree can be used again, and means its correctness is somebody’s job rather than a footnote in a node.

---

*cl-consensus / 20–26 June / 41 commits*

## From follower to citizen

Last week’s engine could validate the chain. This week it joins the network. The progression is explicit in the commit subjects, phase by phase: an inbound listener that serves headers, then block storage and serving blocks on request, then announcing new blocks to inbound peers, then accepting and relaying transactions with an orphan pool. Peer discovery over the address protocol with DNS seeds and an address manager, and a download that pulls from several discovered public nodes at once and re-requests whatever stalls.

Reorganisation — the part that separates a node from a downloader — landed as a persistent undo store wired into the live download path, with best-chain activation proven equivalent to the existing logic, the store bounded so it prunes below the reorg window, and a regression gate that mines a real reorg on a private network rather than simulating one.

The mempool grew the policy that makes it a mempool rather than a list: a fee floor, dust rules, replace-by-fee, ancestor and descendant links, eviction under pressure, expiry, and persistence across restarts. And then a wallet, in three phases — encodings and hierarchical key derivation, address tracking and balances, and building, signing and handing back a transaction ready to broadcast — extended the same week to taproot key-path spends, multi-leaf script trees, mnemonic generation and an encrypted seed at rest.

The week ends in release preparation: a licence, a changelog, an inventory of what has actually been verified and how, environment-specific defaults scrubbed out of the source, a single entry point that runs the node, an offline gate suite, and a fuzzer for the parsers with bounded allocation on untrusted input. Which is the difference between code that works on the machine it was written on and software.

---

*modus / 24–26 June / N1 to N5*

## Floats that know what they are

Modus spent the week on the numeric tower, in five numbered stages, and the theme is type identity. A float had been a float; the standard has four of them, and code can ask which one it is holding, print it with the right exponent marker, and rely on an operation on a single-float returning a single-float rather than quietly widening.

So: distinct subtags for the float types, `typep` and `type-of` and `subtypep` answering correctly about them, the reader producing genuine single-floats, typed literals, per-type exponent markers in the printer, and format contagion preserved through arithmetic, transcendentals and coercion. Then the fixed-format float directive, with digit reduction under a width constraint and precision taken from the value’s own type. Then `logbitp` at huge indices, `1+` and `1-` going through checked arithmetic so they promote on overflow instead of wrapping, and complex numbers completed — square roots and logarithms of negatives returning complex results, with the contagion rules that go with them.

One commit in the middle is a good example of what this stage of a project costs: a single-float’s new subtag collided with the one already used for compiled modules, and had to move. And the bignum work has a cluster of its own — shifts corrupting values above 124 bits, bit operations routed to the general engine when either side is a bignum, and a documented compiler limitation where an inline shift can corrupt a bignum pointer.

> Crash-triage guidance: default away from “GC fragility”.

> — modus — 2da37ee

That is a note to the project’s future self, and it is the right lesson from the last two months. When something crashed, the first hypothesis had been the garbage collector or the code layout, and it was almost always something ordinary and findable instead — a docstring, a timeout, a keyword parser, a subtag collision. Writing the correction down where the next session will read it is the cheapest fix in this entire backfill.

---

**Method.** Commits by *author* date, 20 to 26 June 2026, across the git repositories in the modus-lisp workspace — at this point, four. Speedups are the ones the commits report. Line counts exclude generated build artefacts. Produced with `bin/week 2026-06-27 --log`.

[← All issues](https://modus-lisp.github.io/issues/) · [crier](https://modus-lisp.github.io/)
