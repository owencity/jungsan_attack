"""운영 DNS를 호출하지 않고 범위 제한·재시도·실제 결과 재검사를 검증한다."""
import importlib.util
import pathlib
import unittest

spec = importlib.util.spec_from_file_location("dns_sync", pathlib.Path(__file__).with_name("cloudflare-dns.py"))
dns = importlib.util.module_from_spec(spec)
spec.loader.exec_module(dns)
TUNNEL = "11111111-2222-4333-8444-555555555555"
ZONE = "a" * 32
RECORD = "b" * 32


class FakeCloudflare:
    def __init__(self, records, ignore_write=False):
        self.records = records
        self.calls = []
        self.ignore_write = ignore_write

    def request(self, method, path, body=None):
        self.calls.append((method, path, body))
        if path.startswith("/zones?"):
            return [{"name": dns.ZONE_NAME, "id": ZONE}]
        if method == "GET":
            return self.records
        if not self.ignore_write:
            self.records = [{"id": RECORD, **body}]
        return self.records


class DnsSpec(unittest.TestCase):
    def test_missing_record_created_then_retry_does_not_write(self):
        client = FakeCloudflare([])
        self.assertEqual(dns.sync(client, TUNNEL), "created")
        self.assertEqual(dns.sync(client, TUNNEL, ZONE), "unchanged")
        self.assertEqual([method for method, _, _ in client.calls if method != "GET"], ["POST"])

    def test_wrong_tunnel_updated_only_named_record(self):
        client = FakeCloudflare([{"name": dns.HOSTNAME, "id": RECORD, "type": "CNAME", "content": "old.cfargotunnel.com", "proxied": False}])
        self.assertEqual(dns.sync(client, TUNNEL, ZONE), "updated")
        writes = [call for call in client.calls if call[0] != "GET"]
        self.assertEqual(len(writes), 1)
        self.assertEqual(writes[0][1], f"/zones/{ZONE}/dns_records/{RECORD}")
        self.assertEqual(writes[0][2]["name"], dns.HOSTNAME)

    def test_duplicate_records_stop_without_write(self):
        client = FakeCloudflare([{"name": dns.HOSTNAME}, {"name": dns.HOSTNAME}])
        with self.assertRaises(dns.DnsError):
            dns.sync(client, TUNNEL, ZONE)
        self.assertTrue(all(call[0] == "GET" for call in client.calls))

    def test_other_hostname_stops_without_write(self):
        client = FakeCloudflare([{"name": "n8n.devkdk.com"}])
        with self.assertRaises(dns.DnsError):
            dns.sync(client, TUNNEL, ZONE)
        self.assertTrue(all(call[0] == "GET" for call in client.calls))

    def test_a_record_not_replaced(self):
        client = FakeCloudflare([{"name": dns.HOSTNAME, "type": "A"}])
        with self.assertRaises(dns.DnsError):
            dns.sync(client, TUNNEL, ZONE)
        self.assertTrue(all(call[0] == "GET" for call in client.calls))

    def test_failed_write_is_not_reported_as_success(self):
        client = FakeCloudflare([], ignore_write=True)
        with self.assertRaises(dns.DnsError):
            dns.sync(client, TUNNEL, ZONE)

    def test_invalid_identifier_makes_no_request(self):
        client = FakeCloudflare([])
        for tunnel, zone in [("invalid", ZONE), (TUNNEL, "../../other")]:
            with self.assertRaises(dns.DnsError):
                dns.sync(client, tunnel, zone)
        self.assertEqual(client.calls, [])


if __name__ == "__main__":
    unittest.main()
