# Type system review — session notes / handoff

Branch: `bugfix/type-system-review`. This file is a handoff for picking this work back up in a
fresh session with no prior conversation context.

## What Jype is

Jype (`jype-main`) is a Java 24 type-system model implementing the JLS SE24 spec: expression
type-checking, overload resolution, generic inference, etc., built on a constraint-solver library
called **Almonds** (`honeyroasted.almonds`), which is a *separate sibling project* the user owns
locally at `C:\Users\johnr\IdeaProjects\Almonds` (GitHub: `HoneyRoasted/Almonds`, published via
JitPack as `com.github.HoneyRoasted:Almonds:<version>`). `jype-stub` is a YAML-based test-fixture
module used by some tests (`YamlDslTests`).

## What this session did

Earlier work (before this file existed) found and fixed ~10 bugs in the solver (NPE, ambiguity
mis-detection, LUB variance bugs, missing vararg support, a `ConstraintBranch` identity-collision
bug that required adding `Constraint.identity`/`orderedIdentity` to Almonds, etc.) — all covered by
tests already in the repo.

Then the user gave the directive **"fix everything except: numbers 3, 4, 5, 7, 13, and 16"** from a
12-ish item feature-gap list. Excluded (out of scope, do not touch unless asked): operator
expressions, array-creation expressions, conditional expressions (`?:`), anonymous class
instantiation, switch-expression modeling, unchecked-conversion tracking.

**All 12 non-excluded items are done, tested, and the full suite (110 tests) is green.** In order:

1. Array constructor references (`T[]::new`).
2. Intersection-typed lambda/method-ref/ctor-ref targets (`(Runnable & Serializable) ...`).
3. `findClassTypes` BFS bug + `JIntersectionType` missing `knownDirectSupertypes()` override (was
   silently returning empty, breaking subtype checks/LUB/member-lookup through intersection types).
4. Protected-access refinement (JLS §6.6.2): qualifying-expression-type check added to
   `JClassType.accessFrom(other, receiverType)`.
5. Qualified/unqualified `super` invocation: new `JExpressionInformation.SuperReference` +
   handling in `JReduceMethodInvocation`.
6. "Pertinent to applicability" (JLS §18.5.2): `JReduceLambdaExpression` no longer eagerly rejects
   implicitly-typed lambdas whose SAM parameter types are still-unresolved inference variables.
7. General override-validity checker: new `JCheckOverrideValidity` operation (JLS §8.4.8.3).
8. `*Throws` constraint reducers (JLS §18.2.6/§8.4.6) — **see "Known gaps" below, only partially
   done**.
9. Capture conversion triggering (JLS §5.1.10): `JMemberLookup.captureConvert` wired into
   `JReduceMethodInvocation`/`JReduceGetField`/`JReduceInvocationRefExpression` — **see gaps below**.
10. Explicit casting (JLS §5.5): new `JExpressionInformation.Cast` + `JReduceCast`.
11. `instanceof` type comparison (JLS §15.20.2): new `JExpressionInformation.InstanceOf` +
    `JReduceInstanceOf` (reuses the same castability rule as explicit casting).
12. Generic-vs-generic most-specific-method comparison (JLS §18.5.4) — **see gaps below, this is a
    pragmatic approximation, not the full spec procedure**.

Item 12's investigation also surfaced and led to fixing/refactoring things in **Almonds itself**
(see next section) — this was the deepest rabbit hole of the session.

## Session 2: operator, conditional, and array-creation expressions

Picked back up from "Known gaps" item 5 below. User explicitly scoped this session to 3 of the 5
originally-excluded expression shapes: **operator expressions, conditional expressions (`?:`), and
array-creation expressions** (anonymous class instantiation and switch-expression modeling are
still not started — see updated item 5). Before implementing, verified with the user that each new
shape actually needs its own `JExpressionInformation` type rather than reusing the existing (and,
it turns out, entirely unused-elsewhere) `JExpressionInformation.Multi` — Multi computes an
*intersection* of homogeneous children with no side-data, which doesn't fit any of these (kind
dispatch, LUB/promotion instead of intersection, or a result type that's independent of the
children entirely for array creation).

**All 3 done, tested, full suite green (152 tests, up from 110).** New shapes, all in
`JExpressionInformation.java`, each with a dedicated reducer in
`system/solver/constraints/reduction/`:

- **`UnaryOperator`** (`+ - ~ !`) and **`BinaryOperator`** (arithmetic incl. String `+`
  concatenation, shift, relational, equality, bitwise/logical, `&&`/`||`) — JLS §15.15/§15.17-24.
  `++`/`--` deliberately not modeled (no assignable-lvalue notion in this expression-only model).
  New shared numeric-promotion helper: `system/solver/JOperatorTypes.java`.
- **`Conditional`** (`?:`) — JLS §15.25. Boolean/numeric/reference merge, including the
  byte-and-short-merges-to-short special case and the constant-int-narrows-to-byte/short/char rule;
  poly branches (a lambda or method/constructor reference on either side) instead propagate the
  outer target type straight through to both branches per JLS's own poly-conditional-expression
  rule, rather than trying to compute a merged type at all.
- **`ArrayCreation`** (dimension form `new T[e1][e2]`, optionally with trailing dimensionless
  brackets, and initializer form `new T[]{...}`) plus **`ArrayInitializer`** (the bare
  `{e1, e2, ...}`, meaningful only nested inside an `ArrayCreation` or another `ArrayInitializer`).
  Dimension-expression legality turned out to need *no* bespoke logic at all — the existing
  `ASSIGNMENT`-context compatibility check against `int` already implements JLS §15.10.1's "char,
  byte, short, or int, boxed-unboxed but no narrowing from long" exactly.

**Important architectural finding, worth internalizing before adding more shapes**: this codebase
has a hard, codebase-wide contract that `isSimplyTyped() == true` must guarantee
`getSimpleType()` is always present (several reducers — `JReduceSimplyTypedExpression`,
`JExpressionSimplyTyped`, `JReduceGetField`, `JReduceMethodInvocation`,
`JReduceInvocationRefExpression` — call `.get()` on it unchecked). The first draft of
`UnaryOperator`/`BinaryOperator` made `isSimplyTyped()` structural ("operand is simply typed"),
which is *not* the same as "legal" (e.g. `~true` has a simply-typed operand but no valid result
type) — this violated the contract and crashed those reducers with `NoSuchElementException`. Also
discovered along the way: **Almonds' `ConstraintMapper.Unary.accept(branch)` runs every registered
mapper's filter+accept against the branch every pass regardless of list order** — multiple mappers
*can* legitimately fire on the same constraint in the same pass (this is how `JReduceCast` and the
generic `JReduceSimplyTypedExpression` already both harmlessly process the same `Cast` constraint
today); mapper list ordering is not a "first match wins" dispatch mechanism, so a new shape can't
rely on being listed earlier to shield a later generic reducer from ever seeing it.

The fix: `UnaryOperator`/`BinaryOperator`/`Conditional` are **never simply typed** (`isSimplyTyped()`
defaults to `false`, matching `Lambda`/`Instantiation`/`MethodInvocation`'s existing convention for
"needs real solving to type") and each got a `Typed` wrapper for consistency (though, like `Multi`'s
`toSimple()`, nothing in the codebase actually calls `.toSimple(...)` anywhere — it's unused
scaffolding for all these shapes today). Their dedicated reducers resolve each operand two ways:
directly when already simply typed (re-asserting the operand's own `ExpressionCompatible` so any
nested cast/operator legality still gets threaded through), or — mirroring the existing
`JReduceGetField`/`JReduceMethodInvocation` pattern for a non-simply-typed receiver/source — via the
operand's own sub-solve against a fresh metavariable (`STRICT_INVOCATION` context, resolved with
`JResolveBounds.findInstantiation`, merged back in with a `combine(ConstraintBranch, Snapshot)`
helper copied from those same two files). `ArrayCreation` didn't need this: unlike the operators,
its own type is fixed (`componentType` + dimension count) independent of whether the dimension
expressions/initializer are themselves legal, so — like `Cast`/`InstanceOf` — it's always simply
typed, with legality checked as an unrelated side constraint.

**New gap surfaced by this session** (same spirit as items 1-4 below — call it out rather than
silently under-deliver): a bare lambda or method/constructor reference used directly as a
`UnaryOperator`/`BinaryOperator` operand isn't supported (its sub-solve against an unbounded
metavariable can never succeed, since those forms require an actual target functional-interface
type to resolve at all) — not a real-world loss since that's not legal Java either (`-(() -> {})`
isn't valid), but worth knowing if this pattern gets reused for a shape where it *would* matter.
`Conditional` handles the equivalent situation correctly by special-casing poly branches instead of
trying to sub-solve them standalone.

## Almonds-side changes this session

- Added `Constraint.identity`/`orderedIdentity` (from earlier work, already committed before this
  file existed).
- **`ConstraintSolver.postSolve(Consumer<ConstraintTree> hook)`**: a builder method that registers a
  hook run once after `solve()` finishes applying all appliers. Jype's `JTypeOperationsImpl.inferenceSolver()`
  uses this instead of subclassing `ConstraintSolver` to override `solve()`.
- **`ConstraintMapperApplier.Type` is now chosen per use case in Jype, not hardcoded.** Almonds has
  three algorithms (`ConstraintMapperApplier.of(mappers, type)`):
  - `ORDERED` — intentionally short-circuits to a single true/false verdict as soon as any tracked
    branch resolves `TRUE`. Correct for "just tell me if this is compatible" checks.
  - `UNTRIMMED` — fully processes every branch that isn't trimmed (trimmed only happens on `FALSE`,
    never on `TRUE`, so multiple simultaneously-`TRUE` candidate branches legitimately coexist and
    all need full processing). Correct whenever multiple candidate branches must all be resolved
    (e.g. overload resolution with several applicable candidates).
  - `EXHAUSTIVE` — reprocesses literally everything every pass, including already-`FALSE`/trimmed
    branches. Primarily for debugging; the user expects this is almost never the right choice.
  - **Do not "fix" `OrderedConstraintMapperApplier`'s short-circuit** — that was tried and reverted
    this session (see git history: commits `8fdee75` then `8587758` revert on the Almonds repo). It
    is working as designed. The actual bug was that Jype's solver was using `ORDERED` in places that
    need `UNTRIMMED` (see `JTypeOperations`/`JTypeOperationsImpl` below).
- `JTypeOperations` interface: each method that returns a `ConstraintMapperApplier`
  (`reductionApplier`, `resolutionApplier`, `verifyApplier`, `incorporationApplier`,
  `compatibilityApplier`, `initialBoundsApplier`) now has a `(ConstraintMapperApplier.Type)`-taking
  overload; the old no-arg versions are `default` methods delegating with "the correct algorithm
  for that use case" (`UNTRIMMED` for all of them except `compatibilityApplier`, which is `ORDERED`
  since it's used for plain true/false compatibility checks). `JTypeOperationsImpl` only implements
  the new overloads, building a fresh applier on demand from plain `List<ConstraintMapper>`
  constants (renamed from the old `*_APPLIER` constants to `*_MAPPERS`).

Current Almonds version: **0.0.29**, published and resolvable via JitPack
(`com.github.HoneyRoasted:Almonds:0.0.29`). `jype-main/build.gradle` already points at it; no
`mavenLocal()` leftover in `buildSrc/shared.gradle`.

### Workflow for future Almonds changes (established this session)

1. Edit source in `C:\Users\johnr\IdeaProjects\Almonds`.
2. Bump the **patch** version only in `Almonds/build.gradle` (`version 'X.Y.Z'`) — project is
   pre-1.0, not following semver yet, per the user.
3. `cd Almonds && ./gradlew test` (Almonds has its own test suite) then `./gradlew publishToMavenLocal`.
4. Temporarily point Jype at the local build to verify before publishing for real:
   - Add `mavenLocal()` as the **first** entry in `Jype/buildSrc/shared.gradle`'s `repositories {}`.
   - Change `jype-main/build.gradle`'s dependency to `api("honeyroasted.almonds:Almonds:X.Y.Z")`
     (note: **different groupId** than the JitPack coordinate — local publish uses the project's own
     `group 'honeyroasted.almonds'`, not `com.github.HoneyRoasted`).
   - `cd jype-main && ../gradlew clean test` — confirm all pass.
5. Commit + `git push origin main` in Almonds. Per the user: pushing a commit with a new version
   number to `main` triggers CI which creates a matching git tag automatically.
6. JitPack builds lazily on first request and can take a few minutes. If
   `../gradlew compileJava --refresh-dependencies` immediately fails with "Could not find
   com.github.HoneyRoasted:Almonds:X.Y.Z", check `https://jitpack.io/api/builds/com.github.HoneyRoasted/Almonds/X.Y.Z`
   — if it reports `"status":"tagNotFound"`, the CI-created tag hasn't reached JitPack's index yet;
   hitting `https://jitpack.io/com/github/HoneyRoasted/Almonds/X.Y.Z/` directly (or just retrying the
   Gradle build after a bit) pokes JitPack into discovering/building it. Re-check with the same API
   URL until `"status":"ok"`.
7. Once resolvable, switch `jype-main/build.gradle` back to
   `api("com.github.HoneyRoasted:Almonds:X.Y.Z")` and remove the temporary `mavenLocal()` line from
   `buildSrc/shared.gradle`. Run the full suite once more against the real published artifact.

## Session 3: partial-item cleanup + fixing a real hang bug in the resolution algorithm

Picked up the (pre-Session-3) items 1-4 from "Known gaps" — chained invocation, lambda-body throws,
capture conversion, and JLS §18.5.4 — excluding item 6 (cosmetic, already fixed directly by the user
before this session). The "Known gaps" list below has since been renumbered to drop what got fixed.
Results:

- **Item 1 (chained invocation) - done.** Added `JReduceMethodInvocationChainTests.java` (4 tests:
  a simple `foo().next().value()` chain, wrong-return-type rejection, unknown-method rejection on
  the resolved receiver, and overload resolution through a chained receiver). All pass with **no
  code changes needed** - the existing non-simply-typed-receiver branch already worked correctly,
  it just lacked test coverage. Removed the stale `//TODO test this rigorously` comment.

- **Item 2 (lambda-body throws-checking) - done.** `JReduceLambdaExpression` now special-cases a
  lambda body that is itself a method/constructor invocation (`JExpressionInformation.Invocation`):
  instead of deferring to the ordinary `body → R` compatibility constraint, it eagerly resolves the
  body (directly if simply typed, otherwise via its own sub-solve against a fresh metavariable -
  same pattern as `JReduceBinaryOperator`/`JReduceConditionalExpression` from Session 2), reads the
  `JTypeContext.ChosenMethod` off each resulting true branch, and runs it through the same
  `JMemberLookup.addThrowsConstraints` check already used for plain method/constructor references,
  discarding any candidate whose checked exceptions aren't covered by the SAM's throws clause. Works
  for both value-returning and void SAMs (void case resolves the body against an unconstrained
  metavariable purely to discover the chosen overload, since a void SAM doesn't otherwise care about
  the body's value). New tests: `JReduceLambdaThrowsTests.java` (8 tests covering value-returning
  and void SAMs, method and constructor invocation bodies, both directions, plus a regression check
  that non-invocation bodies like `() -> "hello"` are unaffected). Verified the negative-case tests
  actually fail without the fix (temporarily disabled the new code path, confirmed 3 of the 8 tests
  failed as expected, restored it).

- **Item 8 (new gap from Session 2, "bare lambda/method-ref as operator operand") - verified, no fix
  needed.** Confirmed `-(() -> 0)` and `String::valueOf + 5` both correctly resolve to `false`
  rather than hanging (added `@Timeout`-guarded regression tests to `JReduceUnaryOperatorTests`/
  `JReduceBinaryOperatorTests`). Not valid Java either way, so this was just a safety check.

- **Item 3 (capture conversion) - re-scoped; found and partly fixed a worse bug, root cause of the
  remaining part is understood but not fixed.** The *original* scenario described in this item
  (`box.set(5)` where `box: Box<? extends Number>`) turns out to **already be correctly rejected** -
  direct testing showed `solved = false` as expected. So the specific "consumer-position soundness"
  concern as originally described doesn't reproduce. However, while probing around it, found that
  **`box.set(box.get())` (passing a captured wildcard's own producer output back as the consumer
  argument - always legal in real Java, since it's provably the same unknown type) hangs the solver
  indefinitely** - not slow, a genuine non-terminating loop that even an OS-level `timeout` kill
  after 30-60s was needed to stop (JUnit's `@Timeout` can't interrupt it - it's a tight loop with no
  interruption checks). Root-caused via a from-scratch standalone repro (bypassing Gradle/JUnit
  entirely - `java -cp <resolved classpath> HangRepro`, classpath built once from
  `~/.gradle/caches/modules-2` + the compiled class dirs, see scratchpad if it still exists) plus a
  throwaway diagnostic `ConstraintMapper.All` registered first in the mapper lists that dumped
  `branch.size()` and a per-constraint-type breakdown every pass:
  - **Found and fixed two real, unrelated bugs** while investigating (both confirmed via direct code
    reading, not just the hang): `JIncorporationEqualSubtype.java` and `JIncorporationEqualEqual.java`
    each had a copy-paste bug in their "substitute a solved metavariable into both sides of an
    Equal/Subtype constraint" branch - both computed `rlRes` (substituted left) and `rrRes`
    (substituted right) correctly, but then called `addSubtypeBound`/`addEqualBound` with `rlRes`
    passed for **both** the left and right argument instead of `rlRes, rrRes`. Fixed both (one-line
    diffs). Confirmed via the full suite (still green) but **did not** by themselves fix the hang -
    this specific repro doesn't happen to exercise the code path where the bug would have mattered.
  - **Found and partially fixed a second, more fundamental bug**: `JResolveBounds`'s "the bound set
    contains a bound of the form `G<...,αi,...> = capture(G<...>)`" fallback (JLS §18.4's
    capture-avoiding resolution path, `hasCapture` branch) computes a candidate resolution for each
    dependent metavariable using fresh "shadow" variables, but only ever records that resolution as
    an **`ASSUMED`**-status `Equal` bound - never as `TRUE`. Since `findInstantiation()` (the
    function that decides "is this metavariable resolved yet") only counts `Equal`/`Instantiation`
    bounds with `status.isTrue()`, those `ASSUMED` bounds are invisible to it, so
    `foundAllInstantiations` can never become true through this path - meaning `JResolveBounds`
    (a `ConstraintMapper.All` whose filter is just "branch is true", so it never stops re-firing)
    keeps re-invoking the *entire* fallback on every later pass, each time minting a brand-new set
    of fresh "shadow" variables (never reused/cached) and re-running a nested
    `incorporationApplier()+reductionApplier()` consistency-check solve against an ever-growing copy
    of the branch's constraints. **Fixed**: when a captured metavariable *does* have a proper
    (concrete) lower or upper bound available, the fallback now also records a `TRUE`
    `Instantiation(mvt, resolvedType)` once the nested consistency check passes, so
    `findInstantiation` can see it and the branch actually converges. This is a real, verified
    improvement (full suite still green) for the case where a captured variable's resolution *can*
    be pinned down from other constraints.
  - **That fix alone does not close the `box.set(box.get())` hang**, because in that specific
    scenario neither `alpha_get` nor `alpha_set` (the two independently-captured metavariables -
    `get()` and `set()` each call `JMemberLookup.captureConvert` separately even though they share
    the same syntactic receiver, so they get *different* fresh metavariables) ever has a *proper*
    (non-metavar) lower/upper bound - they're only ever related to *each other* via
    `Subtype(alpha_get, alpha_set)`, an under-determined system where "resolving to a concrete type"
    isn't actually the right answer (real Java doesn't resolve `T` here either - it just needs to
    verify structural compatibility with `T` left opaque throughout). So `capturedResolutions` stays
    empty for both variables in this case, and the underlying issue remained: the nested
    nothing-cached consistency-check solve re-running on an ever-growing input, combined with the
    `O(n²)` pairwise incorporation rules (`JIncorporationSubtypeSubtype`/`EqualSubtype`/`EqualEqual`)
    amplifying that growth each round. Confirmed via the diagnostic instrumentation: constraint
    count grew 10 → 31 → 120 → 489 (stable for ~4 raw mapper-passes at each level before jumping,
    consistent with one `JResolveBounds` re-invocation per jump) with the above fix in place,
    identically to without it.
  - **Root cause and actual fix**: `JResolveBounds` is a `ConstraintMapper.All` whose filter is just
    "branch is true" - it never stops re-firing on its own, and had no memory of "I already verified
    this exact capture-dependent subset is consistent." So every single pass, for as long as the
    branch stayed true (forever, in this scenario), it re-ran the *entire* capture fallback from
    scratch: minting a brand-new never-reused set of fresh "shadow" variables, then merging the
    nested temp-tree's *entire* derived bound set - including bounds mentioning those throwaway
    shadow variables - back into the main branch. Since those merged-in bounds are themselves
    metavariable-involving `Equal` bounds, they re-entered `discoverDependencies()`'s graph on the
    *next* pass, growing `varsAndDeps`/`subset` to include the shadow variables too, which then also
    needed their own (newly-generated, never-reused) shadow variables the pass after - an unbounded
    chain, independent of and in addition to the O(n²) incorporation growth within any single pass.
    Fixed with two changes in `JResolveBounds.accept()`:
    1. **Memoization**: right after computing `subset` and `hasCapture`, build
       `Constraint.identity(subset)` (the same "cheap identity-based memoization" facility added to
       Almonds earlier in this project specifically for this kind of "don't redo a derivation"
       need - see the `Constraint.identity`/`orderedIdentity` entry under "Almonds-side changes"
       below) as a marker; if it's already `TRUE` on the branch, return immediately - nothing left to
       derive for this subset. Once the nested consistency check succeeds, this marker gets recorded
       as `TRUE` (alongside any `capturedResolutions`), so every later pass short-circuits instantly.
    2. **No more leaking scaffolding into the main branch**: when `hasCapture`, the final merge no
       longer does `snap.constraints().forEach(branch::add)` at all (that path is now only taken for
       the non-capture "candidates" case, which already worked correctly). Only the concrete
       `capturedResolutions` (if any) and the consistency marker get added to the real branch - the
       throwaway shadow variables and their bounds live and die entirely inside the temp tree,
       never entering the main dependency graph.
  - **Verified fixed**: the standalone non-Gradle repro (`java -cp <classpath> HangRepro`, built
    from `~/.gradle/caches/modules-2` jars + the compiled class dirs - regenerate if needed, nothing
    from it persists in the repo) now solves `box.set(box.get())` in well under a second instead of
    hanging. Re-enabled the test as `setWithResultOfGetOnExtendsWildcardTypeChecks` in
    `JReduceMethodInvocationCaptureTests.java` (kept a `@Timeout` as a tripwire, not because timing
    out is an acceptable outcome), and added `setOnExtendsWildcardWithUnrelatedValueIsRejected` as
    the matching negative case (`box.set(anInteger)` must still be rejected - confirms the fix didn't
    just make everything vacuously true). Full suite: **168/168 passing, 0 skipped, 0 failures**.

1. **Full JLS §18.5.4 generic-vs-generic most-specific procedure - done this session (Session 4).**
   The previous attempt (described below, kept for context) was reverted because it broke a test
   whose *expected* outcome was itself never verified against real `javac` - it was assumed from
   memory of the spec. This session got the literal §15.12.2.5/§18.5.4 text (Oracle's docs.oracle.com
   page for chapter 15 is too large for the sandboxed `WebFetch` tool to render in full - it silently
   truncates mid-chapter, well before reaching §15.12 - so the fix was to `curl` the raw chapter-15
   HTML directly into the scratchpad and `sed`-strip tags/entities to extract just §15.12.2.5's text
   locally) and, critically, **verified every expected outcome against real `javac` (JDK 24, found at
   `/d/JDKs/24+36/bin/javac`)** with tiny standalone repros before writing a single line of
   implementation or test code - see "Workflow preferences" below, now promoted to a hard rule.
   - The key spec subtlety the previous attempt missed: §15.12.2.5's "is m1 more specific than m2"
     test is directional, and only invokes §18.5.4 (fresh inference variables for the *second*
     method's own type parameters) when **m2** (the second/target method in that specific direction)
     is generic - if m2 is non-generic, it's a direct pointwise `Si <: Ti` check with no inference at
     all. Critically, **m1's own formal parameter types are never substituted, in either role** - even
     when m1 is generic, its type parameters stay as their own opaque, declared-bound type variables,
     never resolved to whatever they happened to be inferred as for this specific call. The previous
     attempt used fresh variables for m2 (correct) but still used m1's actual-inferred-for-this-call
     types instead of m1's raw declared types (incorrect) - that mismatch is exactly what made the
     previous attempt's regression case behave symmetrically/ambiguously instead of picking a winner.
   - Ground truth, confirmed with real javac (not assumed): `<T> String pick(T a, T b)` vs
     `String pick(Object a, String b)`, called `pick("x","y")`, resolves to the **non-generic**
     overload - the *opposite* of what the old (now-corrected) regression test asserted. The old test
     encoded a wrong expectation that had never been checked against a real compiler; it passed only
     because the old implementation's bug happened to produce the same (wrong) answer the test
     expected. Fixed the test (renamed `pickNonGenericWinsOverGeneric`) plus added two more
     javac-verified cases: `fooGenericWinsOverNonGeneric` (a generic overload correctly beating a
     non-generic one) and `listWinsOverCollectionForBothGenericOverloads` (genuine generic-vs-generic,
     `<T> m(List<T>)` vs `<T> m(Collection<T>)`) - all in
     `JResolveMostSpecificMethodGenericTests.java`.
   - Rewrote `JResolveMostSpecificMethod.java`: `declaredParameters(cm)` (renamed from
     `effectiveParameters`, dropped the old `genericResolver` entirely) now returns a method's raw
     declared parameter types (vararg-expanded for arg count, but never substituted). `isMoreSpecific`
     implements §15.12.2.5's direct rule when m2 isn't generic; `isMoreSpecificGeneric` implements
     §18.5.4 itself - mints a fresh `JMetaVarType` + `Infer` constraint per m2 type parameter (seeds
     the initial bound set from declared bounds via the existing `JBuildInitialBounds` mapper, the
     same mechanism `JReduceMethodInvocation.createTypeParams` already uses for ordinary call-site
     inference), θ-substitutes m2's declared parameter types, short-circuits false on any proper-type
     `Ti` that isn't more specific, otherwise adds a `Subtype(Si, Ti)` constraint, then runs the whole
     thing through the ordinary `inferenceSolver()` (reentrant - safe, since the synthetic branch has
     no `ChosenMethod` tag so the solver's own `JResolveMostSpecificMethod.prune()` postSolve hook is
     a no-op on it) and checks for any surviving `TRUE` branch.
   - **Found and fixed a second, independent, previously-undiscovered bug while chasing down the
     `listWinsOverCollectionForBothGenericOverloads` test** (which initially failed with *both*
     directions spuriously true): `JReduceSubtype.identifySuperclass` had
     `ct.relativeSupertype(ct)` where it should have been `ct.relativeSupertype(target)` (a
     copy/paste-style bug - every other call site in the same file and in `JSubtypeGenericClass`
     correctly passes `target`). This one-line bug made `identifySuperclass` always report "found"
     (trivially, comparing a type against itself) whenever the search target type was itself a plain
     `JClassType`, completely ignoring what was actually being searched for - e.g. it would report
     that `Collection<T>` has a `List<...>` supertype, which is backwards. It only manifests for a
     `Subtype` constraint where the metavariable is nested *inside* a type argument rather than being
     the constraint's top-level type (the `left instanceof JMetaVarType || right instanceof
     JMetaVarType` fast path a few lines up in the same method catches the top-level case, which is
     why this had never been hit before) - exactly the shape §18.5.4's generic-vs-generic comparison
     produces (`List<T> <: Collection<alpha>`) and apparently nothing else in the existing suite did.
     Fixed with the one-line correction; full suite stayed green.
   - Full suite: **170/170 passing, 0 skipped, 0 failures** (168 baseline + net +2: three new/rewritten
     tests in `JResolveMostSpecificMethodGenericTests.java`, one old wrongly-asserting test replaced).

   **Follow-up, same session: closed the functional-interface gap flagged above.** §15.12.2.5 (used
   whenever `Ti` is a proper type - i.e. the common case, either `m2` isn't generic at all or that
   parameter position just doesn't mention `m2`'s type parameter) and §18.5.4's own elaborate bullet
   (used when `Ti` isn't proper) both have a separate branch for when `Si`/`Ti` are *both functional
   interface types* and *unrelated* (neither's interface is a super/subinterface of the other's) -
   compare their function types (JLS §9.9) instead of a plain `Si <: Ti`. Implemented the
   §15.12.2.5 version (`moreSpecificType`/`moreSpecificFunctionalInterface`/`moreSpecificReturnType`
   in `JResolveMostSpecificMethod.java`), which is what actually fires in both the "`Ti` proper" fast
   path inside the generic branch and the plain non-generic-`m2` direct comparison - i.e. it covers
   ordinary overloads like `void m(Runnable r)` vs `void m(Callable<String> c)`, the realistic case
   this pattern shows up in. Verified against real javac first (JDK 24): unrelated interfaces
   `StringMaker { String make(); }` vs `CharSequenceMaker { CharSequence make(); }`, called with
   `pick(() -> "hi")`, picks `StringMaker` since `String <: CharSequence` once compared by return
   type - added as `unrelatedFunctionalInterfacesComparedByFunctionType` in
   `JResolveMostSpecificMethodTests.java`; confirmed it fails without the fix (temporarily stubbed
   `moreSpecificType` back to plain `isSubtype`, reran, confirmed failure, restored) before trusting it.
   `findFunctionalMethod` (SAM lookup, JLS §9.8) was already implemented in
   `JReduceLambdaExpression` from earlier lambda work - widened to `public` and reused rather than
   duplicated.
   - **Still not covered - a narrower, deeper remainder of the same gap**: §18.5.4's *own* elaborate
     MTS/MTT bullet (reached when `Ti` is *not* proper, i.e. `m2` is generic **and** its own type
     parameter appears directly inside a functional-interface-typed parameter position, e.g.
     `<T> pick(Foo<T> f)` vs `<T> pick(Bar<T> f)` where `Foo`/`Bar` are unrelated functional
     interfaces) still falls back to a plain `Subtype(Si, Ti)` constraint rather than the equivalent
     constraint-generating function-type comparison. This is a materially rarer pattern (needs *both*
     candidates generic *and* the generic parameter to sit inside the functional-interface type
     itself) than the case that's now fixed.
   - **Also not modeled** (documented directly in the new code's javadoc, not just here): capture
     conversion of `Si` before taking its function type (the MTS vs MTS' distinction the literal spec
     draws - only matters when `Si` itself has a top-level wildcard-parameterized type argument);
     exact-method-reference-specific return-type bullets (falls back to plain `RS <: RT`); recursion
     into switch-expression result expressions (this codebase doesn't model switch expressions at
     all yet - see the "still not started" item below).

   <details><summary>Original (reverted) Session-3 attempt, kept for history</summary>

   The existing approximation in `JResolveMostSpecificMethod` (`genericResolver`/
   `effectiveParameters`) compared two candidates using the types *actually inferred for this
   specific call* (composing each branch's `Infer` bindings with its final metavariable
   instantiations) **on both sides** of the comparison. That session tried replacing that with what
   seemed like the more literal reading: when the "other" candidate (m2) is itself generic, compare
   using *fresh* inference variables for m2's type parameters instead of the types m2 happened to be
   inferred to for this call. This concretely broke a test that (as this session discovered) was
   itself wrong - the reverted attempt was actually closer to correct than the test it broke, but
   without spec access or a real compiler to check against, the session couldn't tell the difference
   and played it safe by reverting.

   </details>

2. **Still excluded, not evaluated** (from the original gap-review numbering, items 3/4/5/7/13/16):
   operator expressions, array-creation expressions, and conditional expressions (`?:`) are now
   **done** — see "Session 2" above. Still not started: **anonymous class instantiation**,
   **switch-expression modeling**, and **unchecked-conversion tracking**. These were skipped per
   direct user instruction, not because they're low-value — worth revisiting if there's appetite to
   keep going. Switch-expression modeling in particular will likely hit the same "no statement/arm
   model" wall noted in the lambda-body-throws item for lambda bodies.

## Workflow preferences established this session (worth following if continuing)

- Work one item at a time; after each, add/adjust tests, then run the **full** suite (not just the
  new test) before moving on.
- When claiming a fix, verify the regression test actually *fails* without the fix (temporarily
  revert, rerun, confirm failure, then restore) — don't just trust that a passing test after the fix
  proves the bug was real.
- **When implementing spec-derived behavior (JLS or otherwise), verify expected outcomes against a
  real, authoritative implementation before writing the test that asserts them - not just a careful
  reading of the spec text (established Session 4, after discovering a previous session's regression
  test encoded a wrong expectation nobody had checked against real `javac`).** This repo has a real
  JDK available locally (`javac`/`java` on PATH, JDK 24 confirmed at `/d/JDKs/24+36/bin/`) - for any
  overload-resolution/type-inference question, write the two-liner, `javac` it, run it, and use the
  actual output as ground truth rather than trusting memory or a close spec reading alone. Spec text
  is dense and easy to misapply (e.g. missing which of two methods plays which directional role in a
  procedure); a compiler is unambiguous.
- If the `WebFetch` tool truncates a large spec/doc page before reaching the section you need
  (chapter 15 of the JLS is large enough to hit this), fall back to `curl`-ing the raw HTML into the
  scratchpad directly and `sed`-stripping tags/entities (`sed -e 's/<[^>]*>//g'` plus a few `&entity;`
  substitutions) to read the exact section locally - don't guess from a truncated fetch or from
  memory.
- Prefer the smallest defensible fix; call out explicitly when scoping something down rather than
  silently under-delivering.
- Ask before any destructive/external action (force-push, git tag/release, restructuring build
  files permanently) — several checkpoints across sessions were explicit `AskUserQuestion` calls
  before continuing into deeper/riskier territory (e.g. before touching Almonds at all, before
  pushing to GitHub, before spending unbounded time debugging the Session 3 hang).
- When something hangs, reach for a standalone `java -cp <classpath> SomeMainClass` repro (no
  Gradle, no JUnit) as fast as possible rather than iterating through `./gradlew test` — Gradle's
  daemon architecture makes killing a genuinely hung test slow and sometimes unreliable (worker
  JVMs can outlive a killed client; `./gradlew --stop` plus checking `jps -l` for stray
  `GradleWorkerMain`/daemon processes is the reliable cleanup). `jstack <pid>` on a hung JVM (or a
  cheap `throw` after N iterations in a temporary instrumented reducer) finds the actual stuck
  location far faster than guessing.
- Current test count (pre-Session-5): **171 tests, all passing, 0 skipped** (`jype-main`, up from
  110 at the start of Session 1 — see "Session 2"/"Session 3"/"Session 4" above), plus Almonds' own
  test suite (small, in `Almonds/src/test`).

## Session 5: the three narrower §18.5.4/§15.12.2.5 remainders flagged at the end of Session 4

Picked up, in order, the three items explicitly called out as not yet covered at the end of
Session 4's "Follow-up" note: (1) the generic-vs-generic MTS/MTT bullet for a type parameter nested
inside a functional-interface parameter position, (2) capture conversion of Si before taking its
function type, (3) exact-method-reference-specific return-type bullets. **All 3 done, tested, full
suite green: 177/177 passing, 0 skipped** (up from 171).

- **Item 1 (generic-vs-generic MTS/MTT bullet) - done.** `JResolveMostSpecificMethod.isMoreSpecificGeneric`'s
  per-position loop previously had only two cases: `Ti` proper (direct `moreSpecificType` check) or
  fall through to a plain `Subtype(Si, Ti)` constraint. Added a third case - `Si`/`Ti` both
  functional interface types with unrelated interfaces - that calls new
  `generateFunctionalInterfaceConstraints`, a constraint-*generating* sibling of the existing
  (boolean-deciding) `moreSpecificFunctionalInterface`: since `Ti` here mentions one of m2's own
  still-unresolved inference variables, the function-type comparison can't be decided immediately
  and instead emits `Equal`/`Subtype` bounds into the same nested `inferenceSolver()` solve
  `isMoreSpecificGeneric` already runs for everything else. Mirrors the literal §18.5.4 text
  (type-parameter-bound equality, formal-parameter subtype+equality pair, return-type recursion)
  with the same "escape hatch" simplifications already established for
  `moreSpecificFunctionalInterface` (documented inline). New `generateReturnTypeConstraints` is the
  constraint-generating counterpart of `moreSpecificReturnType`. Verified against real javac (JDK
  24): `<T> pick(Foo<T>)` (`Foo<T>{String make();}`) vs `<T> pick(Bar<T>)` (`Bar<T>{T make();}`),
  called `pick(() -> "hi")`, picks `Foo` - added as
  `fooWinsOverBarForGenericVsGenericWithFunctionalInterfaceParameter` in
  `JResolveMostSpecificMethodGenericTests.java`.

- **Item 2 (capture conversion of Si) - done, plus a related fix to the already-shipped
  non-generic path.** Added `JResolveMostSpecificMethod.captureConvert` - a **standalone,
  type-level** JLS §5.1.10 capture conversion, deliberately *not* reusing
  `JMemberLookup.captureConvert`/`JIncorporationCapture` (confirmed via a dedicated research pass:
  that pair is solver-integrated and reactive - it mints unbounded fresh metavars and only derives
  their bounds from *other* constraints already present in an active branch, which doesn't exist in
  this one-shot, non-solver comparison). Instead, bounds are computed directly from the wildcard's
  own bound plus the class's declared type-parameter bound and written straight onto the fresh
  `JMetaVarType`'s own mutable bound-set fields (confirmed via a research pass that this is legal -
  `upperBounds()`/`lowerBounds()` are plain mutable `Set`s, not solver-managed state) - **and** each
  bound is also emitted as a real `Subtype` constraint into the nested solve (via
  `JTypeConstraints.Subtype.createBound`, which does both: mutates the metavar's bound-set *and*
  returns the constraint), since the nested solve only resolves variables through actual bound
  constraints (mirroring `JBuildInitialBounds`'s treatment of ordinary `Infer`-registered
  variables) - a captured variable with only field-level bounds and no matching constraint turned
  out to be invisible to it. Direct unit tests in new `JResolveMostSpecificMethodCaptureTests.java`
  verify the bound math itself (`? extends Number` → upper bound `Number`, no lower bound;
  `? super Integer` → lower bound `Integer`, upper bound falls back to the class's own declared
  bound; non-wildcard argument → no-op) rather than only through full overload-resolution
  scenarios - **real javac (verified with JDK 24) turns out to treat every generic-vs-generic
  functional-interface comparison whose formal parameters mention the compared type parameter as
  ambiguous, regardless of whether capture is involved** (confirmed by testing progressively
  simpler variants, including a non-wildcard/non-capture control case that is *also* ambiguous for
  the same reason - the §18.5.4 formal-parameter bullet's generated equality constraint is very
  easy to fail to satisfy), which made it impractical to isolate capture's effect through a
  "which overload wins" integration test alone.
  - **Related fix found and applied while implementing this**: the *already-shipped* (Session 4)
    `moreSpecificFunctionalInterface` (the non-generic/`Ti`-proper boolean path, used e.g. by the
    existing `unrelatedFunctionalInterfacesComparedByFunctionType` test) never captured `Si` either
    - its own javadoc explicitly named this as a known simplification. Rewrote it to take the raw
    `Si`/`Ti` types (instead of pre-extracted SAM `JMethodReference`s) and capture `Si` internally
    via the same new `captureConvert`, mirroring `generateFunctionalInterfaceConstraints`'s
    MTS-vs-MTS' treatment exactly, just deciding each comparison immediately (`isSubtype`/`typeEquals`)
    instead of generating constraints, since this path is only reached once `Ti` is already a
    proper type. Found by testing an integration scenario that should have been decidable
    (`Foo<? extends Number>` non-generic vs `<T> Bar<T>` generic) and getting a surprising
    "Bar wins" result; traced via temporary debug instrumentation (added and removed - not left in
    the tree) to `moreSpecificType`'s `Ti`-proper branch, which was silently skipping capture
    conversion. Confirmed the existing suite (including the pre-existing StringMaker/CharSequenceMaker
    test, which has no wildcards and so is unaffected) still passes after the rewrite.
  - New test `fooStillWinsWhenSiHasWildcardTypeArgumentRequiringCapture` in
    `JResolveMostSpecificMethodGenericTests.java` exercises the capture-conversion code path
    end-to-end (through the full pipeline) even though, in that specific scenario, `Foo`'s SAM
    doesn't depend on its own type parameter so the captured bound has no bearing on *which*
    overload wins - it's a regression/no-crash check, not a "capture changes the answer" proof;
    the actual bound math is what `JResolveMostSpecificMethodCaptureTests` verifies directly.

- **Item 3 (exact-method-reference return-type bullets) - done.** Added
  `isExactMethodReference`/`exactMethodReferenceDeclaration`/`exactMethodReferenceReturnsPrimitive`
  implementing JLS §15.13.1's exact-method-reference test as a **pragmatic approximation**
  (documented inline): array-constructor references are always exact; otherwise, exactly one
  accessible, non-variable-arity, non-generic-or-explicitly-witnessed member (method or
  constructor) matching the reference's name must exist. Raw-type detection is skipped entirely
  (this codebase doesn't model raw types - see the unchecked-conversion-tracking non-goal in
  memory) and accessibility uses the same ordinary member-lookup already used elsewhere in this
  file rather than the full JLS accessibility procedure. Wired into both `moreSpecificReturnType`
  and its constraint-generating counterpart `generateReturnTypeConstraints`, inserted between the
  existing lambda and conditional-expression cases, matching the literal spec's bullet order.
  - **Important discovery while designing the regression test**: a *top-level* method-reference
    argument being compared for "primitive vs reference SAM return type" is **preempted by
    phase-based overload resolution** in this codebase before the most-specific-method comparison
    (and therefore this new bullet) is ever reached - matching a method's primitive return type
    against a primitive-returning SAM succeeds by *strict* invocation (no boxing) while matching it
    against a reference-returning SAM needs boxing and only succeeds by *loose* invocation, and
    strict-phase candidates are filtered in before loose ones ever get to the specificity-comparison
    step (`JResolveMostSpecificMethod.pruneGroup`'s own first phase-filtering pass, unrelated to this
    session's changes). Confirmed via temporary debug instrumentation (added and removed) showing
    the "primitive" candidate had both a phase-0 and a phase-1 branch while the "reference" candidate
    had only phase-1, so it was invalidated by phase before ever reaching `isMoreSpecific`. Worked
    around by wrapping the method reference in a lambda (`() -> Host::five` instead of bare
    `Host::five`) so the *outer* argument's own phase-applicability doesn't depend on the inner
    method reference's boxing behavior, and the comparison instead recurses into the lambda's result
    expression via the existing "RS and RT are functional interface types" bullet - this reaches the
    new code without the phase short-circuit. Verified against real javac (JDK 24) both with and
    without this wrapping trick to confirm the wrapped version still resolves the same way
    (`Outer1` - the primitive-returning-SAM side - wins) and that the fix is what makes it pass
    (temporarily disabled, reran, confirmed 0 surviving candidates i.e. wrongly ambiguous, restored).
    New test: `exactMethodReferenceWithPrimitiveReturnPicksPrimitiveMaker` in
    `JResolveMostSpecificMethodTests.java`.

**Still not modeled** (unchanged from Session 4, and out of scope for this session too): capture
conversion is now handled for the §18.5.4/§15.12.2.5 functional-interface bullets specifically, but
nowhere else in the codebase; the §18.5.4 bullet's own "mentions one of MTS's type parameters and
the corresponding MTT part is improper → false" escape hatches are still skipped (same
simplification `moreSpecificFunctionalInterface` already had); recursion into switch-expression
result expressions remains unmodeled (switch expressions aren't modeled at all). None of these are
believed to matter for realistic code - see inline javadoc on each for the precise boundary.

**Remaining scope, unchanged**: anonymous class instantiation and switch-expression modeling are
still not started (see [[project_remaining_scope]] memory); unchecked-conversion tracking remains
explicitly out of scope per prior user direction.

- Current test count: **177 tests, all passing, 0 skipped** (`jype-main`).

## Session 6: full JLS SE 24 audit + fix-everything pass (started 2026-11-07)

Plan: `C:\Users\johnr\.claude\plans\finish-fixing-any-remaining-squishy-pelican.md` (items 1-49, 7 phases).
User rules this session: comments only `// JLS §x.y.z <case>`; per item javac repro -> Jype test with
same verdict -> confirm fails pre-fix -> full suite -> commit; scratch javac repros deleted at end;
Phase 7 = differential fuzzing vs javac. Unchecked *warnings* stay out of scope, unchecked-conversion
legality/typing is in scope.

Setup: Jackson 3 jype-stub migration committed on `main` (129db16), merged into this branch (2aa984f).
Baseline: 177 tests green.

### Progress log

- **Item 1 (unique inference variables) + item 6 (§18.4 resolution rewrite) — done.**
  `JVarType.createMetaVar()`/`JVarMetavarVisitor` keyed new metavars on `identityHashCode(typeParam)`,
  so every α minted from the same type parameter was *equal* (nested `id(id(x))`, two captures of the
  same receiver, ...). Now a global counter in `JMetaVarTypeImpl`. `JReduceInstantiation` /
  `JReduceInstantiationRefExpression` built the result type via a second `parameterizedWithMetaVars()`
  call (worked only by the collision) — now use the same α map; `JReduceInstantiation` also merged the
  outer branch's var map *after* its fresh vars (outer could override) — fixed order.
  Unique α exposed `JResolveBounds` bugs (NPE in `trySubsetWithBase`, un-closed dependency map,
  capture fallback using fresh *inference* vars instead of fresh *type* vars, reversed throws check).
  Rewrote `JResolveBounds` straight from §18.4: dependency rules incl. capture direction + transitive
  closure; minimal subset = first sink SCC (Tarjan) over uninstantiated vars; candidates
  lub / RuntimeException (throws, RuntimeException <: every proper upper bound) / glb; on failure or
  capture: fresh type variables Y (new `JFreshTypeVariables`, virtual location `Y#n`) with
  lower = lub(proper lowers), upper = glb(Ui θ), well-formedness check, capture bounds removed,
  αi = Yi incorporated. `JVarType` gained `lowerBounds()` (impl, delegate, copy, equals, deep mapper).
  javac check: `Box<? extends Number> box; box.set(box.get())` is **rejected** by javac
  ("Number cannot be converted to CAP#1") — the old Session-3 test asserted it type-checks (it only
  passed because both captures shared one α). Test flipped to
  `setWithResultOfGetOnExtendsWildcardIsRejected`; `wildcardMemberAccessRecordsACaptureConstraint` now
  asserts the captured α resolves to a fresh type variable bounded by Number. Suite 177/177.

- **Relation fuzz harness (Phase 7 started early as verification tool).** `jype-main/src/test/java/honeyroasted/jype/fuzz/`:
  `TypeWorldGenerator` (seeded random class/interface hierarchies, generic params incl. F-bounds,
  holder `H<T extends Number, U extends T, V extends Comparable<V>, W, S extends CharSequence & Comparable<String>>`
  with random field types: primitives, arrays, wildcards, nested generics), `JavacOracle` (in-process
  javac via `JavacTask`, drops ill-formed fields and recompiles, keeps javac's internal `Types`),
  `RelationFuzzTest` (`@Tag("fuzz")`) comparing javac `isSubtype/isSameType/isAssignable/isCastable`
  vs Jype `isSubtype/typeEquals/isCompatible(ASSIGNMENT)/isCompatible(EXPLICIT_CAST)` on random field
  pairs, per-check timeout (`TimedRunner`). Run: `./gradlew :jype-main:fuzzTest -Pfuzz.seed=N -Pfuzz.count=M`
  (report in `jype-main/build/fuzz-failures/relations-N.txt`); default `test` excludes tag `fuzz`.
  Reflection resolution now also tries the thread context class loader (`JReflectionTypeResolution.loadClass`).
  First run: 2400 checks, **156 mismatches**.
- **Items 2, 3, 9, 12, 13 (relations core) — done via a rewrite rather than patching mappers.**
  The 13 compatibility mappers (`JSubtype*`, `JCompatible*`) all fired on the same constraint and were
  ANDed by Almonds, giving `T <: T` false, `U <: T` false (U extends T), etc. Replaced with a direct
  decision procedure `JTypeRelations` (§4.10 subtyping incl. type-var lower bounds, intersections, arrays,
  null; §4.10.2 capture-then-containment; §4.5.1 containment; §5.1.10 capture to fresh type variables
  `CAP#n`; §5.1.9 unchecked conversion only in compatibility, mirroring javac `isSubtypeUnchecked`;
  §5.3 strict/loose incl. unboxing via supertypes like javac `unboxedType`; raw supertypes erased §4.8)
  and `JCastability` (faithful port of javac `Types.isCastable`: rewriteQuantifiers, asSub/adapt,
  disjointType/notSoftSubtype, sideCast/sideCastFinal; sealed DisjointChecker still TODO — no
  permitted-subclass model yet). Compatibility list is now `JEqualType`, `JProperSubtype`,
  `JProperCompatible`, `JProperContains` + the two expression mappers. `JTypeOperations.isSubtype/isCompatible`
  short-circuit to `JTypeRelations` for proper types.
  Other bugs found by the fuzzer and fixed: `JType.baseCaseEquivalence` pre-inserted the pair it then
  queried (so `CharSequence & Comparable<String>` "equalled" any class); metavar equivalence used a
  global mutable equality set (item 2: `createBound` no longer mutates metavars); `JVarType.upperBound()`
  returned the null type for an unbounded var; wildcard equality compared a per-instance `identity`
  (copies of `?` were unequal) — now structural, `?` == `? extends Object`; intersection `simplify`
  dropped both of two equal-but-distinct children (B15). Member-access capture (`JMemberLookup.captureConvert`)
  and §15.12.2.5 capture (`JResolveMostSpecificMethod.captureConvert`) now use real capture for proper types.
  YAML `unchecked_wildcard_test` asserted raw `List <: List<? super Object>` (javac: false; it is unchecked
  *conversion*); split into subtype=false + assignment=true cases.
  After: seeds 1-5, ~67k checks, **0 mismatches**. Suite 179/179.

- **Item 17 (lub/glb) — done.** `JFindLeastUpperBound` rewritten after javac `Types.lub` (JLS §4.10.4 leaves
  the infinite-type case to the implementation): erased-candidate set from closures, closureMin,
  per-candidate `asSuper` + intersect/merge (containment first, else `? extends lub(...)` with a
  recursion guard yielding `?`), array and mixed array/class cases (Serializable & Cloneable), null
  operands ignored. `JFindGreatestLowerBound` of the empty set is now Object. Relation fuzzer gained a
  behavioural lub check (subtype relations of javac's vs Jype's lub against every world type).
  javac-verified updates to `FindLeastUpperBoundTests`: lub(Bar, Baz) = `Foo<? extends Foo<?>>`;
  lub(Box<? super Integer>, Box<Number>) = `Box<? super Integer>` (javac merge keeps the containing
  argument; the old test asserted a union of lower bounds); lub(raw-Box user, Box<String> user) = raw `Box`.
  Fuzzer-found fixes: `JVarTypeImpl.hashCode` depended on recursive bounds (equal vars hashed
  differently, substitution map lookups missed) — now location-only; `JVarTypeResolveVisitor` re-visited
  its replacement with a fresh cache (non-simultaneous substitution, infinite loop for
  `class C2 extends C1<C1>`) — now simultaneous per JLS; deep structural mapping rebuilt a
  `JClassReference`'s declared supertypes during substitution — references are now atomic unless the
  visitor is structural. Seeds 1,6,7 x 40 worlds: 0 mismatches. Suite 179/179.

- **Expression fuzz harness.** `ExpressionFuzzTest` + `ExpressionWorldGenerator` + `FuzzExpr` (sealed-ish family
  of expression shapes rendering both Java source and `JExpressionInformation`): random static generic /
  overloaded / varargs methods, static fields, literals, lambdas, method & constructor refs, `new`,
  casts, conditionals, unary/binary operators; probes `static void probeN() { T x = <expr>; }`; javac
  accept/reject per probe line vs Jype `ExpressionCompatible(expr, ASSIGNMENT, T)`.
- **Items 34, 35 (access, field access) — done.** `JClassType.accessFrom` (§6.6): private access within the
  same top-level class (nestmates), package access before protected (a same-package subclass got
  PROTECTED, losing package-private access), protected via any enclosing subclass (§6.6.2.1).
  New `JFieldLookup`: §8.3 hiding, §8.3.3 ambiguity (distinct inherited fields), private fields not
  inherited, inherited field types substituted through `asSuper` (generic superclass fields), raw
  receiver → erased field type (§4.8), `arr.length` (§10.7), static-only through a class name,
  capture of the field type (§6.5.6.1). `GetField.getSimpleType` used to return the `JFieldReference`
  itself and `isSimplyTyped` was true even when no field existed (NoSuchElementException risk); it and
  `JReduceGetField` also both fired, so `Object x = M.v` was rejected. Now simply-typed iff the field
  resolves; `JReduceGetField` only handles non-simply-typed receivers. Old test
  `privateMethodIsNotAccessibleFromAnotherClass` used two nested classes of the same top level (javac:
  accessible) — replaced by a top-level fixture `fixtures/privateaccess/PrivateOwner` plus a nestmate
  positive test. Lambda explicit parameter types are now `List<JType>` (primitive `int p` crashed).

- **Items 8, 10, 11, 19, 23, 24 (partial), 29, 30, 31, 33, 36 (partial), 42 — inference core and invocation rewrite.**
  Found via a hang (`float x = (2.5 ? new ArrayList<>() : M.v7)`): type-level reducers *dropped* what
  they reduced, and incorporation kept re-deriving the same formula forever. Now:
  - `JReduceSubtype/Equal/Contains/Compatible` rewritten straight from §18.2.2–§18.2.4; reduced
    constraints are kept as TRUE, bounds become ASSUMED. Fixes: `S <= ?` was FALSE; §18.2.4
    structural equality (class args, outer types, arrays, wildcard pairs, `α = primitive` FALSE)
    was missing; array supertype search looped forever; type-var lower bound case; strict phase never
    boxes (§18.5.1); `T primitive → ‹S = box(T)›`; unchecked rule (raw S → parameterized T) was
    reversed — now records an `UncheckedConversion` marker.
  - Incorporation (`JIncorporation*` + `JIncorporationSupport`) only combines *bounds* (a top-level
    inference variable); derived non-bound formulas go back to reduction as UNKNOWN; subtyping instead
    of `isAssignableTo` (boxing) for proper checks; full §18.3.1 substitution rules.
  - Hash codes made shallow/consistent (class refs by namespace, methods by location, parameterized
    types by class + shallow args); the old deep hashes with `seen`-set copying were a huge cost.
  - §18.4 resolution checks glb well-formedness (two unrelated class bounds → fail), javac-verified:
    `Character x = M.m3()` with `<A extends Number> A m3()` is rejected.
  - New `JStandaloneTyping` (+ `JTypeContext.ExpressionType` metadata): standalone typing of operands
    and receivers via a loose sub-solve that reads the expression's real type (e.g. a method's return
    type), so primitive results survive (the old fresh-α trick boxed or failed). Operators/conditional
    use it (`JReduceOperands`).
  - Conditional (§15.25): boolean/numeric classification (method invocations by declared return type),
    reference conditionals in assignment/invocation contexts are poly (§15.25.3); javac-verified
    `Object o = c ? aBoolean : anInt` compiles (old test asserted rejection — fixed), `int x = ...` rejected.
  - `JOperatorTypes`: primitive identity comparisons replaced with `typeEquals`; unboxing via supertypes.
  - New `JMethodLookup` (§8.4.8/§9.4.1/§9.2/§10.7/§4.8: inherited members substituted through
    `asSuper`, private/static-interface methods not inherited, overriding by subsignature, Object members
    of interfaces, array `clone()`, raw erasure, `getClass()` → `Class<? extends |T|>` §4.3.2) and
    `JInvocationSelection`: §15.12.2.1 potential applicability (arity, explicit type-arg count, lambda
    arity), §15.12.2.2 pertinence, §18.5.1 phases decided by nested solves of *arguments only* (the
    target no longer participates in applicability), §15.12.2.5 most specific with preferred-method
    rules (concrete vs abstract/default, most specific return), then §18.5.2.1 invocation constraints
    (unchecked → erased return §15.12.2.6, capture of wildcard return, `throws α` §18.1.3, explicit
    type-arg bound check). `JReduceMethodInvocation` rewritten on these (static form must pick a static
    method, `super.m` must not be abstract, `Interface.super.m` supported).
  Expression fuzz seed 1 × 15 worlds: 600 probes, 2 mismatches left (void-SAM lambda body; ctor-ref).
  Suite 180/180.

- **Items 25, 26, 27, 28, 32, 37 (most), 40 — lambdas, deferral, functional interfaces, instantiation.**
  - Lambda model: `Lambda.Simple(List<LambdaParameter>, List<JType> explicitTypes, LambdaBody)` with
    `LambdaBody.Expression` / `LambdaBody.Block(statements, returnExpressions, hasVoidReturn,
    canCompleteNormally)`; old `(body, types, count, implicitReturn)` constructor kept (implicitReturn=false
    = empty void block). `LambdaParameter` references are substituted with the parameter types before the
    body is reduced (`JExpressionSubstitution`).
  - `JReduceLambdaExpression` rewritten: §18.5.2.2 deferral — a lambda whose target is an inference
    variable, or an implicitly typed lambda whose function-type parameter types mention unresolved
    inference variables, becomes a `Deferred` constraint (ASSUMED); `JResolveBounds` then resolves only
    those input variables (and dependencies) and `JReduceDeferred` re-issues the constraint. This replaces
    the earlier deliberate deviation of reducing implicit lambdas with improper parameter types.
    Ground target type: §9.9 non-wildcard parameterization (implicit) / §18.5.3 inference (explicit);
    generic function types rejected (§15.27.3); void function type needs a statement-expression body or
    void-compatible block (javac-verified `F2 x = () -> 300` rejected); value function type needs
    value-compatible block. Checked exceptions (§18.2.5) via `LambdaThrowsCheck` constraints on every
    invocation in the body, reduced once the invocation's thrown types (`JTypeContext.ThrownTypes`, θ
    applied) are known. Old unused `LambdaThrows/InvocationReferenceThrows/InstantiationReferenceThrows`
    removed (stub DSL `expr_throws` now maps to the check).
  - Statement contexts use target `void`: invocation/instantiation skip the return compatibility.
  - `JResolveBounds` moved into the main inference loop (so deferred constraints can be reduced after
    resolution) and waits until incorporation is saturated (§18.4 works on an incorporated bound set —
    without this, `new ArrayList<>(Collections.emptyList())` → `List<String>` resolved emptyList's T to a
    fresh Y instead of String). `JVerifyBounds` substitutes instantiations and decides bounds; a still
    deferred constraint fails.
  - `JFunctionalInterfaces` (§9.8): abstract methods of all superinterfaces substituted through the
    parameterization (`UnaryOperator<String>.apply(String)`), Object public methods excluded by
    signature, overridden methods removed, override-equivalent abstract methods merged (subsignature +
    return-type-substitutable).
  - `JReduceInstantiation` on `JInvocationSelection`: abstract/interface rejected (§15.9.1), diamond
    infers class + constructor type parameters (§15.9.3), explicit class arguments substitute
    constructor signatures, generic constructors with explicit constructor type arguments, raw form
    (erased signatures), qualified `outer.new Inner()` and implicit enclosing instance (§15.9.2), via new
    `Instantiation.Full` (old `Simple` keeps diamond-when-empty semantics).
  Fuzz: expressions seed 1 (1 mismatch left: constructor reference), relations seed 2 (0). Suite 180/180.

- **Item 39 (method/constructor references) — rewritten.** `JReduceInvocationRefExpression`: ground target via
  §9.9 non-wildcard parameterization (§15.13.2), deferral for inexact references until parameter types are
  resolved and for inference-variable targets (§18.5.2.2), compile-time declaration through
  `JInvocationSelection` with synthetic arguments of the function-type parameter types: `ReferenceType::m`
  runs both §15.13.1 searches (static with P1..Pn, unbound receiver with P2..Pn on the raw-type
  parameterization found from P1, plus ‹P1 <: ReferenceType›) and applies the static/instance
  selection rule; `Primary::m` must pick an instance method; `super::m` / `I.super::m` must pick a
  non-abstract instance method; return ‹R' → R› unless void; thrown types checked (§18.2.5). Varargs and
  `getClass()` handled by the engine/lookup. `JReduceInstantiationRefExpression`: `ClassType::new` reduces to
  a synthetic class instance creation (diamond when raw generic, §15.13.1), `T[]::new` needs a single
  int-compatible parameter. New `InvocationReference.Full` / `InstantiationReference.Full` carry the
  declaring class for access checks. Engine gained `selectDetailed` (applicable set + chosen).
  Test fix: `exactMethodReferenceWithPrimitiveReturnPicksPrimitiveMaker` read the first `ChosenMethod` in
  branch metadata, which is now the nested reference's synthetic invocation; it now selects `pick`'s.

- **Fuzz-driven fixes (expression generator v2).** Generator now has a fixed library of realistic generic /
  overloaded / functional methods (`id`, `listOf(T...)`, `apply`, `max`, `over(...)` overloads, `run(Runnable)` vs
  `run(Callable<T>)`, `risky(F3<E>) throws E`, `compose`, ...), lambda parameter references, block lambdas,
  instance calls, and type-directed probes; acceptance rose from ~10% to ~45% of probes. Oracle hardened:
  declarations compiled alone; probes compiled in batches with an always-failing sentinel — javac silently
  abandons attribution after some probes (seen with `Consumer<String> x = () -> () -> M.run(M.s0)`), so a
  missing sentinel error triggers bisection to isolate the probe. Fixes found:
  - §18.5.2.1 early-resolution bullets (α resolved in B2 for raw-only supertypes of the target's class,
    wildcard-parameterized / conflicting bounds, primitive targets with wrapper bounds) — javac:
    `Collection<?> x = id(rawList)` compiles. Strict context + primitive target for a poly invocation fails.
  - javac's incorporation checks derived proper bounds with unchecked subtyping (`Infer` IS_SUBTYPE →
    `isSubtypeUnchecked`); Jype now does the same in incorporation and bound verification. **Deliberate
    javac-alignment deviation** from a literal JLS §18.3.1 reading (‹S <: T› reduces by subtyping).
  - Casts (§15.16): only lambdas/method references take the cast type as target; other operands are typed
    standalone then checked by §5.5 (`(Supplier<Double>) new ArrayList<>()` compiles). `instanceof`
    (§15.20.2) rewritten: standalone reference operand, castability, non-reifiable target only for a checked
    cast (§4.7 reifiability). `JReduceSimplyTypedExpression`/`JExpressionSimplyTyped` no longer fire on
    Cast/InstanceOf/ArrayCreation (their `drop` erased the dedicated reducer's FALSE — the B6 pattern).
  - Standalone typing rejects lambdas / method references / conditionals containing them (no target).
  - Constant narrowing (§5.2) now applies inside inference (`byte x = 'c'`, `short x = 1`, lambda returns).
  - Type variables: equality by declaration location only (fresh `Y#n` compared unequal to deep-visitor
    copies of itself, breaking `<A extends Comparable<A>> A m()`); deep copies keep metadata.
  - Casting captured types: capture variables remember their wildcard (`JCapturedWildcard`) and
    `rewriteQuantifiers` handles them like javac's `visitCapturedType` (`(ArrayList<String>) id(List<? super Integer>)` rejected).
  Fuzz: expressions seeds 1–5 × 20 worlds × 80 probes: 0 mismatches; relations seed 8: 0. Suite 180/180.

- **Item 43 (constant expressions §15.29) — done.** `JFieldReference` now carries `constantValue()` (ConstantValue
  attribute: binary resolver reads it directly; reflection resolver reads the declaring class file via
  `java.lang.classfile` in `JReflectionConstantValues` — no class initialization). `JConstantEvaluator`
  folds literals, constant variables, casts to primitive/String, unary/binary/conditional operators with
  Java conversion semantics. `JExpressionAssignmentConstant` applies §5.2 narrowing to any constant
  expression of type byte/short/char/int targeting byte/short/char (or boxes) and other reducers defer to it;
  conditional constant narrowing uses the evaluator. `JFieldReferenceImpl.copy` also dropped modifiers (fixed).
  javac-verified matrix in `JConstantExpressionTests` (16 cases: `byte b = 1 + 2` ok, `127 + 1` rejected,
  `byte b = CONST` ok vs non-final rejected, `flag ? 1 : 2` rejected, `true ? 1 : 2` ok, `1 << 7` rejected, ...).

- **Items 38/41/44 (anonymous classes, switch expressions, new expression shapes) + sealed types — done.**
  - Sealed model: `JClassType.permittedSubclasses()` / `isSealed()` (reflection: `getPermittedSubclasses`,
    binary: `PermittedSubclasses` attribute, lazy delegates). `JCastability` ports javac's sealed
    `areDisjoint` (§5.1.6.1): `(Sealed) new Open()` and `(Sealed) (Iface) null` rejected, `(Iface) new Open()` ok.
  - Anonymous classes (§15.9.5): `Instantiation.Anonymous`; rejected for final / enum / sealed supertypes and
    for interface supertypes with arguments; diamond requires denotable inferred arguments (§15.9.3) — the
    `Denotable` check is attached in `JInvocationSelection` to the θ-substituted result (it was first put on
    the declared `ArrayList<E>`, fuzz found `max(new ArrayList<>(charExpr) {})` accepted).
  - New reducers: `JReduceArrayAccess` (§15.10.3: array type, promoted int index, captured component),
    `JReduceAssignment` (§15.26 simple/compound incl. implicit narrowing cast, final/array-length rejection,
    declared uncaptured variable types; §15.14/15.15 ++/--), `JReduceSwitch` (§15.28: poly in assignment /
    invocation contexts, standalone typing like §15.25 incl. §5.6 numeric choice with constants, selector
    rule §14.11.1: primitive selectors only char/byte/short/int without patterns, any reference type).
    `Compatible.Context.STANDALONE` added so conditionals/switches know they are not poly (standalone sub-solve).
  - Unqualified invocation (§15.12.1) via `ImplicitReceiver`: innermost enclosing class with a member of that
    name; static-context check. `LocalVariable`, `ClassLiteral` (Class<|T|>, boxed primitives), `This` shapes.
  - §15.12.2.1 potential compatibility now checks lambda body shape (void SAM needs statement expression or
    void-compatible block: `consume(p -> 300)` picks the Function overload) and method-reference arity /
    static-vs-instance rules; switch expressions recurse into results.
  - Perf: exact-method-reference detection used the old `JMemberLookup.allMethods` (quadratic bridge search,
    9.3 s for `consume(String::length)`); now `JMethodLookup.members` (24 ms).
  - Standalone simply-typed receivers re-captured on every `getSimpleType`, so the validity side-constraint
    compared `List<CAP#5>` to `List<CAP#3>` (`lnum.get(0)` rejected); the side-constraint now targets the erasure.
  - `JIntersectionType` is a `JArgumentType` (inferred `List<Number & Comparable<...>>` crashed hashCode).
  - Fuzz harness: probe methods take locals (`l0..l5`), 1/4 are instance methods, new FuzzExpr shapes (Local,
    ClassLit, This, Assign, IncDec, Index, SwitchExpr, Anon, Unqualified); javac reports a disabled preview
    feature once per file, so a preview diagnostic forces batch bisection.
  - javac-verified matrix in `JExpressionShapeTests` (40 cases). Fuzz: expressions seeds 1–10 × 20 worlds: 0
    mismatches; relations seeds 1–2: 0. Suite 188/188.

- **Item 18 (erasure §4.6, projections §4.10.5 / B28) — done.**
  - `JStructuralTypeMappingVisitor.visitMethodType` rebuilt method references without their modifiers, so
    members of raw types (erased) lost `public`/`varargs`/`static`: `raw.va(1, 2, 3)` was rejected.
  - `JErasureTypeVisitor`: `?`/`? super` erase to Object (crashed on empty bounds), intersections erase to
    the leftmost component, erased generic methods drop their type parameters (`String s = raw.gen("x")`
    rejected, `raw.<String>gen("x")` accepted, matching javac).
  - Projections rewritten to javac's `TypeProjection` / JLS §4.10.5: the old visitors never set the class
    reference of the projected type, dropped outer types, wrapped wildcard arguments in another wildcard,
    and used `accept(Predicate)` (an all-match visitor) as "mentions". New `JType.mentions` (any-match).
    Cycles through F-bounds project to Object (upward) / undefined (downward).
  - Tests: `JRawMemberErasureTests` (7 javac-verified cases), `JProjectionTests` (`var` types of
    `List<? extends Number>`, `List<? super Integer>`, `Comparable<?>`, `Rec<?>` ≡ `Rec<? extends Rec<?>>`
    as checked with javac assignments both ways). Suite 191/191, fuzz seed 11: 0 mismatches.
