# A Lightning node in 34 hours

**This Week in Modus №26** · 29 August – 4 September 2026 · 304 commits · 37 repos

<https://modus-lisp.github.io/issues/2026-09-05/>

A repository that did not exist on Monday spent Tuesday and half of Wednesday becoming a Lightning node, and finished by getting itself punished by Core Lightning for publishing a revoked commitment — which is the proof, not the bug. Around it: thirty repositories gained their first CI gate in a day, and every failure that turned up was in the new harness rather than the code. And the floor reached the thing it has been walking toward since July — a real `quickload`, fetching a real library off the network, onto a filesystem with no operating system beneath it.

| Commits | Repositories | To a Lightning node | First CI gates | Green, of 46 |
|---|---|---|---|---|
| 304 | 37 | 34h | 30 | 39 |

---

*cl-payments / 48 commits / 1–2 September*

## Phase 0 to punished, in a day and a half

The repository did not exist before 1 September. Its first commit is at 02:18 UTC that morning and reads *Phase 0-1: BOLT #1 wire + BOLT #8 Noise_XK transport* — the byte format two Lightning nodes speak, and the encrypted handshake they speak it over. Thirty-four hours later this had happened:

*cl-payments — the first thirty-four hours*

```
01 Sep 02:18   74f8f5f   BOLT #1 wire + BOLT #8 Noise_XK transport
01 Sep 11:06   553662b   BOLT #9 feature bits + the BOLT #1 peer protocol
01 Sep 19:18   5fe3f8a   BOLT #2 channel establishment — a real channel is open
02 Sep 04:53   e61acf4   BOLT #11 invoices, reproducing the spec's examples byte for byte
02 Sep 05:14   d16b99b   Phase 6 complete: cl-payments pays
02 Sep 12:06   18314b1   force-close and sweep, and Core Lightning punishes our
                         revoked commitment
```

That last line is the one worth reading twice, because it looks like a failure and is the strongest result in it. Lightning’s security rests on penalties: if a node publishes an old channel state — a commitment it has already revoked — the counterparty is supposed to detect it and take the whole channel balance. Getting Core Lightning to *successfully punish you* means your revoked commitment was well-formed enough to be recognised, your revocation secrets were derived correctly, and the reference implementation agreed with you about what cheating looks like. You cannot fake that by getting the happy path right.

The rest of the week’s 48 commits close the same loop from the other side: the daemon watches the chain and punishes in its turn, an old commitment of its own on chain is treated as forfeit rather than swept every block, anchor channels are negotiated and used end to end between two daemons, second-stage HTLCs get inputs of their own, and there are watchtowers. The Sphinx onion of BOLT #4 and the commitment format of BOLT #3 are both checked byte for byte against the specification’s own appendix vectors.

And it is graded by things with no stake in it. The README’s own summary — *a from-scratch Lightning Network implementation in Common Lisp, verified against Core Lightning and LND on a private signet* — names two independent reference implementations and a real chain, with the chain view itself validated by [cl-consensus](https://github.com/modus-lisp/cl-consensus), the workspace’s own Bitcoin node. A payment is forwarded *between two Core Lightning nodes*, which means both neighbours believed it. LND interop arrives on its own commit, for `channel_reestablish` and `announce_channel`. This is the pattern [№ 17](https://modus-lisp.github.io/issues/2026-07-04/) named as the house rule: build the oracle before the feature, and never let the thing that wrote the code be the thing that grades it.

The one dependency worth noting points back into the same week: *deps: pin secp256k1-fast to the pushed thread-safety fix*. The curve library made `CT-MUL-G` usable from more than one thread on 1 September, and a Lightning daemon signing on several connections at once is what needed it.

---

*thirty repositories / 30 August*

## The gates that already existed

Nearly every repository here already had tests. Most had oracles — the compressors check themselves against zlib, the font stack against `ttx` and HarfBuzz, the curve library against real signatures. What almost none of them had was anything that ran those tests when somebody pushed. The suites were written and nobody was pressing the button.

Thirty repositories got their first workflow file on 30 August. The commit subject repeats, and it is the honest one:

> ci: run the gates that already existed

> — the subject in twelve repositories; three more say “run the tests that were already here”

That is the shape of the day. In fifteen repositories the work was writing no tests at all. Four got a gate they genuinely did not have — pigment, warp, warren and webrtc-media — and the rest is thirty `gates.yml` files, a fixture path corrected in one place, an `ASDF` load added in another, and then the tail that always follows: making a harness that has never run agree with thirty repositories that have never been run by one.

### Four reasons a green repository goes red

The striking part is that every failure found on 30 August was in the harness. Not one was a test that had been quietly failing. Each was found once, in one repository, then fixed in all of them, which is why the same subject appears in as many as twenty-four repositories at once. Ninety-five of the week’s 304 commits are CI commits.

> `--script` implies `--no-userinit`, so quicklisp’s init file never loads and a library installed by the step above is invisible to the gate; it also does not load ASDF, which a gate whose first form is `(asdf:load-system …)` needs. Both failures look like the repo’s fault and are the harness’s — warp went red on “Component bordeaux-threads not found” with bordeaux-threads installed one step earlier.

> — cabinet — bc9eef0, *ci: do not run gates under --script* — the same commit landed in fifteen repositories

The other three are the same class. A gate run from a directory the runner had discovered and then truncated, fixed by giving it a source registry and keeping discovered paths whole. A test that assumed its own system was loaded, in a process where nothing had loaded it. And an oracle reading `chipz` and `salza2` symbols in a process where neither had been quickloaded, because nothing in the repository declares a dependency that only the oracle needs.

The fourth has an argument attached:

> The one step here that reaches the network. A transient failure in it is indistinguishable from a broken repository: loom went red on “Connection reset by peer” partway through the install and was diagnosed as having a non-hermetic test suite — it does not, and its suite never ran that day. A badge that goes red on somebody else’s CDN teaches people to ignore it.

> — cabinet — b1aecf8, *ci: retry the quicklisp install*

Three repositories reverted their own gate the same day rather than leave a red badge up while the harness was still wrong — cairn, loom and operandi each carry a `Revert “ci: run the gates that already existed”`, and two of the three landed again once the four fixes above were in. A gate that is wrong about the code is worse than no gate, and the week treats it that way.

Where that leaves the organisation, measured rather than asserted: **46 repositories, 39 green, one failing, six with no CI at all.** The one failure is operandi, whose gate is reverted and whose last run is therefore still the red one.

---

*modus / 59 commits / 29 August – 3 September*

## quickload, onto a filesystem that is not there

Since [№ 17](https://modus-lisp.github.io/issues/2026-07-04/) the same target has been visible at the end of a long corridor: for a Lisp machine to install software the way every other Lisp does, `quickload` has to work — and `quickload` wants a filesystem, which a machine with no operating system does not have. cabinet is the answer the workspace already built for something else: a hierarchical filesystem living inside one pagetree keyspace, written for the Bitcoin node’s storage and reused here.

This week the two meet, in a single seam under Common Lisp’s own `open` and `load`:

> The full unmodified-client flow: client sources loaded FROM cabinet, `ql-dist:install-dist` fetches the mirror over hosted sockets and writes the dist INTO cabinet, the release tarball saves byte-perfect, gunzip and minitar unpack into cabinet, and ASDF loads all 18 alexandria files from cabinet — `FLATTEN` ⇒ `(1 2 3 4)`.

> — modus — e338d38, *HOSTED WORKS: genuine ql:quickload of alexandria with cabinet as the ONLY filesystem*

An unmodified quicklisp client, reaching a real mirror, unpacking a real tarball, into a filesystem that is a B-tree in a keyspace. Nothing was taught that it was not talking to a disk.

### The bug that made the tests lie

Getting there needed the week’s best root cause, and it is a good example of why this dispatch reads commit messages rather than diffs. aarch64 Linux has no `open`, `stat`, `unlink`, `rename` or `mkdir` syscalls — only the `*at` variants — so eleven `%sys-*` functions are overridden for that architecture. Each override faithfully reproduced the syscall shape. Each one dropped the first branch of the original: the test for whether a cabinet is mounted.

> THE FAILURE WAS SILENT AND DATA-WRONG, NOT A CRASH. For a path the host also has, the real open succeeded and returned the HOST file’s bytes. Measured with a sentinel: cabinet holds `CABINET-SENTINEL-42`; before this commit aarch64 `with-open-file` returned the host’s “2021-02-13” while x64 returned the sentinel.

> — modus — fc74cd7a, *#283 FIX*

So on aarch64 every file operation went to the real kernel even with a cabinet mounted, and the passing tests were passing because they were reading the host’s files. x64 has no such override, which is exactly why x64 was correct and aarch64 was not — the two architectures were not running the same code, and only one of them was being believed. The same week closes two more of the same family: aarch64 runtime-JIT allocations now set their GC bitmap bits, so CLOS objects built at runtime survive collection on the Pi — validated on silicon, nine collections deep — and the JIT’s constant vector is ported to aarch64, replacing a stopgap that rejected every page carrying a constant because baked literals went stale when the collector fired mid-thunk.

### A merge that reports its own scope

When the branch lands on 1 September the merge commit is a gate result rather than an announcement, and it is worth quoting for the last paragraph:

*modus — dcf10cdb, cabinet-fs → main*

```
passed 17515 -> 17519   CHUNK-CRASH 0 -> 0   FILE-WEDGE 30 -> 30

The single lost test is 24718 = COMPILE.5 — a REVEAL rather than a
regression: the defect predates the branch and was invisible only because
CL:COMPILE did not compile, so the compiled-literal path never ran.

16 of the 17 commits are aa64 / cabinet / library-loading work that the
x64 ANSI gate is structurally blind to.
```

Four tests gained, one lost, the lost one explained and filed, and then the admission that the gate it just passed cannot see most of what the branch did. That last line is the discipline: a green gate is evidence about the things the gate can measure, and saying which those are is part of reporting the result.

---

*cl-transport / a30b756, 895e8ed, 4dc9862*

## What may leave, and how

The transport library already knew how bytes travel — direct, through SOCKS5, through Tor, or through any backend registered at runtime. Nothing used that to decide whether they *may*. This week it does, and the placement is the whole design.

The obvious place to enforce a network policy is where a carrier is chosen. That is the place the commit rejects, in one sentence:

> Declaring is not enforcing, and a policy expressed only where carriers are chosen governs exactly the callers that already agreed to be governed.

> — cl-transport — a30b756

So the gate sits lower, on `socket-connect` and `socket-send`, where seal, weft, conch, usocket and any quicklisp library nobody here has read all end up anyway. The halves meet at a default: `*carrier*` is `:raw` unless something bound it, and `:raw` means *nobody dialled this*. That single default is what lets a policy of `require * via cl-transport` mean what it says — code that opened its own socket carries `:raw`, satisfies no carrier requirement, and is refused.

Which makes it a migration tool as much as a policy. Turn the requirement on, and every path in the workspace still opening its own socket names itself in the log, so moving the rest of the org onto one dialler becomes a list to work through rather than a refactor to plan. kiln already arms it before anything dials out.

---

*glass, glass-sdl / 56 commits*

## A seat has a density

The desktop work splits cleanly in two. The first half is HiDPI, approached carefully: scale goes on the *seat* — the thing a viewer sits at — and not on the framebuffer, for a stated reason. A framebuffer is a rectangle of real pixels, which is a good definition and should not acquire a second kind of pixel. Screen width and height stay device pixels and every drawing primitive still takes them; the scale says only what a layout number is multiplied by before it becomes one.

It is a rational rather than an integer, because 3/2 is a scale real displays use and rounding it away at the door would make fractional scaling unreachable later rather than merely unimplemented. The multiply-and-round lives in exactly two places, so a value is rounded once — a title bar rounded up beside contents rounded down is a one-pixel seam nobody can find afterwards. The commit is candid about what it is worth today:

> Both are the kind of change whose whole value is that it changes nothing today, which is also what the gate asserts.

> — glass — 2bb70e9

Downstream: pop-up menus that scale and carry the density their pixels were drawn at, damage measured at the density it was drawn at, and a magnifier for applications that do not know about density — which, the commit notes, is nearly all of them. On the glass-sdl side the window is created in points, asks the panel for its pixels, and keeps the two apart. A macOS quirk gets handled on the way: the platform blocks the event loop while a window is being dragged, so the desktop learns to draw during the drag, drop frames rather than sizes on a slow one, and stop replaying the drag once the mouse comes up.

The second half is sound, both directions. This machine’s speakers, then its microphone, then a *Mixer* window — what is playing, who is listening, what is silent — and a music player “in the shape everybody already knows”. The microphone gets three postures rather than a boolean, can be offered rather than opened, gives itself back, and leaves the indicator lit a moment longer the way a camera light does. Dictation stays aimed at the window you pointed it at and says where the words are going; a local viewer counts as a keyboard, so dictation stops requiring a VNC server to reach a desktop running on the same machine.

---

*glass / c43e559, 5d392c3*

## A running image, showing its own source

Two commits give the control socket back what a form printed, then add `source-of` — so a running desktop can be asked what a function’s definition looks like:

*glass — c43e559*

```
kiln eval "(source-of #'cl-transport.gate:connections)"
kiln eval "(disassemble #'cl-transport.gate::loopback-p)"
```

The implementation choice is the point. Common Lisp can hand back a function’s form without reading any file, and the commit refuses to:

> The text is read back *out of the file* the definition was compiled from, rather than reconstructed. `function-lambda-expression` can hand back a form, but it is re-printed with macros expanded, formatting invented and every comment gone — and in this codebase the comments carry the reasoning, so that is precisely the half worth having and the half that is lost.

> — glass — c43e559

Reading the file tells the truth about the file, which may have moved on from the image; reconstructing the form tells the truth about the image, and throws away why any of it is the way it is. This project keeps choosing the reasoning.

---

*glass-webrtc, webrtc-data / 1 September*

## A demo directory becomes a repository

The forty-third repository in the workspace is not new work. `glass-webrtc` — the gateway that puts a glass desktop through a browser’s WebRTC stack — had been living in `webrtc-data/demo/`, and this week it moved out with its history: 125 commits, reaching back well before the move, into a repository of its own with an ASDF system instead of a script. webrtc-data loses 31,241 lines and keeps a pointer.

warp follows it, the devices file the monitor reads moving too, and kiln re-locks against the new address. It is the pattern from [№ 19](https://modus-lisp.github.io/issues/2026-07-18/) onwards: a thing that has become load-bearing stops being a subdirectory of the thing it was first demonstrated with.

---

**Method.** Commits by *author* date, 29 August to 4 September 2026, across the 44 repositories of the modus-lisp organisation, counting each repository’s default branch. Line counts exclude generated acceptance-gate artefacts and committed scratch dumps. Produced with `bin/week 2026-09-05 --log`, plus the GitHub API for cl-payments. The organisation’s `.github` profile repository and this site are not counted, on the grounds that a dispatch does not report on itself.

**Two corrections to the tool, both made this week.** `bin/week` counted whatever branch a repository happened to have checked out, which is not what landed: a repository sitting on a feature branch reports that branch’s work as the week’s, and one whose branch is behind reports nothing at all. It now counts `main` or `master` explicitly, and prints what it finds off that branch rather than omitting it silently.

The second was worse, and this issue was published with it. The ledger walks the directories in one workspace, so a repository nobody has cloned is not a quiet week — it is an invisible one. cl-payments was created, taken to interop with two reference implementations and pushed inside this week, and the first version of this issue did not mention it, because it was never on the disk being measured. `bin/week` now asks the organisation what repositories exist and names any it cannot see. Earlier issues were measured before either fix and their figures stand as published.

**CI figures.** “30 first gates” counts repositories whose earliest commit adding `.github/workflows/` is dated 30 August 2026. The green/failing/ungated numbers are the newest workflow run per repository across the whole organisation, read with `bin/ci` on 6 September 2026 — 46 repositories, which is more than the 43 cloned here.

[← All issues](https://modus-lisp.github.io/issues/) · [crier](https://modus-lisp.github.io/)
