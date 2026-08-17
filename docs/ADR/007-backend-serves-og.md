# ADR-007 · 공유 링크는 백엔드가 HTML로 응답한다

**상태** 확정 · **관련** `SPEC.md` §3, `API.md` §5.1

## 맥락

`SPEC.md` §3은 공유 링크가 **반드시 웹 URL**이어야 한다고 정했다. 참여자는 앱을
설치하지 않고 단톡방 링크로 들어온다(`ADR-003`). 그래서 **단톡방에 뜨는 링크 카드가
이 서비스의 첫 화면**이다.

그런데 카드가 만들어지지 않는다. 배포본을 실측한 결과다.

```
GET https://jungsan.devkdk.com/  (index.html)
  og: 태그          0개
  <title>           김동규 | Backend Developer
  <meta description> 백엔드 개발자 김동규의 개인 사이트 — … 24시간이모자라 …
```

**지금 단톡방에 정산 링크를 붙이면 카드에 포트폴리오 제목이 뜬다.** 받은 사람은
무슨 링크인지 알 수 없어 누르지 않는다.

### 프론트에서 고칠 수 없다

카카오톡·슬랙 등은 링크를 붙이면 **자기 크롤러로 그 URL을 직접 GET** 해서
HTML `<head>`의 `og:*` 태그를 읽어 카드를 만든다.

```
사람의 브라우저          카카오톡 크롤러
① HTML 받음             ① HTML 받음
② JS 실행               ② JS 실행 안 함
③ API 호출              ③ API 호출 안 함
④ 모임 이름 보임         ④ <title> 만 읽고 끝
```

프론트는 CSR SPA다. **모임 이름은 JS가 API를 부른 뒤에 생긴다.** 크롤러가 받는
HTML에는 그 정보가 아직 없으므로 `document.title`을 바꿔도 카드는 바뀌지 않는다.

**이것은 화면 코드의 문제가 아니라 누가 그 HTML을 만들어 주느냐의 문제다.**

## 결정

**공유 링크를 백엔드 호스트로 두고, 백엔드가 OG 태그가 든 HTML을 응답한다.**

```
단톡방에 뿌리는 주소   https://join.devkdk.com/g/{token}      ← 백엔드
사람이 도달하는 주소   https://jungsan.devkdk.com/g/{token}   ← 프론트
```

### 크롤러와 사람을 구분하지 않는다

**같은 HTML 하나로 둘 다 처리된다.**

```html
<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8">
  <meta property="og:type"        content="website">
  <meta property="og:site_name"   content="정산어택">
  <meta property="og:title"       content="8월 팀 회식 · 정산어택">
  <meta property="og:description" content="동규님이 정산에 초대했습니다. 참석한 차수를 체크해주세요.">
  <meta property="og:image"       content="https://join.devkdk.com/static/og-512.png">
  <meta property="og:url"         content="https://join.devkdk.com/g/{token}">
  <title>8월 팀 회식 · 정산어택</title>
</head>
<body>
  <script>location.replace("https://jungsan.devkdk.com/g/{token}")</script>
  <noscript><a href="https://jungsan.devkdk.com/g/{token}">정산어택에서 열기</a></noscript>
</body>
</html>
```

```
크롤러 → JS 를 실행하지 않는다  →  og: 태그만 읽고 카드를 만든다
사람   → JS 를 실행한다        →  즉시 프론트로 넘어간다
```

**User-Agent 를 판별하지 않는다.** 이게 이 결정의 핵심이다 — 아래 §근거 참조.

### 경로는 프론트와 같게 둔다

호스트가 다르므로 **`/g/{token}` 을 양쪽에서 쓸 수 있다.** 리다이렉트는 호스트만
바꾸는 것이 되어, 프론트 라우팅을 하나도 건드리지 않는다.

### 토큰이 없거나 틀리면

```
404 → OG 태그 없이 "링크가 만료되었거나 잘못된 주소입니다" HTML
```

**404 에도 HTML을 준다.** JSON을 주면 카톡 카드에 원문 JSON이 노출된다.
그리고 **모임이 존재하는지 여부를 카드로 알려주지 않는다** — 존재 여부가
`API.md` §5.1의 무작위 대입 방어와 직결된다.

### 요청 제한을 여기서 건다

`API.md` §5.1이 정한 값을 **이 HTML 엔드포인트에도 같이 적용한다.**

```
IP 당 요청        분당 30회
연속 404          5회 → 10분 차단
성공(200)         세지 않는다
```

무작위 대입은 JSON API 뿐 아니라 **이 HTML 주소로도 들어온다.** 오히려 이쪽이
로그인도 필요 없어 더 쉬운 표적이다.

## 근거

- **User-Agent 판별을 없앨 수 있다.** 이것이 A안을 고른 첫 번째 이유다.
  UA 목록은 카카오톡이 값을 바꾸거나 새 메신저가 생기면 조용히 낡는다.
  그리고 **실패가 에러로 나타나지 않는다** — 예외도 로그도 없이 "카드가 안 예쁨"으로만
  드러나므로, 단톡방에 뿌려보고 눈으로 확인해야 알 수 있다. 판별을 아예 없애는 것이 맞다
- **프론트 라우팅을 건드리지 않는다.** 호스트가 다르므로 `/g/{token}` 이 충돌하지 않는다
- **경로가 짧다.** 카카오톡 크롤러 → 터널 → 백엔드. 프록시 한 단이 없다
- **OG 문구를 만드는 곳이 데이터가 있는 곳과 같다.** 모임 이름·주최자 이름을
  가진 것은 백엔드다. 다른 계층에서 만들면 그 계층도 API를 불러야 한다
- **Cloudflare 설정을 되돌리지 않는다.** `jungsan.devkdk.com` 은 DNS only(회색 구름)로
  두었고, Vercel이 인증서를 직접 발급해 동작을 확인했다. 그대로 유지된다

### 검토했다가 버린 안

#### B · Vercel rewrite 로 `/g/*` 를 백엔드에 프록시

`jungsan.devkdk.com/g/{token}` 주소를 그대로 쓸 수 있어 매력적이다. **그런데
사람을 되돌릴 곳이 없다.** `/g/{token}` 으로 보내면 다시 프록시로 들어가 순환한다.

앱 경로를 `/join/{token}` 으로 옮기면 해결되지만, 그러면 **공유는 `/g/`, 실제 앱은
`/join/` 인 두 경로를 계속 관리**해야 한다. 주소를 예쁘게 유지한 이득이 절반 사라진다.

그리고 모든 클릭이 `Vercel → 터널 → 백엔드` 를 거친다.

#### C · Cloudflare Worker 가 User-Agent 로 갈라 보낸다

주소가 그대로고 순환도 없다. **요청 제한을 Cloudflare 가 걸어 우리 서버에 닿기
전에 끊는 이점**도 있다.

버린 이유는 **UA 판별이 필수라는 점**이다(위 §근거 첫 항목). 부수적으로
`jungsan.devkdk.com` 을 오렌지 구름으로 되돌리고 `SSL/TLS` 를 `Full (strict)` 로
맞춰야 한다 — 방금 확인한 설정을 뒤집는 것이다.

#### D · Vercel Function 이 OG HTML 을 만든다

주소도 그대로고 UA 판별도 없다. 버린 이유는 **OG 문구를 만드는 곳이 데이터가
있는 곳과 갈라지는 것**이다. 함수가 다시 백엔드 API를 불러야 하고, API 주소·
타임아웃·오류 처리를 프론트 인프라에 한 벌 더 두게 된다.

## 대가

- **단톡방에 뜨는 주소가 `jungsan.devkdk.com` 이 아니다.** `join.devkdk.com` 이
  초대 링크로 읽히도록 고른 이름이지만, 앱 주소와 다르다는 사실은 남는다
- **리다이렉트가 한 번 낀다.** 카카오톡 인앱 브라우저에서 짧은 깜빡임이 있다.
  `location.replace` 라 뒤로 가기 이력에는 남지 않는다
- **주소창에서 복사한 링크는 카드가 안 뜬다.** 참여자가 `jungsan.devkdk.com/g/…` 를
  복사해 다시 공유하면 OG 를 거치지 않는다. 주최자 화면이 항상
  `join.devkdk.com` 주소를 보여주고 복사 버튼을 주는 것으로 줄인다
- **백엔드가 내려가면 링크가 죽는다.** 다만 백엔드 없이는 앱 자체가 동작하지
  않으므로 새로 생기는 취약점은 아니다

## 재검토 조건

- 프론트를 SSR(Next.js 등)로 옮기면 이 결정이 필요 없어진다. **그때는 프론트가
  직접 OG를 낼 수 있으므로 이 엔드포인트를 걷어낸다**
- 카카오톡이 JS 를 실행하는 크롤러로 바뀌면 (가능성 낮음) 역시 필요 없어진다

## 회원님이 해야 하는 콘솔 작업

**백엔드가 뜬 뒤에** Cloudflare 에서 한 번만 하면 된다. 이미 `dev_turnel` 이
`api.devkdk.com` 을 태우고 있으므로 그 터널에 호스트 하나를 더 붙이는 것이다.

```
Cloudflare > Zero Trust > Networks > Tunnels > dev_turnel > Configure
  Public Hostname > Add a public hostname
     Subdomain   join
     Domain      devkdk.com
     Path        (비움)
     Service     HTTP  →  localhost:8080
```

**DNS 레코드는 따로 만들지 않는다.** 터널에 호스트를 추가하면 Cloudflare 가
CNAME 을 자동으로 만든다.

> `api.devkdk.com` 과 같은 포트를 가리켜도 된다. 같은 Spring 앱이 경로로 갈라서
> 응답한다 — `/api/v1/**` 은 JSON, `/g/**` 은 HTML.
> 호스트를 나누는 이유는 순전히 **단톡방에 보이는 주소** 때문이다.
