# A 284B model, from Lisp

**This Week in Modus №34** · 25–30 October 2026 · 266 commits · 9 repos

<https://modus-lisp.github.io/issues/2026-10-31/>

At 22:22 on Tuesday a new repository loaded a half-billion-parameter model in Common Lisp. By Thursday afternoon the same code was running DeepSeek V4 Flash, a 284B mixture of experts, from the SSD of a 48 GB laptop, about as fast as the C engine it was ported from. On Friday it trained GPT-2. The layout library from last week was rewritten from the papers and checked against a thousand graphs. And Modus moved its kernel out of the user’s package, gained a generational collector, and booted on a 486.

| Commits | Repositories | New this week | tok/s at 284B | Green, of 63 |
|---|---|---|---|---|
| 266 | 9 | 1 | 9.8 | 43 |

---

*illatio / 78 commits / 27–30 October*

## Inference, carried in

*Illatio* is Latin for “a carrying in; an inference”. It is a language-model engine in Common Lisp: GGUF weights, quantized kernels, a byte-level BPE tokenizer, sampling, chat templates, tool calls and an OpenAI-compatible server. It has no C or Objective-C of its own and no BLAS. It calls a few libc functions and the Objective-C runtime through SBCL’s alien interface, and on Apple silicon it drives the GPU through Metal from Lisp.

The log reads like a stopwatch. The first commit, at 22:22 on the 27th, is Qwen2.5-0.5B: the file format, Q8_0 kernels, the tokenizer and the forward pass. At 07:17 the next morning, *illatio runs on modus*, giving llama.cpp’s tokens, slowly. Then Qwen3-4B, K-quants, a 27B Qwen3.5, and at 21:43, *the GPU: qwen35 decode on Metal, driven from Lisp*. At 23:22, DeepSeek V4 Flash’s forward pass on the CPU. At 00:58, *checked against ds4’s own numbers, at this model’s noise floor*. Through the 29th every layer moved to the GPU, with the routed experts streamed from disk. The author, that evening: *common lisp running ds4f on a 48GB MBP, written 100% by claude in 48 hours, based on llama.cpp and ds4*.

Those are the README’s numbers, from one machine. The model file is 86.7 GB, 78 of it routed experts, on a machine with 48. ds4 streams them from the SSD too. The difference in decode is in what illatio keeps: an expert pool sized to the GPU’s working set, the next layer’s experts guessed and read ahead, and expert reads that bypass the page cache. The README’s afterword puts it plainly: *most of the work was not arithmetic but logistics*.

“Ported from” is meant literally, and `THIRD-PARTY.md` says from what. The Metal kernels’ structure, the block dequantizers and the CPU kernels’ arithmetic order come from ggml and llama.cpp, and the GPU kernels are Metal shaders rather than Lisp. The DeepSeek port, its tokenizer and its expert hotlist come from ds4. GPT-2 training comes from llm.c. All three are MIT, and so is illatio. Every model is checked against its reference: llama.cpp for the Qwens, ds4’s CPU path for DeepSeek.

On the 30th it learned to train: GPT-2’s forward, backward and AdamW from llm.c’s CPU reference, then the whole step in one Metal command buffer. Then it played modded-nanogpt’s speedrun game, sized for a laptop: a 6-layer, 384-channel model on FineWeb, run to a validation loss of 4.30. llm.c’s recipe ended its run at 5.37. With modded-nanogpt’s later changes, Muon among them, the third record of the day reached 4.30 in **52.1 minutes**. Those times are on this laptop’s own scale, not comparable to the H100 records. The author, on the day: *it just took me longer to decide on a library name than for claude to get rudimentary inference in common lisp*. operandi gained `--illatio` the same week: the model runs inside the agent’s own image, with no server at all.

---

*trellis / d4648ed / 27 October*

## Rewritten from the papers

Last issue described trellis as what it then was: a port of Eclipse ELK’s layered engine. ELK is EPL-licensed and the workspace is MIT, so on the 27th trellis was replaced outright. The repository now begins with one commit: *a complete graph layout library, implementing the published algorithms directly from their papers and checked against ELK 0.13*. The ported history is gone from `main`.

The commit body reads like a bibliography. It uses Eades, Lin & Smyth for breaking cycles and Gansner et al.’s network simplex for layering. Crossing reduction follows Schulze, Spönemann & von Hanxleden, with crossings counted by Barth, Jünger & Mutzel’s accumulator tree. Nodes are placed by Brandes & Köpf, with the corrections Brandes, Walter & Zink published in 2020, and edges routed orthogonally after Weber. Beside the layered algorithm sit tree, radial, force, stress, packing and the rest: fourteen algorithms in about five thousand lines.

The reference suite works the way json-simple’s does against jzon. It holds a thousand random graphs, each laid out by ELK itself, and both engines face the same four checks: finite coordinates, no overlapping siblings, every edge routed to its endpoints, children inside their parents. trellis passes all four on **985** of the thousand; ELK on 515, with 96 errors and 8 timeouts. The fifteen trellis misses are mostly `fixed` layouts whose given positions overlap. Where ELK’s own layout passed, layered graphs come out with fewer crossings and no edges through nodes.

One honest gap shows on this site’s front page. trellis does not yet implement network-simplex *node placement*, the compact alternative to Brandes & Köpf from the same Gansner paper. The dependency graph is correct, but about a third wider than it was.

---

*modus / 134 commits*

## The kernel leaves CL-USER

Until this week every kernel function sat in the runtime’s table under its bare name, so a user’s definition could quietly replace it. The commit that changed this measured it: *a CL-USER (defun %format-impl …) … replaced the kernel’s function for the kernel’s own callers too: measured, that one DEFUN broke FORMAT and killed the script*. Internals now live in `MODUS-INT`, SBCL’s `SB-INT` in spirit. The public surface is `MODUS`, and the networking, SSH, crypto, runtime and board code each have a package of their own. Redefining a kernel function is still possible, but you have to name `MODUS-INT::` to do it.

The collector became generational, first on the Pi Zero board image in emulation, then on hosted aarch64 and x86-64. The design doc keeps its own measurement honest. Tenuring 42.7 MB during a remote-desktop drag cut the mean pause from 242 to 129 ms and the time spent paused from 4.9% to 1.0%. The frame rate did not move, *so GC was not what limits a drag*. A card table and write barrier followed, so minor collections scan only dirty cards.

The ports went down-market. The i386 image builds with soft float and runs on a 486, where QEMU’s 486 model traps every SSE2 instruction. It matched SBCL on 3,000 of 3,000 tests, its float battery came out identical to the hard-float image’s, and a scan counted no x87 or SSE instruction executed. An x87 mode covers the 486DX through the Pentium III. The full CL image boots on 68k, with a soft-float build for the FPU-less 68LC040. Running last week’s ANSI suite on i386, arm32 and RISC-V found real bugs: arm32 and RISC-V asked for the working directory with x86-64’s syscall number, so the suite could not find its own files. The self-hosting diverse double-compile is a byte-identical fixpoint again, and now part of the merge gate.

---

*the rest / cl-deposits, operandi, cl-sev-snp, scribe*

## Also this week

**cl-deposits**’ 31 commits are mostly rules. A ledger now carries a named ruleset (DEP-18), October’s convicting rules apply only to ledgers whose ruleset enforces them, and balance commitments are emitted and verified. Its red team runs the new rulesets against a live devnet with stochastic deploys. Two new documents, `CHAOS.md` and `ADVERSARIAL.md`, are backlogs of faults and attacks not yet tried.

**operandi** ported its macOS sandbox launcher to Lisp, with a rules language per user and per project. Its `Glob` tool now matches in Lisp, because the bash version always found nothing on macOS. **cl-sev-snp** reads Tinfoil’s version-3 attestation documents. **scribe** stopped drawing each glyph at a different fraction of a pixel, which straightened every label in trellis’s examples.

---

*the gates / 31 October*

## A pin that moved alone

One repository was failing: kiln’s image build, red since the 25th. A lock update on the 24th moved glass to a commit that calls `SCRIBE:WITH-THREAD-STATE`, and left scribe pinned at a commit from 12 August that predates it. kiln’s lock now pins scribe’s current master, and the image builds again. `bin/siblings` checks that every dependency is pinned, but not that the pins agree, so it could not have caught this.

As of publication: **63 repositories, 43 green, none failing, 20 without a workflow.** illatio and the rewritten trellis have none yet.

---

**Method.** Commits by *author* date, 25 to 30 October 2026, across the 62 repositories cloned from the modus-lisp organisation, counting each repository’s default branch as the union of the local branch and `origin/`. Produced with `bin/week 2026-10-31 --since 2026-10-25 --log`; the span starts on the 25th because № 33 ran through the 24th. Ten modus commits authored on 6 October were merged on the 30th. They fall in № 31’s span, which was published while they sat on a branch, so no ledger has counted them. trellis’s 35 commits in № 33 were the port; that history was replaced and those commits no longer exist on `main`. 43 unmerged commits on `illatio/cuda`, `modus/rmb-on-main` and `modus-lisp-fixes`, `cl-deposits/soak-v6` and `kiln/modus-lisp-fixes` are not counted, nor is kiln’s lock fix, dated today. The performance and conformance figures are the repositories’ own, from their READMEs and commit bodies. The green and failing figures count the organisation’s 63 repositories, which includes the profile repository this ledger does not.

**Outside the log.** The author’s nostr notes were read with `bin/notes 2026-10-31 --since 2026-10-25`. Both quotations about illatio’s speed are from them.

[← All issues](https://modus-lisp.github.io/issues/) · [crier](https://modus-lisp.github.io/)
