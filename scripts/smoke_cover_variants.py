"""Smoke test for cover variants against a running backend. Exits non-zero on failure."""
import json, random, sys, time, urllib.request, urllib.error

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

def isbn13():
    d = [9, 7, 8] + [random.randint(0, 9) for _ in range(9)]
    d.append((10 - sum(x * (1 if i % 2 == 0 else 3) for i, x in enumerate(d)) % 10) % 10)
    return "".join(map(str, d))

def img(name):
    return {"imageUrl": f"http://localhost:9000/bookcovers/{name}.jpg",
            "thumbnailUrl": f"http://localhost:9000/bookcovers/{name}_t.jpg",
            "originalFileName": f"{name}.jpg", "width": 800, "height": 1200}

def check(label, cond, extra=""):
    print(("PASS " if cond else "FAIL ") + label + (f"  {extra}" if extra and not cond else ""))
    if not cond:
        sys.exit(1)

email = f"cover{random.randint(0, 10**9)}@test.com"
call("POST", "/auth/signup", {"email": email, "password": "password123", "displayName": "cover", "role": "MEMBER"})
_, login = call("POST", "/auth/login", {"email": email, "password": "password123"})
T = login["accessToken"]

book = {"title": "채식주의자", "originalTitle": "채식주의자", "countryCode": "KR", "languageCode": "ko",
        "year": 2007, "genre": "novel", "isbn": isbn13(), "authorName": "한강", "publisherName": "창비",
        "frontCover": img("first")}
s, b = call("POST", "/books/original", book, T)
check("create original book -> 201", s == 201, b)
bid, first_cover = b["id"], b["frontCover"]["id"]
check("new book lists its front cover as the only variant", [c["id"] for c in b["covers"]] == [first_cover], b["covers"])

s, g = call("GET", f"/books/original/{bid}")
check("GET original book -> 200 (lazy-load fix)", s == 200, g)

s, v = call("POST", f"/books/{bid}/covers",
            {**img("recover"), "label": "10쇄 리커버", "printingFrom": 10, "usedFrom": "2016-05-20"}, T)
check("add cover variant -> 201", s == 201, v)
check("variant keeps metadata", (v["label"], v["printingFrom"], v["usedFrom"]) == ("10쇄 리커버", 10, "2016-05-20"), v)

s, g = call("GET", f"/books/original/{bid}")
check("book now has 2 covers", len(g["covers"]) == 2, g["covers"])
check("representative unchanged after adding", g["frontCover"]["id"] == first_cover)

s, _ = call("PUT", f"/books/{bid}/covers/{v['id']}/representative", None, T)
check("set representative -> 204", s == 204, s)
s, g = call("GET", f"/books/original/{bid}")
check("representative switched", g["frontCover"]["id"] == v["id"], g["frontCover"])
check("old representative kept as variant (no orphan delete)", first_cover in [c["id"] for c in g["covers"]], g["covers"])
thumbs = []
for _ in range(20):  # index sync is async (Kafka); 404 means the document is not indexed yet
    try:
        with urllib.request.urlopen(f"http://localhost:9200/book_covers/_doc/original_{bid}") as r:
            thumbs = [json.loads(r.read())["_source"].get("thumbnailUrl", "")]
    except urllib.error.HTTPError as e:
        if e.code != 404:
            raise
    if thumbs and thumbs[0].endswith("recover_t.jpg"):
        break
    time.sleep(0.5)
check("search index shows new representative thumbnail", thumbs and thumbs[0].endswith("recover_t.jpg"), thumbs)

s, other = call("POST", "/books/original", {**book, "isbn": isbn13(), "frontCover": img("other")}, T)
s, e = call("PUT", f"/books/{bid}/covers/{other['frontCover']['id']}/representative", None, T)
check("cover of another book rejected -> 400", s == 400, (s, e))

s, _ = call("POST", "/books/00000000-0000-0000-0000-000000000000/covers", img("x"), T)
check("unknown book -> 404", s == 404, s)
s, _ = call("POST", f"/books/{bid}/covers", {**img("x"), "printingFrom": 0}, T)
check("printingFrom 0 rejected -> 400", s == 400, s)
s, _ = call("POST", f"/books/{bid}/covers", img("x"))
check("no token -> 401", s == 401, s)

tr = {"title": "The Vegetarian", "countryCode": "GB", "languageCode": "en", "year": 2015,
      "isbn": isbn13(), "authorName": "Han Kang", "publisherName": "Portobello", "translatorName": "Deborah Smith",
      "frontCover": img("tr")}
s, t = call("POST", f"/books/translated/original/{bid}", tr, T)
check("create translated book -> 201", s == 201, t)
s, tv = call("POST", f"/books/{t['id']}/covers", {**img("tr2"), "label": "Booker winner edition"}, T)
check("add variant to translated book -> 201", s == 201, tv)
s, tg = call("GET", f"/books/translated/{t['id']}")
check("translated book lists 2 covers", s == 200 and len(tg["covers"]) == 2, tg)

s, _ = call("POST", f"/likes/ORIGINAL_COVER/{v['id']}", None, T)
s2, cnt = call("GET", f"/likes/ORIGINAL_COVER/{v['id']}/count", None, T)
check("variant is likeable", s in (200, 201) and s2 == 200 and "1" in json.dumps(cnt), (s, s2, cnt))

s, deleted = call("DELETE", f"/books/original/{other['id']}", None, T)
s, _ = call("POST", f"/books/{other['id']}/covers", img("x"), T)
check("deleted book -> 404", s == 404, s)
print("ALL PASS")
