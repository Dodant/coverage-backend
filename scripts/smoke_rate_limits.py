"""Smoke test for per-user rate limits on likes, comments and uploads against a running local backend.
Clears local rate-limit keys in Redis first so it can be rerun. Exits non-zero on failure."""
import json, random, struct, subprocess, sys, urllib.request, urllib.error, zlib
from concurrent.futures import ThreadPoolExecutor

B = "http://localhost:8080/api"

def call(method, path, body=None, token=None, raw=None, ctype="application/json"):
    data = raw if raw is not None else (json.dumps(body).encode() if body is not None else None)
    req = urllib.request.Request(B + path, method=method, data=data, headers={"Content-Type": ctype})
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req) as r:
            return r.status, r.headers, r.read()
    except urllib.error.HTTPError as e:
        return e.code, e.headers, e.read()

def check(label, cond, extra=""):
    print(("PASS " if cond else "FAIL ") + label + (f"  {extra}" if extra and not cond else ""))
    if not cond:
        sys.exit(1)

def isbn13():
    d = [9, 7, 8] + [random.randint(0, 9) for _ in range(9)]
    d.append((10 - sum(x * (1 if i % 2 == 0 else 3) for i, x in enumerate(d)) % 10) % 10)
    return "".join(map(str, d))

def user():
    email = f"rl{random.randint(0, 10**9)}@test.com"
    call("POST", "/auth/signup", {"email": email, "password": "password123", "displayName": "rl", "role": "MEMBER"})
    return json.loads(call("POST", "/auth/login", {"email": email, "password": "password123"})[2])["accessToken"]

def png(w=800, h=1200):
    chunk = lambda t, d: struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t + d))
    rows = zlib.compress(b"".join(b"\0" + b"\x80" * w * 3 for _ in range(h)))
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 2, 0, 0, 0)) + chunk(b"IDAT", rows) + chunk(b"IEND", b"")

subprocess.run("docker exec bookcover-redis sh -c \"redis-cli --scan --pattern 'rate_limit:*' | xargs -r redis-cli del\"",
               shell=True, check=True, capture_output=True)

u = user()
# 책 생성도 업로드 한도(10/시간)를 쓰므로 다른 사용자로 만들어 u의 업로드 예산을 온전히 남긴다
s, _, b = call("POST", "/books/original", {
    "title": "제한 테스트", "originalTitle": "Limit Test", "countryCode": "KR", "languageCode": "ko",
    "year": 2020, "genre": "novel", "isbn": isbn13(), "authorName": "작가", "publisherName": "출판사",
    "frontCover": {"imageUrl": "http://localhost:9000/b/a.jpg", "thumbnailUrl": "http://localhost:9000/b/a_t.jpg",
                   "originalFileName": "a.jpg"}}, user())
check("create test book -> 201", s == 201, (s, b[:200]))
cover_id = json.loads(b)["frontCover"]["id"]

# comments: 30/min
s, _, _ = call("POST", "/comments", {"targetType": "ORIGINAL_COVER", "targetId": cover_id, "content": ""}, u)
check("rejected comment (400) does not use up the budget", s == 400, s)
comment = lambda _: call("POST", "/comments", {"targetType": "ORIGINAL_COVER", "targetId": cover_id, "content": "도배"}, u)
with ThreadPoolExecutor(20) as pool:
    results = list(pool.map(comment, range(40)))
codes = [r[0] for r in results]
check("40 concurrent comments -> exactly 30 accepted", codes.count(201) == 30 and codes.count(429) == 10, sorted(codes))
cid = json.loads(next(r[2] for r in results if r[0] == 201))["id"]
s, _, _ = call("PUT", f"/comments/{cid}", {"content": "수정 도배"}, u)
check("editing comments shares the comment budget -> 429", s == 429, s)
s, h, _ = call("POST", "/comments", {"targetType": "ORIGINAL_COVER", "targetId": cover_id, "content": "x"}, u)
check("429 tells the client how long to wait (18.5)", s == 429 and h.get("Retry-After"), dict(h))
s, _, _ = call("POST", "/comments", {"targetType": "ORIGINAL_COVER", "targetId": cover_id, "content": "다른 사람"}, user())
check("limit is per user (another user can still comment)", s == 201, s)

# likes: 100/min (toggle)
codes = [call("POST", f"/likes/ORIGINAL_COVER/{cover_id}", None, u)[0] for _ in range(101)]
check("101st like toggle within a minute -> 429", set(codes[:100]) <= {200, 201} and codes[100] == 429, codes[-3:])

# uploads: 10/hour, only successful uploads count
boundary = "----smoke"
form = lambda name, ctype, body: (f"--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; filename=\"{name}\"\r\n"
                                  f"Content-Type: {ctype}\r\n\r\n").encode() + body + f"\r\n--{boundary}--\r\n".encode()
junk, good = form("a.jpg", "image/jpeg", b"not-an-image"), form("a.png", "image/png", png())
up = lambda path, body: call("POST", path, None, u, raw=body, ctype=f"multipart/form-data; boundary={boundary}")[0]
codes = [up("/covers", junk) for _ in range(11)]
check("11 rejected uploads (400) do not use up the budget", codes == [400] * 11, codes)
codes = [up("/covers", good) for _ in range(6)] + [up("/covers/redesign", good) for _ in range(5)]
check("11th successful upload within an hour -> 429 (covers + redesigns share the budget)",
      all(200 <= c < 300 for c in codes[:10]) and codes[10] == 429, codes)
# failures are refunded only up to 3x the limit per window, so deliberate failures cannot be sent forever
u2 = user()
u = u2
codes = [up("/covers", junk) for _ in range(41)]
check("41st deliberately failing upload within an hour -> 429 (30 refunded, then 10 counted)",
      codes[:40] == [400] * 40 and codes[40] == 429, codes[-3:])
print("ALL PASS")
