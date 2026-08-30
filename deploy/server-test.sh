#!/usr/bin/env bash
# AI Edu 后端全链路测试(在服务器上跑): 验证 前端入口 → Java 网关 → Python 打通
# 用法(在 Java 所在机器上):
#   bash server-test.sh
# 可配: JAVA_URL(默认 http://127.0.0.1:9627) PY_URL(默认 http://114.132.222.92:9527) TOKEN(默认 my-secret-token-123)
set -u
JAVA_URL="${JAVA_URL:-http://127.0.0.1:9627}"
PY_URL="${PY_URL:-http://114.132.222.92:9527}"
TOKEN="${TOKEN:-my-secret-token-123}"
COOKIE="$(mktemp)"
PASS=0; FAIL=0
ok()   { echo "  ✅ $1"; PASS=$((PASS+1)); }
bad()  { echo "  ❌ $1"; FAIL=$((FAIL+1)); }
t()    { echo; echo "== $1 =="; }

# 1. Python 健康(服务器 → Python)
t "1. Python 服务健康 ($PY_URL)"
if curl -sf --max-time 8 "$PY_URL/health" >/dev/null 2>&1; then ok "Python /health 可达"; else bad "Python /health 不通(安全组/未部署?)"; fi

# 2. Python RAG 直达(带内部 token)
t "2. Python RAG 引导直达(证明 Python RAG 本身可用)"
if curl -sf --max-time 8 "$PY_URL/api/rag/assistant/guide?current_project=ai-tutoring" -H "x-internal-token: $TOKEN" | grep -q suggestions; then
  ok "Python guide 返回 suggestions"
else
  bad "Python guide 失败(token 不对/接口异常?)"
fi

# 3. Java 登录拿 session(证明 Java 起来 + 会话可用)
t "3. Java 登录获取 session ($JAVA_URL)"
LOGIN=$(curl -sf --max-time 8 -c "$COOKIE" -X POST "$JAVA_URL/api/unauth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"student","password":"123456"}' 2>&1)
if echo "$LOGIN" | grep -q '"code"' && echo "$LOGIN" | grep -qE '"role"\s*:\s*"STUDENT"'; then
  ok "Java 登录成功(student/STUDENT)"
else
  bad "Java 登录失败: $(echo "$LOGIN" | head -c 120)"
fi

# 4. Java → Python 引导全链路(Java 转发到 Python)
t "4. Java 网关 → Python 引导(打通 Java↔Python 的关键)"
if curl -sf --max-time 15 -b "$COOKIE" "$JAVA_URL/api/rag/assistant/guide?currentProject=ai-tutoring" | grep -q suggestions; then
  ok "Java 转发 Python guide 成功(Java↔Python 通)"
else
  bad "Java 转发 Python guide 失败(看 Java 日志: journalctl -u ai-edu-backend -f)"
fi

# 5. Java → Python 真实问答(非流式 done+stages, 完整 RAG 链路)
t "5. Java → Python 真实问答(rag-system 问题)"
RESP=$(curl -sf --max-time 60 -b "$COOKIE" -X POST "$JAVA_URL/api/rag/assistant/ask/sync" \
  -H "Content-Type: application/json" \
  -d '{"question":"RAG问答系统是干什么的","sessionId":"srv-test-1","currentProject":"rag-system","stream":false,"topK":3}' 2>&1)
if echo "$RESP" | grep -q '"answer"' && echo "$RESP" | grep -q '"stages"'; then
  ok "Java→Python 问答成功(有 answer+stages, 打通 ✅)"
  echo "     答案预览: $(echo "$RESP" | grep -oE '"answer":"[^"]{0,80}' | head -1)..."
else
  bad "Java→Python 问答失败: $(echo "$RESP" | head -c 150)"
fi

rm -f "$COOKIE"
echo; echo "========================"
echo " 通过 $PASS / 失败 $FAIL"
echo " 结论: $([ $FAIL -eq 0 ] && echo '✅ 后端链路打通' || echo '❌ 有环节不通, 看上面失败项')"
