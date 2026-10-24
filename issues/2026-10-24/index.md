# Every last test

**This Week in Modus №33** · 17–24 October 2026 · 278 commits · 33 repos

<https://modus-lisp.github.io/issues/2026-10-24/>

The first ANSI test ran on 9 April: 191 cases, written by hand. At 20:46 on Saturday the x86-64 build passed every test in the unmodified upstream suite — 21,681 of 21,681. How long that took depends on where you start counting: 198 days from the first test, thirty from the first run of the real suite, or five for the last thousand. This issue counts all three. The same week, a graph layout algorithm was ported to Lisp in five days, and the secret keys of nine repositories moved somewhere the garbage collector cannot copy them.

| Commits | Repositories | New this week | of 21,681 ANSI | Green, of 62 |
|---|---|---|---|---|
| 278 | 33 | 3 | 21,681 | 43 |

---

*modus / d01929b9 / 24 October*

## 21,681 of 21,681

> Upstream ansi-test: **21681 of 21681** (STRUCT-TEST-65/5/9/20, 0 lost).

> — modus — d01929b9, 24 October, 20:46 UTC

The suite is Paul Dietz’s ANSI Common Lisp conformance tests, as maintained at common-lisp.net, pinned at one commit and loaded unmodified. Since Friday it is also the merge gate: `scripts/acceptance-gate.sh` runs it on the plain command-line build at both the base and the candidate, compares the sets of passing tests by name, and re-runs anything lost on both binaries alone before it counts. Every commit in the countdown below reports *0 lost* against that check.

The last four were all the same test in four shapes: a structure constructor called with more arguments than `CALL-ARGUMENTS-LIMIT`, which was 120. The fix makes the limit `MOST-POSITIVE-FIXNUM`: past 120, the rest travel as a list. Finding it turned up something worse on the other back ends. The overflow-argument copy stopped at 32 words on i386, RISC-V, PowerPC, 68k and arm32, so any `&rest` call of 33 to 120 arguments there built its list from slots nothing had copied — silently, inside any compiled function.

So, how long. The honest answer has three parts, because the thing being measured changed twice.

The April “100%” is the instructive row. It was true of the runner, which compiled a corpus into the image, and the corpus was not the suite: the September census found test macros never expanded, whole files in no chapter list, and generators that ran at load time and were never run at all. Moving to the upstream suite as it ships, driven by its own harness, is what made the number mean what it says. This week the baked runners were retired, and with them `real-ansi-gen.lisp`, a 13 MB generated dump of 187,675 lines that had been tracked at the root.

One stretch of the curve is not progress, and the log says so. On the 22nd an `IGNORE-ERRORS` that did not catch, in a new literal-object scan, made `COMPILE-FILE` abort on any file containing `#()`. The suite’s `ansi-aux.lsp` is one, and *the upstream sweep lost 1,100 tests* until it was fixed, nineteen minutes later. The gate also gained a rule: every image must still build, because *the 32-bit CLIs were broken for three days without any gate noticing*.

This is the x86-64 build. The rest of modus’s 125 commits went wider rather than deeper. On Saturday a bare i386 image booted to its REPL with a collector, then gained NE2000 networking, DHCP, TCP, a byte-exact HTTP fetch and SSH. A bare arm32 image reached its REPL under QEMU, and SSH on the Pi’s CL image now runs as isolated threads, with the network domain owning the NIC. Underneath all of it, large hash tables became compact dicts keyed by address, and CLOS instances keep their slots in their own vector.

---

*trellis / 35 commits / 17–22 October*

## A layout engine, ported

Last week the front page gained a dependency graph, laid out by Eclipse ELK’s layered algorithm running on the JVM, the one piece of the site that needed Java. trellis is that algorithm in Common Lisp. Its first commit is dated the 17th, and by the 22nd it was 1.0.0. It is a port rather than a clean-room design: ELK’s class structure and option names carry over, it reads and writes ELK’s JSON so input meant for elkjs works unchanged, and ELK’s own layered test suite is ported alongside it.

> Most of it passes. One check fails: `PartitionPostProcessorTest.testPartitionOrder` on `withEdgeLabels.elkt`, which ELK’s own build fails too.

> — trellis — README, Status

Of ELK’s algorithms, only `layered` is ported. Spline routing runs, but its geometry has not been checked against ELK’s reference output. trellis adds one option of its own, `trellis.candidates`, which lays a graph out with several random seeds and keeps the one with the fewest crossings. On Thursday the site switched to it, and the Gradle build went. On the front-page graph the commit records 6 crossings, against 14 from Java ELK with the settings it had used. That compares eight seeds with one, so it is a better result, not evidence of a better engine. The author’s note on the day: *agents need pretty diagrams too*.

---

*natrium, secp256k1-fast, and seven more / 23 October*

## Keys the collector never moves

A copying garbage collector is good for most data and bad for secrets. Each collection can leave the old copy of a key behind in from-space, where nothing wipes it. On the 23rd natrium gained **gossamer**, which makes one rule checkable: a secret lives only in fixed memory, and every function that takes one says so with `CHECK-FIXED`, so a key copied onto the moving heap is an error at the first boundary it crosses.

On SBCL the fixed memory is static vectors. On modus it is a secret arena: private anonymous mappings, `mlock`’d, marked `MADV_DONTDUMP` and `MADV_WIPEONFORK`, which no modus collector scans or moves. Bare metal has no backend yet, and `CHECK-FIXED` refuses there rather than pretend. secp256k1-fast made private keys fixed 32-byte buffers with every secret operation constant-time. It also checks for residue: a test scans the heap before and after signing, and signing must add no key-shaped object.

The same day the change went through everything that holds a key: cl-nostr, beacon, cl-consensus’s wallet, cl-payments, cl-deposits’ node and wallet, and operandi’s agent key, whose file text is now overwritten once it is in fixed memory. skep dropped its vendored curve for secp256k1-fast.

---

*witness, wax, operandi / 23 October*

## Who says so

Last issue ended with operandi refusing a route that was encrypted but not attested. Attestation answers what a machine is running. It does not answer whether that image sends your prompt only where it claims. Those are separate questions, and on the 23rd they got a repository of their own.

> When you are about to encrypt to an enclave, you need to know two things. First, whether the key really belongs to a machine running code you trust. Second, who says so.

> — witness — README

**witness** reads signed statements on Nostr — *attested*, *reproduced*, *routed*, *endorsed*, *revoked* — and weighs them by a web of trust you already have: whom you follow, to a depth you choose. operandi uses it at once. Maple’s route table changes with its releases every few days, so operandi no longer keeps one. Under `--confidential`, `--maple MODEL` assumes the cleartext can go anywhere Maple can send it, unless a *routed* statement from your web of trust covers that model on the image the enclave attests to *now*. If Maple deploys a new image mid-session, it is checked again before anything is sent. Statements are read locally for now; nothing is fetched from relays yet.

**wax**, from the same evening, is the other end: a signer. One BIP-39 seed, a key per role on a hardened path, and nothing signed without a policy — `:deny` by default, `:approve` to ask a human, `:auto` for named callers. It has no network client, and no parser for anything it did not define, so that the code holding your seed stays small enough to audit.

---

*the rest / cl-nostr, cl-deposits, kiln, json-simple, seal*

## Also this week

**cl-nostr** and **beacon** speak UDP: one signed event per datagram, optionally sealed with NIP-44. The author put it more briefly: *cl-nostr and beacon (relay) now support udp*. **cl-deposits** gets a fee ladder and an anchor for vault rotations, spend challenges instead of a search (DEP-03, DEP-06), and an answer to a redelivered operation that is its original result (DEP-17). Its red team now refuses to attack the signet devnet unless told to.

**kiln** learned to build behind an intercepting proxy, with `--extra-ca` and `--https-proxy`, and `kiln sync` keeps every org repository as a live checkout beside it. **cl-transport** gained a carrier for HTTP `CONNECT` proxies and dropped usocket and bordeaux-threads. **json-simple** now prints floats by Steele–White/Burger–Dybvig digit generation and parses them by Clinger’s fast path. **seal** distrusts eight compromised eMudhra intermediate keys.

---

*the gates / 24 October*

## One red, then none

One repository was failing at publication: kiln’s macOS turnkey job, since Tuesday. Its seed script runs on the host for `kiln build`, and the CA helper added for `--extra-ca` named Debian’s bundle path, which a Mac does not have, so every fetch failed before it started. kiln now uses the first system bundle that exists, and the job passes again. That fix is dated the 24th and counted above.

As of publication: **62 repositories, 43 green, none failing, 19 without a workflow.** Two of the three new repositories, wax and witness, arrived with CI on their first day; trellis has none yet.

---

**Method.** Commits by *author* date, 17 to 24 October 2026 inclusive, across the 61 repositories cloned from the modus-lisp organisation, counting each repository’s default branch as the union of the local branch and `origin/`. Produced with `bin/week 2026-10-24 --until 2026-10-25 --log`. The span runs a day past the usual Friday because the result this issue is named for landed on Saturday evening; next week’s starts on the 25th. Thirteen of the counted commits are crier’s own repairs: the eleven one-line sibling lists and kiln’s lock from the 17th, described in № 32, and kiln’s CA fix today. Five commits on modus branches that had not merged (`perf-substrate-main`, `zero-next`) are not counted. The ANSI timeline is assembled from commit bodies, and its rows measure different things, as the second column says. The green and failing figures count the organisation’s 62 repositories, which includes the profile repository this ledger does not.

**Outside the log.** The author’s nostr notes were read with `bin/notes 2026-10-24`. The two short quotations in italics that are not from commits — on trellis, and on UDP — are from them.

[← All issues](https://modus-lisp.github.io/issues/) · [crier](https://modus-lisp.github.io/)
