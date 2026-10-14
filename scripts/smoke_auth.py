"""Smoke test for token separation, refresh rotation, logout and withdrawal against a running local backend.
BASE env var overrides the API root (default http://localhost:8080/api). Exits non-zero on failure."""
import json, os, random, sys, urllib.request, urllib.error

B = os.environ.get("BASE", "http://localhost:8080/api")

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

email = f"auth{random.randint(0, 10**9)}@test.com"
pw = "password123"
s, a = call("POST", "/auth/signup", {"email": email, "password": pw, "displayName": "authsmoke", "role": "MEMBER"})
check("signup -> 201", s == 201, (s, a))
access, refresh = a["accessToken"], a["refreshToken"]

s, _ = call("GET", "/users/me", None, access)
check("access token works", s == 200, s)
s, _ = call("GET", "/users/me", None, refresh)
check("refresh token cannot be used as access token -> 401", s == 401, s)
s, _ = call("POST", "/auth/refresh", {"refreshToken": access})
check("access token cannot be used to refresh -> 400", s == 400, s)

s, r = call("POST", "/auth/refresh", {"refreshToken": refresh})
check("refresh -> 200 with new tokens", s == 200 and r["refreshToken"] != refresh, (s, r))
s, _ = call("POST", "/auth/refresh", {"refreshToken": refresh})
check("same refresh token twice -> 400", s == 400, s)

refresh2 = r["refreshToken"]
s, _ = call("POST", "/auth/logout", {"refreshToken": refresh2})
check("logout -> 204", s == 204, s)
s, _ = call("POST", "/auth/refresh", {"refreshToken": refresh2})
check("refresh after logout -> 400", s == 400, s)
s, _ = call("POST", "/auth/logout", {"refreshToken": "garbage"})
check("logout with garbage token -> 204", s == 204, s)

_, l = call("POST", "/auth/login", {"email": email, "password": pw})
access, refresh, uid = l["accessToken"], l["refreshToken"], l["user"]["id"]
s, _ = call("DELETE", "/users/me", {"password": "wrong-password"}, access)
check("withdraw with wrong password -> 400", s == 400, s)
s, _ = call("DELETE", "/users/me", {"password": pw}, access)
check("withdraw -> 204", s == 204, s)
s, _ = call("GET", "/users/me", None, access)
check("old access token rejected after withdraw -> 401", s == 401, s)
s, _ = call("POST", "/auth/refresh", {"refreshToken": refresh})
check("old refresh token rejected after withdraw -> 400", s == 400, s)
s, _ = call("POST", "/auth/login", {"email": email, "password": pw})
check("login after withdraw fails", s in (400, 401), s)
s, a2 = call("POST", "/auth/signup", {"email": email, "password": pw, "displayName": "again", "role": "MEMBER"})
check("same email can sign up again -> 201", s == 201, s)
s, p = call("GET", f"/users/{uid}", None, a2["accessToken"])
check("withdrawn profile is anonymized", s == 200 and p["displayName"] == "탈퇴한 회원" and not p.get("websiteUrl"), (s, p))
print("ALL PASS")
