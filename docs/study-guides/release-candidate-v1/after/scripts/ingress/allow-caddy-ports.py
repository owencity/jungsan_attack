#!/usr/bin/env python3
"""OCI 기존 iptables를 유지하고 Caddy의 TCP 80·443만 허용한다."""

import os
from pathlib import Path
import shlex
import subprocess
import sys
import tempfile

RULES = tuple(
    f'-A INPUT -p tcp -m tcp --dport {port} -m comment --comment jeongsan-caddy -j ACCEPT'
    for port in (80, 443)
)


def plan_rules(original):
    lines = original.splitlines()
    reject_indices = [
        index for index, line in enumerate(lines)
        if line == '-A INPUT -j REJECT --reject-with icmp-host-prohibited'
    ]
    if len(reject_indices) != 1:
        raise ValueError('예상한 OCI INPUT 최종 거절 규칙이 하나여야 한다.')
    reject_index = reject_indices[0]
    if not any(
        line.startswith('-A INPUT ') and '--dport 22 ' in line and line.endswith('-j ACCEPT')
        for line in lines[:reject_index]
    ):
        raise ValueError('기존 SSH 22 허용 규칙을 먼저 확인해야 한다.')
    for rule in RULES:
        matches = [index for index, line in enumerate(lines) if line == rule]
        if len(matches) > 1 or (matches and matches[0] > reject_index):
            raise ValueError('Caddy 허용 규칙이 중복됐거나 최종 거절 뒤에 있다.')
    missing = [rule for rule in RULES if rule not in lines]
    return '\n'.join(lines[:reject_index] + missing + lines[reject_index:]) + '\n', missing


def run(command, **kwargs):
    return subprocess.run(command, check=True, text=True, **kwargs)


def main():
    if os.geteuid() != 0 or len(sys.argv) != 2:
        raise SystemExit('root 권한과 설치 스크립트가 만든 백업 디렉터리가 필요하다.')
    backup_directory = Path(sys.argv[1]).resolve()
    if backup_directory.parent != Path('/var/backups/jeongsan-caddy') or not backup_directory.is_dir():
        raise SystemExit('정산어택 Caddy 설치 백업 디렉터리만 사용한다.')
    saved_path = Path('/etc/iptables/rules.v4')
    saved_rules = saved_path.read_text()
    active_rules = run(['iptables', '-w', '10', '-S', 'INPUT'], capture_output=True).stdout
    planned_saved, _ = plan_rules(saved_rules)
    _, missing_active = plan_rules(active_rules)
    (backup_directory / 'rules.v4').write_text(saved_rules)
    os.chmod(backup_directory / 'rules.v4', 0o600)
    # Docker·Tailscale 동적 체인은 복원하지 않고 현황 참고 자료로만 보존한다.
    active_snapshot = backup_directory / 'iptables-live.txt'
    with active_snapshot.open('w') as snapshot:
        run(['iptables-save'], stdout=snapshot)
    os.chmod(active_snapshot, 0o600)

    temporary_path = None
    inserted_rules = []
    try:
        with tempfile.NamedTemporaryFile(mode='w', dir=saved_path.parent, delete=False) as candidate:
            temporary_path = Path(candidate.name)
            candidate.write(planned_saved)
        os.chmod(temporary_path, saved_path.stat().st_mode & 0o777)
        # --test는 문법만 확인한다. 실제 체인을 덮어쓰는 restore/save는 하지 않는다.
        with temporary_path.open() as candidate:
            run(['iptables-restore', '--test'], stdin=candidate)
        for rule in missing_active:
            current_lines = run(['iptables', '-w', '10', '-S', 'INPUT'], capture_output=True).stdout.splitlines()
            _, still_missing = plan_rules('\n'.join(current_lines))
            if rule not in still_missing:
                continue
            chain_rules = [line for line in current_lines if line.startswith('-A INPUT ')]
            reject_position = chain_rules.index('-A INPUT -j REJECT --reject-with icmp-host-prohibited') + 1
            run(['iptables', '-w', '10', '-I', 'INPUT', str(reject_position), *shlex.split(rule)[2:]])
            inserted_rules.append(rule)
        os.replace(temporary_path, saved_path)
        temporary_path = None
    except Exception:
        # 실패 시 이 실행이 추가한 두 규칙만 제거한다. 기존 체인을 전체 복원하지 않는다.
        for rule in reversed(inserted_rules):
            subprocess.run(['iptables', '-w', '10', '-D', 'INPUT', *shlex.split(rule)[2:]], check=False)
        raise
    finally:
        if temporary_path is not None:
            temporary_path.unlink(missing_ok=True)
    print('TCP 80·443 허용과 재부팅용 rules.v4 저장 완료. 기존 규칙은 유지했다.')


if __name__ == '__main__':
    main()
