"""Smoke test for POST /reports and POST /takedowns against a running local backend.
Clears local rate-limit keys in Redis first so it can be rerun. Exits non-zero on failure."""
import json, random, subprocess, sys, urllib.request, urllib.error

B = "http://localhost:8080/api"

def call(method, path, body=None, token=None):
    req = urllib.request.Request(B + path, method=method,
                                 data=json.dumps(body).encode() if body is not None else None,
                                 headers={"Content-Type": "application/json"})
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req) as r:
            raw = r.read()
            return r.status, json.loads(raw) if raw else None
    except urllib.error.HTTPError as e:
        raw = e.read()
        return e.code, json.loads(raw) if raw else None

def check(label, cond, extra=""):
    print(("PASS " if cond else "FAIL ") + label + (f"  {extra}" if extra and not cond else ""))
    if not cond:
        sys.exit(1)

def isbn13():
    d = [9, 7, 8] + [random.randint(0, 9) for _ in range(9)]
    d.append((10 - sum(x * (1 if i % 2 == 0 else 3) for i, x in enumerate(d)) % 10) % 10)
    return "".join(map(str, d))

def user():
    email = f"rep{random.randint(0, 10**9)}@test.com"
    call("POST", "/auth/signup", {"email": email, "password": "password123", "displayName": "rep", "role": "MEMBER"})
    return call("POST", "/auth/login", {"email": email, "password": "password123"})[1]["accessToken"]

def book(token):
    s, b = call("POST", "/books/original", {
        "title": "신고 테스트", "originalTitle": "Report Test", "countryCode": "KR", "languageCode": "ko",
        "year": 2020, "genre": "novel", "isbn": isbn13(), "authorName": "작가", "publisherName": "출판사",
        "frontCover": {"imageUrl": "http://localhost:9000/b/a.jpg", "thumbnailUrl": "http://localhost:9000/b/a_t.jpg",
                       "originalFileName": "a.jpg"}}, token)
    assert s == 201, b
    return b["id"]

subprocess.run("docker exec bookcover-redis sh -c \"redis-cli --scan --pattern 'rate_limit:*' | xargs -r redis-cli del\"",
               shell=True, check=True, capture_output=True)

# --- reports ---
u1, u2, u3 = user(), user(), user()
bid = book(u1)
rep = {"targetType": "ORIGINAL_BOOK", "targetId": bid, "reason": "WRONG_INFO", "description": "출판연도 오류"}

s, _ = call("POST", "/reports", rep)
check("report without token -> 401", s == 401, s)
s, r = call("POST", "/reports", rep, u1)
check("report -> 201", s == 201 and r["status"] == "PENDING" and r["targetId"] == bid, (s, r))
s, _ = call("POST", "/reports", rep, u1)
check("duplicate report -> 400", s == 400, s)
s, _ = call("POST", "/reports", {**rep, "targetId": "00000000-0000-0000-0000-000000000000"}, u2)
check("unknown target -> 404", s == 404, s)
s, _ = call("POST", "/reports", {**rep, "reason": None}, u2)
check("missing reason -> 400", s == 400, s)
s, _ = call("POST", "/reports", {**rep, "description": "x" * 1001}, u2)
check("description over 1000 chars -> 400", s == 400, s)

call("POST", "/reports", rep, u2)
s, _ = call("POST", "/reports", rep, u3)
_, g = call("GET", f"/books/original/{bid}")
check("3 reports auto-hide the book", s == 201 and g.get("status") == "HIDDEN", (s, g.get("status")))

u4 = user()
codes = [call("POST", "/reports", {**rep, "targetId": book(u4)}, u4)[0] for _ in range(6)]
check("6th report within an hour -> 429", codes == [201] * 5 + [429], codes)

# --- takedowns (anonymous) ---
td = {"requesterName": "홍길동", "requesterEmail": "rights@publisher.example", "requesterOrganization": "창비 저작권팀",
      "targetUrl": f"https://coverage.example/books/{bid}", "targetId": bid,
      "reason": "당사 저작물의 무단 게시", "evidenceUrl": "https://publisher.example/proof.pdf"}
s, t = call("POST", "/takedowns", td)
check("anonymous takedown -> 201", s == 201 and t["status"] == "PENDING" and t["requesterEmail"] == td["requesterEmail"], (s, t))
s, _ = call("POST", "/takedowns", {**td, "requesterEmail": "not-an-email"})
check("bad email -> 400", s == 400, s)
s, _ = call("POST", "/takedowns", {**td, "targetUrl": "javascript:alert(1)"})
check("bad target URL -> 400", s == 400, s)
s, _ = call("POST", "/takedowns", {**td, "reason": ""})
check("blank reason -> 400", s == 400, s)
codes = [call("POST", "/takedowns", td)[0] for _ in range(5)]
check("6th takedown from same IP within an hour -> 429", codes == [201] * 4 + [429], codes)
s, _ = call("GET", "/admin/takedowns", None, u1)
check("non-admin cannot read takedown queue -> 403", s == 403, s)
print("ALL PASS")
