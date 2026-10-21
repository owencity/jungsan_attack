"""로컬 CI 또는 지정 운영 호스트의 CORS 경계만 확인한다. 제공자 인증·리다이렉트는 실행하지 않는다."""
import argparse
import json
import urllib.error
import urllib.request

parser = argparse.ArgumentParser()
parser.add_argument('--production', action='store_true')
arguments = parser.parse_args()
base = 'https://jungsan-api.devkdk.com' if arguments.production else 'http://127.0.0.1:18081'
apple_origin = 'https://appleid.apple.com'
callback = '/api/v1/auth/apple/callback'

class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, response, code, message, headers, new_url):
        return None

opener = urllib.request.build_opener(NoRedirect())
checks = []

def call(method, path, origin=None, form=None, preflight_method=None):
    headers = {}
    if origin:
        headers['Origin'] = origin
    if form is not None:
        headers['Content-Type'] = 'application/x-www-form-urlencoded'
    if preflight_method:
        headers['Access-Control-Request-Method'] = preflight_method
    request = urllib.request.Request(base + path, headers=headers, method=method,
                                     data=form.encode() if form is not None else None)
    try:
        response = opener.open(request, timeout=20)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        return response.status, response.headers, response.read()

def check(condition, label, status):
    assert condition, (label, status)
    checks.append({'check': label, 'status': status})

status, headers, body = call('POST', callback, apple_origin, 'code=x&state=y')
check(status == 401 and json.loads(body)['code'] == 'UNAUTHENTICATED'
      and headers.get('Access-Control-Allow-Origin') == apple_origin
      and headers.get('Access-Control-Allow-Credentials') == 'true', 'invalid state reaches existing authentication handler', status)
for method, path, form in [('GET', '/api/v1/auth/me', None),
                           ('GET', '/api/v1/auth/kakao/callback?code=x&state=y', None),
                           ('POST', '/api/v1/auth/app/exchange', '')]:
    status, headers, body = call(method, path, apple_origin, form)
    check(status == 403 and body == b'Invalid CORS request' and not headers.get('Access-Control-Allow-Origin'),
          'Apple origin denied: ' + path.split('?')[0], status)
for origin in ['https://evil.test', 'https://appleid.apple.com.evil.test', 'http://appleid.apple.com']:
    status, headers, body = call('POST', callback, origin, 'code=x&state=y')
    check(status == 403 and body == b'Invalid CORS request', 'callback rejects origin: ' + origin, status)
status, headers, body = call('OPTIONS', callback, apple_origin, preflight_method='POST')
check(status == 200 and headers.get('Access-Control-Allow-Methods') == 'POST', 'Apple POST preflight allowed', status)
status, headers, body = call('OPTIONS', callback, apple_origin, preflight_method='GET')
check(status == 403, 'Apple GET preflight denied', status)
status, headers, body = call('GET', '/actuator/health')
check(status == 200 and json.loads(body)['status'] == 'UP', 'health', status)
if arguments.production:
    # Cookie値・state・nonce・Locationは出力しない。未完了のchallengeは既存の5分TTLで失効する。
    for client in ['web', 'app']:
        query = '?client=' + client + ('&codeChallenge=' + 'a' * 43 if client == 'app' else '')
        status, headers, body = call('GET', '/api/v1/auth/apple/login' + query)
        cookie = headers.get('Set-Cookie', '')
        check(status == 302 and all(attribute in cookie for attribute in
              ['jeongsan_auth_', 'SameSite=None', 'Secure', 'HttpOnly', 'Path=/api/v1/auth', 'Max-Age=300']),
              client + ' production state cookie flags', status)
    status, headers, body = call('GET', '/api/v1/auth/me', 'https://jungsan.devkdk.com')
    check(status == 401 and headers.get('Access-Control-Allow-Origin') == 'https://jungsan.devkdk.com',
          'existing frontend CORS preserved', status)
print(json.dumps({'base': base, 'passed': True, 'checks': checks,
                  'real_account_browser_e2e': 'not performed'}, indent=2))
