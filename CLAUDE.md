# Jype

Jype models Java's type system as a library: types, subtyping, conversions, capture, lub/glb, and
JLS chapter 18 type inference for expressions (method/constructor invocations, lambdas, method
references, conditionals, switch expressions, operators, casts, assignments, and so on). The target
spec is **JLS SE 24**, and **javac 24 is the ground truth** wherever the two disagree.

## Modules

- `jype-main`: the type system API, the resolvers (reflection and class file), and the solver.
- `jype-stub`: a YAML DSL for declaring stub classes and writing solver tests
  (`jype-main/src/test/resources/stub_tests/*.yml`, run by `YamlDslTests`).
- The constraint solver comes from the separate **Almonds** library (`com.github.HoneyRoasted:Almonds`,
  source in `../Almonds`). Utilities come from `com.github.HoneyRoasted:Collect`.

The version is in `buildSrc/shared.gradle`. The toolchain is Java 24 (a local JDK lives at `/d/JDKs/24+36/bin/`).

## Build and test

```sh
./gradlew :jype-main:test :jype-stub:test          # unit tests plus the YAML suite (tag "fuzz" excluded)
./gradlew :jype-main:fuzzTest --tests '*ExpressionFuzzTest*' -Pfuzz.seed=1 -Pfuzz.count=20
./gradlew :jype-main:fuzzTest --tests '*RelationFuzzTest*'   -Pfuzz.seed=1 -Pfuzz.count=20
```

- **The fuzz tests** live in `jype-main/src/test/java/honeyroasted/jype/fuzz`.
  - They generate random class hierarchies and expressions, compile them in-process with javac, and compare the results with Jype.
  - `RelationFuzzTest` uses javac's internal `Types` as the oracle. `ExpressionFuzzTest` uses javac's accept/reject verdict per probe.
  - Mismatches and reproducers are written to `jype-main/build/fuzz-failures/<kind>-<seed>.txt`.
  - The tests need the `--add-exports jdk.compiler/...` flags already configured in `jype-main/build.gradle`.
- **Expression probes** are compiled in batches. A sentinel error line detects javac silently giving up on a batch, and a reported preview-feature error also forces bisection. In either case the harness bisects the batch.
- **Hangs:** reproduce outside Gradle (`java -cp ...`) and use `jstack`. Clean up stray Gradle workers with `./gradlew --stop`.

## Code conventions (user preferences)

- **Comments:** only `// JLS §x.y.z <short case label>` comments that reference the spec case a branch handles. No prose comments or javadoc in new code.
- One top-level type per file. Expression shapes stay nested inside `JExpressionInformation`, which is deliberate scoping.
- Always brace `if`/`for`/`while` bodies.
- Keep method, constructor and record headers on one line.
- Versioning: while on `0.0.X`, bump only `X`.
- **Verifying a change:**
  1. Write a tiny javac repro and record javac's verdict.
  2. Write a Jype test that asserts the same verdict.
  3. Confirm the test fails without the fix.
  4. Run the full suite.
  5. Run a few fuzz seeds.

  Delete one-off repros afterwards.
- Make small, isolated commits. Never push without being asked.

## Solver architecture (`jype-main/src/main/java/honeyroasted/jype/system/solver`)

- **Almonds mapper semantics.** These matter when writing a reducer:
  - All matching mappers fire in one pass, and their changes are queued and then executed.
  - A branch's status is the AND of its constraint statuses, and ASSUMED counts as true.
  - Reducers set reduced constraints to TRUE instead of dropping them; otherwise incorporation re-derives them and loops.
  - Mapper filters must be mutually exclusive per constraint shape.
- **`JTypeRelations`** is the direct relation engine for proper types: subtyping (plain and unchecked), containment, compatibility contexts, capture, erasure, `asSuper`, castability via `JCastability` (a port of javac's `isCastable` plus the sealed-type disjointness check), and glb well-formedness.
- **`constraints/`**
  - `reduction/` holds the JLS §18.2 reducers, one per expression shape.
  - `incorporation/` holds the §18.3 rules.
  - `inference/` holds resolution (`JResolveBounds`, §18.4), verification (`JVerifyBounds`) and the most-specific-method logic (`JResolveMostSpecificMethod`, §15.12.2.5 and §18.5.4).
  - `compatibility/` holds the proper-type relation constraints.
- **Overload selection** for invocations, instantiations and method references runs through `JInvocationSelection`: phases, pertinence to applicability, most-specific, and the invocation type with §18.5.2.1 early resolution. `JMethodLookup`/`JFieldLookup` handle members, `JFunctionalInterfaces` handles §9.9 function types, `JStandaloneTyping` types standalone sub-expressions, and `JConstantEvaluator` handles §15.29.
- **Wiring.** Mapper lists are in `operations/JTypeOperationsImpl`. A new reducer must be added to both the reduction list and the inference list.

### Deliberate javac alignments

Where javac departs from the JLS text, Jype follows javac:

- Incorporation and bound checks use unchecked subtyping, like javac's `Infer` `IS_SUBTYPE`.
- Casting is a port of javac's `isCastable`.
- lub follows javac's `Types.lub`/`merge`.
- Primitive selectors with pattern labels are rejected, because primitive patterns are a preview feature.
- Most-specific: a switch expression never makes one functional interface more specific than another. javac's `MostSpecificFunctionReturnChecker` has no switch case.
- The non-capture §18.4 second resolution attempt follows javac's `instantiateAsUninferredVars`: glb of the upper bounds, with no lub lower bound.

### Not modelled

- Statement-level analysis: definite assignment, reachability, and exception flow outside lambdas.
- Switch `case` labels. The selector type and the result expressions are modelled.
- Unchecked *warnings* are out of scope. Unchecked-conversion *legality* is in scope.

## Changing Almonds

1. Edit `../Almonds`, bump its patch version, and run `./gradlew test publishToMavenLocal`.
2. To test it in Jype, temporarily add `mavenLocal()` first in `buildSrc/shared.gradle` and depend on `honeyroasted.almonds:Almonds:X.Y.Z`.
3. Push Almonds `main`; CI tags the release.
4. Wait for JitPack (`https://jitpack.io/api/builds/com.github.HoneyRoasted/Almonds/X.Y.Z`).
5. Switch back to the JitPack coordinate and remove `mavenLocal()`.

## Reverting the type-system-review merge

The JLS SE 24 review (branch `bugfix/type-system-review`, version 0.0.48) was merged into `main` with a
`--no-ff` merge commit titled `Merge branch 'bugfix/type-system-review'`.

- `main` before the merge: **`129db16e0c3b771b8f443c062949e9db257786a7`** ("Migrate jype-stub YAML
  serialization to Jackson 3", version 0.0.47). It is also tagged locally as `pre-type-system-review`.
- **If the merge has not been pushed:** `git checkout main && git reset --hard pre-type-system-review`.
  This discards the merge; the branch `bugfix/type-system-review` still holds the work.
- **If it has been pushed:** `git revert -m 1 <merge commit>`. Find the commit with
  `git log --merges --oneline -1 main`.
- The detailed per-session review notes (`TYPE_SYSTEM_REVIEW_NOTES.md`) were removed in the commit just
  before the merge. The last version is at `81eb05cf09a0eb05b8c29d84be136c65538368b5`:
  `git show 81eb05c:TYPE_SYSTEM_REVIEW_NOTES.md`.
