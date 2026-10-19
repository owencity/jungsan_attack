#!/usr/bin/env python3
"""실제 Caddy로 경로·401·POST 전달과 오류 로그의 인증 정보 제거를 검증한다."""
import http.client
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
from pathlib import Path
import socket
import subprocess
import tempfile
import threading
import time


class ApplicationHandler(BaseHTTPRequestHandler):
    requests = []

    def log_message(self, *_):
        pass

    def do_GET(self):
        self.respond(200)

    def do_POST(self):
        self.respond(401)

    def respond(self, status):
        length = int(self.headers.get('Content-Length', '0'))
        body = self.rfile.read(length).decode()
        type(self).requests.append((self.command, self.path, body))
        response = json.dumps({'method': self.command, 'path': self.path, 'body': body}).encode()
        self.send_response(status)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', 'https://jungsan.devkdk.com')
        self.send_header('Set-Cookie', 'dummy=value; HttpOnly; Secure; SameSite=Lax')
        self.send_header('Content-Length', str(len(response)))
        self.end_headers()
        self.wfile.write(response)


def main():
    root = Path(__file__).resolve().parent.parent
    adapted = subprocess.run(
        ['caddy', 'adapt', '--config', str(root / 'infra/caddy/Caddyfile'), '--adapter', 'caddyfile'],
        check=True, text=True, capture_output=True,
    )
    config = json.loads(adapted.stdout)
    application = ThreadingHTTPServer(('127.0.0.1', 0), ApplicationHandler)
    application_thread = threading.Thread(target=application.serve_forever, daemon=True)
    application_thread.start()
    with socket.socket() as listener:
        listener.bind(('127.0.0.1', 0))
        ingress_port = listener.getsockname()[1]

    # 운영 인증서·서비스·관리 API와 충돌하지 않는 임시 HTTP 리스너에서 라우팅만 검증한다.
    # 실제 TLS 신뢰 검증은 운영 도메인으로 별도 실행한다.
    config['admin'] = {'disabled': True}
    server = config['apps']['http']['servers']['srv0']
    server['listen'] = [f'127.0.0.1:{ingress_port}']
    server['automatic_https'] = {'disable': True}
    config['apps'].pop('tls', None)

    def replace_upstream(value):
        if isinstance(value, dict):
            for key, entry in value.items():
                if key == 'dial' and entry == '127.0.0.1:18080':
                    value[key] = f'127.0.0.1:{application.server_port}'
                else:
                    replace_upstream(entry)
        elif isinstance(value, list):
            for entry in value:
                replace_upstream(entry)
    replace_upstream(server)

    def request(method, path, body=None):
        connection = http.client.HTTPConnection('127.0.0.1', ingress_port, timeout=5)
        connection.request(method, path, body=body, headers={
            'Host': 'jungsan-api.devkdk.com',
            'Authorization': 'Bearer DUMMY_TOKEN',
            'Cookie': 'session=DUMMY_COOKIE',
        })
        response = connection.getresponse()
        result = response.status, dict(response.getheaders()), response.read().decode()
        connection.close()
        return result

    with tempfile.TemporaryFile(mode='w+') as log:
        process = subprocess.Popen(['caddy', 'run', '--config', '-'], stdin=subprocess.PIPE, stdout=log, stderr=log, text=True)
        process.stdin.write(json.dumps(config))
        process.stdin.close()
        try:
            for _ in range(50):
                if process.poll() is not None:
                    raise RuntimeError('테스트 Caddy가 기동 중 종료됐다.')
                try:
                    request('GET', '/not-public')
                    break
                except (OSError, http.client.HTTPException):
                    time.sleep(0.1)
            else:
                raise RuntimeError('테스트 Caddy 시작 시간 초과.')

            status, headers, body = request('GET', '/actuator/health')
            assert status == 200 and json.loads(body)['path'] == '/actuator/health'
            status, headers, body = request('POST', '/api/v1/auth/apple/callback?state=DUMMY_STATE', 'code=DUMMY_CODE')
            assert status == 401
            assert json.loads(body) == {'method': 'POST', 'path': '/api/v1/auth/apple/callback?state=DUMMY_STATE', 'body': 'code=DUMMY_CODE'}
            assert headers['Access-Control-Allow-Origin'] == 'https://jungsan.devkdk.com'
            assert 'HttpOnly; Secure; SameSite=Lax' in headers['Set-Cookie']
            before_denied = len(ApplicationHandler.requests)
            for path in ('/actuator/prometheus', '/actuator/metrics', '/actuator/health/extra', '/api', '/admin'):
                assert request('GET', path)[0] == 404
            assert len(ApplicationHandler.requests) == before_denied
            # upstream 장애로 오류 로그가 실제 발생해도 쿼리·인증 헤더가 삭제되는지 확인한다.
            application.shutdown()
            application.server_close()
            status, _, _ = request('GET', '/api/v1/failure?code=DUMMY_CODE&state=DUMMY_STATE')
            assert status == 502
        finally:
            process.terminate()
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=5)
            application.shutdown()
            application.server_close()
        log.seek(0)
        entries = [json.loads(line) for line in log if line.startswith('{')]
        error_entries = [entry for entry in entries if entry.get('logger') == 'http.log.error']
        assert error_entries, '오류 로그가 실제로 생성되어야 제거 동작을 검증할 수 있다.'
        for entry in error_entries:
            assert 'uri' not in entry['request'] and 'headers' not in entry['request']
            assert not any(dummy in json.dumps(entry) for dummy in ('DUMMY_CODE', 'DUMMY_STATE', 'DUMMY_TOKEN', 'DUMMY_COOKIE'))
    print('실제 Caddy 검증 통과: 경로 제한·POST/401·CORS/쿠키 전달·장애 502·오류 로그 인증 정보 제거.')


if __name__ == '__main__':
    main()
