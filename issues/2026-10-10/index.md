# The chain verifies

**This Week in Modus №31** · 3–9 October 2026 · 294 commits · 22 repos

<https://modus-lisp.github.io/issues/2026-10-10/>

Two issues ago this archive reported a bare-metal Lisp learning to boot as an encrypted guest, and noted that nothing had yet executed a VMGEXIT. Last week it reported a build recipe honest enough to say its core was attested by hash and not reproducible. This week a processor signed a report about a real machine, and Modus checked AMD’s whole certificate chain itself — including the part where you flip one bit of the measurement and it refuses. Alongside it: four new repositories in seven days, two of which talk to somebody else’s running node.

| Commits | Repositories | New this week | Clouds attested | Green, of 56 |
|---|---|---|---|---|
| 294 | 22 | 4 | 2 | 39 |

---

*modus / test/snp / 7–8 October*

## ARK to ASK to VCEK to report

An AMD SEV-SNP attestation report is a block of bytes signed by a key that only the processor’s security chip holds, saying what was loaded into a virtual machine before it ran. Believing one means walking a certificate chain: AMD’s root key signs a signing key, which signs a per-chip key, which signs the report. Everyone does this with AMD’s own tooling. This week Modus does it itself, in Lisp, and the commit lists what that involved:

> DER parsing of the certificates, SHA-384 (SHA-512 with its own IV), RSASSA-PSS (SHA-384, MGF1-SHA-384, salt 48, e = 65537) for ARK, ASK and VCEK/VLEK, and ECDSA over secp384r1 for the report. Profile is checked; anything else is refused.

> — modus — dcc812c7, *SNP: full report verification in Lisp*

Against real hardware, twice. A cloud VPS on an EPYC 7713P, whose report is signed by a per-chip VCEK: every link passes. An EC2 `c6a` instance, which uses the per-vendor VLEK path through a different intermediate: every link passes. And then the part that makes it a verifier rather than a parser — *flipping one bit of the measurement is rejected; a wrong signer is rejected.* A checker that has never said no is not known to work.

The verifier goes on to require that the report was produced at VMPL0, and to check validity, chip ID, TCB and the debug policy — because a report from a guest with debugging enabled attests to a machine somebody can single-step. Along the way the ECDSA is rewritten in Jacobian coordinates with a joint scalar multiplication, eleven times faster, which is the difference between a verification you run and one you mean to.

### What it does not establish

The same commit names what it has not checked: certificate validity periods, and the chip-id and TCB extensions inside the certificates as opposed to in the report. And a separate commit exists only to record a failure:

> SNP: VPS launch measurement not reproduced from the published firmware.

> — modus — 05a7edb2, a record with no code in it

That is the gap that matters, and it is the one [№ 29](https://modus-lisp.github.io/issues/2026-09-26/) predicted. A signed report says *this machine booted an image whose hash is X*. It does not say what X is. Turning the hash into a claim about software means rebuilding the measurement yourself from firmware you can read — and on this provider’s published firmware, it did not come out the same. The report is verified; the thing it measures is not yet pinned. A second experiment in the same series answers a sharper question, and the answer is a limit on the whole approach.

The question was whether a customer can get anything of their own into an EC2 launch measurement — specifically, Secure Boot keys, which would let a report mean *“this machine booted an image signed by modus.”* The experiment is a proper control: two AMIs from one snapshot, differing only in their UEFI variable data, four guests on identical instance types.

*modus — test/snp/ec2-varstore/RESULT.md, 8 October*

```
guest       variable store      measurement
source      none                7a89ccea…dc766
control-1   none                7a89ccea…dc766
control-2   none                7a89ccea…dc766
vars        our KEK + db        7a89ccea…dc766
```

The `vars` guest really did boot with the keys — its EFI variables hold `modus KEK (development)` and `modus db (development)`, and the controls’ hold none. The measurement is identical anyway. The record draws the consequence without softening it: *“EC2 SEV-SNP cannot vouch for anything past AWS’s firmware.”* Attesting your own image there is not a matter of configuring it correctly; the platform does not offer it. Which is exactly why the other half of this week exists.

And the attestation work found a bug underneath itself. Parsing a report inside Modus produced wrong SHA-256 and SHA-512 digests under `--eval` in the hosted CLI. The fix runs four commits: the constants were being initialised lazily and sometimes not at all, the padding was writing a truncated bit length, and the hashes now work block-wise so a message is not bounded by memory. A verifier that had been written on top of a quietly broken hash would have verified nothing, convincingly.

---

*kiln, modus / Nitro / 6–7 October*

## An enclave, built without their tools

AWS Nitro Enclaves are the other half of the same idea: a stripped virtual machine with no persistent storage, no interactive access and no external network, which measures what it boots into platform configuration registers. The normal way to build one is Docker plus `nitro-cli`. kiln now builds the image directly — *“the hosted modus as an AWS Nitro Enclave image, without Docker or nitro-cli”* — signs the EIF so that PCR8 is computed from the signing certificate, and ships a saved heap so the enclave starts from a core rather than installing at boot.

The measured result is in the subject line of the commit that got it running: *the enclave boots from a saved core; SSH banner 2.7 s after run-enclave*. The enclave serves SSH over vsock, because an enclave has no network and vsock is the only door. Several commits around it are the small indignities of somebody else’s init: the application ramdisk must put the program under `rootfs/`, `/cmd` wants one argument per line, and the Nitro Security Module self-test must not take the console down with it when it fails.

And that is the answer to the EC2 result above, named in the same record: the AWS route that attests the image is Nitro — PCR0 to PCR2 over the enclave image, PCR8 over who signed it. SNP attestation of a reproducible image needs a host where you supply the firmware; Nitro measures what you actually shipped. Two platforms, two attestation stories, and the week establishes which question each one can answer. Which the author summarised on nostr, as a list:

*“everywhere you want to be” — nostr, 7 October*

```
iOS                        aarch64
Android                    x64
Linux                      arm32
pi zero 2w hardware        i386
browser                    riscv32/64
nitro                      ppc32/64
sev-snp
```

---

*cl-fips, cl-marmot, json-simple, fs-fat / 72 commits*

## Four repositories, seven days

Two of them are protocol implementations that were taken all the way to interoperating with a running implementation somebody else wrote, which is the only test that counts.

[**cl-fips**](https://github.com/modus-lisp/cl-fips) is a node for the Free Internetworking Peering System — Noise IK and XK links, a spanning tree, reachability announcements, transit forwarding. Thirty-six commits in two days, and the subjects are a ladder of things checked against the real upstream node rather than against a mock: an echo round trip, then node-initiated data, then a probe-size limit derived from the link MTU, then *a middle node between two real upstream nodes*, then reaching a node two hops away through a real relay. Two commits are security fixes found by playing adversary against itself — closing an FSP rekey hijack, and making a forged SessionSetup unable to displace a rekey. By Friday the whole daemon runs inside Modus over both TCP and UDP, and there is an IPv6 shim so that `http://<npub>.fips/` resolves.

[**cl-marmot**](https://github.com/modus-lisp/cl-marmot) is MLS over Nostr — RFC 9420, the protocol White Noise speaks. It passes the MLS working group’s own interop vectors for cipher suite 1, then joins a group created by `wn` and messages both ways, then reaches **22 of 22** on an asserted interop script against it. It reached version 1.0.0 on Friday with an API freeze, and the commit before that is the one worth noting: *README: what 1.0 does not do*. Transport is `wss://` through seal, pure-CL TLS 1.3 with full certificate validation. It runs on Modus.

What that adds up to is easier to show than to describe. A Claude Code session asked cl-marmot to start a conversation; the other end was White Noise, on a phone, in the hands of a person:

![A phone messaging screen titled Easy Condor. At 12:16, an incoming message: "Hi! This conversation was started by cl-marmot, a Marmot client written in Common Lisp (MLS over Nostr, running on SBCL), at your request from a Claude Code session. Reply here and I'll see it." At 12:20, an outgoing reply: "great to hear from you, claude!" Then an incoming message: "Great to hear from you too! Your reply came through end to end: White Noise on your phone, cl-marmot in Common Lisp on this side."](https://modus-lisp.github.io/assets/img/marmot-wn-2026-10-09.jpg)

***Both ends of an MLS group.** White Noise on the phone; cl-marmot, six days old, on the other side. The group is end-to-end encrypted by a specification neither implementation wrote, and the two share nothing but the document. screenshot by the author, 9 October 2026*

[**json-simple**](https://github.com/modus-lisp/json-simple) is the now-familiar move: a jzon-compatible parser and printer, same mapping, byte-identical output, with jzon itself wired in as an oracle on every push. It exists because jzon was the last third-party system in the agent’s closure, and cl-nostr is already moving onto it. One commit is a small monument to this project’s habits — *float: use the host’s FLOAT and SCALE-FLOAT (modus now rounds subnormals correctly)* — a workaround deleted because the floor underneath it was fixed.

[**fs-fat**](https://github.com/modus-lisp/fs-fat) arrives as a single commit of 4,882 lines: FAT32 and exFAT, readers and writers, in portable Common Lisp. A bare-metal machine that wants to read an SD card needs this, and nothing in the workspace had it.

---

*modus, kiln / 98 and 22 commits*

## Also this week

**Android.** Modus runs as an Android app — a NativeActivity that runs the image as a child, with the keyboard and key events built the same way the iOS shim has them, and kiln’s blit, speaker and scale calls wired through. The phone work from [№ 30](https://modus-lisp.github.io/issues/2026-10-03/) now has a second phone.

**kiln on iOS** grows a Notes app to type into, a caret you can move, touch-sized chrome and one desk pixel per point — and a keyboard that follows the focus. There is something funny about a project this concerned with page tables spending a day on whether Notes looks like a phone’s notes, and it is also exactly right: a system nobody can use is a system nobody checks.

**The ANSI grind** continues underneath all of it, and the commits are the usual mix of specification and consequence: numeric comparisons signalling `TYPE-ERROR` for a non-number, `WITH-STANDARD-IO-SYNTAX` binding its variables rather than assigning them, `#n=` labels being per-`READ`, `EQUAL` hash tables bucketing list keys by structure, and `FUNCALL` of an undefined symbol signalling `UNDEFINED-FUNCTION`. A compiler change lands with the right failure mode too: *CLI builds fail when the compiler skips a form*.

**klog**, in kiln, is this week’s neatest small thing, and its origin is on the record. The agent asked for a log it could not get at; the answer, posted the same day the commit landed, was *“why don’t you make a log handler that sends logs as giftwrapped DMs to an npub that you bake in via kiln”*. So kiln’s event log is now NIP-59 gift-wrapped direct messages to a baked-in key. A machine with no console, inside an enclave, can still tell you what it is doing, over a relay, encrypted to one reader.

---

*the gates / 10 October*

## Thirty-three days

webp-pure and webrtc-media have been red since 8 September on `Component :REEL not found`; warren since the 9th; loom and weft since the 21st, for the same reason, because they depend on webp-pure. The sibling-checkout list still has not been told that VP8 moved into reel. **56 repositories, 39 green, 6 failing, 11 with no CI.**

Eleven ungated now. Two of this week’s four repositories brought a workflow with them — cl-marmot and json-simple, the latter running jzon as an oracle on every push. The other two did not. cl-fips, thirty-six commits with two security fixes and interop against a live node, has nothing watching it; neither does fs-fat, whose whole job is to write a filesystem correctly.

---

**Method.** Commits by *author* date, 3 to 9 October 2026, across the 56 repositories cloned from the modus-lisp organisation, counting each repository’s default branch as the union of the local branch and `origin/`. Produced with `bin/week 2026-10-10 --log`. Thirty-three commits sit on branches that had not merged — modus’s `zero-hid`, `zero-marmot`, `virtio-net` and `compile-speed`, kiln’s `zero` and `android` — and are not counted.

**Outside the log.** The author’s nostr notes were read as in [№ 1](https://modus-lisp.github.io/issues/2025-03-15/), with a new tool: `bin/notes`, which pulls a week’s notes from five relays and merges them by event id. It is in the repository rather than in a scratch directory because this is the fourth issue to want it. The “everywhere you want to be” list and the klog remark above are both from it, each quoted with its own timestamp.

[← All issues](https://modus-lisp.github.io/issues/) · [crier](https://modus-lisp.github.io/)
