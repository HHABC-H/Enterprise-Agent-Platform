const AUTH_STORAGE_KEY = 'agent-platform-auth';
const PAGES = new Set(['chat', 'ingest', 'graph', 'approval', 'evaluation']);
const state = { auth: null, sessionId: null, lastDocumentId: null };
const get = (selector) => document.querySelector(selector);
const newChatWelcome = get('#messages').firstElementChild?.cloneNode(true);

function loadAuth() {
  try { return JSON.parse(localStorage.getItem(AUTH_STORAGE_KEY)); } catch { return null; }
}

function splitValues(value) {
  return String(value || '').split(',').map((item) => item.trim()).filter(Boolean);
}

function pretty(value) {
  return JSON.stringify(value, null, 2);
}

function setOutput(selector, value) {
  get(selector).textContent = typeof value === 'string' ? value : pretty(value);
}

function setBadge(selector, text, error = false) {
  const badge = get(selector);
  badge.textContent = text;
  badge.classList.toggle('error', error);
}

function setStatus(message, error = false) {
  const target = get('#status');
  target.textContent = message;
  target.classList.toggle('error', error);
}

function userContext() {
  return { tenantId: state.auth?.tenantId, userId: state.auth?.username };
}

async function request(path, options = {}) {
  const { authenticated = true, ...fetchOptions } = options;
  const headers = { 'Content-Type': 'application/json', ...(fetchOptions.headers || {}) };
  if (authenticated && state.auth?.accessToken) headers.Authorization = `Bearer ${state.auth.accessToken}`;
  const response = await fetch(path, { ...fetchOptions, headers });
  const body = await response.json().catch(() => null);
  if (!response.ok) throw new Error(body?.message || `请求失败（HTTP ${response.status}）`);
  return body?.data;
}

function setGraphSelectionStatus(message, error = false) {
  const target = get('#graph-selection-status');
  target.textContent = message;
  target.classList.toggle('error', error);
}

function replaceSelectOptions(select, items, valueOf, labelOf, placeholder) {
  select.replaceChildren(new Option(placeholder, ''));
  items.forEach((item) => select.add(new Option(labelOf(item), valueOf(item))));
}

function updateGraphQueryAvailability() {
  const documentSelect = get('#graph-form [name="documentId"]');
  const versionSelect = get('#graph-form [name="version"]');
  get('#graph-form button[type="submit"]').disabled = documentSelect.disabled || versionSelect.disabled || !documentSelect.value || !versionSelect.value;
}

function resetGraphVersions(message) {
  const versionSelect = get('#graph-form [name="version"]');
  replaceSelectOptions(versionSelect, [], () => '', () => '', message);
  versionSelect.disabled = true;
  updateGraphQueryAvailability();
}

async function refreshGraphVersions(documentId, preferredVersion) {
  const documentSelect = get('#graph-form [name="documentId"]');
  const versionSelect = get('#graph-form [name="version"]');
  if (!documentId) {
    resetGraphVersions('请先选择文档');
    setGraphSelectionStatus('请选择一个文档后再加载版本。');
    return;
  }
  versionSelect.disabled = true;
  replaceSelectOptions(versionSelect, [], () => '', () => '', '正在加载版本…');
  updateGraphQueryAvailability();
  setGraphSelectionStatus('正在加载文档版本…');
  try {
    const versions = await request(`/api/documents/${encodeURIComponent(documentId)}/versions?${new URLSearchParams(userContext())}`);
    if (documentSelect.value !== documentId) return;
    const items = Array.isArray(versions) ? versions : [];
    if (!items.length) {
      resetGraphVersions('该文档没有可用版本');
      setGraphSelectionStatus('该文档没有可用于图谱查询的版本。');
      return;
    }
    replaceSelectOptions(versionSelect, items, (item) => item.version, (item) => item.version, '请选择版本');
    versionSelect.value = items.some((item) => item.version === preferredVersion) ? preferredVersion : items[0].version;
    versionSelect.disabled = false;
    updateGraphQueryAvailability();
    setGraphSelectionStatus(`已加载 ${items.length} 个版本，可以查询关系图谱。`);
  } catch (error) {
    if (documentSelect.value !== documentId) return;
    resetGraphVersions('版本加载失败');
    setGraphSelectionStatus(error.message, true);
  }
}

async function refreshGraphDocuments(preferredDocumentId = state.lastDocumentId, preferredVersion) {
  const documentSelect = get('#graph-form [name="documentId"]');
  documentSelect.disabled = true;
  replaceSelectOptions(documentSelect, [], () => '', () => '', '正在加载可访问文档…');
  resetGraphVersions('请先选择文档');
  setGraphSelectionStatus('正在加载当前账号可访问的文档…');
  try {
    const documents = await request(`/api/documents?${new URLSearchParams(userContext())}`);
    const items = Array.isArray(documents) ? documents : [];
    if (!items.length) {
      replaceSelectOptions(documentSelect, [], () => '', () => '', '没有可访问文档');
      setGraphSelectionStatus('当前没有可用于图谱查询的文档。');
      return;
    }
    replaceSelectOptions(documentSelect, items, (item) => item.documentId,
      (item) => `${item.source || '未命名文档'}（${item.documentId}）`, '请选择文档');
    documentSelect.value = items.some((item) => item.documentId === preferredDocumentId) ? preferredDocumentId : items[0].documentId;
    documentSelect.disabled = false;
    await refreshGraphVersions(documentSelect.value, preferredVersion);
  } catch (error) {
    replaceSelectOptions(documentSelect, [], () => '', () => '', '文档加载失败');
    documentSelect.disabled = true;
    resetGraphVersions('请先选择文档');
    setGraphSelectionStatus(error.message, true);
  }
}

function getPage() {
  const page = location.hash.replace('#', '');
  return PAGES.has(page) ? page : 'chat';
}

function navigate(page) {
  location.hash = page;
}

function renderRoute() {
  const page = getPage();
  document.querySelectorAll('[data-page-view]').forEach((element) => { element.hidden = element.dataset.pageView !== page; });
  document.querySelectorAll('[data-page]').forEach((element) => element.classList.toggle('active', element.dataset.page === page));
  get('#chat-sessions').hidden = page !== 'chat';
  if (page === 'approval' && state.auth) refreshPending().catch((error) => { get('#pending-list').textContent = error.message; });
  if (page === 'graph' && state.auth) refreshGraphDocuments().catch((error) => setGraphSelectionStatus(error.message, true));
}

function syncAuth() {
  const loggedIn = Boolean(state.auth?.accessToken);
  get('#auth-gate').hidden = loggedIn;
  get('#identity').textContent = loggedIn ? `${state.auth.username} · ${state.auth.tenantId}` : '未登录';
}

function renderMessage(role, content) {
  const article = document.createElement('article');
  article.className = `message ${role}`;
  const label = document.createElement('span');
  label.className = 'message-label';
  label.textContent = role === 'user' ? '你' : '智忆';
  const text = document.createElement('div');
  text.className = 'message-content';
  text.textContent = content;
  article.append(label, text);
  get('#messages').append(article);
  get('#messages').scrollTop = get('#messages').scrollHeight;
  return article;
}

function renderSources(data, target) {
  const sources = [...(data.evidence || []).map((item) => ({ title: item.source || '知识库', url: '', snippet: item.content || '' })), ...(data.webResults || [])];
  if (!sources.length) return;
  const list = document.createElement('div');
  list.className = 'sources';
  const title = document.createElement('strong');
  title.textContent = '参考来源';
  list.append(title);
  sources.slice(0, 5).forEach((source) => {
    const element = source.url ? document.createElement('a') : document.createElement('div');
    if (source.url) { element.href = source.url; element.target = '_blank'; element.rel = 'noreferrer'; }
    element.className = 'source-card';
    element.textContent = `${source.title}${source.snippet ? `：${source.snippet.slice(0, 120)}` : ''}`;
    list.append(element);
  });
  target.append(list);
}

async function loadMessages(sessionId) {
  const messages = await request(`/api/chat/sessions/${encodeURIComponent(sessionId)}/messages`);
  const root = get('#messages');
  root.replaceChildren();
  if (!messages.length) {
    root.innerHTML = '<div class="welcome"><div class="welcome-mark">◈</div><h2>开始新对话</h2><p>我会在本次会话中记住最近 20 条消息。</p></div>';
    return;
  }
  messages.filter((item) => item.role !== 'system').forEach((item) => renderMessage(item.role, item.content));
}

function renderSessions(sessions) {
  const root = get('#session-list');
  root.replaceChildren();
  if (!sessions.length) { root.innerHTML = '<p class="empty-sessions">还没有对话</p>'; return; }
  sessions.forEach((session) => {
    const button = document.createElement('button');
    button.className = `session ${session.sessionId === state.sessionId ? 'active' : ''}`;
    button.textContent = session.title;
    button.addEventListener('click', async () => {
      state.sessionId = session.sessionId;
      get('#chat-title').textContent = session.title;
      await loadMessages(session.sessionId);
      await refreshSessions();
    });
    root.append(button);
  });
}

async function refreshSessions() {
  renderSessions(await request('/api/chat/sessions'));
}

async function createSession() {
  const session = await request('/api/chat/sessions', { method: 'POST', body: '{}' });
  state.sessionId = session.sessionId;
  get('#chat-title').textContent = '新对话';
  await loadMessages(state.sessionId);
  await refreshSessions();
  get('#question').focus();
}

function showNewChat() {
  state.sessionId = null;
  get('#chat-title').textContent = '新对话';
  get('#messages').replaceChildren(...(newChatWelcome ? [newChatWelcome.cloneNode(true)] : []));
}

async function initializeChat() {
  if (!state.auth) return;
  await refreshSessions();
}

function renderPending(items) {
  const root = get('#pending-list');
  root.replaceChildren();
  if (!items.length) { root.textContent = '当前没有待审批事项。'; return; }
  items.forEach((item) => {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'pending-item';
    const title = document.createElement('strong');
    title.textContent = item.workflowId;
    const meta = document.createElement('span');
    meta.textContent = `版本 ${item.version ?? 0} · ${item.status || '等待审批'}`;
    button.append(title, meta);
    button.addEventListener('click', () => {
      get('#approval-form [name="workflowId"]').value = item.workflowId;
      get('#approval-form [name="version"]').value = item.version ?? 0;
    });
    root.append(button);
  });
}

async function refreshPending() {
  const context = userContext();
  const params = new URLSearchParams({ ...context, approver: 'true' });
  renderPending(await request(`/api/workflows/pending?${params}`));
}

get('#chat-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const input = get('#question');
  const question = input.value.trim();
  if (!question) return;
  const searching = get('#web-search').checked;
  get('#send').disabled = true;
  setStatus(searching ? '正在联网搜索并生成回答…' : '正在生成回答…');
  try {
    if (!state.sessionId) await createSession();
    renderMessage('user', question);
    input.value = '';
    const data = await request('/api/chat', { method: 'POST', body: JSON.stringify({ sessionId: state.sessionId, question, webSearchEnabled: searching }) });
    const message = renderMessage('assistant', data.answer || data.refusalReason || '暂时没有可用回答。');
    renderSources(data, message);
    get('#chat-title').textContent = question.slice(0, 30);
    await refreshSessions();
    setStatus('');
  } catch (error) { setStatus(error.message, true); }
  finally { get('#send').disabled = false; input.focus(); }
});

get('#ingest-form [name="documentFile"]').addEventListener('change', (event) => {
  const file = event.currentTarget.files[0];
  get('#selected-file').textContent = file ? `已选择：${file.name}` : '支持 .md、.markdown、.txt 文件。';
});

get('#ingest-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  const file = form.get('documentFile');
  if (!(file instanceof File) || !file.name) return;
  const extension = file.name.slice(file.name.lastIndexOf('.')).toLowerCase();
  if (!['.md', '.markdown', '.txt'].includes(extension)) { setOutput('#ingest-result', '仅支持 .md、.markdown、.txt 文件。'); return; }
  setBadge('#ingest-badge', '正在入库');
  try {
    const data = await request('/api/documents/markdown', { method: 'POST', body: JSON.stringify({ tenantId: state.auth.tenantId, originalFileName: file.name, permissionTags: splitValues(form.get('permissionTags')), allowedUserIds: splitValues(form.get('allowedUserIds')), markdown: await file.text() }) });
    state.lastDocumentId = data.documentId;
    await refreshGraphDocuments(data.documentId, data.version);
    setOutput('#ingest-result', { documentId: data.documentId, source: data.source, version: data.version, chunkCount: data.chunkCount });
    setBadge('#ingest-badge', '入库已提交');
  } catch (error) { setOutput('#ingest-result', error.message); setBadge('#ingest-badge', '提交失败', true); }
});

get('#query-ingestion-status').addEventListener('click', async () => {
  if (!state.lastDocumentId) { setOutput('#ingest-result', '请先完成一次文档入库。'); return; }
  try {
    const data = await request(`/api/documents/${encodeURIComponent(state.lastDocumentId)}/ingestion-status?tenantId=${encodeURIComponent(state.auth.tenantId)}`);
    setOutput('#ingest-result', data);
  } catch (error) { setOutput('#ingest-result', error.message); }
});

get('#graph-form [name="documentId"]').addEventListener('change', (event) => {
  refreshGraphVersions(event.currentTarget.value).catch((error) => setGraphSelectionStatus(error.message, true));
});

get('#graph-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  setBadge('#graph-badge', '正在查询');
  try {
    const params = new URLSearchParams({ ...userContext(), ...Object.fromEntries(new FormData(event.currentTarget).entries()) });
    const data = await request(`/api/graph/relations?${params}`);
    setOutput('#graph-result', data);
    setBadge('#graph-badge', `${data.relations?.length || 0} 条关系`);
  } catch (error) { setOutput('#graph-result', error.message); setBadge('#graph-badge', '查询失败', true); }
});

get('#refresh-pending').addEventListener('click', () => refreshPending().catch((error) => { get('#pending-list').textContent = error.message; }));

get('#approval-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  setBadge('#approval-badge', '正在提交');
  try {
    const data = await request(`/api/workflows/${encodeURIComponent(form.get('workflowId'))}/approval`, { method: 'POST', body: JSON.stringify({ approverId: state.auth.username, decision: form.get('decision'), comment: form.get('comment'), version: Number(form.get('version')) }) });
    setOutput('#approval-result', data);
    setBadge('#approval-badge', '提交成功');
    await refreshPending();
  } catch (error) { setOutput('#approval-result', error.message); setBadge('#approval-badge', '提交失败', true); }
});

get('#run-evaluation').addEventListener('click', async (event) => {
  event.currentTarget.disabled = true;
  event.currentTarget.textContent = '评测运行中…';
  try { setOutput('#evaluation-result', await request('/api/evaluations/run', { method: 'POST', body: '{}' })); }
  catch (error) { setOutput('#evaluation-result', error.message); }
  finally { event.currentTarget.disabled = false; event.currentTarget.textContent = '运行最小评测'; }
});

get('#new-chat').addEventListener('click', async () => { navigate('chat'); showNewChat(); await refreshSessions().catch((error) => setStatus(error.message, true)); get('#question').focus(); });
get('#web-search').addEventListener('change', (event) => { get('#search-state').textContent = event.target.checked ? '联网搜索已开启' : '联网搜索关闭'; });
get('#logout').addEventListener('click', () => { localStorage.removeItem(AUTH_STORAGE_KEY); state.auth = null; state.sessionId = null; syncAuth(); navigate('chat'); });

document.querySelectorAll('[data-page]').forEach((button) => button.addEventListener('click', () => navigate(button.dataset.page)));
document.querySelectorAll('[data-tab]').forEach((button) => button.addEventListener('click', () => {
  const login = button.dataset.tab === 'login';
  get('#login-form').hidden = !login;
  get('#register-form').hidden = login;
  document.querySelectorAll('[data-tab]').forEach((item) => item.classList.toggle('active', item === button));
}));

get('#register-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  try {
    await request('/api/auth/register', { authenticated: false, method: 'POST', body: JSON.stringify(Object.fromEntries(new FormData(event.currentTarget).entries())) });
    get('#auth-message').textContent = '注册成功，请登录。';
    document.querySelector('[data-tab="login"]').click();
  } catch (error) { get('#auth-message').textContent = error.message; }
});

get('#login-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  try {
    state.auth = await request('/api/auth/login', { authenticated: false, method: 'POST', body: JSON.stringify(Object.fromEntries(new FormData(event.currentTarget).entries())) });
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(state.auth));
    syncAuth();
    await initializeChat();
  } catch (error) { get('#auth-message').textContent = error.message; }
});

state.auth = loadAuth();
syncAuth();
window.addEventListener('hashchange', renderRoute);
renderRoute();
initializeChat().catch((error) => setStatus(error.message, true));
