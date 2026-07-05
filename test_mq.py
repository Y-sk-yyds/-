#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""RabbitMQ 复诊提醒异步测试脚本"""
import requests
import json
import time

BASE = "http://localhost:8080/api"

# 登录
r = requests.post(f"{BASE}/auth/login", json={"username": "doctor01", "password": "admin123"})
token = r.json()["data"]["token"]
headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}

# 1. 挂号
r = requests.post(f"{BASE}/registrations", json={"patientId": 16, "scheduleId": 5}, headers=headers)
reg_id = r.json()["data"]["id"]
print(f"1. 挂号 ID={reg_id}")

# 2. 接诊
r = requests.put(f"{BASE}/consultations/{reg_id}/start", headers=headers)
cons_id = r.json()["data"]["id"]
print(f"2. 就诊 ID={cons_id}")

# 3. 完成就诊
body = {"complaint": "头痛", "diagnosis": "偏头痛", "advice": "一天后复诊"}
start = time.time()
r = requests.put(f"{BASE}/consultations/{cons_id}/complete", json=body, headers=headers)
elapsed = (time.time() - start) * 1000
print(f"3. 完成就诊 HTTP={r.status_code} 耗时={elapsed:.0f}ms")
print(f"   响应: {r.json()['message']}")

print(f"\n结论: 前端响应仅 {elapsed:.0f}ms，MQ 消费在后台异步进行")
