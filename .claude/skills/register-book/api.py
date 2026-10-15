"""COVERAGE 백엔드 호출 CLI (register-book 스킬). 입출력은 JSON. Python 3.9 표준 라이브러리 + macOS sips.

환경변수: COVERAGE_API_BASE(기본 http://localhost:8080/api), COVERAGE_EMAIL, COVERAGE_PASSWORD
쓰기 명령은 매번 로그인한다(액세스 토큰 24h, 파일에 저장하지 않음).
"""
import argparse
import datetime
import http.server
import ipaddress
import json
import os
import re
import socket
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from pathlib import Path

BASE = os.environ.get("COVERAGE_API_BASE", "http://localhost:8080/api").rstrip("/")
UA = "coverage-agent/0.1"
# 출판사 CDN이 봇 UA를 막는 경우가 있어 이미지는 브라우저 UA로 받는다
BROWSER_UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126 Safari/537.36"
HERE = Path(__file__).resolve().parent
RUNS = HERE / "runs"
_token = None


# ---- ISBN ----

def _isbn10_ok(s):
    if not re.fullmatch(r"\d{9}[\dX]", s):
        return False
    return sum((10 - i) * (10 if c == "X" else int(c)) for i, c in enumerate(s)) % 11 == 0


def _isbn13_check(first12):
    return str((10 - sum(int(c) * (1 if i % 2 == 0 else 3) for i, c in enumerate(first12)) % 10) % 10)


def to_isbn13(raw):
    """하이픈·공백을 지우고 ISBN-13으로 바꾼다. 체크섬이 틀리거나 형식이 아니면 None."""
    if not raw:
        return None
    s = re.sub(r"[\s-]", "", str(raw)).upper()
    if len(s) == 10:
        return "978" + s[:9] + _isbn13_check("978" + s[:9]) if _isbn10_ok(s) else None
    if re.fullmatch(r"\d{13}", s) and s[:3] in ("978", "979") and _isbn13_check(s[:12]) == s[12]:
        return s
    return None


def to_isbn10(isbn13):
    """978로 시작하는 ISBN-13의 ISBN-10 형태(서버에 10자리로 저장된 책 조회용). 없으면 None."""
    if not isbn13 or not isbn13.startswith("978"):
        return None
    core = isbn13[3:12]
    check = (11 - sum((10 - i) * int(c) for i, c in enumerate(core)) % 11) % 11
    return core + ("X" if check == 10 else str(check))


def self_check():
    assert to_isbn13("0-553-44818-8") == "9780553448184"   # The Vegetarian (Hogarth)
    assert to_isbn13("978-89-364-3359-8") == "9788936433598"  # 채식주의자 (창비)
    assert to_isbn13("080442957X") == "9780804429573"       # 체크 문자 X
    assert to_isbn13("0553448189") is None                  # ISBN-10 체크섬 오류
    assert to_isbn13("9788936433599") is None               # ISBN-13 체크섬 오류
    assert to_isbn13("1234567890123") is None               # 978/979 아님
    assert to_isbn13("") is None and to_isbn13(None) is None
    assert to_isbn10("9780553448184") == "0553448188"
    assert to_isbn10("9780804429573") == "080442957X"
    assert to_isbn10("9791234567896") is None
    for bad in ("file:///etc/passwd", "http://127.0.0.1:9000/x.jpg", "http://169.254.169.254/", "http://10.0.0.1/a.jpg"):
        try:
            check_public_url(bad)
            raise AssertionError(bad)
        except ValueError:
            pass
    assert sniff(b"\xff\xd8\xff") == "image/jpeg" and sniff(b"RIFF\0\0\0\0WEBP") == "image/webp"
    return {"selfCheck": "ok"}


# ---- HTTP ----

class ApiError(Exception):
    def __init__(self, status, body):
        super().__init__(f"HTTP {status}: {body}")
        self.status, self.body = status, body


def request(method, path, body=None, auth=False, files=None):
    """429는 Retry-After만큼 기다렸다가 재시도, 5xx는 1회만 재시도."""
    url = path if path.startswith("http") else BASE + path
    headers = {"User-Agent": UA, "Accept": "application/json"}
    data = None
    if files:
        boundary = uuid.uuid4().hex
        name, filename, ctype, content = files
        data = (f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"; filename="{filename}"\r\n'
                f"Content-Type: {ctype}\r\n\r\n").encode() + content + f"\r\n--{boundary}--\r\n".encode()
        headers["Content-Type"] = f"multipart/form-data; boundary={boundary}"
    elif body is not None:
        data = json.dumps(body).encode()
        headers["Content-Type"] = "application/json"
    if auth:
        headers["Authorization"] = "Bearer " + login()["accessToken"]
    retried_5xx = False
    for _ in range(10):
        req = urllib.request.Request(url, data=data, method=method, headers=headers)
        try:
            with urllib.request.urlopen(req, timeout=60) as r:
                raw = r.read()
                return json.loads(raw) if raw else None
        except urllib.error.HTTPError as e:
            raw = e.read().decode(errors="replace")
            try:
                parsed = json.loads(raw)
            except ValueError:
                parsed = raw[:500]
            if e.code == 429:
                wait = int(e.headers.get("Retry-After") or 60)
                print(f"429: {wait}s 대기 후 재시도", file=sys.stderr)
                time.sleep(wait)
                continue
            if e.code >= 500 and not retried_5xx:
                retried_5xx = True
                time.sleep(2)
                continue
            raise ApiError(e.code, parsed)
    raise ApiError(429, "재시도 횟수 초과")


def login():
    global _token
    if _token is None:
        email, pw = os.environ.get("COVERAGE_EMAIL"), os.environ.get("COVERAGE_PASSWORD")
        if not email or not pw:
            sys.exit("COVERAGE_EMAIL, COVERAGE_PASSWORD 환경변수가 필요합니다")
        _token = request("POST", "/auth/login", {"email": email, "password": pw})
    return _token


def find_by_isbn(raw):
    """서버 중복 조회. 서버는 ISBN을 하이픈만 지워 저장하므로 13자리와 10자리 둘 다 찾는다."""
    i13 = to_isbn13(raw)
    if not i13:
        return {"isbn": raw, "valid": False, "matches": []}
    matches = []
    for form in filter(None, (i13, to_isbn10(i13))):
        matches += request("GET", "/books/original/check-duplicate?isbn=" + form)
    return {"isbn": i13, "valid": True, "matches": matches}


def search(title):
    return request("GET", "/search?" + urllib.parse.urlencode({"query": title, "size": 20}))


# ---- 표지 이미지 ----

def check_public_url(url):
    """plan의 URL은 웹에서 모은 값이라 http(s) 공개 주소만 연다 (file://, localhost, 사설망 차단)."""
    u = urllib.parse.urlparse(url)
    if u.scheme not in ("http", "https") or not u.hostname:
        raise ValueError(f"http(s) URL만 허용: {url}")
    for info in socket.getaddrinfo(u.hostname, u.port or (443 if u.scheme == "https" else 80)):
        ip = ipaddress.ip_address(info[4][0])
        if not ip.is_global:
            raise ValueError(f"공개 주소가 아님({ip}): {url}")


class _PublicRedirects(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        check_public_url(newurl)
        return super().redirect_request(req, fp, code, msg, headers, newurl)


# ponytail: DNS를 검사 뒤 다시 조회하므로 DNS rebinding은 막지 못한다. 로컬 전용 도구라 여기까지만
_opener = urllib.request.build_opener(_PublicRedirects)


def fetch_image(url):
    """(bytes, content_type). 실패하면 예외."""
    check_public_url(url)
    req = urllib.request.Request(url, headers={"User-Agent": BROWSER_UA})
    with _opener.open(req, timeout=60) as r:
        return r.read(), r.headers.get_content_type()


def sniff(data):
    """파일 앞부분으로 실제 이미지 형식 판별 (서버가 보내는 Content-Type은 틀릴 수 있다)."""
    if data[:2] == b"\xff\xd8":
        return "image/jpeg"
    if data[:8] == b"\x89PNG\r\n\x1a\n":
        return "image/png"
    if data[:4] == b"RIFF" and data[8:12] == b"WEBP":
        return "image/webp"
    if data[:4] in (b"II*\x00", b"MM\x00*"):
        return "image/tiff"
    return None


def _sips(data, *args):
    """ponytail: macOS sips에 의존. 다른 OS에서 돌리려면 Pillow나 ImageMagick으로 교체."""
    with tempfile.TemporaryDirectory() as d:
        src, dst = Path(d) / "in", Path(d) / "out.jpg"
        src.write_bytes(data)
        out = subprocess.run(["sips", *args, str(src), "--out", str(dst)], check=True, capture_output=True, text=True)
        return out.stdout, (dst.read_bytes() if dst.exists() else None)


def image_size(data):
    """(width, height). 읽지 못하면 None."""
    try:
        out, _ = _sips(data, "-g", "pixelWidth", "-g", "pixelHeight")
    except (subprocess.CalledProcessError, OSError):
        return None
    w, h = re.search(r"pixelWidth: (\d+)", out), re.search(r"pixelHeight: (\d+)", out)
    return (int(w.group(1)), int(h.group(1))) if w and h else None


def to_jpeg(data):
    """서버가 받지 않거나 읽지 못하는 형식(출판사 원본 TIFF, WebP)을 sRGB JPEG로 바꾼다."""
    return _sips(data, "-m", "/System/Library/ColorSync/Profiles/sRGB Profile.icc",
                 "-s", "format", "jpeg", "-s", "formatOptions", "90")[1]


def best_cover(candidates):
    """후보 (url, 제공처)를 모두 내려받아 픽셀 수가 가장 큰 이미지 (url, bytes, content_type, w, h, 제공처). 없으면 None."""
    best = None
    for u, source_name in candidates:
        try:
            data, ctype = fetch_image(u)
        except (urllib.error.URLError, OSError, ValueError) as e:
            print(f"표지 다운로드 실패: {u} ({e})", file=sys.stderr)
            continue
        ctype = sniff(data) or ctype  # OL은 WebP를 image/jpeg로 준다
        # 백엔드는 WebP를 허용 형식으로 받지만 ImageIO에 WebP 리더가 없어 크기를 못 읽는다(400). TIFF는 아예 받지 않는다
        if ctype in ("image/tiff", "image/webp"):
            try:
                data, ctype = to_jpeg(data), "image/jpeg"
            except (subprocess.CalledProcessError, OSError) as e:
                print(f"{ctype} 변환 실패: {u} ({e})", file=sys.stderr)
                continue
        if len(data) > 10 * 1024 * 1024:  # 서버 업로드 한도
            print(f"10MB 초과로 제외: {u}", file=sys.stderr)
            continue
        size = image_size(data)
        if ctype in IMAGE_EXT and size and (best is None or size[0] * size[1] > best[3] * best[4]):
            best = (u, data, ctype, size[0], size[1], source_name)
    return best


# 후보에 제공처(sourceName)가 없을 때 도메인으로 붙이는 이름. 매핑에 없으면 도메인 그대로
SOURCE_NAMES = {
    "covers.openlibrary.org": "Open Library",
    "images.penguinrandomhouse.com": "Penguin Random House",
    "images.randomhouse.com": "Penguin Random House",
    "books.google.com": "Google Books",
    "books.googleusercontent.com": "Google Books",
}


def cover_urls(item):
    """plan 항목의 표지 후보 → [(url, 제공처)]. 후보는 URL 문자열 또는 {"url", "sourceName"}."""
    out = []
    for c in item.get("coverSourceUrls") or []:
        url, name = (c, None) if isinstance(c, str) else (c["url"], c.get("sourceName"))
        host = urllib.parse.urlparse(url).hostname or ""
        out.append((url, name or SOURCE_NAMES.get(host, host)))
    return out


IMAGE_EXT = {"image/jpeg": "jpg", "image/png": "png", "image/webp": "webp"}


def upload_cover(source_url, content=None, ctype=None, source_name=None):
    """이미지를 POST /covers에 multipart로 올리고, 책 생성·표지 추가용 표지 정보를 돌려준다."""
    if content is None:
        best = best_cover([(source_url, source_name)])
        if best is None:
            raise ApiError(0, f"이미지를 쓸 수 없음: {source_url}")
        content, ctype = best[1], best[2]
    ext = IMAGE_EXT[ctype]
    name = re.sub(r"[^\w.-]", "_", urllib.parse.urlparse(source_url).path.rsplit("/", 1)[-1]) or "cover"
    if not name.endswith("." + ext):
        name += "." + ext
    up = request("POST", "/covers", auth=True, files=("file", name, ctype, content))
    return {"imageUrl": up["originalUrl"], "thumbnailUrl": up["thumbnailUrl"],
            "originalFileName": up["originalFileName"], "width": up["width"], "height": up["height"],
            "fileSize": up["fileSize"], "imageSource": "LINK", "sourceUrl": source_url,
            "sourceName": source_name or SOURCE_NAMES.get(urllib.parse.urlparse(source_url).hostname)}


# ---- 등록 ----

# 서버 DTO에 보낼 필드 (plan 항목의 나머지 키 sources/coverSourceUrls/notes 등은 기록용)
ORIGINAL_FIELDS = ("title", "originalTitle", "countryCode", "languageCode", "year", "genre", "isbn",
                   "authorName", "publisherName", "edition")
TRANSLATION_FIELDS = ("title", "countryCode", "languageCode", "year", "isbn", "authorName",
                      "translatorName", "publisherName", "edition")

# 서버가 요구하지 않아도 에이전트가 반드시 채우는 항목 (표지는 register에서 따로 확인)
REQUIRED = ("title", "languageCode", "authorName", "sources")


def find_translation(original_id, item):
    """ISBN이 없거나 서버에 ISBN 없이 등록된 번역서: 같은 원서의 같은 언어·같은 제목이면 같은 책으로 본다."""
    title = item["title"].strip().casefold()
    return [{"id": t["id"], "title": t["title"], "type": "TranslatedBook"}
            for t in request("GET", f"/books/translated/by-original/{original_id}")
            if t.get("languageCode") == item["languageCode"] and (t.get("title") or "").strip().casefold() == title]


def upgrade_cover(res, existing, best, dry_run):
    """이미 있는 책: 후보 표지가 지금 표지들보다 크면 표지 변형으로 추가하고 대표로 지정한다.
    대표 표지가 사람이 올린 것(imageSource가 LINK가 아님)이면 건드리지 않는다."""
    res = {**res, "status": "skipped", "existingId": existing["id"], "existingType": existing["type"]}
    if best is None:
        return {**res, "reason": "서버에 같은 책 존재"}
    path = "/books/original/" if existing["type"] == "OriginalBook" else "/books/translated/"
    try:
        book = request("GET", path + existing["id"])
    except ApiError as e:
        if e.status == 404:  # 삭제·숨김된 책
            return {**res, "reason": "서버에 같은 책 존재 (조회 불가: 삭제·숨김)"}
        raise
    front, covers = book.get("frontCover") or {}, book.get("covers") or []
    res["coverUrl"] = front.get("imageUrl")
    area = best[3] * best[4]
    largest = max([(c.get("width") or 0) * (c.get("height") or 0) for c in covers + [front]])
    if any(c.get("sourceUrl") == best[0] for c in covers + [front]):
        return {**res, "reason": "서버에 같은 책 존재, 같은 표지 이미 있음"}
    if area <= largest:
        return {**res, "reason": f"서버에 같은 책 존재, 표지가 더 크지 않음 ({best[3]}x{best[4]})"}
    if front.get("imageSource") != "LINK":
        return {**res, "reason": f"서버에 같은 책 존재, 대표 표지가 사람이 올린 것({front.get('imageSource')})이라 유지"}
    upgrade = {"coverSourceUrl": best[0], "coverSourceName": best[5], "from": f"{front.get('width')}x{front.get('height')}",
               "to": f"{best[3]}x{best[4]}"}
    if dry_run:
        return {**res, "status": "would-upgrade-cover", **upgrade}
    cover = upload_cover(best[0], best[1], best[2], best[5])
    added = request("POST", f"/books/{existing['id']}/covers", {**cover, "label": "공개 API 고해상도 표지"}, auth=True)
    request("PUT", f"/books/{existing['id']}/covers/{added['id']}/representative", auth=True)
    return {**res, "status": "cover-upgraded", "coverId": added["id"], "coverUrl": cover["imageUrl"], **upgrade}


def register(kind, item, original_id=None, dry_run=False):
    """한 권 등록. status: created | skipped | cover-upgraded | failed | would-create | would-upgrade-cover"""
    res = {"kind": kind, "title": item.get("title"), "languageCode": item.get("languageCode")}
    missing = [k for k in REQUIRED if not item.get(k)]
    if missing:
        return {**res, "status": "failed", "reason": "필수 항목 없음: " + ", ".join(missing)}
    i13 = to_isbn13(item.get("isbn"))
    if item.get("isbn") and not i13:
        return {**res, "status": "failed", "reason": f"ISBN 무효: {item.get('isbn')!r}"}
    if kind == "original" and not i13:  # 원서는 번역서를 붙이는 기준이라 ISBN으로만 중복을 판단한다
        return {**res, "status": "failed", "reason": "원서는 ISBN 필수"}
    res["isbn"] = i13
    try:
        found = find_by_isbn(i13)["matches"] if i13 else []
        if not found and kind == "translation" and original_id:
            found = find_translation(original_id, item)
        best = best_cover(cover_urls(item))
        if found:
            expected = "OriginalBook" if kind == "original" else "TranslatedBook"
            if found[0]["type"] != expected:
                return {**res, "status": "skipped", "existingId": found[0]["id"], "existingType": found[0]["type"],
                        "reason": f"서버에 같은 ISBN이 {found[0]['type']}로 존재 (표지 교체 안 함)"}
            return upgrade_cover(res, found[0], best, dry_run)
        if best is None:
            return {**res, "status": "failed", "reason": "쓸 수 있는 표지 이미지 없음 (앞면 표지 필수)"}
        res.update(coverSourceUrl=best[0], coverSourceName=best[5], coverSize=f"{best[3]}x{best[4]}")
        if dry_run:
            return {**res, "status": "would-create"}
        cover = upload_cover(best[0], best[1], best[2], best[5])
        fields = ORIGINAL_FIELDS if kind == "original" else TRANSLATION_FIELDS
        payload = {k: item[k] for k in fields if item.get(k) is not None}
        payload.update(isbn=i13, frontCover=cover)
        path = "/books/original" if kind == "original" else f"/books/translated/original/{original_id}"
        book = request("POST", path, payload, auth=True)
        return {**res, "status": "created", "id": book["id"], "coverUrl": cover["imageUrl"]}
    except ApiError as e:
        if e.status == 409 and isinstance(e.body, dict) and e.body.get("code") == "DUPLICATE_ISBN":
            details = e.body.get("details") or {}
            return {**res, "status": "skipped", "reason": "서버에 같은 ISBN 존재(409)",
                    "existingId": details.get("existingBookId")}
        return {**res, "status": "failed", "reason": str(e)}


def apply(plan, dry_run):
    """plan의 원서 → 번역서 순으로 등록. 원서가 생성/기존 원서로 확인돼야 번역서를 연결한다.
    results[0]은 plan.original, results[i+1]은 plan.translations[i]에 대응한다(뷰어가 이 순서에 의존)."""
    orig = register("original", plan["original"], dry_run=dry_run)
    original_id = orig.get("id")
    if orig.get("existingId"):
        if orig.get("existingType") == "TranslatedBook":
            orig.update(status="failed", reason="원서 ISBN이 서버에서 번역서로 등록돼 있음")
        else:
            original_id = orig["existingId"]
    results = [orig]
    for t in plan.get("translations", []):
        if original_id is None and not (dry_run and orig["status"] == "would-create"):
            results.append({"kind": "translation", "title": t.get("title"), "languageCode": t.get("languageCode"),
                            "status": "failed", "reason": "원서를 등록하지 못해 연결할 수 없음"})
            continue
        results.append(register("translation", t, original_id, dry_run))
    summary = {}
    for r in results:
        summary[r["status"]] = summary.get(r["status"], 0) + 1
    return {"dryRun": dry_run, "summary": summary, "results": results}


def save_run(plan, outcome):
    RUNS.mkdir(exist_ok=True)
    now = datetime.datetime.now()
    safe = re.sub(r"[^\w-]", "_", plan.get("query") or plan["original"]["title"])
    path = RUNS / f"{now:%Y-%m-%d}_{safe}_{now:%H%M%S}{'_dry-run' if outcome['dryRun'] else ''}.json"
    path.write_text(json.dumps({"ranAt": now.isoformat(timespec="seconds"), "apiBase": BASE,
                                **outcome, "plan": plan}, ensure_ascii=False, indent=1))
    return str(path)


def print_table(outcome):
    print(f"{'구분':<11} {'언어':<4} {'ISBN':<14} {'상태':<12} 제목 / 사유", file=sys.stderr)
    for r in outcome["results"]:
        print(f"{r['kind']:<11} {r.get('languageCode') or '':<4} {r.get('isbn') or '-':<14} {r['status']:<12} "
              f"{r.get('title')}  {r.get('reason') or ''}"
              f"{'  표지 ' + r['coverSize'] + ' ' + str(r.get('coverSourceName')) if r.get('coverSize') else ''}", file=sys.stderr)
    print(json.dumps(outcome["summary"], ensure_ascii=False), file=sys.stderr)


# ---- 실행 기록 API (백오피스가 읽는다) ----

# 백오피스(coverage-backoffice) 주소. 실행 기록은 에이전트를 돌린 컴퓨터에만 있어 백오피스가 이 서버에서 직접 읽는다
BACKOFFICE_ORIGIN = os.environ.get("COVERAGE_BACKOFFICE_ORIGIN", "http://localhost:5173")


class RunsApi(http.server.BaseHTTPRequestHandler):
    """runs/의 실행 기록(*.json, plan 제외)을 JSON으로 준다. 읽기 전용, localhost 전용."""

    def do_GET(self):
        port = self.server.server_address[1]
        if self.headers.get("Host") not in {f"localhost:{port}", f"127.0.0.1:{port}"}:  # DNS rebinding 차단
            return self.send_error(403)
        path = urllib.parse.unquote(urllib.parse.urlparse(self.path).path)
        if path == "/runs":
            runs = []
            for f in sorted(RUNS.glob("*.json"), reverse=True):
                if f.name.endswith(".plan.json"):
                    continue
                r = json.loads(f.read_text())
                runs.append({"file": f.name, "ranAt": r.get("ranAt"), "dryRun": r.get("dryRun"),
                             "query": (r.get("plan") or {}).get("query"), "summary": r.get("summary")})
            return self._send(json.dumps(runs, ensure_ascii=False).encode(), "application/json")
        name = path[len("/runs/"):] if path.startswith("/runs/") else ""
        f = RUNS / name
        if name and "/" not in name and name.endswith(".json") and f.is_file():
            return self._send(f.read_bytes(), "application/json")
        self.send_error(404)

    def _send(self, body, ctype):
        self.send_response(200)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Access-Control-Allow-Origin", BACKOFFICE_ORIGIN)
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, *args):
        pass


def main():
    p = argparse.ArgumentParser(description="COVERAGE 백엔드 호출 CLI (JSON 출력). "
                                "환경변수 COVERAGE_API_BASE, COVERAGE_EMAIL, COVERAGE_PASSWORD")
    sub = p.add_subparsers(dest="cmd", required=True)
    sub.add_parser("login", help="로그인 확인, 사용자 ID 출력 (레이트 리밋 면제 설정용)")
    sub.add_parser("isbn", help="ISBN-13 정규화·체크섬 검증").add_argument("isbn")
    sub.add_parser("cover-size", help="이미지 URL의 실제 형식·크기").add_argument("url")
    sub.add_parser("find-by-isbn", help="ISBN으로 서버 중복 조회 (ISBN-13/10 모두)").add_argument("isbn")
    sub.add_parser("search", help="제목으로 서버 검색").add_argument("title")
    sp = sub.add_parser("apply", help="plan JSON(원서+번역서) 등록. 기본은 dry-run, --execute일 때만 POST")
    sp.add_argument("plan_json")
    sp.add_argument("--execute", action="store_true", help="실제 등록")
    sub.add_parser("serve", help="runs/ 실행 기록 JSON API, 백오피스가 읽는다 (http://localhost:PORT/runs)").add_argument("--port", type=int, default=8765)
    sub.add_parser("self-check", help="ISBN·형식 판별 assert self-check")
    a = p.parse_args()

    try:
        if a.cmd == "login":
            u = login()["user"]
            out = {"userId": u["id"], "email": u["email"], "role": u["role"]}
        elif a.cmd == "isbn":
            i13 = to_isbn13(a.isbn)
            out = {"input": a.isbn, "isbn13": i13, "isbn10": to_isbn10(i13), "valid": bool(i13)}
        elif a.cmd == "cover-size":
            data, ctype = fetch_image(a.url)
            size = image_size(data)
            out = {"url": a.url, "type": sniff(data) or ctype, "bytes": len(data),
                   "size": f"{size[0]}x{size[1]}" if size else None}
        elif a.cmd == "find-by-isbn":
            out = find_by_isbn(a.isbn)
        elif a.cmd == "search":
            out = search(a.title)
        elif a.cmd == "serve":
            print(f"http://localhost:{a.port}/runs (백오피스 {BACKOFFICE_ORIGIN}에서 읽기 허용)", file=sys.stderr)
            http.server.ThreadingHTTPServer(("127.0.0.1", a.port), RunsApi).serve_forever()
        elif a.cmd == "self-check":
            out = self_check()
        else:
            plan = json.loads(Path(a.plan_json).read_text())
            out = apply(plan, dry_run=not a.execute)
            out["runFile"] = save_run(plan, out)
            print_table(out)
    except ApiError as e:
        out = {"error": e.status, "body": e.body}
    except (urllib.error.URLError, OSError, ValueError) as e:
        out = {"error": 0, "body": str(e)}
    json.dump(out, sys.stdout, ensure_ascii=False, indent=1)
    print()
    sys.exit(1 if isinstance(out, dict) and "error" in out else 0)


if __name__ == "__main__":
    main()
