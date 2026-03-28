<template>
  <div class="branching-demo">
    <div class="d-flex flex-wrap gap-2 justify-content-between mb-3">
      <h1 class="h4 mb-0">Branching workflow <span class="badge text-bg-secondary">Demo</span></h1>
      <button type="button" class="btn btn-outline-secondary btn-sm" :disabled="loading" @click="loadGraph">Reload</button>
    </div>
    <p class="text-muted small mb-3">
      Server-resolved conditional flow. Start the API on port 8080 and use <code>npm run dev</code> (or set <code>VITE_API_ORIGIN</code>).
    </p>

    <div v-if="loadError" class="alert alert-warning">
      <div class="fw-semibold mb-1">Could not load demo workflow</div>
      <div class="small mb-0">{{ loadError }}</div>
    </div>
    <div v-else-if="loading" class="text-muted py-4">Loading…</div>

    <div v-else-if="hasGraph" class="row g-4">
      <div class="col-xl-7">
        <div class="card shadow-sm">
          <div class="card-header py-2 small fw-semibold">Flow</div>
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
                  v-for="e in flowEdges"
                  :key="'e' + e.id"
                  :x1="nodeCenter(e.fromNodeId).x"
                  :y1="nodeCenter(e.fromNodeId).y"
                  :x2="nodeCenter(e.toNodeId).x"
                  :y2="nodeCenter(e.toNodeId).y"
                  stroke="currentColor"
                  stroke-width="1.2"
                  opacity="0.5"
                  :marker-end="`url(#${arrowMarkerId})`"
                />
              </svg>
              <div
                v-for="n in flowNodes"
                :key="'n' + n.id"
                class="card position-absolute shadow-sm small"
                :class="{ 'border-primary': highlightId === n.id }"
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
              <li v-for="e in flowEdges" :key="'l' + e.id">{{ edgeLabel(e) }}</li>
            </ul>
          </div>
        </div>
      </div>
      <div class="col-xl-5">
        <div class="card shadow-sm">
          <div class="card-header py-2 small fw-semibold">Try run</div>
          <div class="card-body">
            <div v-if="!runStarted">
              <button type="button" class="btn btn-primary btn-sm" @click="startRun">Start</button>
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
                  <button
                    v-for="c in currentRunNode.choices"
                    :key="c.id"
                    type="button"
                    class="btn btn-outline-primary btn-sm text-start"
                    @click="submitAnswer(c.id)"
                  >
                    {{ c.label }}
                  </button>
                </div>
                <div v-else-if="currentRunNode.questionType === 'TEXT'">
                  <textarea v-model="textAnswer" class="form-control form-control-sm mb-2" rows="3" />
                  <button type="button" class="btn btn-primary btn-sm" :disabled="resolving" @click="submitText">Continue</button>
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

const highlightId = computed(() => (runStarted.value && !runFinished.value ? currentNodeId.value : null))
const hasGraph = computed(() => graph.value?.versionId != null && Array.isArray(graph.value.nodes))
const flowNodes = computed(() => graph.value?.nodes || [])
const flowEdges = computed(() => graph.value?.edges || [])

const nodeById = computed(() => {
  const m = new Map()
  flowNodes.value.forEach((n) => m.set(n.id, n))
  return m
})

const currentRunNode = computed(() => {
  if (currentNodeId.value == null) return null
  return nodeById.value.get(currentNodeId.value)
})

const canvasW = computed(() => {
  if (!flowNodes.value.length) return 400
  let max = 0
  flowNodes.value.forEach((n) => {
    max = Math.max(max, (n.posX || 0) + NODE_W)
  })
  return max + PAD * 2
})

const canvasH = computed(() => {
  if (!flowNodes.value.length) return 240
  let max = 0
  flowNodes.value.forEach((n) => {
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

function nodeCenter(id) {
  const n = nodeById.value.get(id)
  if (!n) return { x: 0, y: 0 }
  return { x: PAD + (n.posX || 0) + NODE_W / 2, y: PAD + (n.posY || 0) + NODE_H / 2 }
}

function edgeLabel(e) {
  const f = nodeById.value.get(e.fromNodeId)?.stableKey || e.fromNodeId
  const t = nodeById.value.get(e.toNodeId)?.stableKey || e.toNodeId
  let c = e.conditionKind
  if (e.conditionValue) c += ` (${e.conditionValue})`
  return `${f} → ${t} · ${c}`
}

async function loadGraph() {
  loading.value = true
  loadError.value = ''
  try {
    const { data } = await api.get('/api/demo/branching-workflows/graph')
    if (!data?.versionId || !Array.isArray(data.nodes)) {
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
    graph.value = { ...data, nodes: data.nodes || [], edges: data.edges || [] }
  } catch (e) {
    loadError.value = formatLoadError(e)
    graph.value = null
  } finally {
    loading.value = false
  }
}

function startRun() {
  if (!graph.value?.startNodeId) return
  runStarted.value = true
  runFinished.value = false
  resolveError.value = ''
  textAnswer.value = ''
  currentNodeId.value = graph.value.startNodeId
  pathLabels.value = []
  const s = nodeById.value.get(graph.value.startNodeId)
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
    const next = data.nextNode
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
    resolveError.value = e.response?.data?.message || e.message || 'Failed.'
  } finally {
    resolving.value = false
  }
}

loadGraph()
</script>
