# A heuristic is a bug you haven't hit yet

**This Week in Modus №8** · 25 April – 1 May 2026 · 159 commits · 1 repo

<https://modus-lisp.github.io/issues/2026-05-02/>

Some tests passed or failed depending on where the compiled code happened to sit in memory. A fuzzer that inserts do-nothing instructions found fourteen of them, and underneath were three type predicates that had never actually checked anything — they had been ruling things out.

| Commits | Source files | Lines added | Tests that flipped |
|---|---|---|---|
| 159 | 24 | +11k | 14 |

---

*modus / 26–27 April / a62eff6*

## Tests that flip when the code moves

The harness stopped losing tests last week, which made a subtler problem visible: some results were not stable. A change with no semantic content — anything that made the binary a little larger or a little differently arranged — could turn a passing test into a failing one somewhere else entirely. That is the worst kind of bug to chase, because every attempt to look at it moves it.

So rather than chase it, the project built an instrument. A fuzzer compiles the test binary nine times, injecting *N* do-nothing instructions at every function call site for *N* from zero to eight, and diffs which tests change verdict. One byte at thousands of call sites shifts the whole binary by thousands of bytes per increment.

> The pass count dips at N = 1, 2 and 4 and recovers fully at 0, 3, and 5 through 8. That non-monotonic pattern is the signature of an alignment bug — something needing to sit at a particular address modulo *K* — and not of general instability. Only 14 tests flip; the other 9,156 are stable.

> — modus — a62eff6

Fourteen out of nine thousand is a diagnosis rather than a catastrophe, and the shape of the graph turned a vague fear into a specific hypothesis. There is also an honest retreat in the middle of the week: an attempt to print diagnostics from inside the suspect function added about thirty bytes to it, and thirty bytes times every call site shifted the binary enough to change the failure. The commit names the property — any observation changes what is observed — and stops, listing what would actually work instead.

---

*modus / 26–27 April / 7203e19, fd27d1c, 9a11f24*

## Three predicates that were guessing

The root causes, when they came, were all the same mistake. Asking “is this a function?” or “is this a vector?” had been implemented not by looking at the object but by eliminating everything it wasn’t.

*modus — how `vectorp` used to work*

```
(and (not (consp obj)) (not (null obj)) (not (integerp obj))
     (not (characterp obj)) (not (eq obj t)))
```

Every heap-allocated object survives that gauntlet, so closures, bignums, ratios, generic functions and packages were all vectors as far as `vectorp` was concerned. Replacing it with a real check on the object’s subtag *lost* one test and fixed 21,057 correctness checks — a trade the commit message states in its own subject line rather than burying.

`functionp` had the same shape and a sharper failure. It ruled out functions by asking whether the value was an integer — and a raw function address *is* an integer more than half the time, depending on the low bits of wherever the function landed. So `functionp` returned the wrong answer for a real function about 57% of the time, and which 57% depended on the layout of the binary. That is the fragility, precisely.

The third is not a predicate but has the same character: the instruction that reads an object’s subtag was emitting a bare load from nine bytes before the pointer, with no check that the pointer was a heap object at all. A fixnum or an immediate sends that load to a random address. Worse, the immediate for `T` happens to end in the same nibble that marks a heap pointer, so reading its subtag lands exactly one byte past the end of a mapped page — which is why the crashes clustered in CLOS tests, where comparing a result to `T` is routine. Making the deref tag-safe closed that whole family.

Four tests still flipped on pure instruction injection at the end of the week, with the hypotheses written down and unresolved. One earlier suspect was retired as a misdiagnosis, and two attempted fixes — full sixteen-byte alignment, and caller-saving in the comparison slow path — were reverted because each grew the code enough to move the problem somewhere else.

---

*modus / 159 commits, most with a number in the subject*

## A number on every commit

The other thing to notice about this week is what the log looks like. Almost every subject line now ends in a measured delta, because there is finally a harness that can produce one:

*modus — a sample of the week's subjects*

```
&rest in funcall: callee-side rest-list packing prologue      +426
Closure auto-capture enabled                                  +290
format: rewrite ~^, ~[~], ~*, ~R, ~(...~), brace-scanner      +146
format: ~:{ and ~:@{ — list-of-sublists iteration             +141
aref/aset/array-length/stringp peel array wrappers            +114
defun wrappers so #'cons etc are callable                      +92
compiler: fix &key parameter order in preprocess-params        +90
equalp: char-equal for chars + element-wise for arrays         +71
```

Two of those are worth expanding. Closures built from a `&rest` lambda — which is what `formatter` produces — worked when called by name and corrupted memory when called through `funcall`, because a direct call packs the rest list at compile time and `funcall` cannot: it does not know the callee’s signature. The fix moves the packing to the callee, which reads the argument count from a slot the caller always sets. That is 426 tests from one calling-convention change.

And a rule from the standard turns up over and over in the week’s subjects: when a keyword argument is supplied twice, the leftmost one wins. It had to be fixed in `member`, `assoc`, `find`, `position`, the substitution family and the shared keyword parser — each time worth a handful of tests, and never something anyone would have thought to check without being told.

---

*modus / 28–29 April*

## FORMAT, and arrays that are not flat

`format` got most of a rewrite: the brace scanner, iteration over a list of sublists, the escape directive checking the right enclosing list, conditional and goto directives, case conversion, the integer directives with their minimum column, pad character, comma grouping and sign handling, plurals, and right-aligned padding. It is the single largest block of tests recovered in the week, which is unsurprising — a great deal of the ANSI suite prints something and compares the string.

The other structural change is that an array stopped being a flat run of memory. Once `:fill-pointer`, `:adjustable` and displacement exist, an array is a wrapper around storage, and every function that touches one has to look through it. So `aref`, `aset`, `array-length` and `stringp` learned to peel wrappers, and then so did `elt`, `reduce`, `search`, `coerce`, `concatenate`, `every`, `some`, `find`, `position` and `count`. A related correction runs through the same commits: a string’s elements are characters, not the bytes underneath them, and several functions had been handing out the numbers.

---

*also this week*

## Two smaller things

- **CLOS became runtime-defined.** `defclass`, `defgeneric` and `defmethod` are now handled by `eval` as well as by the compiler, `make-instance` is a real top-level function, and effective slots are computed from the class precedence list — the beginning of a CLOS that is a runtime object system rather than a compile-time trick.

- **The ELF output grew symbols.** Section headers, a symbol table and a string table, with one entry per function pointing at its real prologue. Nothing in Modus needs that; a debugger does. It is the difference between an address and a name in every crash report from here on.

---

**Method.** Commits by *author* date, 25 April to 1 May 2026, across the git repositories in the modus-lisp workspace — at this point, one. The per-commit test deltas are the ones the commits report. Line counts exclude generated build artefacts. Produced with `bin/week 2026-05-02 --log`.

[← All issues](https://modus-lisp.github.io/issues/) · [crier](https://modus-lisp.github.io/)
