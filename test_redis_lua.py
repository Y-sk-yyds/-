#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""Redis Lua 原子扣减号源 - 并发测试"""
import requests
import concurrent.futures
import time

BASE = "http://localhost:8080/api"
SCHEDULE_ID = 6  # 张医生 2026-07-05 下午 (别用已消耗的)
START_PID = 25    # 换个起始ID避免重复
CONCURRENT = 15   # 并发数（超过剩余9个号源）

# 登录
r = requests.post(f"{BASE}/auth/login", json={"username": "doctor01", "password": "admin123"})
token = r.json()["data"]["token"]
headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}

# 查初始号源
r = requests.get(f"{BASE}/schedules", headers=headers)
for s in r.json()["data"]["records"]:
    if s["id"] == SCHEDULE_ID:
        before = s["remainNum"]
        print(f"初始号源: remain={before}")
        break

# 并发挂号
success = 0
fail = 0

def register(pid):
    try:
        r = requests.post(f"{BASE}/registrations", json={"patientId": pid, "scheduleId": SCHEDULE_ID}, headers=headers, timeout=10)
        if r.status_code == 200 and r.json()["code"] == 200:
            return (True, r.json()["data"]["queueNum"])
        return (False, r.json().get("message", str(r.status_code)))
    except Exception as e:
        return (False, str(e))

print(f"\n发送 {CONCURRENT} 个并发挂号请求...")
start = time.time()

with concurrent.futures.ThreadPoolExecutor(max_workers=CONCURRENT) as executor:
    futures = [executor.submit(register, START_PID + i) for i in range(CONCURRENT)]
    for f in concurrent.futures.as_completed(futures):
        ok, info = f.result()
        if ok:
            success += 1
        else:
            fail += 1

elapsed = (time.time() - start) * 1000

# 查最终号源
r = requests.get(f"{BASE}/schedules", headers=headers)
for s in r.json()["data"]["records"]:
    if s["id"] == SCHEDULE_ID:
        after = s["remainNum"]
        break

print(f"\n=== 结果 ===")
print(f"耗时: {elapsed:.0f}ms")
print(f"成功: {success} 人次")
print(f"失败: {fail} 人次")
print(f"号源变化: {before} → {after} (扣减 {before - after})")
print(f"结论: {'✅ 通过 - 号源扣减准确' if before - after == success and after >= 0 else '❌ 异常 - 号源不一致'}")
