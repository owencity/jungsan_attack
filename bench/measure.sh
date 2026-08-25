#!/usr/bin/env bash
# 유휴 상태 메모리 실측. 안정화를 기다린 뒤 docker stats 를 여러 번 떠서 평균을 낸다.
# 한 번만 재면 기동 직후 피크가 잡혀 과대평가된다.
set -u
cd "$(dirname "$0")"

SETTLE=${SETTLE:-90}
SAMPLES=${SAMPLES:-6}
INTERVAL=${INTERVAL:-10}

echo "== 기동 =="
docker compose -f compose.measure.yml up -d || exit 1

echo "== 안정화 대기 ${SETTLE}초 =="
sleep "$SETTLE"

echo "== ${INTERVAL}초 간격으로 ${SAMPLES}회 표본 =="
: > /tmp/samples.txt
for i in $(seq 1 "$SAMPLES"); do
  docker stats --no-stream --format '{{.Name}}\t{{.MemUsage}}\t{{.MemPerc}}\t{{.CPUPerc}}' >> /tmp/samples.txt
  printf '  표본 %d/%d\n' "$i" "$SAMPLES"
  [ "$i" -lt "$SAMPLES" ] && sleep "$INTERVAL"
done

echo
echo "== 결과 (MiB 평균) =="
python - <<'PY'
import re, collections
rows = collections.defaultdict(list)
for line in open('/tmp/samples.txt', encoding='utf-8', errors='replace'):
    p = line.rstrip('\n').split('\t')
    if len(p) < 4: continue
    name, usage = p[0], p[1]
    m = re.match(r'([\d.]+)\s*([KMG]i?B)', usage)
    if not m: continue
    v, u = float(m.group(1)), m.group(2)
    mib = v / 1024 if u.startswith('K') else v if u.startswith('M') else v * 1024
    rows[name].append(mib)

total = 0.0
print(f"{'컨테이너':<28}{'평균 MiB':>10}{'최대 MiB':>10}{'표본':>6}")
print('-' * 56)
for name in sorted(rows):
    vs = rows[name]
    avg, mx = sum(vs)/len(vs), max(vs)
    total += avg
    print(f"{name:<28}{avg:>10.0f}{mx:>10.0f}{len(vs):>6}")
print('-' * 56)
print(f"{'인프라 합계':<28}{total:>10.0f} MiB  ({total/1024:.2f} GiB)")
print()
print(f"{'앱 JVM 예상 1GB 포함':<28}{total+1024:>10.0f} MiB  ({(total+1024)/1024:.2f} GiB)")
print(f"{'12GB 대비 여유':<28}{12*1024-(total+1024):>10.0f} MiB")
PY
echo
echo "== 정리하려면 =="
echo "  docker compose -f bench/compose.measure.yml down"
