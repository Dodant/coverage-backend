"""Smoke test for cover credits and series against a running backend. Exits non-zero on failure.

BASE=http://localhost:8081/api python3 scripts/smoke_credits_series.py   (default :8080)
Designer merge needs ADMIN, so the test user is promoted via `docker exec bookcover-postgres`.
"""
import json, os, random, subprocess, sys, time, urllib.parse, urllib.request, urllib.error

B = os.environ.get("BASE", "http://localhost:8080/api")
R = random.randint(0, 10**9)

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

def img(name, credits=None):
    c = {"imageUrl": f"http://localhost:9000/bookcovers/{name}.jpg",
         "thumbnailUrl": f"http://localhost:9000/bookcovers/{name}_t.jpg",
         "originalFileName": f"{name}.jpg", "width": 800, "height": 1200}
    if credits is not None:
        c["credits"] = credits
    return c

def check(label, cond, extra=""):
    print(("PASS " if cond else "FAIL ") + label + (f"  {extra}" if extra and not cond else ""))
    if not cond:
        sys.exit(1)

def credit_set(cover):
    return {(c["name"], c["role"]) for c in cover["credits"]}

D1, D2, ILL, PHO = (f"{n}{R}" for n in ("디자이너가", "디자이너나", "일러스트", "사진가"))
PUB, SERIES = f"출판사{R}", f"세계문학전집{R}"

email = f"credit{R}@test.com"
call("POST", "/auth/signup", {"email": email, "password": "password123", "displayName": "credit", "role": "MEMBER"})
_, login = call("POST", "/auth/login", {"email": email, "password": "password123"})
T = login["accessToken"]

# 1. 원서 등록: 앞면 크레딧 2개(+중복 1개), 뒷면 크레딧, 시리즈
s, ob = call("POST", "/books/original", {
    "title": "이방인", "originalTitle": "L'Étranger", "countryCode": "FR", "languageCode": "fr",
    "year": 1942, "genre": "novel", "isbn": isbn13(), "authorName": "카뮈", "publisherName": PUB,
    "seriesName": SERIES, "seriesNumber": "12",
    "frontCover": img("front", [{"designerName": D1, "role": "DESIGN"},
                                {"designerName": ILL, "role": "ILLUSTRATION"},
                                {"designerName": D1, "role": "DESIGN"}]),
    "backCover": img("back", [{"designerName": PHO, "role": "PHOTOGRAPHY"}])}, T)
check("create original with credits+series -> 201", s == 201, ob)
check("front credits deduplicated", credit_set(ob["frontCover"]) == {(D1, "DESIGN"), (ILL, "ILLUSTRATION")}, ob["frontCover"])
check("back cover credits", credit_set(ob["backCover"]) == {(PHO, "PHOTOGRAPHY")}, ob["backCover"])
check("series attached", ob["series"]["name"] == SERIES and ob["seriesNumber"] == "12", ob)
check("no book-level designer field", "designer" not in ob)
bid, series_id = ob["id"], ob["series"]["id"]
ids = {c["name"]: c["designerId"] for c in ob["frontCover"]["credits"] + ob["backCover"]["credits"]}

# 2. 같은 출판사·이름의 시리즈는 재사용
s, tb = call("POST", f"/books/translated/original/{bid}", {
    "title": "이방인", "countryCode": "KR", "languageCode": "ko", "year": 1987, "isbn": isbn13(),
    "authorName": "카뮈", "translatorName": "김화영", "publisherName": PUB,
    "seriesName": SERIES, "seriesNumber": "3", "frontCover": img("kr", [{"designerName": D1, "role": "DESIGN"}])}, T)
check("create translated in same series -> 201", s == 201, tb)
check("same publisher+name reuses series", tb["series"]["id"] == series_id, tb)

# 3. 잘못된 크레딧 -> 400
s, _ = call("POST", f"/books/{bid}/covers", {**img("bad"), "credits": [{"designerName": D1}]}, T)
check("credit without role -> 400", s == 400)
s, _ = call("POST", f"/books/{bid}/covers", {**img("bad"), "credits": [{"role": "DESIGN"}]}, T)
check("credit without designer -> 400", s == 400)

# 4. 표지 변형에 다른 디자이너 크레딧 (리커버)
s, v = call("POST", f"/books/{bid}/covers", {**img("recover"), "label": "리커버",
                                              "credits": [{"designerName": D2, "role": "DESIGN"}]}, T)
check("add variant with credit -> 201", s == 201, v)
check("variant has its own credit", credit_set(v) == {(D2, "DESIGN")}, v)
ids[D2] = v["credits"][0]["designerId"]

# 5. 크레딧 교체: D1 DESIGN 유지, ILL 제거, D2 DESIGN·ART_DIRECTION 추가
s, u = call("PUT", f"/books/original/{bid}", {"frontCover": {"credits": [
    {"designerId": ids[D1], "role": "DESIGN"},
    {"designerId": ids[D2], "role": "DESIGN"},
    {"designerId": ids[D2], "role": "ART_DIRECTION"}]}}, T)
check("replace front credits -> 200", s == 200, u)
check("front credits replaced", credit_set(u["frontCover"]) == {(D1, "DESIGN"), (D2, "DESIGN"), (D2, "ART_DIRECTION")}, u["frontCover"])
check("back credits untouched", credit_set(u["backCover"]) == {(PHO, "PHOTOGRAPHY")}, u["backCover"])

# 6. 시리즈 조회 (비로그인)
s, sr = call("GET", f"/series/{series_id}")
check("GET series public -> 200", s == 200 and sr["publisherName"] == PUB, sr)
s, books = call("GET", f"/series/{series_id}/books")
check("series books ordered numerically", [b["seriesNumber"] for b in books] == ["3", "12"], books)
check("series has both books", {b["type"] for b in books} == {"ORIGINAL", "TRANSLATED"}, books)

# 7. 디자이너 작업 표지 목록: 원서·번역서, 앞면 변형·뒷면 포함, 빠진 크레딧은 제외
for name, expected in ((D1, 2), (D2, 2), (PHO, 1), (ILL, 0)):
    s, p = call("GET", f"/designers/{ids[name]}")
    check(f"designer {name} coverCount == {expected}", s == 200 and p["coverCount"] == expected, p)
s, page = call("GET", f"/designers/{ids[D1]}/covers")
check("covers include original and translated", {c["book"]["type"] for c in page["content"]} == {"ORIGINAL", "TRANSLATED"}, page)
s, page = call("GET", f"/designers/{ids[D2]}/covers")
front = next(c for c in page["content"] if c["coverId"] == u["frontCover"]["id"])
check("roles on one cover grouped", front["roles"] == ["DESIGN", "ART_DIRECTION"], front)
s, page = call("GET", f"/designers/{ids[D2]}/covers?role=ART_DIRECTION")
check("role filter", [c["coverId"] for c in page["content"]] == [u["frontCover"]["id"]], page)
s, page = call("GET", f"/designers/{ids[PHO]}/covers")
check("back cover resolves its book", [(c["coverType"], c["book"]["id"]) for c in page["content"]] == [("BACK", bid)], page)
s, _ = call("GET", f"/designers/{ids[D1]}/books")
check("old /designers/{id}/books removed", s != 200, s)

# 8. 검색 인덱스에 크레딧 이름 반영 (Kafka 비동기)
found = False
for _ in range(20):
    s, res = call("GET", "/search?query=" + urllib.parse.quote(D2))
    if s == 200 and any(i["id"] == f"original_{bid}" for i in res["items"]):
        found = True
        break
    time.sleep(1)
check("search by credited designer finds book", found, res)

# 9. 디자이너 병합 (ADMIN): D2 -> D1, 같은 표지·역할은 하나로 합쳐짐
subprocess.run(["docker", "exec", "bookcover-postgres", "psql", "-U", "bookcover", "-d", "bookcover", "-qc",
                f"UPDATE users SET role='ADMIN' WHERE email='{email}';"], check=True, capture_output=True)
s, _ = call("POST", "/admin/entities/designers/merge", {"sourceId": ids[D2], "targetId": ids[D1]}, T)
check("merge designers -> 2xx", 200 <= s < 300, s)
s, g = call("GET", f"/books/original/{bid}")
check("merged front credits deduplicated", credit_set(g["frontCover"]) == {(D1, "DESIGN"), (D1, "ART_DIRECTION")}, g["frontCover"])
variant = next(c for c in g["covers"] if c["id"] == v["id"])
check("variant credit moved to target", credit_set(variant) == {(D1, "DESIGN")}, variant)

# 10. 삭제된 책의 표지는 작업 목록에서 빠짐
s, _ = call("DELETE", f"/books/translated/{tb['id']}", None, T)
check("delete translated -> 204", s == 204, s)
s, page = call("GET", f"/designers/{ids[D1]}/covers")
check("deleted book's cover excluded", all(c["book"]["type"] == "ORIGINAL" for c in page["content"]), page)

print("ALL PASS")
