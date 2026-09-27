# The last two roots

**This Week in Modus №29** · 19–27 September 2026 · 300 commits · 15 repos

<https://modus-lisp.github.io/issues/2026-09-26/>

Somebody audited what the agent actually loads, and found C entering a fifty-two-system Common Lisp program through exactly two dependencies. The first went this week, taking twenty-four systems with it. The second is the database, so SQLite was written: pager, b-trees, query planner, window functions, full-text search, R-trees, in two days, checked against version 3.40 down to which of two equal rows a `UNION` keeps. Elsewhere, five architectures turned out to be passing a test that could not fail.

| Commits | Repositories | New this week | Systems loaded | Architectures, checked |
|---|---|---|---|---|
| 300 | 15 | 3 | 52→28 | 8 |

---

*operandi, quill, sqlite-pure / 64 commits*

## An audit, and what it cost

operandi is this workspace’s agent loop — the thing that reads files, runs commands, calls a model and iterates. It is ordinary Common Lisp, and like any ordinary Common Lisp program it reaches for Quicklisp when it wants HTTP or a database. That is fine everywhere except the one place this project is aimed at, where there is no C library to bind to and no dynamic linker to bind it with.

So somebody read the load closure. The finding is stated exactly:

> Toward running operandi on an implementation with no FFI. Auditing the load closure found CFFI entering through exactly two roots, and dexador was the big one: cl+ssl (OpenSSL), iolib, and static-vectors, pulling in over thirty of the fifty-two systems operandi loaded — for a client that makes JSON POSTs and reads one server-sent-event stream.

> — operandi — 9e6180c, *http: drop dexador for seal*

Thirty of fifty-two systems, and an OpenSSL binding, to POST some JSON. They are replaced by seal, the workspace’s own TLS and HTTP client, on natrium’s crypto — pure Lisp end to end. The commit is careful about the contract it has to keep: a non-2xx must still signal with the body intact, because a provider’s refusal is only legible from the body, and losing it turns every refusal into *“[empty response from model]”*. That is the sort of detail that decides whether a replacement is real.

A third C path went the same day, by a different route. The prompt’s line editor, linedit, pulled in osicat — a C groveller — and needed a 245-line extension for multi-line input besides. It is replaced by [**quill**](https://github.com/modus-lisp/quill), which did not exist on Friday: Emacs keys, multi-line input, persistent history with reverse search, bracketed paste, completion, paren matching. No FFI and no dependencies; the only per-implementation file is `port.lisp`, for raw mode and timed reads. It gets the terminal’s width from a cursor-position report rather than an `ioctl`, because an `ioctl` is a syscall and a syscall is a thing a bare-metal Lisp may not have. Fifty-seven checks over the pure layers, thirteen more under a real pty, and the history file format is unchanged so an existing `~/.operandi/history` carries over.

What the audit leaves, stated as a number in the commit that did the work:

*operandi — 9e6180c*

```
Closure: 52 systems -> 28.  CFFI now enters ONLY through sqlite.
```

---

*sqlite-pure / 40 commits / 26–27 September*

## And then the database

One root left, and it is the awkward one. cl-sqlite binds libsqlite3, and the agent has kept its audit log in SQLite since it was written. There is no pure-Lisp SQLite to swap in.

There is now. [**sqlite-pure**](https://github.com/modus-lisp/sqlite-pure) was created on Saturday. By Sunday evening it had forty commits, and this is what they contain:

*sqlite-pure — two days*

```
Saturday   pager, b-trees, records, SQL parser and executor; joins, aggregates,
           compound selects, CTEs, views, constraints, indexes
           triggers, date/time, JSON1, window functions with RANGE/GROUPS frames
           SAVEPOINT, POSIX locking, TEMP tables, ATTACH/DETACH, foreign keys
           RIGHT and FULL OUTER JOIN, UPDATE...FROM, VACUUM, generated columns
           STRICT tables, math functions, auto-vacuum, WAL writes
Sunday     R-tree virtual tables, EXPLAIN QUERY PLAN, FTS3, FTS4, FTS5
           four tokenizers, bm25/highlight/snippet, dbstat, Geopoly
```

The claim that matters is not the feature list, though. It is this, from the README:

> A database written here opens in the `sqlite3` shell and passes its `PRAGMA integrity_check`; a database written by SQLite opens here; and the two can share one file at the same time, using SQLite’s own locking protocol.

> — sqlite-pure — README.md

Sharing a live WAL database with a real SQLite connection means implementing the `-shm` wal-index protocol, which is not a file format so much as an agreement about shared memory. And the differential suite goes further than format compatibility into behaviour nobody documents: type affinity, collation, error messages in SQLite’s own wording, *which of two equal rows a `UNION` keeps*, and which index the planner picks — because that decides the order `group_concat` sees.

The float work is the best of it. SQLite’s decimal-to-double parser and its `printf` run in x87 long double and are not correctly rounded. sqlite-pure reproduces that, bit for bit, wrongness included — and then measures the residue: about one literal in six thousand lands one unit in the last place away. A compatible implementation is one that is wrong in the same places, and this one knows how often it isn’t.

Four repositories now exist because a C dependency had to go: seal for TLS, natrium for the ciphers under it, quill for the prompt, sqlite-pure for the log. That is the shape of this whole workspace in miniature — fifty repositories, most of them a thing somebody else would have linked.

---

*modus / 23 September*

## A test that could not fail

Modus has claimed nine architectures since [№ 2](https://modus-lisp.github.io/issues/2026-02-28/). This week somebody checked, and the README was corrected in a commit whose subject is the most useful four words in it: *wrong in BOTH directions*.

> The tiers I wrote understated four architectures and overstated three, and the fixpoint section named a chain that does not exist. … Overstated: “Supported” claimed x86-64, AArch64 and i386 “accept SSH”. SSH is unproven on EVERY target — the gate marks all six ssh cells CANNOT PROVE.

> — modus — 80422a6b

The understatement and the overstatement had the same cause, and it is the one worth remembering:

> Five of the nine emitted a binary that nothing ever observed. “Runs in QEMU” was measured by the image being produced, not by anything it did — so the back ends had accumulated real codegen bugs behind a check that could not fail.

> — modus — 5b1fbe6c

The replacement harness is deliberately crude: the payload writes its answer to a fixed physical address and spins, and QEMU’s monitor reads the memory back. No serial driver, no interrupt controller, nothing per-architecture that could itself be the bug. Eight architectures then pass a ten-rung ladder — call, add, multiply, branch, argument, recursion, factorial, cons, loop — *with the answer checked*: x86-64, AArch64, i386, ARM32, RISC-V 64, PPC64, PPC32, 68k.

What had been hiding behind the check that could not fail: no entry jump emitted at all for RISC-V, PPC or 68k, so the boot stub fell through into whichever function came first; three translators returning no function map, so the linker *guessed* each function’s offset and the entry jump landed mid-prologue; RISC-V’s 64-bit constant loader using `t0` as scratch while its caller passed `t0` as the destination; 68k using D0 and D1 as both scratch and argument registers. Real bugs, all of them, in code that had been reported as working for seven months.

The same discipline shows up twice more in the week. A library ladder — twenty-two real Common Lisp libraries, run on x64 and i386 from one tree — found six shared runtime bugs, the best of them that a keyword-headed form was being treated as a macro call, so `(:metaclass x)` ran a user macro named `METACLASS`; cl-annot defines exactly that, and the expansion recursed until the stack died. And the ladder’s own positive control turned out to be broken in the way controls fail:

> A BUILD FAILURE SATISFIED THE POSITIVE CONTROL, and that is the exact failure mode the control exists to prevent, wearing the control’s own clothes.

> — modus — c3477698

A broken `chmod` in three build scripts had made every target fail to build, and the gate printed *“positive control failed as required — the gate can fail”* and then reported fourteen failures. The control now demands a wrong *answer*, not a non-zero exit.

---

*modus / 24–27 September*

## The same image, from three compilers

[№ 27](https://modus-lisp.github.io/issues/2026-09-12/) reported that three host Lisps produce byte-identical output once Modus is doing the compiling. This week that argument gets its strongest form yet: `modus-sh --compile-uefi` builds the bare-metal UEFI Common Lisp image from inside Modus, and it is compared against the same image built by SBCL.

*modus — 6d311fc9, test/run-uefi-ddc.sh*

```
SBCL static build          35,951,104 bytes   md5 76ec6cfb...
modus-sh --compile-uefi    35,951,104 bytes   md5 76ec6cfb...
                           reproducible run to run; boots under OVMF and evaluates
                           holds across SBCL, CCL and ABCL hosts
```

Diverse double compilation is Thompson’s answer to a compiler that lies about its own source, and it only works if the two compilers are genuinely independent. A trusted host and the self-hosted target agreeing to the byte on a thirty-six-megabyte bootable image is about as concrete as that argument gets.

Getting there found two bugs that had nothing to do with the comparison and everything to do with correctness. The better one: `compile-quote` stored vector-literal elements with an instruction whose slot index is an eight-bit immediate, so element 256 landed on slot zero — and *any `#(...)` literal longer than 255 elements was wrong in every image Modus had ever built*. It was invisible because the one such literal in the boot path, a 347-byte stub blob, had been baked into `modus-sh` itself and was being emitted wrong consistently. Two compilers agreeing is how you notice that both were quietly wrong in the same way.

Alongside it the Fixpoint of Theseus passes again — Gen0 to Gen3, with SHA(Gen1) equal to SHA(Gen3) — and the ANSI runner grows to contain every test the corpus defines, 1,905 more than before, which moved the denominator everything else is measured against.

---

*cl-deposits / 24 commits / 26–27 September*

## Attacking your own protocol

cl-deposits is a clean-room implementation of the Bitcoin Deposits protocol — operator-run ledgers published as quorum-cosigned hash chains over Nostr, with Taproot reserves validated by cl-consensus. It is the sibling of [№ 26](https://modus-lisp.github.io/issues/2026-09-05/)’s Lightning node, which will be its Lightning rail. It has been here since the 5th; the ledger is counted from this week because this week it was cloned.

What it did this weekend is unusual enough to report on its own: it wrote down nine claims the specification makes — strict-majority cosign, equivocation, provable censorship, inactivity, co-sign refusal, fraud-proof forgery, lottery fairness, transport, the Tier 0 limit — and then built the attack that tests each one. The first is a harness in which an operator asks its quorum to cosign a credit that is not backed by reserves, with an honest arm where every cosigner refuses and a colluding arm where two cosign blind. The findings are numbered to seven and every one is marked *organic*, meaning the soak turned it up before anyone went looking. One of them is the kind of sentence a protocol implementation should be frightened of — *the custody lottery’s N is decided after it is committed to* — and it is followed two commits later by the mitigation and a recovery sweep. Deciding a lottery’s parameter after committing to the lottery is not a bug in the code; it is a bug in the design, found by the code.

---

*the rest / weft, operandi, loom, reel*

## Also this week

**weft**, the layout engine, gives scripts the measurement surface a real page expects: `getBoundingClientRect` read from the layout tree, the `offset*` and `client*` metrics, `offsetParent`, a scrolling area, a window that knows its own size, `IntersectionObserver` and `ResizeObserver`. And the Acid3 gate is made *able to fail* — the same lesson as the architecture ladder, arrived at in a different repository in the same week.

**operandi**, besides shedding C: a worker tier that puts the orchestrator and the swarm on different models, typed verdicts so an investigation returns claims rather than essays, a findings ledger so a settled claim outlives the run that settled it, and a question phase where the harness decides whether to delegate. Also the small mercies — images pasted from the clipboard, multi-line history that survives a restart, and a backgrounded grandchild that no longer wedges the agent forever.

---

*the gates / 27 September*

## Still one line

Three weeks ago VP8 moved into reel and three workflows went red because their sibling-checkout lists were not told. Nobody has told them yet. webp-pure, webrtc-media and warren have now been red for nineteen days on `Component :REEL not found`, and loom and weft fail the same way. **51 repositories, 35 green, 8 failing, 8 with no CI.**

Of the failures, five are that one line, one is operandi’s gate reverted a month ago, one is kiln’s fast test tripping a reader error, and the newest is cl-deposits — nineteen minutes old at the time of writing, which is at least a gate doing its job. Of the eight with nothing watching them, two arrived yesterday: sqlite-pure, whose differential suite against SQLite 3.40 is the best-tested thing in this issue and runs nowhere automatically, and quill, which has seventy checks and the same problem.

---

**Method.** Commits by *author* date, 19 to 27 September 2026, across the 50 repositories cloned from the modus-lisp organisation, counting each repository’s default branch as the union of the local branch and `origin/`. Produced with `bin/week 2026-09-26 --until 2026-09-28 --log`.

**A nine-day week.** This issue covers nine days rather than seven, at the author’s request, because three repositories were created on the 26th and holding them for a fortnight would have made two issues wrong instead of one. `bin/week` now takes `--since` and `--until` so that a span can be moved without moving the issue’s date, and prints `[SPAN OVERRIDDEN]` when it has been. The next issue starts on 28 September: `bin/week 2026-10-03 --since 2026-09-28`.

**Line counts.** sqlite-pure’s 35,869 added lines include its differential test corpus and character tables extracted from SQLite; cl-deposits’ figure covers only the 24 commits inside this window, not the 39 that preceded it.

[← All issues](https://modus-lisp.github.io/issues/) · [crier](https://modus-lisp.github.io/)
