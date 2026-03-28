<template>
  <div class="branching-demo">
    <div class="d-flex flex-wrap gap-2 justify-content-between mb-3">
      <h1 class="h4 mb-0">Branching workflow <span class="badge text-bg-secondary">Demo</span></h1>
      <button type="button" class="btn btn-outline-secondary btn-sm" :disabled="loading" @click="loadGraph">Reload</button>
    </div>
    <p class="text-muted small mb-3">
      Server-resolved conditional flow. The diagram reflects your <strong>draft</strong> below; <strong>Try run</strong> uses the
      last <strong>saved</strong> workflow until you click Save.
    </p>

    <div v-if="loadError" class="alert alert-warning">
      <div class="fw-semibold mb-1">Could not load demo workflow</div>
      <div class="small mb-0">{{ loadError }}</div>
    </div>
    <div v-else-if="loading" class="text-muted py-4">Loading…</div>

    <div v-else-if="hasGraph" class="d-flex flex-column gap-4">
      <div class="row g-4">
        <div class="col-xl-7">
          <div class="card shadow-sm">
            <div class="card-header py-2 small fw-semibold d-flex justify-content-between align-items-center">
              <span>Flow (draft preview)</span>
              <span v-if="!previewGraph.startNodeId" class="text-warning small">Set a valid start step key</span>
            </div>
            <div class="card-body p-2 overflow-auto">
              <div
                class="border rounded bg-body-secondary position-relative"
                :style="{ width: canvasW + 'px', height: canvasH + 'px', minWidth: '100%' }"
              >
                <svg class="position-absolute top-0 start-0" :width="canvasW" :height="canvasH" aria-hidden="true" style="pointer-events: none">
                  <defs>
                    <marker :id="arrowMarkerId" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto">
                      <path d="M0,0 L8,4 L0,8 z" fill="currentColor" />
                    </marker>
                  </defs>
                  <line
                    v-for="e in previewFlowEdges"
                    :key="'e' + e.id"
                    :x1="previewNodeCenter(e.fromNodeId).x"
                    :y1="previewNodeCenter(e.fromNodeId).y"
                    :x2="previewNodeCenter(e.toNodeId).x"
                    :y2="previewNodeCenter(e.toNodeId).y"
                    stroke="currentColor"
                    stroke-width="1.2"
                    opacity="0.5"
                    :marker-end="`url(#${arrowMarkerId})`"
                  />
                </svg>
                <div
                  v-for="n in previewFlowNodes"
                  :key="'n' + n.id"
                  class="card position-absolute shadow-sm small"
                  :class="{ 'border-primary': previewHighlightId === n.id }"
                  :style="nodeStyle(n)"
                >
                  <div class="card-body p-2">
                    <div class="text-muted text-uppercase" style="font-size: 0.65rem">{{ n.questionType }}</div>
                    <div class="fw-semibold">{{ n.title }}</div>
                    <div class="text-muted font-monospace" style="font-size: 0.65rem">{{ n.stableKey }}</div>
                  </div>
                </div>
              </div>
              <ul class="small text-muted mt-2 mb-0 ps-3">
                <li v-for="e in previewFlowEdges" :key="'l' + e.id">{{ previewEdgeLabel(e) }}</li>
              </ul>
            </div>
          </div>
        </div>
        <div class="col-xl-5">
          <div class="card shadow-sm">
            <div class="card-header py-2 small fw-semibold">Try run <span class="text-muted fw-normal">(saved graph)</span></div>
            <div class="card-body">
              <div v-if="!runStarted">
                <button type="button" class="btn btn-primary btn-sm" @click="startRun">Start</button>
                <p v-if="startRunError" class="small text-danger mt-2 mb-0">{{ startRunError }}</p>
              </div>
              <template v-else>
                <div v-if="runFinished" class="alert alert-success small">
                  <div class="fw-semibold">{{ endTitle }}</div>
                  <div v-if="endBody" class="mt-1">{{ endBody }}</div>
                  <button type="button" class="btn btn-outline-secondary btn-sm mt-2" @click="resetRun">Reset</button>
                </div>
                <template v-else-if="currentRunNode">
                  <p class="fw-medium mb-1">{{ currentRunNode.title }}</p>
                  <p v-if="currentRunNode.body" class="small text-muted mb-3">{{ currentRunNode.body }}</p>
                  <div v-if="currentRunNode.questionType === 'YES_NO'" class="d-flex gap-2">
                    <button type="button" class="btn btn-outline-primary btn-sm" @click="submitAnswer('yes')">Yes</button>
                    <button type="button" class="btn btn-outline-primary btn-sm" @click="submitAnswer('no')">No</button>
                  </div>
                  <div v-else-if="currentRunNode.questionType === 'CHOICE'" class="d-grid gap-1">
                    <template v-if="currentRunNode.choices?.length">
                      <button
                        v-for="c in currentRunNode.choices"
                        :key="c.id"
                        type="button"
                        class="btn btn-outline-primary btn-sm text-start"
                        @click="submitAnswer(c.id)"
                      >
                        {{ c.label }}
                      </button>
                    </template>
                    <p v-else class="small text-warning mb-0">
                      No choices returned for this step (check <code>choices_json</code> in the database).
                    </p>
                  </div>
                  <div v-else-if="currentRunNode.questionType === 'TEXT'">
                    <textarea v-model="textAnswer" class="form-control form-control-sm mb-2" rows="3" />
                    <button type="button" class="btn btn-primary btn-sm" :disabled="resolving" @click="submitText">Continue</button>
                  </div>
                  <div v-else class="alert alert-warning small py-2 mb-0">
                    Unsupported step type
                    <span class="font-monospace">{{ currentRunNode.questionType || '—' }}</span>
                    . Expected YES_NO, CHOICE, TEXT, or END.
                  </div>
                  <p v-if="resolveError" class="small text-danger mt-2 mb-0">{{ resolveError }}</p>
                  <button type="button" class="btn btn-link btn-sm px-0 mt-2" @click="resetRun">Cancel</button>
                </template>
              </template>
            </div>
          </div>
          <div class="card shadow-sm mt-3">
            <div class="card-header py-2 small fw-semibold">Path</div>
            <div class="card-body small">
              <ol v-if="pathLabels.length" class="mb-0 ps-3">
                <li v-for="(p, i) in pathLabels" :key="i">{{ p }}</li>
              </ol>
              <p v-else class="text-muted mb-0">—</p>
            </div>
          </div>
        </div>
      </div>

      <div class="card shadow-sm">
        <div class="card-header py-2 small fw-semibold d-flex flex-wrap gap-2 justify-content-between align-items-center">
          <span>Edit workflow</span>
          <div class="d-flex flex-wrap gap-2">
            <button type="button" class="btn btn-outline-secondary btn-sm" :disabled="saving" @click="resetDraft">Reset draft</button>
            <button type="button" class="btn btn-primary btn-sm" :disabled="saving" @click="saveWorkflow">
              {{ saving ? 'Saving…' : 'Save to server' }}
            </button>
          </div>
        </div>
        <div class="card-body">
          <p v-if="saveSuccess" class="small text-success mb-2">{{ saveSuccess }}</p>
          <p v-if="saveError" class="small text-danger mb-2">{{ saveError }}</p>

          <div class="row g-3 mb-4">
            <div class="col-md-6">
              <label class="form-label small mb-1">Start step (stable key)</label>
              <select v-model="draftStartStableKey" class="form-select form-select-sm">
                <option v-for="k in draftStableKeyOptions" :key="k" :value="k">{{ k }}</option>
              </select>
            </div>
            <div class="col-md-6 d-flex align-items-end gap-2">
              <button type="button" class="btn btn-outline-primary btn-sm" @click="addDraftNode">Add step</button>
              <button type="button" class="btn btn-outline-primary btn-sm" @click="addDraftEdge">Add transition</button>
            </div>
          </div>

          <h2 class="h6 text-muted mb-2">Steps</h2>
          <p class="small text-muted mb-2">Stable key must stay unique. For <code>CHOICE</code>, set JSON like
            <code>[{"id":"a","label":"Option A"}]</code></p>
          <div class="vstack gap-3 mb-4">
            <div v-for="(node, idx) in draftNodes" :key="'dn' + idx" class="border rounded p-3 bg-body-tertiary">
              <div class="d-flex justify-content-between align-items-start gap-2 mb-2">
                <span class="small fw-semibold text-muted">Step {{ idx + 1 }}</span>
                <button type="button" class="btn btn-outline-danger btn-sm py-0" @click="removeDraftNode(idx)">Remove</button>
              </div>
              <div class="row g-2 small">
                <div class="col-md-3">
                  <label class="form-label mb-0">Stable key</label>
                  <input v-model="node.stableKey" type="text" class="form-control form-control-sm font-monospace" autocomplete="off" />
                </div>
                <div class="col-md-3">
                  <label class="form-label mb-0">Type</label>
                  <select v-model="node.questionType" class="form-select form-select-sm">
                    <option v-for="t in questionTypes" :key="t" :value="t">{{ t }}</option>
                  </select>
                </div>
                <div class="col-md-3">
                  <label class="form-label mb-0">posX</label>
                  <input v-model.number="node.posX" type="number" class="form-control form-control-sm" />
                </div>
                <div class="col-md-3">
                  <label class="form-label mb-0">posY</label>
                  <input v-model.number="node.posY" type="number" class="form-control form-control-sm" />
                </div>
                <div class="col-12">
                  <label class="form-label mb-0">Title</label>
                  <input v-model="node.title" type="text" class="form-control form-control-sm" />
                </div>
                <div class="col-12">
                  <label class="form-label mb-0">Body / help text</label>
                  <textarea v-model="node.body" class="form-control form-control-sm" rows="2" />
                </div>
                <div v-if="node.questionType === 'CHOICE'" class="col-12">
                  <label class="form-label mb-0">Choices JSON</label>
                  <textarea v-model="node.choicesJson" class="form-control form-control-sm font-monospace" rows="3" placeholder='[{"id":"pci","label":"PCI DSS"}]' />
                </div>
              </div>
            </div>
          </div>

          <h2 class="h6 text-muted mb-2">Transitions</h2>
          <p class="small text-muted mb-2">Order matters: first matching transition wins. Use <code>OPTION</code> + value for each choice id;
            <code>ALWAYS</code> as fallback after specific branches.</p>
          <div class="table-responsive">
            <table class="table table-sm align-middle mb-0">
              <thead>
                <tr>
                  <th>From key</th>
                  <th>To key</th>
                  <th>Sort</th>
                  <th>Condition</th>
                  <th>Value</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(edge, idx) in draftEdges" :key="'de' + idx">
                  <td>
                    <select v-model="edge.fromStableKey" class="form-select form-select-sm">
                      <option value="">—</option>
                      <option v-for="k in draftStableKeyOptions" :key="'f' + idx + k" :value="k">{{ k }}</option>
                    </select>
                  </td>
                  <td>
                    <select v-model="edge.toStableKey" class="form-select form-select-sm">
                      <option value="">—</option>
                      <option v-for="k in draftStableKeyOptions" :key="'t' + idx + k" :value="k">{{ k }}</option>
                    </select>
                  </td>
                  <td style="width: 4.5rem">
                    <input v-model.number="edge.sortOrder" type="number" class="form-control form-control-sm" />
                  </td>
                  <td>
                    <select v-model="edge.conditionKind" class="form-select form-select-sm">
                      <option v-for="c in conditionKinds" :key="c" :value="c">{{ c }}</option>
                    </select>
                  </td>
                  <td>
                    <input
                      v-model="edge.conditionValue"
                      type="text"
                      class="form-control form-control-sm font-monospace"
                      :disabled="edge.conditionKind !== 'OPTION'"
                      placeholder="choice id"
                    />
                  </td>
                  <td class="text-end">
                    <button type="button" class="btn btn-outline-danger btn-sm py-0" @click="draftEdges.splice(idx, 1)">×</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <p v-if="!draftEdges.length" class="small text-muted mb-0 mt-2">No transitions — add at least one from the start step.</p>
        </div>
      </div>
    </div>

    <div v-else class="alert alert-secondary small mb-0">
      No graph to display. If this persists after Reload, open DevTools → Network and check
      <code>GET /api/demo/branching-workflows/graph</code> (expect 200 and a JSON body with
      <code>nodes</code>).
    </div>
  </div>
</template>

<script setup>
import { computed, ref, useId } from 'vue'
import api from '../../services/api'
import { formatLoadError } from '../../utils/loadErrorFormat'

const arrowMarkerId = useId().replace(/:/g, '_')

const NODE_W = 176
const NODE_H = 76
const PAD = 24

const questionTypes = ['YES_NO', 'TEXT', 'CHOICE', 'END']
const conditionKinds = ['ALWAYS', 'YES', 'NO', 'OPTION']

const loading = ref(true)
const loadError = ref('')
const graph = ref(null)
const runStarted = ref(false)
const runFinished = ref(false)
const currentNodeId = ref(null)
const endTitle = ref('')
const endBody = ref('')
const pathLabels = ref([])
const textAnswer = ref('')
const resolving = ref(false)
const resolveError = ref('')
const startRunError = ref('')

const draftStartStableKey = ref('')
/** @type {import('vue').Ref<Array<{ stableKey: string, title: string, body: string, questionType: string, choicesJson: string, posX: number, posY: number }>>} */
const draftNodes = ref([])
/** @type {import('vue').Ref<Array<{ fromStableKey: string, toStableKey: string, sortOrder: number, conditionKind: string, conditionValue: string }>>} */
const draftEdges = ref([])

const saving = ref(false)
const saveError = ref('')
const saveSuccess = ref('')

const hasGraph = computed(() => {
  const g = graph.value
  if (!g || !Array.isArray(g.nodes)) return false
  const vid = g.versionId
  return vid != null && Number.isFinite(Number(vid))
})
const flowNodes = computed(() => graph.value?.nodes || [])

const nodeById = computed(() => {
  const m = new Map()
  flowNodes.value.forEach((n) => m.set(n.id, n))
  return m
})

const currentRunNode = computed(() => {
  if (currentNodeId.value == null) return null
  return nodeById.value.get(currentNodeId.value)
})

const previewFlowNodes = computed(() => previewGraph.value.nodes || [])
const previewFlowEdges = computed(() => previewGraph.value.edges || [])
const previewNodeById = computed(() => {
  const m = new Map()
  previewFlowNodes.value.forEach((n) => m.set(n.id, n))
  return m
})
const previewHighlightId = computed(() => {
  if (!runStarted.value || runFinished.value || !graph.value) return null
  const sid = graph.value.startNodeId
  const startKey = flowNodes.value.find((n) => n.id === sid)?.stableKey
  if (!startKey) return null
  const draftId = previewFlowNodes.value.find((n) => n.stableKey === startKey)?.id
  const curKey = flowNodes.value.find((n) => n.id === currentNodeId.value)?.stableKey
  if (!curKey) return draftId
  return previewFlowNodes.value.find((n) => n.stableKey === curKey)?.id ?? draftId
})

const canvasW = computed(() => {
  if (!previewFlowNodes.value.length) return 400
  let max = 0
  previewFlowNodes.value.forEach((n) => {
    max = Math.max(max, (n.posX || 0) + NODE_W)
  })
  return max + PAD * 2
})

const canvasH = computed(() => {
  if (!previewFlowNodes.value.length) return 240
  let max = 0
  previewFlowNodes.value.forEach((n) => {
    max = Math.max(max, (n.posY || 0) + NODE_H)
  })
  return max + PAD * 2
})

function nodeStyle(n) {
  return {
    left: `${PAD + (n.posX || 0)}px`,
    top: `${PAD + (n.posY || 0)}px`,
    width: `${NODE_W}px`,
    minHeight: `${NODE_H}px`
  }
}

function previewNodeCenter(id) {
  const n = previewNodeById.value.get(id)
  if (!n) return { x: 0, y: 0 }
  return { x: PAD + (n.posX || 0) + NODE_W / 2, y: PAD + (n.posY || 0) + NODE_H / 2 }
}

function previewEdgeLabel(e) {
  const f = previewNodeById.value.get(e.fromNodeId)?.stableKey || e.fromNodeId
  const t = previewNodeById.value.get(e.toNodeId)?.stableKey || e.toNodeId
  let c = e.conditionKind
  if (e.conditionValue) c += ` (${e.conditionValue})`
  return `${f} → ${t} · ${c}`
}

const draftStableKeyOptions = computed(() =>
  draftNodes.value.map((n) => n.stableKey?.trim()).filter(Boolean)
)

const previewGraph = computed(() => {
  const nodes = draftNodes.value
    .map((n) => ({
      stableKey: String(n.stableKey ?? '').trim(),
      title: n.title ?? '',
      body: n.body ?? '',
      questionType: String(n.questionType ?? 'TEXT').trim().toUpperCase(),
      choices: parseChoicesLocal(n.choicesJson),
      posX: Number.isFinite(Number(n.posX)) ? Number(n.posX) : 0,
      posY: Number.isFinite(Number(n.posY)) ? Number(n.posY) : 0
    }))
    .filter((n) => n.stableKey)
    .map((n, idx) => ({
      id: idx + 1,
      ...n
    }))
  const keyToId = new Map(nodes.map((n) => [n.stableKey, n.id]))
  const edges = []
  let eid = 1
  for (const e of draftEdges.value) {
    const fk = String(e.fromStableKey ?? '').trim()
    const tk = String(e.toStableKey ?? '').trim()
    const fid = keyToId.get(fk)
    const tid = keyToId.get(tk)
    if (!fid || !tid) continue
    edges.push({
      id: eid++,
      fromNodeId: fid,
      toNodeId: tid,
      sortOrder: Number.isFinite(Number(e.sortOrder)) ? Number(e.sortOrder) : 0,
      conditionKind: String(e.conditionKind ?? 'ALWAYS').trim().toUpperCase(),
      conditionValue: e.conditionValue != null && String(e.conditionValue).trim() !== '' ? String(e.conditionValue).trim() : null
    })
  }
  const sid = keyToId.get(String(draftStartStableKey.value ?? '').trim())
  return {
    versionId: graph.value?.versionId ?? 0,
    startNodeId: sid ?? null,
    nodes,
    edges
  }
})

function parseChoicesLocal(json) {
  if (json == null || String(json).trim() === '') return []
  try {
    const arr = JSON.parse(String(json))
    if (!Array.isArray(arr)) return []
    return arr
      .map((c) => ({
        id: String(c?.id != null ? c.id : '').trim(),
        label: String(c?.label != null ? c.label : c?.id != null ? c.id : '').trim()
      }))
      .map((c) => ({ ...c, label: c.label || c.id }))
      .filter((c) => c.id)
  } catch {
    return []
  }
}

function toNum(v, fallback = NaN) {
  if (v == null || v === '') return fallback
  const n = Number(v)
  return Number.isFinite(n) ? n : fallback
}

function normalizeBranchingGraph(data) {
  const rawNodes = Array.isArray(data.nodes) ? data.nodes : []
  const nodes = rawNodes.map((n) => {
    const choices = Array.isArray(n.choices)
      ? n.choices
          .map((c) => ({
            id: String(c.id != null ? c.id : '').trim(),
            label: String(c.label != null ? c.label : c.id != null ? c.id : '').trim()
          }))
          .map((c) => ({ ...c, label: c.label || c.id }))
          .filter((c) => c.id)
      : []
    return {
      ...n,
      id: toNum(n.id),
      posX: Number.isFinite(toNum(n.posX)) ? toNum(n.posX) : 0,
      posY: Number.isFinite(toNum(n.posY)) ? toNum(n.posY) : 0,
      questionType: String(n.questionType ?? '').trim().toUpperCase(),
      choices
    }
  })
  const edges = (Array.isArray(data.edges) ? data.edges : []).map((e) => ({
    ...e,
    id: toNum(e.id),
    fromNodeId: toNum(e.fromNodeId),
    toNodeId: toNum(e.toNodeId),
    sortOrder: Number.isFinite(toNum(e.sortOrder)) ? toNum(e.sortOrder) : 0,
    conditionKind: String(e.conditionKind ?? '').trim().toUpperCase(),
    conditionValue: e.conditionValue != null && e.conditionValue !== '' ? String(e.conditionValue).trim() : null
  }))
  const finiteIds = nodes.map((n) => n.id).filter((id) => Number.isFinite(id))
  let startNodeId = toNum(data.startNodeId)
  if (!Number.isFinite(startNodeId) && finiteIds.length) {
    const preferred = nodes.find((n) => n.stableKey === 'card_data')
    startNodeId = Number.isFinite(preferred?.id) ? preferred.id : Math.min(...finiteIds)
  }
  return {
    ...data,
    versionId: toNum(data.versionId),
    workflowId: data.workflowId != null ? toNum(data.workflowId) : data.workflowId,
    startNodeId: Number.isFinite(startNodeId) ? startNodeId : null,
    nodes,
    edges
  }
}

function choicesToJson(choices) {
  if (!choices?.length) return ''
  return JSON.stringify(choices.map((c) => ({ id: c.id, label: c.label })))
}

function initDraftFromGraph() {
  const g = graph.value
  if (!g?.nodes?.length) return
  const idToKey = new Map(g.nodes.map((n) => [n.id, n.stableKey]))
  draftStartStableKey.value = g.nodes.find((n) => n.id === g.startNodeId)?.stableKey || g.nodes[0].stableKey
  draftNodes.value = g.nodes.map((n) => ({
    stableKey: n.stableKey,
    title: n.title || '',
    body: n.body || '',
    questionType: n.questionType || 'TEXT',
    choicesJson: choicesToJson(n.choices),
    posX: n.posX ?? 0,
    posY: n.posY ?? 0
  }))
  draftEdges.value = g.edges.map((e) => ({
    fromStableKey: idToKey.get(e.fromNodeId) || '',
    toStableKey: idToKey.get(e.toNodeId) || '',
    sortOrder: e.sortOrder ?? 0,
    conditionKind: e.conditionKind || 'ALWAYS',
    conditionValue: e.conditionValue != null ? String(e.conditionValue) : ''
  }))
}

function resetDraft() {
  saveError.value = ''
  saveSuccess.value = ''
  initDraftFromGraph()
}

function addDraftNode() {
  draftNodes.value.push({
    stableKey: `step_${Date.now()}`,
    title: 'New step',
    body: '',
    questionType: 'TEXT',
    choicesJson: '',
    posX: 80,
    posY: 80
  })
}

function addDraftEdge() {
  const keys = draftStableKeyOptions.value
  draftEdges.value.push({
    fromStableKey: keys[0] || '',
    toStableKey: keys[1] || keys[0] || '',
    sortOrder: draftEdges.value.length,
    conditionKind: 'ALWAYS',
    conditionValue: ''
  })
}

function removeDraftNode(idx) {
  const key = draftNodes.value[idx]?.stableKey?.trim()
  draftNodes.value.splice(idx, 1)
  if (key) {
    draftEdges.value = draftEdges.value.filter((e) => e.fromStableKey !== key && e.toStableKey !== key)
  }
  if (draftStartStableKey.value === key) {
    draftStartStableKey.value = draftStableKeyOptions.value[0] || ''
  }
}

function validateDraft() {
  const nodes = draftNodes.value.map((n) => ({
    stableKey: String(n.stableKey ?? '').trim(),
    title: n.title ?? '',
    body: n.body ?? '',
    questionType: String(n.questionType ?? 'TEXT').trim().toUpperCase(),
    choicesJson: String(n.choicesJson ?? ''),
    posX: Number(n.posX) || 0,
    posY: Number(n.posY) || 0
  }))
  if (!nodes.length) return 'Add at least one step.'
  const keys = nodes.map((n) => n.stableKey).filter(Boolean)
  if (keys.length !== nodes.length) return 'Each step needs a non-empty stable key.'
  if (new Set(keys).size !== keys.length) return 'Stable keys must be unique.'
  const start = String(draftStartStableKey.value ?? '').trim()
  if (!start || !keys.includes(start)) return 'Pick a start step that matches one of the stable keys.'
  for (const n of nodes) {
    if (n.questionType === 'CHOICE') {
      if (!n.choicesJson.trim()) return `Step "${n.stableKey}" is CHOICE but choices JSON is empty.`
      try {
        const arr = JSON.parse(n.choicesJson)
        if (!Array.isArray(arr) || !arr.length) return `Step "${n.stableKey}": choices JSON must be a non-empty array.`
      } catch {
        return `Step "${n.stableKey}": invalid choices JSON.`
      }
    }
  }
  for (let i = 0; i < draftEdges.value.length; i++) {
    const e = draftEdges.value[i]
    const f = String(e.fromStableKey ?? '').trim()
    const t = String(e.toStableKey ?? '').trim()
    if (!f || !t) return `Transition ${i + 1}: choose both from and to keys.`
    if (!keys.includes(f) || !keys.includes(t)) return `Transition ${i + 1}: unknown stable key.`
    if (e.conditionKind === 'OPTION' && !String(e.conditionValue ?? '').trim()) {
      return `Transition ${i + 1}: OPTION needs a value (choice id).`
    }
  }
  return null
}

function buildSavePayload() {
  const nodes = draftNodes.value.map((n) => ({
    stableKey: String(n.stableKey).trim(),
    title: n.title ?? '',
    body: String(n.body ?? '').trim() === '' ? null : String(n.body),
    questionType: String(n.questionType ?? 'TEXT').trim(),
    choicesJson:
      String(n.questionType ?? '').trim().toUpperCase() === 'CHOICE' && String(n.choicesJson ?? '').trim()
        ? String(n.choicesJson).trim()
        : null,
    posX: Number(n.posX) || 0,
    posY: Number(n.posY) || 0
  }))
  const keySet = new Set(nodes.map((n) => n.stableKey))
  const edges = draftEdges.value.map((e) => ({
    fromStableKey: String(e.fromStableKey).trim(),
    toStableKey: String(e.toStableKey).trim(),
    sortOrder: Number(e.sortOrder) || 0,
    conditionKind: String(e.conditionKind ?? 'ALWAYS').trim(),
    conditionValue:
      String(e.conditionKind ?? '').trim().toUpperCase() === 'OPTION' && String(e.conditionValue ?? '').trim()
        ? String(e.conditionValue).trim()
        : null
  })).filter((e) => keySet.has(e.fromStableKey) && keySet.has(e.toStableKey))
  return {
    versionId: graph.value.versionId,
    startStableKey: String(draftStartStableKey.value).trim(),
    nodes,
    edges
  }
}

async function saveWorkflow() {
  saveError.value = ''
  saveSuccess.value = ''
  const err = validateDraft()
  if (err) {
    saveError.value = err
    return
  }
  saving.value = true
  try {
    const { data } = await api.put('/api/demo/branching-workflows/graph', buildSavePayload())
    graph.value = normalizeBranchingGraph(data)
    if (!Number.isFinite(graph.value.versionId) || graph.value.startNodeId == null || !Number.isFinite(graph.value.startNodeId)) {
      saveError.value = 'Save succeeded but the returned graph looks invalid. Try Reload.'
    } else {
      initDraftFromGraph()
      resetRun()
      saveSuccess.value = 'Saved. Try run now uses this version.'
    }
  } catch (e) {
    saveError.value = formatSaveError(e)
  } finally {
    saving.value = false
  }
}

function formatResolveError(err) {
  const d = err.response?.data
  if (d && typeof d === 'object' && d.message != null) return String(d.message)
  if (typeof d === 'string' && d.trim()) return d.trim()
  return err.message || 'Failed.'
}

function formatSaveError(err) {
  if (err.response?.status === 403) {
    return 'Forbidden (403). You need audit management permission to save; sign out and back in if your role was updated.'
  }
  return formatResolveError(err)
}

async function loadGraph() {
  loading.value = true
  loadError.value = ''
  startRunError.value = ''
  saveError.value = ''
  saveSuccess.value = ''
  try {
    const { data } = await api.get('/api/demo/branching-workflows/graph')
    if (data?.versionId == null || !Array.isArray(data.nodes)) {
      loadError.value = 'Invalid graph response from the API (missing versionId or nodes array).'
      graph.value = null
      return
    }
    if (data.nodes.length === 0) {
      loadError.value =
        'Demo workflow has no steps in the database. Apply Flyway migration V36 and restart the backend, then reload.'
      graph.value = null
      return
    }
    graph.value = normalizeBranchingGraph({ ...data, nodes: data.nodes || [], edges: data.edges || [] })
    if (!Number.isFinite(graph.value.versionId)) {
      loadError.value = 'Invalid workflow version id in API response.'
      graph.value = null
    } else if (graph.value.startNodeId == null || !Number.isFinite(graph.value.startNodeId)) {
      loadError.value =
        'Workflow has no valid start step (startNodeId). Check the demo version in the database or contact an administrator.'
      graph.value = null
    } else {
      initDraftFromGraph()
    }
  } catch (e) {
    loadError.value = formatLoadError(e)
    graph.value = null
  } finally {
    loading.value = false
  }
}

function startRun() {
  startRunError.value = ''
  if (!graph.value) return
  const sid = graph.value.startNodeId
  if (sid == null || !Number.isFinite(sid)) {
    startRunError.value = 'No start step is configured for this workflow.'
    return
  }
  runStarted.value = true
  runFinished.value = false
  resolveError.value = ''
  textAnswer.value = ''
  currentNodeId.value = sid
  pathLabels.value = []
  const s = nodeById.value.get(sid)
  if (s) pathLabels.value.push(s.title)
  if (s?.questionType === 'END') finishAtEnd(s)
}

function finishAtEnd(n) {
  runFinished.value = true
  endTitle.value = n.title || 'Done'
  endBody.value = n.body || ''
}

function resetRun() {
  runStarted.value = false
  runFinished.value = false
  currentNodeId.value = null
  pathLabels.value = []
  resolveError.value = ''
  startRunError.value = ''
  textAnswer.value = ''
}

async function submitAnswer(v) {
  await doResolve(v)
}

async function submitText() {
  await doResolve(textAnswer.value || ' ')
}

async function doResolve(value) {
  if (!graph.value || currentNodeId.value == null) return
  resolving.value = true
  resolveError.value = ''
  try {
    const { data } = await api.post('/api/demo/branching-workflows/resolve', {
      versionId: graph.value.versionId,
      fromNodeId: currentNodeId.value,
      value
    })
    const rawNext = data.nextNode
    const next = rawNext
      ? {
          ...rawNext,
          id: toNum(rawNext.id),
          questionType: String(rawNext.questionType ?? '').trim().toUpperCase()
        }
      : null
    if (data.finished || next?.questionType === 'END') {
      if (next) {
        finishAtEnd(next)
        pathLabels.value.push(next.title)
      } else {
        runFinished.value = true
        endTitle.value = 'Complete'
        endBody.value = ''
      }
      return
    }
    if (!next) {
      runFinished.value = true
      endTitle.value = 'Complete'
      return
    }
    currentNodeId.value = next.id
    pathLabels.value.push(next.title)
    textAnswer.value = ''
  } catch (e) {
    resolveError.value = formatResolveError(e)
  } finally {
    resolving.value = false
  }
}

loadGraph()
</script>
