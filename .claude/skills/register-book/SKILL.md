---
name: register-book
description: 책 제목 하나로 원서와 언어별 번역서를 조사해 COVERAGE 백엔드에 등록한다(기본 dry-run, 승인 후 등록). "책 등록", "번역서 등록", "/register-book 채식주의자", 등록할 책 추천 요청에 사용.
argument-hint: "<책 제목> [--yes]"
allowed-tools: Bash(python3 .claude/skills/register-book/api.py:*), Bash(curl:*), Bash(sips:*), WebSearch, WebFetch, Read, Write(.claude/skills/register-book/runs/**)
---

# register-book

책 제목 하나로 원서 + 언어별 번역서를 찾아 이 저장소의 백엔드에 등록한다.
**조사와 판단은 Claude가 직접**(WebSearch·WebFetch·curl로 공개 서지 API와 출판사 페이지를 보고, 표지는 눈으로 확인),
**서버에 쓰는 일은 `api.py`만** 한다(ISBN 검증, 중복 조회, 표지 업로드, 재시도, dry-run, 기록). 서버에 curl로 직접 쓰지 않는다.

인자: `$ARGUMENTS` — 책 제목, 선택 `--yes`(승인 없이 등록). 제목이 없으면 아래 "책 고르기"로 후보를 제안한다.

모든 명령은 저장소 루트에서 실행한다. `A=.claude/skills/register-book/api.py`
```
python3 $A self-check                # ISBN·형식 판별 assert
python3 $A login                     # 자격증명 확인, 에이전트 계정 ID
python3 $A isbn <ISBN>               # ISBN-13 정규화·체크섬
python3 $A cover-size <이미지URL>     # 실제 형식·픽셀 크기
python3 $A find-by-isbn <ISBN>       # 서버 중복 조회 (13·10자리 둘 다)
python3 $A search "<제목>"            # 서버 제목 검색 (참고용)
python3 $A apply <plan.json> [--execute]   # 기본 dry-run, runs/에 결과 저장
python3 $A serve [--port 8765]       # runs/ 실행 기록 JSON API → 백오피스(coverage-backoffice) '등록 기록' 화면이 읽는다
```

## 환경
- `COVERAGE_API_BASE`(기본 `http://localhost:8080/api`), `COVERAGE_EMAIL`, `COVERAGE_PASSWORD` — 에이전트 전용 계정.
  자격증명은 환경변수로만 받는다. 파일·runs·커밋에 쓰지 않는다. 없으면 사용자에게 export를 요청하고 멈춘다.
- 백엔드는 에이전트 계정 ID를 `RATE_LIMIT_EXEMPT_USER_IDS`, `IMAGE_RESOLUTION_EXEMPT_USER_IDS`에 넣고 띄워야 한다
  (없으면 시간당 5권에서 429 대기, ~500px 표지는 800×1200 미만이라 400).
- 선택 키: `NL_CERT_KEY`(국립중앙도서관, 한국 도서 표지), `NAVER_CLIENT_ID`/`NAVER_CLIENT_SECRET`, `GOOGLE_BOOKS_API_KEY`.
  키를 출처 URL에 남기지 않는다(`sources`에는 키를 뺀 URL).

## 절차
1. **작품 확정**: Wikidata에서 작품 QID, 저자 QID, 원어(P407), 원산국(P495), 최초 출간일(P577), OL work(P648).
2. **판 수집**: Wikidata 판·번역판 + OL work의 editions. 언어마다 대표 1종을 고르고 나머지는 `candidates`.
3. **필드 교차 확인**: 판마다 ISBN·연도·출판사·번역자를 출처 URL과 함께 `sources`에 모은다.
4. **표지**: 후보를 모으고 전부 내려받아 눈으로 확인 → 가장 이른 쇄 후보만 `coverSourceUrls`.
5. `.claude/skills/register-book/runs/<YYYY-MM-DD>_<제목>.plan.json` 작성 → `apply`(dry-run) → 표를 사용자에게 보여주고 승인 요청.
   `--yes`면 바로 `apply --execute`. 끝나면 결과 요약과 백오피스 '등록 기록' 화면 안내(`serve`가 떠 있어야 한다).

## 조회처 (WebFetch 또는 curl)
- Wikidata 검색: `https://www.wikidata.org/w/api.php?action=wbsearchentities&search=<제목>&language=ko&type=item&format=json`
- Wikidata SPARQL: `https://query.wikidata.org/sparql?format=json&query=<urlencoded>` (curl은 `-H 'User-Agent: coverage-agent/0.1'`)
  판 목록:
  ```sparql
  SELECT ?ed ?title ?langCode ?isbn13 ?isbn10 ?translatorLabel ?publisherLabel ?date ?olid WHERE {
    { ?ed wdt:P629 wd:<QID> } UNION { wd:<QID> wdt:P747 ?ed }
    OPTIONAL { ?ed wdt:P1476 ?title } OPTIONAL { ?ed wdt:P407 ?l . ?l wdt:P218 ?langCode }
    OPTIONAL { ?ed wdt:P212 ?isbn13 } OPTIONAL { ?ed wdt:P957 ?isbn10 } OPTIONAL { ?ed wdt:P655 ?translator }
    OPTIONAL { ?ed wdt:P123 ?publisher } OPTIONAL { ?ed wdt:P577 ?date } OPTIONAL { ?ed wdt:P648 ?olid }
    SERVICE wikibase:label { bd:serviceParam wikibase:language "[AUTO_LANGUAGE],en,ko". } }
  ```
- Open Library: `https://openlibrary.org/works/<OL..W>/editions.json?limit=200`, `https://openlibrary.org/isbn/<ISBN>.json`,
  표지 `https://covers.openlibrary.org/b/id/<cover id>-L.jpg` (OL `languages`는 비어 있거나 틀릴 수 있다)
- Google Books: `https://www.googleapis.com/books/v1/volumes?q=isbn:<ISBN>` (키 없으면 자주 429)
- 국립중앙도서관: `https://www.nl.go.kr/seoji/SearchApi.do?cert_key=$NL_CERT_KEY&result_style=json&page_no=1&page_size=10&isbn=<ISBN>` (`TITLE_URL`이 표지)
- 네이버 책: `https://openapi.naver.com/v1/search/book_adv.json?d_isbn=<ISBN>` (헤더 `X-Naver-Client-Id/Secret`)
- openBD(일본): `https://api.openbd.jp/v1/get?isbn=<ISBN>`, 원본 페이지 `https://www.hanmoto.com/bd/isbn/<ISBN>`
- 한국문학번역원 번역서 DB `library.ltikorea.or.kr`, 각국 국립도서관·출판사 페이지
- PRH 공식 표지: `https://images.penguinrandomhouse.com/cover/tif/<ISBN>` (없는 ISBN도 대체 이미지를 주므로 눈으로 확인)
- 위키백과·나무위키는 출처로 세지 않고 이미지도 쓰지 않는다.

## plan JSON
```json
{
  "query": "채식주의자",
  "work": {"wikidata": "Q24082770", "openlibrary": "OL17334243W", "author": "한강", "authorWikidata": "Q5646626", "sources": ["<url>"]},
  "original": {
    "title": "채식주의자", "originalTitle": "채식주의자", "countryCode": "KR", "languageCode": "ko",
    "year": 2007, "genre": "소설", "isbn": "9788936433598", "authorName": "한강", "publisherName": "창비",
    "coverSourceUrls": [{"url": "<이미지>", "sourceName": "창비"}, "https://covers.openlibrary.org/b/id/<id>-L.jpg"],
    "sources": {"isbn": ["<url>"], "year": ["<url>"], "coverPage": ["<출판사 도서 페이지>"]},
    "coverNote": "알려진 가장 이른 쇄: ...",
    "otherPrintingCovers": [{"url": "...", "sourceName": "...", "size": "1559x2403", "note": "나중 쇄: 수상 띠"}],
    "notes": "뺀 필드와 이유"
  },
  "translations": [{"title": "...", "languageCode": "..", "authorName": "한강", "isbn": "...", "countryCode": "..", "year": 0,
                    "translatorName": "...", "publisherName": "...", "coverSourceUrls": ["..."], "sources": {"...": ["<url>"]}}],
  "candidates": [{"languageCode": "..", "title": "...", "isbn": "...", "why": "대표판 아님: ..."}],
  "review": [{"languageCode": "..", "title": "...", "isbn": "...", "reason": "표지 없음 / 필드 충돌 ..."}]
}
```
- 서버로 가는 필드는 원서 `title originalTitle countryCode languageCode year genre isbn authorName publisherName edition`,
  번역서 `title countryCode languageCode year isbn authorName translatorName publisherName edition`. 나머지 키는 기록용.
- `coverSourceUrls` 항목은 URL 또는 `{"url", "sourceName"}`. `sourceName`은 **그 URL을 준 API·출판사 이름**(이미지가 다른 도메인에 있어도).
  `apply`가 전부 내려받아 픽셀 수가 가장 큰 것을 올리고, 서버 표지에 `sourceUrl`·`sourceName`이 남는다.
- `authorName`은 모든 판에서 원서 표기(예: "한강"). 서버가 이름 완전일치로 저자를 찾거나 새로 만든다.
- `translatorName`·`publisherName`은 해당 판의 원어 표기 그대로. `genre`는 한국어(소설, 시, 에세이 …).

## 규칙
1. **동일 작품**: 원저자(Wikidata 저자 QID / OL author key)와 work ID(Wikidata QID / OL work)가 일치해야 한다.
   판이 Wikidata `P629`로 그 작품에 연결되거나 OL에서 그 work의 edition이어야 한다. 제목이 비슷한 것만으로 판정하지 않는다.
2. **원서**: 원어로 된 판 중 초판(가장 이른 출간).
3. **언어별 대표 1종**: 대형 출판사 단행본 > 오디오북·재판. 나머지는 `candidates`. 오디오북·전자책은 후보로만.
4. **ISBN**: `isbn`으로 ISBN-13 정규화·체크섬. 무효면 넣지 않는다(`apply`도 막는다).
5. **중복**: `apply`가 항목마다 서버를 ISBN으로 조회한다. 중복·409는 실패가 아니라 "건너뜀".
6. **필수와 선택**: 필수는 **언어별 제목, 언어, 작가, 표지, 출처**. 연도·국가·출판사·번역자·장르·번역서 ISBN은 확인된 것만 넣고,
   모르거나 출처끼리 엇갈리면 그 필드만 빼고 `notes`에 이유. 출처는 1곳이어도 된다. 언어가 엇갈리면 더 구체적인 출처를 따른다.
   원서는 번역서를 붙이는 기준이라 ISBN 필수. ISBN 없는 번역서는 같은 원서·같은 언어·같은 제목이면 서버에 있는 것으로 본다.
7. **표지 출처**: 그 판(ISBN)의 표지임을 확인한 것만.
   - 공개 API(OL, Google Books, 국립중앙도서관, 네이버, openBD), 출판사 공식 배포 주소(PRH)
   - **출판사 도서 상세 페이지**: WebSearch로 출판사 공식 사이트의 도서 페이지를 찾고 HTML에서 이미지 URL을 뽑는다
     (`curl -sL -A 'Mozilla/5.0' <페이지> | grep -oE '(og:image|src)[^>]*\.(jpe?g|png|webp|tiff?)[^"]*'` 등).
     페이지에 그 판의 ISBN이 있어야 한다. 서점·블로그·위키 이미지는 쓰지 않는다. 페이지 URL은 `sources.coverPage`에.
   - 해상도가 낮아도 등록한다(에이전트 계정 면제). TIFF·WebP는 `apply`가 JPEG로 바꿔 올린다. 10MB 초과는 제외.
   - 표지가 하나도 없으면 등록할 수 없다 → `review`(사유 "표지 없음").
   - **표지 교체**: 서버에 이미 있는 책이라도 후보 중 가장 큰 표지가 서버의 모든 표지보다 크면 `apply`가 변형으로 추가하고 대표로 지정한다
     (`cover-upgraded`). 대표 표지가 사람이 올린 것(imageSource가 LINK가 아님)이면 바꾸지 않는다.
8. **표지 눈으로 확인**: 후보마다 `curl -sL -A 'Mozilla/5.0' -o .claude/skills/register-book/runs/previews/<이름> <URL>`
   → 크면 `sips -Z 800` → Read로 연다. 제목·저자·출판사, 어느 쇄인지(수상 띠·스티커·영화 포스터·개정 디자인), 워터마크를 본다.
   **대표 표지는 알려진 가장 이른 쇄**. 출판사 사이트는 대개 최신 쇄만 고해상도로 보여주므로 해상도보다 쇄가 우선이다.
   나중 쇄는 등록하지 않고 `otherPrintingCovers`에만. 초판 표지를 못 찾으면 `coverNote`에 "알려진 가장 이른 쇄: ...".
   ponytail: 서버에 나중 쇄 표지가 이미 대표로 있으면 더 이른 쇄가 작을 때 교체하지 않는다(크기만 비교).
9. **dry-run 기본**: dry-run 표를 보여주고 승인받은 뒤 `--execute`. `--yes`일 때만 승인 없이.
10. **재시도**: `api.py`가 429는 Retry-After만큼 대기, 5xx는 1회 재시도. Claude가 따로 재시도 루프를 돌리지 않는다.
11. 원서가 실패하면 번역서는 전부 실패로 기록된다. 원서 ISBN이 서버에 이미 있으면 그 원서에 연결한다.

## 책 고르기
원서·번역서 비교 서비스라 **번역서가 많은 책**이 우선이다(노벨문학상 Q37922, 부커상 Q160082, 인터내셔널 부커상 Q2052291).
SPARQL로 수상작(`?work wdt:P166 ?award`)과 수상 작가의 대표작(`?author wdt:P166 ?award; wdt:P800 ?work`)을 따로 뽑아
`wikibase:sitelinks` 많은 순으로 정렬하고(한 질의로 합치면 60초 시간 초과), 서버 `search`로 이미 있는 책을 빼고 제안한다.

## 결과
- `runs/<날짜>_<제목>_<시각>[_dry-run].json`: `summary`, `results`(항목별 status/사유/서버 ID/표지 출처), `plan` 전체.
  `runs/`는 gitignore 대상이다.
- 상태: `created`, `skipped`(중복), `cover-upgraded`, `failed`, dry-run에서는 `would-create`/`would-upgrade-cover`. 검토 필요는 `plan.review`.
- 같은 plan을 다시 실행하면 전부 `skipped`(멱등). 더 큰 표지가 새로 생긴 경우만 `cover-upgraded`.
- 실행 기록은 백오피스 '등록 기록' 화면에서 본다(`python3 $A serve`가 runs/를 읽기 전용 JSON으로 제공, 허용 출처 `COVERAGE_BACKOFFICE_ORIGIN`, 기본 http://localhost:5173).
- 언어가 달라도 비슷한 표지 묶음은 사람이 백오피스 '표지 묶기'에서 만든다(백엔드 `book_covers.similar_group_id`). 에이전트는 묶음을 추측해 만들지 않는다.

## 백엔드 API 메모
- 표지는 2단계: `POST /covers`(multipart `file`) → 응답의 URL로 `frontCover` 구성. 서버는 업로드 기록이 있는 URL만 받는다.
- WebP는 허용 목록에 있지만 ImageIO에 리더가 없어 400 → `apply`가 JPEG로 변환.
- 중복 조회 `GET /books/original/check-duplicate?isbn=`은 원서·번역서, 삭제·숨김까지 본다. 서버는 ISBN을 10→13 변환하지 않는다.
- 로컬 DB에는 스모크 테스트가 만든 가짜 "채식주의자"가 많다. 제목 검색은 참고용, 중복 판정은 ISBN으로만.
- 매핑 안 된 경로도 500이 온다. 500이면 경로 오타부터.
