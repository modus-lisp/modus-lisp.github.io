# Attested, end to end

**This Week in Modus №32** · 10–16 October 2026 · 277 commits · 21 repos

<https://modus-lisp.github.io/issues/2026-10-17/>

Four issues ago a bare-metal Lisp learned to boot as an encrypted guest and had never executed a VMGEXIT. Three weeks later it verified AMD’s certificate chain itself, and recorded that it could not reproduce the launch measurement from the provider’s firmware. On Saturday it did. Modus now boots under SEV-SNP through measured boot, serves key-only SSH, and returns a report whose measurement equals the number computed offline — and the agent that uses all this learned, the same week, to refuse a route that is encrypted but not attested.

| Commits | Repositories | New this week | Red-team suite | Green, of 59 |
|---|---|---|---|---|
| 277 | 21 | 3 | 26/26 | 40 |

---

*modus / baf1bf2f / 10 October*

## The measurement matches

An attestation report is only worth what you can say about the thing it measures. Last issue established the first half — the chain verifies, a flipped bit is rejected — and recorded the half that did not work: the launch measurement could not be reproduced from the published firmware, so the report proved that *something* booted without proving what. This week, on a provider where you supply the firmware:

> On VPSBG’s SEV-SNP VPS (EPYC 7713P, 1 vCPU) Modus now boots through measured boot (UKI via `-kernel`, kernel hashes), serves key-only SSH, and returns a PSP attestation report that verifies against AMD’s chain with a launch measurement **equal to the offline `sev-snp-measure` value**.

> — modus — baf1bf2f

That equality is the whole point. One number comes out of a processor that signed it; the other is computed on a laptop from the firmware and the image. When they agree, a remote party who trusts AMD’s root key and can read the image knows what the machine is running. Everything the archive has reported under this heading since [№ 29](https://modus-lisp.github.io/issues/2026-09-26/) was a step toward those two numbers being the same.

The commit body is a list of what it cost, and it is the most instructive part. Memory a SEV-SNP guest has never written *reads as noise*, so the runtime-metadata page has to be zeroed whole rather than word by word. The shared region must be rescinded with `PVALIDATE` through its *encrypted* mapping before the C-bit is cleared — doing the first `PVALIDATE` through a C=0 mapping killed the virtual machine outright. The secrets page has to be copied out of the range the image is about to be copied over. And a comparison that had always worked, `%core-requested-p`, had to become `EQ` rather than `=`.

None of that is debuggable the usual way, because an SNP guest has no console. So the build grew spin points — `MODUS_SNP_SPIN_AT`, `MODUS_LISP_SPIN_AT`, `MODUS_SNP_VC_FATAL=spin` — to bisect a boot by where it stops. Networking is virtio-net MMIO through the GHCB; SSH gets its keys from `RDRAND`, waits that are timed rather than counted, and `RDTSC` because `RDTSCP` is not on every x86-64.

---

*operandi, cl-sev-snp, seal / 11–14 October*

## Why any of this

This archive has reported the attestation work for a month without ever saying what it was *for*. The answer turned up in the author’s own notes this week, in a reply about something else entirely:

> i keep meaning to try PPQdotAI. routstr and the underlying tinfoil are what sent me down the SEV-SNP path 😂

> — nostr — 407a1388, 2026-10-10 17:10Z

Tinfoil runs language models inside attested enclaves; Routstr routes to them. The whole SEV-SNP campaign — the guest boot, the #VC handler, the certificate chain in Lisp, the EC2 control experiment, the measured boot above — began with wanting an inference provider whose claim you could check rather than accept. This week the two ends meet.

**cl-sev-snp** is the verifier lifted out of modus into a library of its own, and it immediately grows: AWS Nitro attestation documents alongside SEV-SNP reports, the Genoa root key pinned by default, and — three days later — *fetch over seal/http instead of dexador: no FFI*, because a verifier that pulls in OpenSSL to download a certificate has an awkward story to tell.

**operandi** gets a `--tinfoil` backend sealed to Tinfoil’s enclave, and a `--confidential` flag. The interesting commits are the ones that decide what the flag may mean:

*operandi — what counts as confidential, 14 October*

```
a8e43c9   confidential: judge by the hardware root, not the hop count
19df216   confidential: refuse --routstr outright; it is encrypted, not attested
087d0c0   confidential: rename the flag to --confidential-tinfoil
```

A blind relay hides your traffic from the relay. An attested enclave tells you what is running at the other end. They are different properties, and an agent that treated the first as the second would be lying to its user in a way nobody would notice. So the flag refuses the route it cannot attest, and then gets renamed to name the specific root it trusts — because *confidential* on its own is a claim about nothing in particular.

Underneath, **seal** grows the transport that needs: HPKE (RFC 9180, base mode, X25519 and AES-256-GCM) and the Encrypted HTTP Body Protocol framing over it, with key configurations parsed from a provider’s `/.well-known/hpke-keys`. Two TLS fixes land beside it that read like the week’s theme in miniature: keep reading until the ServerHello record is *whole*, and keep the handshake messages coalesced with it.

---

*emissary / 5 commits / 14–15 October*

## A key that answers

The week’s other new repository is the piece that makes an attested machine useful from a distance. [**emissary**](https://github.com/modus-lisp/emissary) evaluates forms sent by allowlisted Nostr keys — gift-wrapped under NIP-17 and NIP-59, each in a fresh and killable Modus process — over three transports: direct messages, Marmot groups, and FIPS.

Which is to say: remote code execution, deliberately, with the authorisation being a signature rather than a password and the isolation being a process rather than a promise. The Marmot transport answers only allowlisted members of groups the agent was invited to, and direct messages are accepted only as kind-14 rumors. Put beside the attested SSH above, the shape is clear enough — a machine that can prove what it is, reachable over a protocol with no servers, taking instructions only from keys it knows.

It started on the 14th as something narrower — `modus-agent`, a thing that ran forms for allowlisted keys — and was generalised the same day into a layer with a pluggable handler, of which running Lisp is merely the default. The commit that did it opens with a definition worth keeping:

> An emissary is a key that acts for the principals on its allowlist: requests arrive over NIP-17 DMs, Marmot groups and FIPS, each with a sender the transport authenticated, and a principal’s request goes to a pluggable HANDLER whose answer returns the way it came. The default handler evaluates the request in a fresh, killable modus process.

> — emissary — 2e8e353

---

*modus / 149 commits*

## And a week of making it fast

Most of modus’s 149 commits are not about attestation at all. They are the performance work the Pi Zero campaign of [№ 28](https://modus-lisp.github.io/issues/2026-09-19/) argued for, now applied broadly: the interpreter is 2.1× faster from constant-folded shift counts and byte-typed fetches, a hot-function JIT promotes a runtime `DEFUN` to native after sixty-four calls or ten milliseconds, a bridged call patches itself to its native callee, and global and special variable reads go through a baked cell rather than a hash probe.

The arithmetic got a rebuild. Bignums move onto limb vectors with a magnitude kernel, Karatsuba multiplication above forty-eight digits, a Lehmer GCD taken from the top sixty-one bits, and a doubling integer square root. Double-float expression trees compile unboxed; `EXP` and `LOG` get fdlibm-style argument reduction and land within one or two units in the last place of libm. Bit arrays become genuinely packed, sixteen to a fixnum word.

And 13,702 lines were deleted, nearly all of them one file: `mvm/ansi-bridge.lisp`, dissolved into the `cl-*` files whose runtime it held and an `ansi-test-aux.lisp` for the test helpers, with thirty-one definitions dropped because a later one in the same group already shadowed them. A bridge is scaffolding; taking it down is how you find out the building stands.

---

*the rest / cl-deposits, glass, natrium, png-pure*

## Also this week

**cl-deposits** takes its red-team suite to **26 of 26**, with authorised confiscation rebuilt from public state rather than from anything the node privately knows, and disputes driven on their own threads with per-ledger locks. One commit is a small lesson in what a passing test is worth: *a reference winner under OBL_WITHHOLD is INVALID (rerun), not a pass*.

**glass** splits the desk into a network thread and a compositor thread, and gives every RFB client a thread that shares nothing — the reader messages the sender rather than reaching into it. **natrium** is re-tuned for Modus specifically: SHA-512 as pairs of 32-bit halves to avoid bignums, Poly1305 decoding limbs from bytes rather than through a 128-bit integer per block, and field multiplication through a new `%mul-split` intrinsic that modus advertises on `*features*`. **png-pure** arrives as a PNG encoder on cram, with QR and BOLT11 demos — so an invoice can become an image without leaving the workspace.

---

*the gates / 17 October*

## Eleven stale lists

Most repositories here clone their modus-lisp dependencies in CI from a list written into the workflow by hand — a transitive closure, maintained by eye. When VP8 moved into reel in September and JSON onto json-simple this month, eleven of those lists were not told. Five had been failing on `Component :REEL not found` for weeks; the other six were just as stale and still green only because they had not run since.

All eleven were corrected on the 17th, the day this was published, with the lists computed from the `.asd` files across the organisation rather than by hand, and all eleven went green within minutes. kiln’s `repos.lock` gained the four repositories that the ones it already pins now require. Those commits fall in next week’s ledger, not this one’s.

Two tools came out of it. `bin/siblings` computes each closure and reports a stale list before CI does. And `bin/ci` now reports the worst of a repository’s *current* workflows on its *default* branch — it had been reporting the newest run of anything, which counted a deleted workflow as a failure and a Pages deploy as a pass.

As of publication: **59 repositories, 40 green, none failing, 18 without a workflow.** Three of those need none — the organisation profile, this site, and the Quicklisp distribution. Most of the rest are repositories created in the last five weeks.

---

**Method.** Commits by *author* date, 10 to 16 October 2026, across the 58 repositories cloned from the modus-lisp organisation, counting each repository’s default branch as the union of the local branch and `origin/`. Produced with `bin/week 2026-10-17 --log`. Seven commits sit on branches that had not merged — modus’s `operandi-on-modus` and `zero-hid`, kiln’s `zero` — and are not counted. The green/failing figures count the organisation’s 59 repositories, which includes the profile repository this ledger does not.

**Outside the log.** The author’s nostr notes were read with `bin/notes 2026-10-17`. The remark that opens “Why any of this” is from them, and it is the only place in a month of commits where the motive for the SEV-SNP work is stated plainly — which is exactly what a second source is for.

[← All issues](https://modus-lisp.github.io/issues/) · [crier](https://modus-lisp.github.io/)
