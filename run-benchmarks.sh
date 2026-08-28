#!/usr/bin/env bash

set -euo pipefail
cd "$(dirname "$0")"

TIER="${TIER:-MEDIUM}"
REVERSE="${REVERSE:-0}"
PARAM_REVERSE="${PARAM_REVERSE:-0}"
PROFILE_GC="${PROFILE_GC:-0}"
EXTRA_ARGS="${EXTRA_ARGS:-}"

docker info >/dev/null 2>&1 || { echo "Docker daemon is not running"; exit 1; }

scenarios=(S01 S02 S03 S04 S05 S06 S07 S08)
if [[ "$REVERSE" == "1" ]]; then
  scenarios=(S08 S07 S06 S05 S04 S03 S02 S01)
fi
if [[ "$PARAM_REVERSE" == "1" ]]; then
  EXTRA_ARGS="$EXTRA_ARGS -p stack=jooq,hibernate -p config=tuned,default"
fi

stamp="$(date +%Y%m%d-%H%M%S)"
suffix=""
if [[ "$REVERSE" == "1" ]]; then suffix="-reversed"; fi
if [[ "$PARAM_REVERSE" == "1" ]]; then suffix="${suffix}-paramrev"; fi
label="${stamp}-${TIER}${suffix}"
outdir="benchmarks/target/results/${label}"
mkdir -p "$outdir"

echo "== installing modules (tests skipped; codegen still runs)"
./mvnw -q install -DskipTests

echo "== matrix: ${scenarios[*]} | tier=$TIER | gc=$PROFILE_GC | results -> $outdir"
for scenario in "${scenarios[@]}"; do
  args="$scenario -p tier=$TIER -rf json -rff target/results/${label}/${scenario}.json"
  [[ "$PROFILE_GC" == "1" ]] && args="$args -prof gc"
  [[ -n "$EXTRA_ARGS" ]] && args="$args $EXTRA_ARGS"
  if [[ "$PARAM_REVERSE" == "1" ]]; then
    case "$scenario" in
      S03) args="$args -p loadStyle=multiload,find" ;;
      S06) args="$args -p reportingStyle=criteria,hql,native" ;;
    esac
  fi
  echo "== $(date +%H:%M:%S) running $scenario"
  ./mvnw -B -ntp -Pbench -pl benchmarks exec:exec -Dbench.args="$args" 2>&1 | tee "$outdir/${scenario}.log" \
    | grep -E '^# Run progress|^Benchmark |Iteration' | tail -2
done

echo "== done: $(ls "$outdir"/*.json 2>/dev/null | wc -l | tr -d ' ') result files in $outdir"
