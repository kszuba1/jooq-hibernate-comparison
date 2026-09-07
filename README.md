# jooq-hibernate-comparison

The practical part of a master's thesis comparing jOOQ and Hibernate: a single order-management domain whose persistence layer is implemented twice behind a shared contract, so both technologies can be driven through identical scenarios and measured.

## Requirements

- JDK 21
- A running Docker daemon

PostgreSQL 16 is started through Testcontainers for the tests, for jOOQ code generation, and for every benchmark fork. There is no in-memory fallback, so nothing builds without Docker. Maven itself is not needed — use the bundled wrapper (`./mvnw`).

## Setup

```
./mvnw verify
```

This migrates a throwaway PostgreSQL with Flyway, generates the jOOQ sources into `persistence-jooq/target/generated-sources/jooq`, and runs the contract tests against both implementations. First run takes a few minutes, mostly pulling the Postgres image.

## Running your first benchmark

Install the modules into the local repository first. The harness resolves its sibling modules from there, so `package` alone is not enough:

```
./mvnw install -DskipTests
```

Then run one scenario. S01 is the CRUD scenario, and the flags below cut it to a single fork with one warmup and one measurement iteration — enough to confirm the harness works:

```
./mvnw -Pbench -pl benchmarks exec:exec \
  -Dbench.args="S01 -f 1 -wi 1 -i 1 -p config=default"
```

Drop the `-f -wi -i` overrides to get the measured configuration declared on the benchmark class. Re-run `./mvnw install -DskipTests` after changing any code outside the `benchmarks` module, otherwise the harness keeps running the previously installed version.

`bench.args` accepts any JMH option (`-h` lists them). The ones that matter here:

| Argument | Meaning |
| --- | --- |
| `S01` … `S08` | scenario selector; `V01` validates the benchmark wiring itself |
| `-p stack=hibernate,jooq` | the two implementations under comparison |
| `-p config=default,tuned` | stock settings vs. tuned ones |
| `-p tier=SMALL\|MEDIUM\|LARGE` | seeded data volume, defaults to `SMALL` |
| `-rf json -rff target/result.json` | write machine-readable results |

Omitting a `-p` runs every value of that axis, so the command above measures both stacks in one go.

## Running the full matrix

```
./run-benchmarks.sh
```

Runs S01–S08 and collects one JMH JSON per scenario under `benchmarks/target/results/<timestamp>-<tier>/`. Expect it to take hours — run it on a quiet, plugged-in machine with no IDE, browser, or other containers competing for the CPU. It is configured through environment variables:

| Variable | Effect |
| --- | --- |
| `TIER` | data volume, defaults to `MEDIUM` |
| `REVERSE=1` | runs the scenarios back to front |
| `PARAM_REVERSE=1` | reverses the parameter order within each scenario |
| `PROFILE_GC=1` | adds the JMH GC profiler for allocation data |
| `EXTRA_ARGS` | extra JMH arguments appended to every run |

The two reverse switches exist as drift controls: JMH otherwise always runs the axes in the same order, so a slow machine warming up over time would look like a property of whichever implementation ran second. Run a pass forward, run one reversed, and compare.

## Module layout

| Module | Contents |
| --- | --- |
| `core` | repository interfaces, shared DTOs, scenario definitions; depends on nothing |
| `db` | Flyway migrations and the deterministic data seeder |
| `persistence-hibernate` | the contracts implemented with Hibernate/JPA |
| `persistence-jooq` | the contracts implemented with jOOQ; hosts the generated code |
| `benchmarks` | the JMH harness, and the only module that sees both implementations |
