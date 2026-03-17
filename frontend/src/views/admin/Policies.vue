<template>
  <div>
    <div class="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-3">
      <div>
        <h1 class="h3 mb-1">Policy Management</h1>
        <p class="text-muted mb-0">
          Author, version, and publish policy baselines with tracked owner attestations.
        </p>
      </div>
      <button type="button" class="btn btn-primary btn-sm" @click="openCreatePolicyModal">
        Add Policy
      </button>
    </div>
    <div class="small text-muted mb-3">
      Use Add Policy for brand-new policies. Edit updates working drafts; publish sends attestations to application owners.
    </div>

    <div class="card shadow-sm">
      <div class="card-body">
        <h2 class="h5 mb-3">Policies</h2>
        <p v-if="!policies.length" class="text-muted mb-0">No policies created yet.</p>
        <div v-else class="policy-grid">
          <article v-for="policy in policies" :key="policy.id" class="policy-card">
            <div class="policy-card-top">
              <div class="d-flex align-items-center gap-2 flex-wrap">
                <span class="badge text-bg-light border">{{ policy.code }}</span>
                <span class="badge text-bg-secondary">{{ policy.status }}</span>
              </div>
              <button type="button" class="btn btn-link btn-sm p-0" @click="openViewModal(policy)">
                Read Policy
              </button>
            </div>

            <h3 class="h6 mb-1">{{ policy.name }}</h3>
            <p class="small text-muted mb-2">
              CSF: {{ (policy.csfFunctions || []).join(', ') || 'Not mapped' }}
            </p>

            <div class="policy-meta-row">
              <span>Published Version: <strong>{{ policy.publishedVersionId || '-' }}</strong></span>
              <span>Next Review: <strong>{{ formatDate(policy.nextReviewAt) }}</strong></span>
              <span>Revisions: <strong>{{ (policy.revisionHistory || []).length }}</strong></span>
            </div>

            <div class="d-flex gap-2 flex-wrap mt-3">
              <button class="btn btn-primary btn-sm" @click="openEditorModal(policy)">
                Edit
              </button>
              <button
                class="btn btn-outline-primary btn-sm"
                :disabled="!latestDraftVersion(policy)"
                @click="publishPolicy(policy)"
              >
                Publish Draft
              </button>
              <button class="btn btn-outline-secondary btn-sm" @click="openVersionModal(policy)">
                New Revision
              </button>
              <button class="btn btn-outline-secondary btn-sm" @click="openMappingModal(policy)">
                CSF Mapping
              </button>
              <button class="btn btn-outline-secondary btn-sm" @click="openHistoryModal(policy)">
                History
              </button>
            </div>
          </article>
        </div>
      </div>
    </div>

    <div v-if="versionModal.open" class="modal-backdrop-custom">
      <div class="card shadow modal-card">
        <div class="card-body">
          <h2 class="h5 mb-3">New Version - {{ versionModal.policy?.code }}</h2>
          <form class="d-grid gap-2" @submit.prevent="submitVersion">
            <input
              v-model="versionModal.title"
              class="form-control form-control-sm"
              placeholder="Version title"
              required
            />
            <textarea
              v-model="versionModal.bodyMarkdown"
              rows="8"
              class="form-control form-control-sm"
              placeholder="Markdown content"
              required
            />
            <div class="d-flex justify-content-end gap-2">
              <button type="button" class="btn btn-outline-secondary btn-sm" @click="closeVersionModal">Cancel</button>
              <button type="submit" class="btn btn-primary btn-sm">Create Version</button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <div v-if="viewModal.open" class="modal-backdrop-custom">
      <div class="card shadow modal-card modal-card-lg">
        <div class="card-body">
          <div class="policy-view-header">
            <div>
              <div class="d-flex align-items-center gap-2 flex-wrap mb-1">
                <span class="badge text-bg-light border">{{ viewModal.policy?.code }}</span>
                <span class="small text-muted">Version {{ viewModal.versionLabel }}</span>
              </div>
              <h2 class="h5 mb-0">{{ viewModal.policy?.name }}</h2>
            </div>
          </div>
          <section class="policy-rendered-body">
            <article class="policy-prose" v-html="viewModal.html"></article>
          </section>
          <div class="d-flex justify-content-end mt-3 pt-2 border-top">
            <button type="button" class="btn btn-outline-secondary btn-sm" @click="closeViewModal">Close</button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="editorModal.open" class="modal-backdrop-custom">
      <div class="card shadow modal-card modal-card-lg">
        <div class="card-body">
          <h2 class="h5 mb-3">
            {{ editorModal.mode === 'create' ? 'Add New Policy' : `Edit Policy - ${editorModal.policy?.code}` }}
          </h2>
          <div v-if="editorModal.mode === 'create'" class="row g-2 mb-3">
            <div class="col-md-3">
              <input v-model="editorModal.code" class="form-control form-control-sm" placeholder="Code (POL-001)" />
            </div>
            <div class="col-md-4">
              <input v-model="editorModal.name" class="form-control form-control-sm" placeholder="Policy name" />
            </div>
            <div class="col-md-5">
              <input v-model="editorModal.description" class="form-control form-control-sm" placeholder="Description" />
            </div>
          </div>
          <div class="mb-2">
            <input
              v-model="editorModal.title"
              class="form-control form-control-sm"
              placeholder="Version title"
              required
            />
          </div>
          <div ref="quillToolbarRef" class="rich-editor-toolbar ql-toolbar ql-snow mb-2">
            <span class="ql-formats">
              <select class="ql-header">
                <option selected></option>
                <option value="1"></option>
                <option value="2"></option>
              </select>
            </span>
            <span class="ql-formats">
              <button class="ql-bold"></button>
              <button class="ql-italic"></button>
              <button class="ql-underline"></button>
            </span>
            <span class="ql-formats">
              <button class="ql-list" value="ordered"></button>
              <button class="ql-list" value="bullet"></button>
              <button class="ql-link"></button>
            </span>
          </div>
          <div
            ref="editorRef"
            class="rich-editor"
            :class="{ 'rich-editor-create': editorModal.mode === 'create' }"
          ></div>
          <div v-if="editorModal.mode === 'edit'" class="small text-muted mt-2">
            Autosave runs every 10 seconds.
            <span v-if="editorState.lastSavedAt">Last saved {{ formatDateTime(editorState.lastSavedAt) }}.</span>
          </div>
          <div class="d-flex justify-content-end gap-2 mt-3">
            <button type="button" class="btn btn-outline-secondary btn-sm" @click="attemptCloseEditorModal">Cancel</button>
            <button type="button" class="btn btn-primary btn-sm" @click="saveEditedPolicy">
              {{ editorModal.mode === 'create' ? 'Create Policy' : 'Save' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="mappingModal.open" class="modal-backdrop-custom">
      <div class="card shadow modal-card">
        <div class="card-body">
          <h2 class="h5 mb-3">Map to NIST CSF 2.0 - {{ mappingModal.policy?.code }}</h2>
          <div class="d-flex flex-wrap gap-3 mb-3">
            <label v-for="family in csfOptions" :key="`map-${family}`" class="form-check-label small">
              <input
                :checked="mappingModal.csfFunctions.includes(family)"
                class="form-check-input me-1"
                type="checkbox"
                @change="toggleCsfSelection(mappingModal.csfFunctions, family)"
              />
              {{ family }}
            </label>
          </div>
          <div class="d-flex justify-content-end gap-2">
            <button type="button" class="btn btn-outline-secondary btn-sm" @click="closeMappingModal">Cancel</button>
            <button type="button" class="btn btn-primary btn-sm" @click="saveMapping">Save Mapping</button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="historyModal.open" class="modal-backdrop-custom">
      <div class="card shadow modal-card">
        <div class="card-body">
          <h2 class="h5 mb-3">Revision History - {{ historyModal.policy?.code }}</h2>
          <div class="table-responsive">
            <table class="table table-sm table-striped align-middle mb-0">
              <thead>
                <tr>
                  <th>When</th>
                  <th>Event</th>
                  <th>Version</th>
                  <th>Actor</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="event in historyModal.events" :key="event.id">
                  <td>{{ formatDateTime(event.createdAt) }}</td>
                  <td>{{ event.eventSummary }}</td>
                  <td>{{ event.policyVersionId || '-' }}</td>
                  <td>{{ event.actorEmail || '-' }}</td>
                </tr>
                <tr v-if="!historyModal.events.length">
                  <td colspan="4" class="text-muted">No revision history yet.</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="d-flex justify-content-end mt-3">
            <button type="button" class="btn btn-outline-secondary btn-sm" @click="closeHistoryModal">Close</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import Quill from 'quill'
import { marked } from 'marked'
import 'quill/dist/quill.snow.css'
import api from '../../services/api'
import { toastError, toastSuccess } from '../../services/toast'

const policies = ref([])
const csfOptions = ['GOVERN', 'IDENTIFY', 'PROTECT', 'DETECT', 'RESPOND', 'RECOVER']
const versionModal = ref({
  open: false,
  policy: null,
  title: '',
  bodyMarkdown: ''
})
const mappingModal = ref({
  open: false,
  policy: null,
  csfFunctions: []
})
const historyModal = ref({
  open: false,
  policy: null,
  events: []
})
const viewModal = ref({
  open: false,
  policy: null,
  versionLabel: '',
  html: ''
})
const editorModal = ref({
  open: false,
  mode: 'edit',
  policy: null,
  versionId: null,
  title: '',
  code: '',
  name: '',
  description: ''
})
const editorRef = ref(null)
const quillToolbarRef = ref(null)
const quillInstance = ref(null)
const autosaveIntervalMs = 10000
const autosaveTimer = ref(null)
const editorState = ref({
  lastSavedTitle: '',
  lastSavedBody: '',
  lastSavedAt: null,
  saving: false
})

async function loadPolicies() {
  const res = await api.get('/api/policies')
  policies.value = res.data || []
}

onMounted(async () => {
  await loadPolicies()
})

onBeforeUnmount(() => {
  stopAutosave()
})

function latestDraftVersion(policy) {
  return (policy.versions || []).find((v) => v.status === 'DRAFT')
}

function openVersionModal(policy) {
  versionModal.value = {
    open: true,
    policy,
    title: `${policy.name} - next revision`,
    bodyMarkdown: '## Scope\n\n## Controls\n\n## Exceptions'
  }
}

function closeVersionModal() {
  versionModal.value.open = false
}

function latestViewableVersion(policy) {
  const versions = policy.versions || []
  return versions.find((v) => v.status === 'PUBLISHED') || versions.find((v) => v.status === 'DRAFT') || versions[0]
}

function openViewModal(policy) {
  const version = latestViewableVersion(policy)
  viewModal.value = {
    open: true,
    policy,
    versionLabel: version ? `v${version.versionNumber} - ${version.title}` : '-',
    html: renderPolicyHtml(version?.bodyMarkdown)
  }
}

function closeViewModal() {
  viewModal.value.open = false
}

function renderPolicyHtml(content) {
  return normalizePolicyContent(content, '<p class="text-muted">No policy body is available.</p>')
}

async function openEditorModal(policy) {
  try {
    const freshPolicy = await loadPolicyByIdSafe(policy) || policy
    let draft = latestDraftVersion(freshPolicy)
    if (!draft) {
      const source = latestViewableVersion(freshPolicy)
      const created = await api.post(`/api/policies/${policy.id}/versions`, {
        title: `${freshPolicy.name} - working draft`,
        bodyMarkdown: source?.bodyMarkdown || '<p></p>'
      })
      draft = created?.data || null
    }
    if (!draft) {
      const refreshed = await loadPolicyByIdSafe(freshPolicy)
      draft = latestDraftVersion(refreshed || freshPolicy)
    }
    if (!draft) {
      toastError('No editable draft version is available for this policy')
      return
    }
    editorModal.value = {
      open: true,
      mode: 'edit',
      policy: freshPolicy,
      versionId: draft.id,
      title: draft.title || `${freshPolicy.name} - draft`,
      code: '',
      name: '',
      description: ''
    }
    await nextTick()
    const fallbackBody = latestViewableVersion(freshPolicy)?.bodyMarkdown || '<p></p>'
    initializeQuill(toEditorHtml(draft.bodyMarkdown || fallbackBody))
    editorState.value = {
      lastSavedTitle: editorModal.value.title,
      lastSavedBody: quillInstance.value?.root?.innerHTML || '',
      lastSavedAt: null,
      saving: false
    }
    startAutosave()
  } catch (e) {
    // Keep this visible in devtools when troubleshooting edit-modal startup.
    console.error('openEditorModal failed', e)
    toastError(e.response?.data?.error || e.message || 'Failed to open policy editor')
  }
}

async function openCreatePolicyModal() {
  editorModal.value = {
    open: true,
    mode: 'create',
    policy: null,
    versionId: null,
    title: 'Policy v1',
    code: '',
    name: '',
    description: ''
  }
  await nextTick()
  initializeQuill('<h2>Purpose</h2><p></p><h2>Scope</h2><p></p><h2>Policy Statements</h2><p></p>')
  editorState.value = {
    lastSavedTitle: '',
    lastSavedBody: '',
    lastSavedAt: null,
    saving: false
  }
  stopAutosave()
}

function closeEditorModal() {
  stopAutosave()
  destroyQuill()
  editorModal.value.open = false
}

function initializeQuill(initialHtml) {
  if (!editorRef.value || !quillToolbarRef.value) return
  if (!quillInstance.value || quillInstance.value.container !== editorRef.value) {
    destroyQuill()
    quillInstance.value = new Quill(editorRef.value, {
      theme: 'snow',
      modules: {
        toolbar: quillToolbarRef.value
      }
    })
  }
  const html = initialHtml && initialHtml.trim() ? initialHtml : '<p></p>'
  // Prefer Quill-native conversion to avoid blot/selection edge-case errors.
  if (quillInstance.value.clipboard?.convert && quillInstance.value.setContents) {
    const delta = quillInstance.value.clipboard.convert({ html })
    quillInstance.value.setContents(delta, 'silent')
    return
  }
  if (quillInstance.value.clipboard?.dangerouslyPasteHTML && quillInstance.value.setText) {
    quillInstance.value.setText('', 'silent')
    quillInstance.value.clipboard.dangerouslyPasteHTML(html)
    return
  }
  quillInstance.value.root.innerHTML = html
}

function destroyQuill() {
  if (!quillInstance.value) return
  if (quillInstance.value.container) {
    quillInstance.value.container.innerHTML = ''
  }
  quillInstance.value = null
}

async function loadPolicyByIdSafe(policy) {
  try {
    const res = await api.get('/api/policies')
    return (res.data || []).find((item) => item.id === policy.id) || policy
  } catch {
    return policy
  }
}

function toEditorHtml(content) {
  const html = normalizePolicyContent(content, '<p></p>')
  return html
}

function normalizePolicyContent(content, fallbackHtml) {
  if (!content || !content.trim()) return fallbackHtml
  const raw = content.trim()
  const looksLikeHtml = /<\/?[a-z][\s\S]*>/i.test(raw)
  if (!looksLikeHtml) return markdownToHtml(raw)
  if (containsSemanticHtml(raw)) return raw
  const textOnly = htmlToPlainText(raw)
  if (looksLikeMarkdown(textOnly)) return markdownToHtml(textOnly)
  return raw
}

function markdownToHtml(value) {
  const normalized = value.replace(/([^\n])\s(#{1,6}\s)/g, '$1\n\n$2')
  return marked.parse(normalized, { gfm: true, breaks: true })
}

function containsSemanticHtml(value) {
  return /<(h[1-6]|ul|ol|li|table|thead|tbody|tr|td|th|blockquote|pre|code|strong|em|u)\b/i.test(value)
}

function looksLikeMarkdown(value) {
  return /(^|\n)\s*#{1,6}\s|(^|\n)\s*[-*]\s|(^|\n)\s*\d+\.\s|```/m.test(value || '')
}

function htmlToPlainText(value) {
  return value
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<\/p>/gi, '\n\n')
    .replace(/<\/li>/gi, '\n')
    .replace(/<li[^>]*>/gi, '- ')
    .replace(/<[^>]+>/g, '')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

function hasUnsavedChanges() {
  const currentBody = quillInstance.value?.root?.innerHTML || ''
  return editorModal.value.title !== editorState.value.lastSavedTitle ||
    currentBody !== editorState.value.lastSavedBody
}

function startAutosave() {
  stopAutosave()
  autosaveTimer.value = window.setInterval(() => {
    void saveEditedPolicy({ closeAfterSave: false, showToast: false })
  }, autosaveIntervalMs)
}

function stopAutosave() {
  if (autosaveTimer.value) {
    window.clearInterval(autosaveTimer.value)
    autosaveTimer.value = null
  }
}

async function saveEditedPolicy(options = {}) {
  const { closeAfterSave = true, showToast = true } = options
  if (editorState.value.saving) return
  if (editorModal.value.mode === 'create') {
    return saveNewPolicy(closeAfterSave, showToast)
  }
  if (!hasUnsavedChanges()) {
    if (closeAfterSave) closeEditorModal()
    return
  }
  try {
    editorState.value.saving = true
    const bodyMarkdown = quillInstance.value?.root?.innerHTML || ''
    await api.put(`/api/policies/${editorModal.value.policy.id}/versions/${editorModal.value.versionId}`, {
      title: editorModal.value.title,
      bodyMarkdown
    })
    editorState.value.lastSavedTitle = editorModal.value.title
    editorState.value.lastSavedBody = bodyMarkdown
    editorState.value.lastSavedAt = new Date().toISOString()
    if (closeAfterSave) closeEditorModal()
    await loadPolicies()
    if (showToast) toastSuccess('Policy draft updated.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to update policy draft')
  } finally {
    editorState.value.saving = false
  }
}

async function saveNewPolicy(closeAfterSave, showToast) {
  if (!editorModal.value.code?.trim() || !editorModal.value.name?.trim()) {
    toastError('Policy code and name are required')
    return
  }
  try {
    editorState.value.saving = true
    const bodyMarkdown = quillInstance.value?.root?.innerHTML || ''
    await api.post('/api/policies', {
      code: editorModal.value.code.trim(),
      name: editorModal.value.name.trim(),
      description: editorModal.value.description || null,
      initialVersionTitle: editorModal.value.title || `${editorModal.value.name.trim()} v1`,
      initialBodyMarkdown: bodyMarkdown,
      csfFunctions: []
    })
    if (closeAfterSave) closeEditorModal()
    await loadPolicies()
    if (showToast) toastSuccess('Policy created.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to create policy')
  } finally {
    editorState.value.saving = false
  }
}

function attemptCloseEditorModal() {
  if (hasUnsavedChanges()) {
    const discard = window.confirm('You have unsaved changes. Click OK to discard them, or Cancel to continue editing.')
    if (!discard) return
  }
  closeEditorModal()
}

function openMappingModal(policy) {
  mappingModal.value = {
    open: true,
    policy,
    csfFunctions: [...(policy.csfFunctions || [])]
  }
}

function closeMappingModal() {
  mappingModal.value.open = false
}

async function saveMapping() {
  try {
    await api.put(`/api/policies/${mappingModal.value.policy.id}/csf-mappings`, {
      csfFunctions: mappingModal.value.csfFunctions
    })
    closeMappingModal()
    await loadPolicies()
    toastSuccess('CSF mappings saved.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to update CSF mappings')
  }
}

async function openHistoryModal(policy) {
  try {
    const res = await api.get(`/api/policies/${policy.id}/revision-history`)
    historyModal.value = {
      open: true,
      policy,
      events: res.data || []
    }
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to load revision history')
  }
}

function closeHistoryModal() {
  historyModal.value.open = false
}

async function submitVersion() {
  try {
    await api.post(`/api/policies/${versionModal.value.policy.id}/versions`, {
      title: versionModal.value.title,
      bodyMarkdown: versionModal.value.bodyMarkdown
    })
    closeVersionModal()
    await loadPolicies()
    toastSuccess('Policy version created.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to create version')
  }
}

async function publishPolicy(policy) {
  const draft = latestDraftVersion(policy)
  if (!draft) return
  try {
    await api.post(`/api/policies/${policy.id}/publish`, {
      policyVersionId: draft.id
    })
    await loadPolicies()
    toastSuccess('Policy published and attestation tasks generated.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to publish policy')
  }
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleDateString()
}

function formatDateTime(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}

function toggleCsfSelection(target, family) {
  const index = target.indexOf(family)
  if (index >= 0) target.splice(index, 1)
  else target.push(family)
}
</script>

<style scoped>
.modal-backdrop-custom {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  z-index: 1200;
  display: grid;
  place-items: center;
  padding: 1rem;
}
.modal-card {
  width: min(760px, 100%);
}

.modal-card-lg {
  width: min(1100px, 100%);
}

.policy-view-header {
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 0.75rem;
  margin-bottom: 0.9rem;
}

.policy-rendered-body {
  max-height: 68vh;
  overflow: auto;
  border: 1px solid #cfd8e3;
  border-radius: 0.5rem;
  padding: 0;
  background: #f1f5f9;
}

.policy-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 0.9rem;
}

.policy-card {
  border: 1px solid #dbe3ee;
  border-radius: 0.65rem;
  padding: 0.9rem;
  background: #fff;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.06);
}

.policy-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.policy-meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  font-size: 0.8rem;
  color: #475569;
}

.policy-prose {
  color: #0f172a;
  font-size: 1rem;
  line-height: 1.72;
  max-width: 900px;
  margin: 0 auto;
  background: #fff;
  border-left: 1px solid #e2e8f0;
  border-right: 1px solid #e2e8f0;
  min-height: 100%;
  border-radius: 0.5rem;
  padding: 1.8rem 2rem;
}

.policy-prose :deep(h1),
.policy-prose :deep(h2),
.policy-prose :deep(h3),
.policy-prose :deep(h4) {
  color: #020617;
  line-height: 1.3;
  margin-top: 1.4rem;
  margin-bottom: 0.7rem;
  font-weight: 700;
}

.policy-prose :deep(h1) {
  font-size: 1.7rem;
  border-bottom: 1px solid #dbe3ee;
  padding-bottom: 0.45rem;
}

.policy-prose :deep(h2) {
  font-size: 1.35rem;
}

.policy-prose :deep(h3) {
  font-size: 1.12rem;
}

.policy-prose :deep(p) {
  margin: 0 0 1rem;
}

.policy-prose :deep(ul),
.policy-prose :deep(ol) {
  margin: 0 0 0.95rem;
  padding-left: 1.2rem;
}

.policy-prose :deep(li) {
  margin: 0.25rem 0;
}

.policy-prose :deep(blockquote) {
  margin: 1rem 0;
  padding: 0.75rem 1rem;
  border-left: 4px solid #60a5fa;
  background: #eff6ff;
  color: #1e3a8a;
  border-radius: 0 0.35rem 0.35rem 0;
}

.policy-prose :deep(a) {
  color: #1d4ed8;
  text-decoration: underline;
}

.policy-prose :deep(hr) {
  border: 0;
  border-top: 1px solid #e5e7eb;
  margin: 1rem 0;
}

.policy-prose :deep(code) {
  background: #eef2f7;
  color: #111827;
  border-radius: 0.25rem;
  padding: 0.1rem 0.3rem;
  font-size: 0.88em;
}

.policy-prose :deep(pre) {
  background: #0b1220;
  color: #dbe7ff;
  border-radius: 0.5rem;
  padding: 0.9rem;
  overflow: auto;
  margin: 1rem 0;
}

.policy-prose :deep(pre code) {
  background: transparent;
  color: inherit;
  padding: 0;
}

.policy-prose :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 0.9rem 0;
  font-size: 0.9rem;
}

.policy-prose :deep(th),
.policy-prose :deep(td) {
  border: 1px solid #cfd8e3;
  padding: 0.5rem 0.6rem;
  vertical-align: top;
}

.policy-prose :deep(th) {
  background: #f8fafc;
  font-weight: 700;
}

.rich-editor-toolbar {
  border: 1px solid #dee2e6;
  border-radius: 0.375rem 0.375rem 0 0;
}

.rich-editor {
  border: 1px solid #dee2e6;
  border-top: 0 !important;
  border-radius: 0 0 0.375rem 0.375rem !important;
  background: #fff;
  overflow: auto;
}

.rich-editor :deep(.ql-editor) {
  min-height: 300px;
}

.rich-editor-create :deep(.ql-editor) {
  min-height: 520px;
}
</style>
