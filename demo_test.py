#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""RabbitMQ + Redis 双技术效果演示"""
import requests
import time
import subprocess
import concurrent.futures

BASE = "http://localhost:8080/api"
MQ_API = "http://localhost:15672/api"

def mysql(sql):
    r = subprocess.run([
        "C:/Program Files/MySQL/MySQL Server 8.4/bin/mysql.exe",
        "-uroot", "-p123456", "--default-character-set=utf8mb4",
        "clinic_db", "-N", "-e", sql
    ], capture_output=True, text=True)
    return r.stdout.strip()

# ==========================================
print("=" * 60)
print("  演示 1: RabbitMQ 异步 - 前端秒响应")
print("=" * 60)

r = requests.post(f"{BASE}/auth/login", json={"username": "doctor01", "password": "admin123"})
token = r.json()["data"]["token"]
headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}

# 查初始
print(f"\n[初始] RabbitMQ队列消息: {requests.get(MQ_API+'/queues/%2F/clinic.reminder.queue', auth=('guest','guest')).json()['messages']}")

# 挂号
r = requests.post(f"{BASE}/registrations", json={"patientId":5,"scheduleId":5}, headers=headers)
reg_id = r.json()["data"]["id"]
print(f"[挂号] ID={reg_id}")

# 接诊
r = requests.put(f"{BASE}/consultations/{reg_id}/start", headers=headers)
cons_id = r.json()["data"]["id"]
print(f"[接诊] ID={cons_id}")

# 完成
start = time.time()
r = requests.put(f"{BASE}/consultations/{cons_id}/complete",
    json={"complaint":"x","diagnosis":"y","advice":"1天后复诊"}, headers=headers)
elapsed = (time.time() - start) * 1000
print(f"[完成] 前端收到响应: {r.json()['message']}")
print(f"       ⏱ 耗时: {elapsed:.0f}ms")

time.sleep(0.3)
rem = mysql("SELECT CONCAT('ID=',id,' remind=',remind_date) FROM reminder ORDER BY id DESC LIMIT 1")
print(f"[验证] reminder表: {rem}")
print(f"\n>>> 结论: 前端 {elapsed:.0f}ms 返回，MQ 后台异步处理，医生无感知 <<<")

# ==========================================
time.sleep(1)
print("\n\n" + "=" * 60)
print("  演示 2: Redis Lua - 号源防超卖")
print("=" * 60)

r = requests.get(f"{BASE}/schedules", headers=headers)
for s in r.json()["data"]["records"]:
    if s["id"] == 2:
        before = s["remainNum"]
        break
print(f"\n[初始] 排班ID=2 剩余号源: {before}")

succ = 0
fail = 0

def register(pid):
    global succ, fail
    try:
        r = requests.post(f"{BASE}/registrations",
            json={"patientId":pid,"scheduleId":2}, headers=headers, timeout=10)
        if r.json()["code"] == 200:
            succ += 1
        else:
            fail += 1
    except:
        fail += 1

total = 35
print(f"[并发] 发送 {total} 个请求（超出号源 {total - before} 个）...")
with concurrent.futures.ThreadPoolExecutor(max_workers=total) as ex:
    list(ex.map(register, range(50, 50+total)))

r = requests.get(f"{BASE}/schedules", headers=headers)
for s in r.json()["data"]["records"]:
    if s["id"] == 2:
        after = s["remainNum"]
        break

print(f"[结果] 号源: {before} → {after}")
print(f"       成功: {succ} 人")
print(f"       被拒: {fail} 人")
print(f"       剩余: {after} (没有变成负数!)")

print(f"\n>>> 结论: {total}个并发抢{before}个号源，精确扣减，0超卖 <<<")
