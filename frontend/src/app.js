const state = { lastDocumentId: '', lastTenantId: '', toastTimer: null };

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

async function readDocumentFile(file) {
  const allowedExtensions = ['.md', '.markdown', '.txt'];
  const extension = file.name.slice(file.name.lastIndexOf('.')).toLowerCase();
  if (!allowedExtensions.includes(extension)) {
    throw new Error('暂只支持 .md、.markdown 和 .txt 文档文件。');
  }
  const content = await file.text();
  if (!content.trim()) throw new Error('上传的文档内容不能为空。');
  return content;
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
  const response = await fetch(path, {
    headers: { 'Content-Type': 'application/json', 'X-Trace-Id': `ui-${Date.now()}`, ...(options.headers || {}) },
    ...options
  });
  const body = await response.json().catch(() => null);
  if (!response.ok) throw new Error(body?.message || `请求失败（HTTP ${response.status}）`);
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
  const summary = document.createElement('p');
  summary.className = 'answer';
  summary.textContent = `文档 ${data.documentId} 已接收，共切分为 ${data.chunkCount} 个 Chunk。`;
  root.append(summary);
  if (Array.isArray(data.chunks) && data.chunks.length) {
    root.append(heading('切分预览'));
    const list = document.createElement('div');
    list.className = 'evidence-list';
    data.chunks.slice(0, 4).forEach((chunk) => list.append(card(chunk.chunkId || 'Chunk', `${chunk.headingPath?.join(' / ') || '无标题'} · ${text(chunk.content).slice(0, 80)}${text(chunk.content).length > 80 ? '…' : ''}`)));
    root.append(list);
  }
}

function renderChat(data) {
  const root = get('#chat-result');
  root.className = 'result-content';
  root.replaceChildren();
  if (data.waitingApproval) {
    const message = document.createElement('p');
    message.className = 'refusal';
    message.textContent = `检索证据已充分，工作流 ${data.workflowId} 正在等待人工审批。`;
    root.append(message);
  } else if (data.refused) {
    const refusal = document.createElement('p');
    refusal.className = 'refusal';
    refusal.textContent = data.refusalReason || '证据不足，系统已拒绝回答。';
    root.append(refusal);
  } else {
    const answer = document.createElement('p');
    answer.className = 'answer';
    answer.textContent = data.answer || '已完成，但未返回正文。';
    root.append(answer);
  }
  if (Array.isArray(data.evidence) && data.evidence.length) {
    root.append(heading('引用证据'));
    const list = document.createElement('div');
    list.className = 'evidence-list';
    data.evidence.forEach((evidence) => list.append(card(`${evidence.source || '未知来源'} · 分数 ${Number(evidence.score || 0).toFixed(4)}`, `文档 ${evidence.documentId} · Chunk ${evidence.chunkId} · ${Array.isArray(evidence.channels) ? evidence.channels.join('、') : ''}`)));
    root.append(list);
  }
  if (Array.isArray(data.trace) && data.trace.length) {
    root.append(heading('状态机轨迹'));
    const trace = document.createElement('div');
    trace.className = 'trace';
    data.trace.forEach((item) => { const node = document.createElement('span'); node.textContent = item; trace.append(node); });
    root.append(trace);
  }
}

function renderGraph(data) {
  const root = get('#graph-result');
  root.className = 'result-content';
  root.replaceChildren();
  const message = document.createElement('p');
  message.className = 'answer';
  const relations = data.relations || [];
  message.textContent = `${data.graphAvailable ? '图谱适配器可用' : '当前使用本地降级图谱'}，共返回 ${relations.length} 条关系。`;
  root.append(message);
  if (!relations.length) {
    const empty = document.createElement('p');
    empty.className = 'refusal';
    empty.textContent = '没有找到可见关系。请先入库对应版本的文档，或检查租户、用户和版本。';
    root.append(empty);
    return;
  }
  const list = document.createElement('div');
  list.className = 'relation-list';
  relations.forEach((relation) => {
    const item = document.createElement('article');
    item.className = 'relation-card';
    const title = document.createElement('strong');
    title.textContent = `${relation.fromId || relation.from || relation.source || '源节点'}  →  ${relation.type || relation.relation || '关系'}  →  ${relation.toId || relation.to || relation.target || '目标节点'}`;
    const meta = document.createElement('p');
    meta.textContent = Object.entries(relation).filter(([key]) => !['fromId', 'from', 'source', 'type', 'relation', 'toId', 'to', 'target'].includes(key)).map(([key, value]) => `${key}: ${text(value)}`).join(' · ') || '受权限与版本范围约束';
    item.append(title, meta);
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

get('#ingest-form [name="documentFile"]').addEventListener('change', (event) => {
  const file = event.currentTarget.files[0];
  const selectedFile = get('#selected-file');
  if (!file) {
    selectedFile.textContent = '尚未选择文件';
    return;
  }
  selectedFile.textContent = `已选择：${file.name}（${formatFileSize(file.size)}）`;
});

get('#ingest-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const button = event.currentTarget.querySelector('button[type="submit"]');
  const form = new FormData(event.currentTarget);
  const file = form.get('documentFile');
  if (!(file instanceof File) || !file.name) {
    notify('请先选择要上传的文档文件。', true);
    return;
  }
  const payload = {
    documentId: form.get('documentId') || null,
    tenantId: form.get('tenantId'), source: form.get('source'), version: form.get('version'),
    permissionTags: splitValues(form.get('permissionTags')), allowedUserIds: splitValues(form.get('allowedUserIds'))
  };
  setBusy(button, true, '正在入库…');
  try {
    payload.markdown = await readDocumentFile(file);
    const response = await request('/api/documents/markdown', { method: 'POST', body: JSON.stringify(payload) });
    state.lastDocumentId = response.data.documentId;
    state.lastTenantId = payload.tenantId;
    renderIngestion(response.data);
    setBadge('#ingest-badge', '入库已提交', 'success');
    get('#graph-form [name="documentId"]').value = state.lastDocumentId;
    get('#graph-form [name="tenantId"]').value = state.lastTenantId;
    notify(`文档已提交，Trace ID：${response.traceId || '无'}`);
  } catch (error) {
    setBadge('#ingest-badge', '提交失败', 'danger');
    notify(error.message, true);
  } finally { setBusy(button, false); }
});

get('#query-ingestion-status').addEventListener('click', async (event) => {
  const documentId = state.lastDocumentId || get('#ingest-form [name="documentId"]').value;
  const tenantId = state.lastTenantId || get('#ingest-form [name="tenantId"]').value;
  if (!documentId || !tenantId) return notify('请先填写文档 ID 和租户 ID。', true);
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
    const type = response.data.refused ? 'danger' : response.data.waitingApproval ? 'warning' : 'success';
    setBadge('#chat-badge', response.data.refused ? '已拒答' : response.data.waitingApproval ? '等待审批' : '已回答', type);
    if (response.data.workflowId) {
      get('#approval-form [name="workflowId"]').value = response.data.workflowId;
      get('#approval').scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
    notify(`问答已完成，Trace ID：${response.traceId || '无'}`);
  } catch (error) { setBadge('#chat-badge', '请求失败', 'danger'); notify(error.message, true); } finally { setBusy(button, false); }
});

get('#graph-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const button = event.currentTarget.querySelector('button[type="submit"]');
  const form = new FormData(event.currentTarget);
  const params = new URLSearchParams(Object.fromEntries(form.entries()));
  setBusy(button, true, '查询中…');
  try {
    const response = await request(`/api/graph/relations?${params}`);
    renderGraph(response.data);
    setBadge('#graph-badge', `${response.data.relations?.length || 0} 条关系`, 'success');
    notify('图谱关系已更新。');
  } catch (error) { setBadge('#graph-badge', '查询失败', 'danger'); notify(error.message, true); } finally { setBusy(button, false); }
});

get('#approval-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const button = event.currentTarget.querySelector('button[type="submit"]');
  const form = new FormData(event.currentTarget);
  const workflowId = form.get('workflowId');
  const payload = { approverId: form.get('approverId'), decision: form.get('decision'), comment: form.get('comment'), version: Number(form.get('version')) };
  setBusy(button, true, '提交中…');
  try {
    const response = await request(`/api/workflows/${encodeURIComponent(workflowId)}/approval`, { method: 'POST', body: JSON.stringify(payload) });
    renderChat(response.data);
    get('#approval-result').className = 'result-content';
    get('#approval-result').replaceChildren(Object.assign(document.createElement('p'), { className: 'answer', textContent: payload.decision === 'APPROVE' ? '审批已通过，问答流程已继续执行。' : '审批已拒绝，问答流程已终止。' }));
    setBadge('#approval-badge', payload.decision === 'APPROVE' ? '审批通过' : '审批拒绝', payload.decision === 'APPROVE' ? 'success' : 'warning');
    setBadge('#chat-badge', response.data.refused ? '已拒答' : '审批后完成', response.data.refused ? 'danger' : 'success');
    notify(`审批已提交，Trace ID：${response.traceId || '无'}`);
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

checkHealth();
