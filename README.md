# jooq-hibernate-comparison

Source code for a master's thesis comparing jOOQ and Hibernate. One order-management application has two persistence layers, one per library, behind shared repository interfaces, so both can be run through the same scenarios and measured.

## Requirements

- JDK 21
- A running Docker daemon

PostgreSQL 16 is started through Testcontainers for the tests, for jOOQ code generation, and for every benchmark fork. There is no in-memory fallback, so nothing builds without Docker. Maven itself is not needed, the bundled wrapper (`./mvnw`) is enough.

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

Then run one scenario. S01 is the CRUD scenario; the flags below reduce it to one fork with one warmup and one measurement iteration, which is enough to check that the harness works:

```
./mvnw -Pbench -pl benchmarks exec:exec \
  -Dbench.args="S01 -f 1 -wi 1 -i 1 -p config=default"
```

Drop the `-f -wi -i` overrides to get the measured configuration declared on the benchmark class. Re-run `./mvnw install -DskipTests` after changing any code outside the `benchmarks` module, otherwise the harness keeps running the previously installed version.

`bench.args` accepts any JMH option (`-h` lists them). The ones used in this project:

| Argument | Meaning |
| --- | --- |
| `S01` to `S08` | scenario selector; `V01` validates the benchmark wiring itself |
| `-p stack=hibernate,jooq` | the two implementations under comparison |
| `-p config=default,tuned` | stock settings vs. tuned ones |
| `-p tier=SMALL\|MEDIUM\|LARGE` | seeded data volume, defaults to `SMALL` |
| `-rf json -rff target/result.json` | write machine-readable results |

Omitting a `-p` runs every value of that axis, so the command above measures both stacks in one go.

## Running the full matrix

```
./run-benchmarks.sh
```

Runs S01-S08 and saves one JMH JSON file per scenario under `benchmarks/target/results/<timestamp>-<tier>/`. A full run takes several hours. Run it on a plugged-in machine with the IDE, browser and other containers closed. Settings are passed as environment variables:

| Variable | Effect |
| --- | --- |
| `TIER` | data volume, defaults to `MEDIUM` |
| `REVERSE=1` | runs the scenarios back to front |
| `PARAM_REVERSE=1` | reverses the parameter order within each scenario |
| `PROFILE_GC=1` | adds the JMH GC profiler for allocation data |
| `EXTRA_ARGS` | extra JMH arguments appended to every run |

JMH always runs the parameters in the same order, so if the machine slows down during a run, the implementation measured second looks worse. The two reverse switches change that order, and comparing a forward and a reversed run shows whether it happened.

## Module layout

| Module | Contents |
| --- | --- |
| `core` | repository interfaces, shared DTOs and scenario definitions, no dependencies |
| `db` | Flyway migrations and the deterministic data seeder |
| `persistence-hibernate` | repository implementations using Hibernate/JPA |
| `persistence-jooq` | repository implementations using jOOQ, plus the generated jOOQ classes |
| `benchmarks` | JMH benchmarks; the only module that depends on both implementations |
