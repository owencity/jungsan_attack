"""CI 전용 운영 프로필에서 Apple 미설정과 카카오 인가·인증 경계를 검증한다."""
import json
import os
import urllib.error
import urllib.parse
import urllib.request

if os.environ.get('GITHUB_ACTIONS') != 'true':
    raise SystemExit('CI 전용 로컬 서버만 검증한다.')
base = 'http://127.0.0.1:18081'

class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, file, code, message, headers, new_url):
        return None

opener = urllib.request.build_opener(NoRedirect())

def get(path):
    try:
        response = opener.open(base + path, timeout=10)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        return response.status, response.headers, response.read()

status, _, body = get('/actuator/health')
assert status == 200 and json.loads(body)['status'] == 'UP'
for client_query in ('', '?client=app&codeChallenge=' + 'a' * 43):
    status, headers, body = get('/api/v1/auth/apple/login' + client_query)
    assert status == 503 and json.loads(body)['code'] == 'AUTH_PROVIDER_UNAVAILABLE'
    assert not headers.get('Location') and not headers.get('Set-Cookie')
status, headers, _ = get('/api/v1/auth/kakao/login')
assert status == 302
location = urllib.parse.urlsplit(headers['Location'])
assert location.scheme == 'https' and location.hostname == 'kauth.kakao.com'
parameters = urllib.parse.parse_qs(location.query)
assert parameters['client_id'] == ['ci-kakao-client']
assert parameters['redirect_uri'] == ['https://api.test/api/v1/auth/kakao/callback']
assert len(parameters['state'][0]) == 43
cookie = headers['Set-Cookie']
assert 'jeongsan_auth_' in cookie and 'HttpOnly' in cookie and 'Secure' in cookie and 'SameSite=Lax' in cookie
status, _, body = get('/api/v1/auth/me')
assert status == 401 and json.loads(body)['code'] == 'UNAUTHENTICATED'
status, _, body = get('/api/v1/me/gatherings')
assert status == 401 and json.loads(body)['code'] == 'UNAUTHENTICATED'
print('prod Apple 미설정 HTTP 6건 통과: health·WEB/APP 503·카카오 인가·인증 가드')
