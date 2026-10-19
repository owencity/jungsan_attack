#!/usr/bin/env python3
"""공유 OCI VM의 SSH·Docker 규칙을 훼손하지 않는 방화벽 변경인지 검증한다."""
import importlib.util
from pathlib import Path
import unittest

module_spec = importlib.util.spec_from_file_location('caddy_firewall', Path(__file__).parent / 'ingress/allow-caddy-ports.py')
firewall = importlib.util.module_from_spec(module_spec)
module_spec.loader.exec_module(firewall)

BASE = '''*filter
:INPUT ACCEPT [0:0]
-A INPUT -j ts-input
-A INPUT -m state --state RELATED,ESTABLISHED -j ACCEPT
-A INPUT -i lo -j ACCEPT
-A INPUT -p tcp -m state --state NEW -m tcp --dport 22 -j ACCEPT
-A INPUT -j REJECT --reject-with icmp-host-prohibited
-A FORWARD -j DOCKER-USER
-A OUTPUT -d 169.254.0.0/16 -j InstanceServices
COMMIT
'''


class FirewallPlanSpec(unittest.TestCase):
    def test_add_only_http_https_before_reject(self):
        planned, missing = firewall.plan_rules(BASE)
        self.assertEqual(len(missing), 2)
        self.assertEqual([line for line in planned.splitlines() if line not in firewall.RULES], BASE.splitlines())
        for rule in firewall.RULES:
            self.assertLess(planned.index(rule), planned.index('-A INPUT -j REJECT'))

    def test_repeat_does_not_duplicate(self):
        planned, _ = firewall.plan_rules(BASE)
        repeated, missing = firewall.plan_rules(planned)
        self.assertEqual(repeated, planned)
        self.assertEqual(missing, [])

    def test_partial_install_adds_only_missing_port(self):
        partial = BASE.replace('-A INPUT -j REJECT', firewall.RULES[0] + '\n-A INPUT -j REJECT')
        _, missing = firewall.plan_rules(partial)
        self.assertEqual(missing, [firewall.RULES[1]])

    def test_unknown_firewall_layout_is_rejected(self):
        with self.assertRaises(ValueError):
            firewall.plan_rules(BASE.replace('-j REJECT --reject-with icmp-host-prohibited', '-j DROP'))

    def test_missing_ssh_allow_is_rejected(self):
        with self.assertRaises(ValueError):
            firewall.plan_rules(BASE.replace('--dport 22', '--dport 2222'))

    def test_ssh_rule_after_reject_is_rejected(self):
        ssh_rule = '-A INPUT -p tcp -m state --state NEW -m tcp --dport 22 -j ACCEPT\n'
        with self.assertRaises(ValueError):
            firewall.plan_rules(BASE.replace(ssh_rule, '').replace('COMMIT', ssh_rule + 'COMMIT'))

    def test_duplicate_rule_is_rejected(self):
        with self.assertRaises(ValueError):
            firewall.plan_rules(BASE.replace('-A INPUT -j REJECT', (firewall.RULES[0] + '\n') * 2 + '-A INPUT -j REJECT'))

    def test_rule_after_reject_is_rejected(self):
        with self.assertRaises(ValueError):
            firewall.plan_rules(BASE.replace('COMMIT', firewall.RULES[0] + '\nCOMMIT'))


if __name__ == '__main__':
    unittest.main()
