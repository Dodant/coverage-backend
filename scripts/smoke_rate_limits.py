"""Smoke test for per-user rate limits on likes, comments and uploads against a running local backend.
Clears local rate-limit keys in Redis first so it can be rerun. Exits non-zero on failure."""
import json, random, subprocess, sys, urllib.request, urllib.error

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

def until_limited(n, fn):
    codes = [fn() for _ in range(n)]
    return codes

subprocess.run("docker exec bookcover-redis sh -c \"redis-cli --scan --pattern 'rate_limit:*' | xargs -r redis-cli del\"",
               shell=True, check=True, capture_output=True)

u = user()
s, _, b = call("POST", "/books/original", {
    "title": "제한 테스트", "originalTitle": "Limit Test", "countryCode": "KR", "languageCode": "ko",
    "year": 2020, "genre": "novel", "isbn": isbn13(), "authorName": "작가", "publisherName": "출판사",
    "frontCover": {"imageUrl": "http://localhost:9000/b/a.jpg", "thumbnailUrl": "http://localhost:9000/b/a_t.jpg",
                   "originalFileName": "a.jpg"}}, u)
cover_id = json.loads(b)["frontCover"]["id"]

# comments: 30/min
codes = until_limited(31, lambda: call("POST", "/comments",
                                       {"targetType": "ORIGINAL_COVER", "targetId": cover_id, "content": "도배"}, u)[0])
check("31st comment within a minute -> 429", codes[:30] == [201] * 30 and codes[30] == 429, codes)
s, h, _ = call("POST", "/comments", {"targetType": "ORIGINAL_COVER", "targetId": cover_id, "content": "x"}, u)
check("429 tells the client how long to wait (18.5)", s == 429 and h.get("Retry-After"), dict(h))
s, _, _ = call("POST", "/comments", {"targetType": "ORIGINAL_COVER", "targetId": cover_id, "content": "다른 사람"}, user())
check("limit is per user (another user can still comment)", s == 201, s)

# likes: 100/min (toggle)
codes = until_limited(101, lambda: call("POST", f"/likes/ORIGINAL_COVER/{cover_id}", None, u)[0])
check("101st like toggle within a minute -> 429", set(codes[:100]) <= {200, 201} and codes[100] == 429, codes[-3:])

# uploads: 10/hour. Limit is checked before image validation, so junk files are enough
boundary = "----smoke"
junk = (f"--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; filename=\"a.jpg\"\r\n"
        f"Content-Type: image/jpeg\r\n\r\nnot-an-image\r\n--{boundary}--\r\n").encode()
up = lambda path: call("POST", path, None, u, raw=junk, ctype=f"multipart/form-data; boundary={boundary}")[0]
codes = [up("/covers") for _ in range(6)] + [up("/covers/redesign") for _ in range(5)]
check("11th upload within an hour -> 429 (covers + redesigns share the budget)",
      codes[:10] == [400] * 10 and codes[10] == 429, codes)
print("ALL PASS")
