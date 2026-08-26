const AUTH_STORAGE_KEY = 'agent-platform-auth';
const state = { lastDocumentId: '', lastTenantId: '', toastTimer: null, auth: null, ingestMetadata: null };

const get = (selector) => document.querySelector(selector);
const text = (value) => value == null ? '' : String(value);

function splitValues(value) {
  return value.split(',').map((item) => item.trim()).filter(Boolean);
}

function formatFileSize(bytes) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function loadAuth() {
  try {
    const auth = JSON.parse(window.localStorage.getItem(AUTH_STORAGE_KEY));
    return auth?.accessToken && auth?.username && auth?.tenantId ? auth : null;
  } catch {
    return null;
  }
}

function persistAuth(auth) {
  state.auth = auth;
  window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth));
  syncIdentity();
}

function clearAuth(message = '') {
  state.auth = null;
  window.localStorage.removeItem(AUTH_STORAGE_KEY);
  syncIdentity();
  if (message) setAuthMessage(message, true);
}

function hasApproverRole() {
  return state.auth?.roles?.includes('APPROVER') || false;
}

function setAuthMessage(message, error = false) {
  const target = get('#auth-message');
  target.textContent = message;
  target.classList.toggle('error', error);
}

function showAuthTab(tab) {
  const login = tab === 'login';
  get('#login-form').hidden = !login;
  get('#register-form').hidden = login;
  document.querySelectorAll('[data-auth-tab]').forEach((button) => button.classList.toggle('active', button.dataset.authTab === tab));
  setAuthMessage('');
}

function syncIdentity() {
  const loggedIn = Boolean(state.auth);
  get('#auth-gate').classList.toggle('hidden', loggedIn);
  get('#identity-username').textContent = loggedIn ? state.auth.username : '未登录';
  get('#identity-details').textContent = loggedIn ? `${state.auth.tenantId} · ${(state.auth.roles || []).join(' / ') || 'USER'}` : '请先登录';
  get('#logout-button').hidden = !loggedIn;
  document.querySelectorAll('.identity-tenant').forEach((input) => { input.value = state.auth?.tenantId || ''; });
  document.querySelectorAll('.identity-user').forEach((input) => { input.value = state.auth?.username || ''; });
  const approvalForm = get('#approval-form');
  const note = get('#approval-access-note');
  const allowed = loggedIn && hasApproverRole();
  approvalForm.querySelectorAll('input, textarea, select, button').forEach((control) => { control.disabled = !allowed; });
  note.hidden = allowed;
  note.textContent = loggedIn ? '当前账户没有审批权限。请使用位于 AI_PLATFORM_WORKFLOW_APPROVER_IDS 白名单中的账户登录。' : '请先登录，系统会根据 JWT 中的角色决定是否允许审批。';
}

function setBadge(selector, label, type = 'muted') {
  const badge = get(selector);
  badge.textContent = label;
  badge.className = `badge ${type}`;
}

function setOutput(selector, data) {
  const output = get(selector);
  output.textContent = JSON.stringify(data, null, 2);
  output.hidden = false;
}

function notify(message, error = false) {
  const toast = get('#toast');
  toast.textContent = message;
  toast.className = `toast show${error ? ' error' : ''}`;
  window.clearTimeout(state.toastTimer);
  state.toastTimer = window.setTimeout(() => { toast.className = 'toast'; }, 3300);
}

function setBusy(button, busy, label) {
  if (busy) {
    button.dataset.label = button.textContent;
    button.textContent = label || '处理中…';
    button.disabled = true;
  } else {
    button.textContent = button.dataset.label || button.textContent;
    button.disabled = false;
  }
}

async function request(path, options = {}) {
  const { authenticated = true, headers = {}, ...fetchOptions } = options;
  const requestHeaders = { 'Content-Type': 'application/json', 'X-Trace-Id': `ui-${Date.now()}`, ...headers };
  if (authenticated && state.auth?.accessToken) requestHeaders.Authorization = `Bearer ${state.auth.accessToken}`;
  const response = await fetch(path, { headers: requestHeaders, ...fetchOptions });
  const body = await response.json().catch(() => null);
  if (!response.ok) {
    if (response.status === 401 && authenticated) clearAuth('登录已失效，请重新登录。');
    throw new Error(body?.message || `请求失败（HTTP ${response.status}）`);
  }
  return body;
}

function card(title, meta) {
  const item = document.createElement('article');
  item.className = 'evidence-card';
  const heading = document.createElement('strong');
  heading.textContent = title;
  const description = document.createElement('p');
  description.className = 'evidence-meta';
  description.textContent = meta;
  item.append(heading, description);
  return item;
}

function heading(value) {
  const element = document.createElement('h4');
  element.className = 'subheading';
  element.textContent = value;
  return element;
}

function renderIngestion(data) {
  const root = get('#ingest-result');
  root.className = 'result-content';
  root.replaceChildren();
  root.append(Object.assign(document.createElement('p'), { className: 'answer', textContent: `文档 ${data.documentId}（来源：${data.source}，版本：${data.version}）已接收，共切分为 ${data.chunkCount} 个 Chunk。` }));
  if (Array.isArray(data.chunks) && data.chunks.length) {
    const list = document.createElement('div');
    list.className = 'evidence-list';
    data.chunks.slice(0, 4).forEach((chunk) => list.append(card(chunk.chunkId || 'Chunk', `${chunk.headingPath?.join(' / ') || '无标题'} · ${text(chunk.content).slice(0, 80)}${text(chunk.content).length > 80 ? '…' : ''}`)));
    root.append(heading('切分预览'), list);
  }
}

function renderChat(data) {
  const root = get('#chat-result');
  root.className = 'result-content';
  root.replaceChildren();
  const message = document.createElement('p');
  if (data.waitingApproval) {
    message.className = 'refusal';
    message.textContent = `检索证据已充分，工作流 ${data.workflowId} 正在等待人工审批。`;
  } else if (data.refused) {
    message.className = 'refusal';
    message.textContent = data.refusalReason || '证据不足，系统已拒绝回答。';
  } else {
    message.className = 'answer';
    message.textContent = data.answer || '已完成，但未返回正文。';
  }
  root.append(message);
  if (Array.isArray(data.evidence) && data.evidence.length) {
    const list = document.createElement('div');
    list.className = 'evidence-list';
    data.evidence.forEach((evidence) => list.append(card(`${evidence.source || '未知来源'} · 分数 ${Number(evidence.score || 0).toFixed(4)}`, `文档 ${evidence.documentId} · Chunk ${evidence.chunkId} · ${(evidence.channels || []).join('、')}`)));
    root.append(heading('引用证据'), list);
  }
  if (Array.isArray(data.trace) && data.trace.length) {
    const trace = document.createElement('div');
    trace.className = 'trace';
    data.trace.forEach((item) => trace.append(Object.assign(document.createElement('span'), { textContent: item })));
    root.append(heading('状态机轨迹'), trace);
  }
}

function renderGraph(data) {
  const root = get('#graph-result');
  root.className = 'result-content';
  root.replaceChildren();
  const relations = data.relations || [];
  root.append(Object.assign(document.createElement('p'), { className: 'answer', textContent: `${data.graphAvailable ? '图谱适配器可用' : '当前使用本地降级图谱'}，共返回 ${relations.length} 条关系。` }));
  if (!relations.length) return;
  const list = document.createElement('div');
  list.className = 'relation-list';
  relations.forEach((relation) => {
    const item = document.createElement('article');
    item.className = 'relation-card';
    item.append(Object.assign(document.createElement('strong'), { textContent: `${relation.fromId || relation.from || relation.source || '源节点'}  ⇒  ${relation.type || relation.relation || '关系'}  ⇒  ${relation.toId || relation.to || relation.target || '目标节点'}` }));
    item.append(Object.assign(document.createElement('p'), { textContent: Object.entries(relation).filter(([key]) => !['fromId', 'from', 'source', 'type', 'relation', 'toId', 'to', 'target'].includes(key)).map(([key, value]) => `${key}: ${text(value)}`).join(' · ') || '受权限与版本范围约束' }));
    list.append(item);
  });
  root.append(list);
}

async function checkHealth() {
  const health = get('#health-status');
  try {
    const response = await fetch('/actuator/health');
    if (!response.ok) throw new Error();
    const data = await response.json();
    health.className = 'health ok';
    health.lastElementChild.textContent = `服务正常 · ${data.status || 'UP'}`;
  } catch {
    health.className = 'health error';
    health.lastElementChild.textContent = '服务暂不可用';
  }
}

document.querySelectorAll('[data-auth-tab]').forEach((button) => button.addEventListener('click', () => showAuthTab(button.dataset.authTab)));

get('#register-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const button = event.currentTarget.querySelector('button');
  const form = new FormData(event.currentTarget);
  setBusy(button, true, '创建中…');
  try {
    await request('/api/auth/register', { authenticated: false, method: 'POST', body: JSON.stringify(Object.fromEntries(form.entries())) });
    get('#login-form [name="username"]').value = form.get('username');
    get('#login-form [name="password"]').value = '';
    showAuthTab('login');
    setAuthMessage('账户创建成功，请使用刚才的账户登录。');
  } catch (error) {
    setAuthMessage(error.message, true);
  } finally {
    setBusy(button, false);
  }
});

get('#login-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const button = event.currentTarget.querySelector('button');
  const form = new FormData(event.currentTarget);
  setBusy(button, true, '登录中…');
  try {
    const response = await request('/api/auth/login', { authenticated: false, method: 'POST', body: JSON.stringify(Object.fromEntries(form.entries())) });
    persistAuth(response.data);
    notify(`欢迎回来，${response.data.username}。`);
  } catch (error) {
    setAuthMessage(error.message, true);
  } finally {
    setBusy(button, false);
  }
});

get('#logout-button').addEventListener('click', () => {
  clearAuth();
  showAuthTab('login');
  setAuthMessage('已退出登录。');
});

function setIngestionMetadata(metadata) {
  state.ingestMetadata = metadata;
  ['documentId', 'source', 'version'].forEach((name) => {
    get(`#ingest-form [name="${name}"]`).value = metadata?.[name] || '';
  });
}

async function previewIngestionMetadata(file) {
  if (!file || !state.auth?.tenantId) return setIngestionMetadata(null);
  try {
    const response = await request('/api/documents/metadata-preview', {
      method: 'POST',
      body: JSON.stringify({ tenantId: state.auth.tenantId, originalFileName: file.name }),
    });
    setIngestionMetadata(response.data);
  } catch (error) {
    setIngestionMetadata(null);
    notify(`无法自动识别文档元数据：${error.message}`, true);
  }
}

get('#ingest-form [name="documentFile"]').addEventListener('change', async (event) => {
  const file = event.currentTarget.files[0];
  get('#selected-file').textContent = file ? `已选择：${file.name}（${formatFileSize(file.size)}）` : '尚未选择文件';
  await previewIngestionMetadata(file);
});

get('#ingest-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const button = event.currentTarget.querySelector('button[type="submit"]');
  const form = new FormData(event.currentTarget);
  const file = form.get('documentFile');
  if (!(file instanceof File) || !file.name) return notify('请先选择要上传的文档文件。', true);
  const extension = file.name.slice(file.name.lastIndexOf('.')).toLowerCase();
  if (!['.md', '.markdown', '.txt'].includes(extension)) return notify('暂只支持 .md、.markdown 和 .txt 文档文件。', true);
  setBusy(button, true, '正在入库…');
  try {
    const payload = { tenantId: form.get('tenantId'), originalFileName: file.name, permissionTags: splitValues(form.get('permissionTags')), allowedUserIds: splitValues(form.get('allowedUserIds')), markdown: await file.text() };
    if (!payload.markdown.trim()) throw new Error('上传的文档内容不能为空。');
    const response = await request('/api/documents/markdown', { method: 'POST', body: JSON.stringify(payload) });
    state.lastDocumentId = response.data.documentId;
    state.lastTenantId = payload.tenantId;
    setIngestionMetadata(response.data);
    get('#graph-form [name="documentId"]').value = state.lastDocumentId;
    get('#graph-form [name="version"]').value = response.data.version;
    renderIngestion(response.data);
    setBadge('#ingest-badge', '入库已提交', 'success');
    notify(`文档已提交，Trace ID：${response.traceId || '无'}`);
  } catch (error) {
    setBadge('#ingest-badge', '提交失败', 'danger');
    notify(error.message, true);
  } finally { setBusy(button, false); }
});

get('#query-ingestion-status').addEventListener('click', async (event) => {
  const documentId = state.lastDocumentId || get('#ingest-form [name="documentId"]').value;
  const tenantId = state.lastTenantId || state.auth?.tenantId;
  if (!documentId || !tenantId) return notify('请先填写文档 ID 并登录。', true);
  setBusy(event.currentTarget, true, '查询中…');
  try {
    const response = await request(`/api/documents/${encodeURIComponent(documentId)}/ingestion-status?tenantId=${encodeURIComponent(tenantId)}`);
    setOutput('#ingestion-status', response.data);
    notify('已获取入库任务状态。');
  } catch (error) { notify(error.message, true); } finally { setBusy(event.currentTarget, false); }
});

get('#chat-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const button = event.currentTarget.querySelector('button[type="submit"]');
  const form = new FormData(event.currentTarget);
  const payload = { tenantId: form.get('tenantId'), userId: form.get('userId'), sessionId: form.get('sessionId'), question: form.get('question'), requireApproval: form.get('requireApproval') === 'on' };
  setBusy(button, true, '正在检索…');
  try {
    const response = await request('/api/chat', { method: 'POST', body: JSON.stringify(payload) });
    renderChat(response.data);
    setBadge('#chat-badge', response.data.refused ? '已拒答' : response.data.waitingApproval ? '等待审批' : '已回答', response.data.refused ? 'danger' : response.data.waitingApproval ? 'warning' : 'success');
    if (response.data.workflowId) get('#approval-form [name="workflowId"]').value = response.data.workflowId;
    notify(`问答已完成，Trace ID：${response.traceId || '无'}`);
  } catch (error) { setBadge('#chat-badge', '请求失败', 'danger'); notify(error.message, true); } finally { setBusy(button, false); }
});

get('#graph-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const button = event.currentTarget.querySelector('button[type="submit"]');
  setBusy(button, true, '查询中…');
  try {
    const params = new URLSearchParams(Object.fromEntries(new FormData(event.currentTarget).entries()));
    const response = await request(`/api/graph/relations?${params}`);
    renderGraph(response.data);
    setBadge('#graph-badge', `${response.data.relations?.length || 0} 条关系`, 'success');
  } catch (error) { setBadge('#graph-badge', '查询失败', 'danger'); notify(error.message, true); } finally { setBusy(button, false); }
});

get('#approval-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  if (!hasApproverRole()) return;
  const button = event.currentTarget.querySelector('button[type="submit"]');
  const form = new FormData(event.currentTarget);
  setBusy(button, true, '提交中…');
  try {
    const response = await request(`/api/workflows/${encodeURIComponent(form.get('workflowId'))}/approval`, { method: 'POST', body: JSON.stringify({ approverId: form.get('approverId'), decision: form.get('decision'), comment: form.get('comment'), version: Number(form.get('version')) }) });
    renderChat(response.data);
    get('#approval-result').className = 'result-content';
    get('#approval-result').textContent = form.get('decision') === 'APPROVE' ? '审批已通过，问答流程已继续执行。' : '审批已拒绝，问答流程已终止。';
    setBadge('#approval-badge', form.get('decision') === 'APPROVE' ? '审批通过' : '审批拒绝', form.get('decision') === 'APPROVE' ? 'success' : 'warning');
  } catch (error) { setBadge('#approval-badge', '提交失败', 'danger'); notify(error.message, true); } finally { setBusy(button, false); }
});

get('#run-evaluation').addEventListener('click', async (event) => {
  setBusy(event.currentTarget, true, '运行中…');
  try {
    const response = await request('/api/evaluations/run', { method: 'POST', body: '{}' });
    setOutput('#evaluation-result', response.data);
    notify(`最小评测完成，Trace ID：${response.traceId || '无'}`);
  } catch (error) { notify(error.message, true); } finally { setBusy(event.currentTarget, false); }
});

document.querySelectorAll('.nav-link').forEach((link) => link.addEventListener('click', () => {
  document.querySelectorAll('.nav-link').forEach((item) => item.classList.remove('active'));
  link.classList.add('active');
}));

function appendFieldHint(input, message) {
  const hint = document.createElement('span');
  hint.className = 'field-hint';
  hint.textContent = message;
  input.closest('label').append(hint);
}

function configureIngestionForm() {
  ['documentId', 'source', 'version'].forEach((name) => {
    const input = get(`#ingest-form [name="${name}"]`);
    input.value = '';
    input.readOnly = true;
    input.placeholder = '选择文件后自动生成';
    input.closest('label').classList.add('auto-generated');
  });
  appendFieldHint(get('#ingest-form [name="documentId"]'), '由文件名生成，同一租户下同名文件视为同一文档。');
  appendFieldHint(get('#ingest-form [name="source"]'), '使用原始文件名（不含扩展名），用于检索引用展示。');
  appendFieldHint(get('#ingest-form [name="version"]'), '首次上传为 v1；同文档再次上传时，服务端按库中最新版本自动递增。');
  appendFieldHint(get('#ingest-form [name="permissionTags"]'), '权限标签：public 表示本租户所有用户可检索；user:alice 只授权用户 alice。多个标签用逗号分隔。');
  appendFieldHint(get('#ingest-form [name="allowedUserIds"]'), '允许用户：额外白名单，填写登录用户名（如 alice,bob）。要做私有文档，请删除 public 再填写白名单或 user:用户名 标签。');
}

configureIngestionForm();
state.auth = loadAuth();
syncIdentity();
checkHealth();
