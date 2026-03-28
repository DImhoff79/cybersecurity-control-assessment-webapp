<template>
  <div class="mapping-studio">
    <div class="alert alert-info d-flex gap-2 align-items-start mb-4">
      <span class="flex-shrink-0" aria-hidden="true">ℹ️</span>
      <div>
        <strong>Mapping studio</strong>
        <p class="mb-0 small">
          Drag from the grip to link a question to a control. Add questions to the library first; remove respects the rule that each control keeps at least one question.
        </p>
      </div>
    </div>

    <div class="d-flex flex-wrap gap-2 align-items-center justify-content-between mb-3">
      <h1 class="h4 mb-0">Question ↔ control mapping</h1>
      <button type="button" class="btn btn-outline-secondary btn-sm" :disabled="loading" @click="load">Refresh</button>
    </div>

    <div class="row g-3 mb-3">
      <div class="col-md-6">
        <label class="form-label small text-muted mb-1" for="studio-q-filter">Filter questions</label>
        <input id="studio-q-filter" v-model="questionFilter" type="search" class="form-control form-control-sm" placeholder="Search…" autocomplete="off" />
      </div>
      <div class="col-md-6">
        <label class="form-label small text-muted mb-1" for="studio-c-filter">Filter controls</label>
        <input id="studio-c-filter" v-model="controlFilter" type="search" class="form-control form-control-sm" placeholder="Search…" autocomplete="off" />
      </div>
      <div class="col-12">
        <div class="d-flex flex-wrap gap-3 align-items-center border-top border-light pt-3">
          <div class="form-check form-check-inline mb-0">
            <input id="studio-only-unmapped" v-model="showOnlyUnmappedQuestions" class="form-check-input" type="checkbox" />
            <label class="form-check-label small" for="studio-only-unmapped">Unmapped questions only</label>
          </div>
          <div class="form-check form-check-inline mb-0">
            <input id="studio-only-empty" v-model="showOnlyControlsWithNoQuestions" class="form-check-input" type="checkbox" />
            <label class="form-check-label small" for="studio-only-empty">Controls with no questions</label>
          </div>
        </div>
      </div>
    </div>

    <div v-if="loading" class="text-muted py-5 text-center">Loading…</div>

    <div v-else class="row g-3 mapping-studio-panels">
      <div class="col-lg-5">
        <div class="card shadow-sm h-100">
          <div class="card-header py-2 d-flex justify-content-between align-items-center">
            <span class="fw-semibold">Questions</span>
            <span class="badge text-bg-secondary">{{ filteredQuestions.length }}</span>
          </div>
          <div class="card-body p-2 mapping-studio-scroll">
            <p class="small text-muted px-2 mb-2">
              <button type="button" class="btn btn-link btn-sm p-0 align-baseline" @click="showAddQuestionModal = true">Add question</button>
              — then drag onto a control.
            </p>
            <div
              v-for="q in filteredQuestions"
              :key="q.id"
              class="mapping-studio-question card mb-2"
              :class="{ 'opacity-50': draggingQuestionId === q.id }"
            >
              <div class="card-body py-2 px-3 studio-question-row-body" @click="onQuestionRowActivate($event, q)">
                <div class="d-flex gap-2 align-items-start">
                  <div
                    class="mapping-studio-drag-handle flex-shrink-0"
                    title="Drag onto a control"
                    draggable="true"
                    @click.stop
                    @dragstart="onQuestionDragStart($event, q)"
                    @dragend="onQuestionDragEnd"
                  >
                    <span class="mapping-studio-drag-grip" aria-hidden="true" />
                  </div>
                  <button type="button" class="btn btn-link text-body text-decoration-none p-0 text-start flex-grow-1 min-w-0 studio-q-detail-trigger rounded">
                    <div class="small text-muted mb-1">
                      #{{ q.id }}
                      <span v-if="q.displayOrder != null"> · order {{ q.displayOrder }}</span>
                      · {{ q.mappings?.length || 0 }} control(s)
                    </div>
                    <div class="small fw-medium">{{ q.questionText }}</div>
                    <div v-if="sortedMappings(q).length" class="d-flex flex-wrap gap-1 mt-2">
                      <span
                        v-for="(m, mIdx) in sortedMappings(q)"
                        :key="`${q.id}-map-${mIdx}-${m.controlDbId ?? m.controlId ?? 'x'}`"
                        class="badge rounded-pill text-bg-light border text-dark fw-normal small"
                      >
                        {{ m.controlId }}
                      </span>
                    </div>
                    <div v-else class="small text-warning-emphasis mt-2">Not linked — drag onto a control</div>
                  </button>
                  <div class="flex-shrink-0 align-self-start studio-question-remove-slot" @click.stop>
                    <button
                      type="button"
                      class="btn btn-outline-danger btn-sm"
                      :disabled="!canRemoveQuestionGlobally(q) || removingQuestionId === q.id"
                      @click="removeQuestionGlobally(q)"
                    >
                      {{ removingQuestionId === q.id ? '…' : 'Remove' }}
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <p v-if="!filteredQuestions.length" class="text-muted small px-2 mb-0">No questions match.</p>
          </div>
        </div>
      </div>

      <div class="col-lg-7">
        <div class="card shadow-sm h-100">
          <div class="card-header py-2 d-flex justify-content-between">
            <span class="fw-semibold">Controls</span>
            <span class="badge text-bg-secondary">{{ filteredControls.length }}</span>
          </div>
          <div class="card-body p-2 mapping-studio-scroll">
            <div
              v-for="c in filteredControls"
              :key="c.id"
              class="mapping-studio-control card mb-2"
              :class="{ 'mapping-studio-control--active': dropHighlightId === c.id }"
              @dragover.prevent="onControlDragOver(c.id)"
              @dragleave="onControlDragLeave(c.id, $event)"
              @drop.prevent="onDropOnControl($event, c)"
            >
              <div class="card-body py-2 px-3">
                <div class="d-flex justify-content-between gap-2 mb-2">
                  <div class="min-w-0">
                    <span class="fw-semibold">{{ c.controlId }}</span>
                    <span class="text-muted"> — {{ c.name }}</span>
                    <p v-if="c.description" class="small text-body-secondary mb-0 mt-2">{{ c.description }}</p>
                  </div>
                  <span class="badge text-bg-light border flex-shrink-0">{{ (c.questions || []).length }} linked</span>
                </div>
                <ul v-if="(c.questions || []).length" class="list-unstyled small mb-0">
                  <li
                    v-for="q in c.questions"
                    :key="`${c.id}-${q.id}`"
                    class="d-flex justify-content-between gap-2 py-1 border-top border-light"
                  >
                    <span class="text-break">{{ q.questionText }}</span>
                    <button
                      type="button"
                      class="btn btn-link btn-sm text-danger p-0 flex-shrink-0 text-nowrap"
                      :disabled="unlinkingKey === unlinkingKeyFor(c.id, q.id)"
                      @click="removeMapping(c, q)"
                    >
                      Remove
                    </button>
                  </li>
                </ul>
                <p v-else class="small text-muted fst-italic mb-0">Drop a question here.</p>
              </div>
            </div>
            <p v-if="!filteredControls.length" class="text-muted small px-2 mb-0">No controls match.</p>
          </div>
        </div>
      </div>
    </div>

    <BsModal v-model="showQuestionDetailModal" :title="detailModalTitle" size="xl" scrollable>
      <div v-if="detailQuestion">
        <section class="mb-4">
          <h3 class="h6 text-muted text-uppercase small mb-2">Question</h3>
          <dl class="row small mb-0">
            <dt class="col-sm-3 text-muted">Ask owners</dt>
            <dd class="col-sm-9">{{ detailQuestion.askOwner !== false ? 'Yes' : 'No' }}</dd>
            <dt class="col-sm-3 text-muted">Text</dt>
            <dd class="col-sm-9">{{ detailQuestion.questionText }}</dd>
            <dt v-if="detailQuestion.helpText" class="col-sm-3 text-muted">Help</dt>
            <dd v-if="detailQuestion.helpText" class="col-sm-9">{{ detailQuestion.helpText }}</dd>
          </dl>
        </section>
        <section>
          <h3 class="h6 text-muted text-uppercase small mb-2">Mapped controls</h3>
          <p v-if="!sortedMappings(detailQuestion).length" class="small text-warning-emphasis">Not linked to any control.</p>
          <div
            v-for="(em, emIdx) in enrichedMappingsForDetail(detailQuestion)"
            :key="detailMappingRowKey(detailQuestion.id, em, emIdx)"
            class="card border mb-3"
          >
            <div class="card-body py-3">
              <div class="d-flex justify-content-between gap-2 mb-2">
                <div>
                  <span class="fw-semibold">{{ em.controlId }}</span>
                  <span class="text-muted"> — {{ em.name }}</span>
                  <span v-if="em.framework" class="badge text-bg-secondary ms-1">{{ em.framework }}</span>
                </div>
                <div class="d-flex flex-column align-items-end gap-1 flex-shrink-0" style="min-width: 7rem">
                  <button
                    type="button"
                    class="btn btn-sm text-nowrap"
                    :class="unmapButtonClass(em)"
                    :disabled="unmapButtonDisabled(em)"
                    :title="unmapButtonTitle(em)"
                    @click.stop="removeMappingFromDetail(em)"
                  >
                    {{ unmapButtonLabel(em) }}
                  </button>
                  <span v-if="unmapBlockedReason(em)" class="small text-muted text-end">{{ unmapBlockedReason(em) }}</span>
                </div>
              </div>
              <p v-if="em.description" class="small text-body-secondary mb-0">{{ em.description }}</p>
            </div>
          </div>
        </section>
      </div>
      <template #footer>
        <button type="button" class="btn btn-primary btn-sm" @click="showQuestionDetailModal = false">Close</button>
      </template>
    </BsModal>

    <BsModal v-model="showAddQuestionModal" title="Add question" size="lg">
      <label class="form-label small" for="new-q-text">Question text</label>
      <textarea id="new-q-text" v-model="newQuestionText" class="form-control form-control-sm mb-2" rows="3" :disabled="addingQuestion" />
      <label class="form-label small" for="new-q-help">Help (optional)</label>
      <input id="new-q-help" v-model="newQuestionHelp" type="text" class="form-control form-control-sm mb-2" :disabled="addingQuestion" />
      <div class="form-check">
        <input id="new-ask-owner" v-model="newAskOwner" class="form-check-input" type="checkbox" :disabled="addingQuestion" />
        <label class="form-check-label small" for="new-ask-owner">Show to application owners</label>
      </div>
      <template #footer>
        <button type="button" class="btn btn-outline-secondary btn-sm" :disabled="addingQuestion" @click="showAddQuestionModal = false">Cancel</button>
        <button type="button" class="btn btn-primary btn-sm" :disabled="addingQuestion" @click="addQuestion">Save</button>
      </template>
    </BsModal>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import BsModal from '../../components/BsModal.vue'
import api from '../../services/api'
import { toastError, toastSuccess, toastWarning } from '../../services/toast'

const loading = ref(true)
const controls = ref([])
const questions = ref([])
const questionFilter = ref('')
const controlFilter = ref('')
const showOnlyUnmappedQuestions = ref(false)
const showOnlyControlsWithNoQuestions = ref(false)
const draggingQuestionId = ref(null)
const dropHighlightId = ref(null)
const unlinkingKey = ref(null)
const newQuestionText = ref('')
const newQuestionHelp = ref('')
const newAskOwner = ref(true)
const addingQuestion = ref(false)
const removingQuestionId = ref(null)
const showAddQuestionModal = ref(false)
const detailQuestion = ref(null)
const showQuestionDetailModal = ref(false)

const detailModalTitle = computed(() => {
  const q = detailQuestion.value
  if (!q) return 'Question'
  const t = (q.questionText || '').trim()
  return t.length <= 72 ? t || 'Question' : `${t.slice(0, 69)}…`
})

watch(showQuestionDetailModal, (open) => {
  if (!open) detailQuestion.value = null
})

function sameEntityId(a, b) {
  if (a == null || b == null) return false
  return Number(a) === Number(b)
}

function sortedMappings(q) {
  if (!q?.mappings?.length) return []
  return [...q.mappings].sort((a, b) => (a.controlId || '').localeCompare(b.controlId || '', undefined, { sensitivity: 'base' }))
}

function openQuestionDetail(q) {
  detailQuestion.value = q
  showQuestionDetailModal.value = true
}

function onQuestionRowActivate(evt, q) {
  if (evt.target.closest('.mapping-studio-drag-handle')) return
  if (evt.target.closest('.studio-question-remove-slot')) return
  openQuestionDetail(q)
}

function controlByDbId(controlDbId) {
  return controls.value.find((x) => sameEntityId(x.id, controlDbId)) || null
}

function unlinkingKeyFor(controlId, questionId) {
  const c = Number(controlId)
  const q = Number(questionId)
  if (!Number.isFinite(c) || !Number.isFinite(q)) return `${controlId}-${questionId}`
  return `${c}-${q}`
}

function enrichedMappingsForDetail(q) {
  return sortedMappings(q).map((m) => {
    const c = controls.value.find((x) => sameEntityId(x.id, m.controlDbId))
    const dbId = m.controlDbId != null ? m.controlDbId : c?.id
    return {
      ...m,
      controlDbId: dbId,
      name: m.name || c?.name || '—',
      controlId: m.controlId || c?.controlId,
      description: (m.description ?? c?.description ?? '').trim() || null,
      framework: m.framework ?? c?.framework ?? null
    }
  })
}

function detailMappingRowKey(qid, em, idx) {
  const p = em.controlDbId != null ? em.controlDbId : em.controlId != null ? em.controlId : idx
  return `dm-${qid}-${p}-${idx}`
}

function canUnmapQuestionFromControl(em) {
  const c = controlByDbId(em.controlDbId)
  if (!c) return true
  const qs = c.questions || []
  const qid = detailQuestion.value?.id
  const onControl = qid != null && qs.some((x) => sameEntityId(x.id, qid))
  if (!onControl) return true
  return qs.length > 1
}

function unmapButtonDisabled(em) {
  const q = detailQuestion.value
  if (!q) return true
  if (!canUnmapQuestionFromControl(em)) return true
  return unlinkingKey.value === unlinkingKeyFor(em.controlDbId, q.id)
}

function unmapButtonClass(em) {
  return canUnmapQuestionFromControl(em) ? 'btn-outline-danger' : 'btn-outline-secondary'
}

function unmapButtonTitle(em) {
  const q = detailQuestion.value
  if (!q) return ''
  if (unlinkingKey.value === unlinkingKeyFor(em.controlDbId, q.id)) return 'Removing…'
  if (canUnmapQuestionFromControl(em)) return 'Remove from this control only'
  return 'Add another question to this control first.'
}

function unmapButtonLabel(em) {
  const q = detailQuestion.value
  if (q && unlinkingKey.value === unlinkingKeyFor(em.controlDbId, q.id)) return 'Removing…'
  return 'Unmap'
}

function unmapBlockedReason(em) {
  if (canUnmapQuestionFromControl(em)) return ''
  return 'Only question on control.'
}

function syncDetailQuestionAfterLoad(questionId) {
  if (!showQuestionDetailModal.value) return
  if (!sameEntityId(detailQuestion.value?.id, questionId)) return
  const updated = questions.value.find((x) => sameEntityId(x.id, questionId))
  if (updated) detailQuestion.value = updated
  else showQuestionDetailModal.value = false
}

async function removeMappingFromDetail(em) {
  const q = detailQuestion.value
  if (!q || unmapButtonDisabled(em)) return
  const control = controlByDbId(em.controlDbId)
  const controlArg = control || { id: em.controlDbId, controlId: em.controlId || String(em.controlDbId) }
  await removeMapping(controlArg, q)
}

async function removeMapping(control, question) {
  if (!confirm(`Remove this question from control ${control.controlId}?`)) return
  const key = unlinkingKeyFor(control.id, question.id)
  const qid = question.id
  unlinkingKey.value = key
  try {
    await api.delete(`/api/controls/${control.id}/questions/${question.id}`)
    toastSuccess('Mapping removed.')
    await load()
    syncDetailQuestionAfterLoad(qid)
  } catch (e) {
    toastError(e.response?.data?.error || e.message || 'Failed.')
    await load()
    syncDetailQuestionAfterLoad(qid)
  } finally {
    unlinkingKey.value = null
  }
}

const filteredQuestions = computed(() => {
  let list = questions.value
  const term = questionFilter.value.trim().toLowerCase()
  if (term) {
    list = list.filter(
      (q) =>
        String(q.id).includes(term) ||
        (q.questionText || '').toLowerCase().includes(term) ||
        (q.helpText || '').toLowerCase().includes(term)
    )
  }
  if (showOnlyUnmappedQuestions.value) list = list.filter((q) => !(q.mappings && q.mappings.length))
  return list
})

const filteredControls = computed(() => {
  let list = controls.value
  const term = controlFilter.value.trim().toLowerCase()
  if (term) {
    list = list.filter(
      (c) =>
        (c.controlId || '').toLowerCase().includes(term) ||
        (c.name || '').toLowerCase().includes(term) ||
        (c.description || '').toLowerCase().includes(term) ||
        String(c.id).includes(term)
    )
  }
  if (showOnlyControlsWithNoQuestions.value) list = list.filter((c) => !(c.questions && c.questions.length))
  return list
})

function canRemoveQuestionGlobally(q) {
  const maps = q.mappings || []
  if (!maps.length) return true
  return maps.every((m) => {
    const c = controls.value.find((x) => sameEntityId(x.id, m.controlDbId))
    return c && (c.questions || []).length > 1
  })
}

async function removeQuestionGlobally(q) {
  if (!canRemoveQuestionGlobally(q)) return
  const maps = [...(q.mappings || [])]
  if (!maps.length) {
    if (!confirm('Delete this unmapped question?')) return
    removingQuestionId.value = q.id
    try {
      await api.delete(`/api/questions/${q.id}`)
      toastSuccess('Deleted.')
      await load()
    } catch (e) {
      toastError(e.response?.data?.message || e.message || 'Failed.')
      await load()
    } finally {
      removingQuestionId.value = null
    }
    return
  }
  if (!confirm(`Remove from all ${maps.length} control(s) and delete if unused?`)) return
  removingQuestionId.value = q.id
  try {
    for (const m of maps) {
      await api.delete(`/api/controls/${m.controlDbId}/questions/${q.id}`)
    }
    toastSuccess('Removed.')
    await load()
  } catch (e) {
    toastError(e.response?.data?.message || e.message || 'Failed.')
    await load()
  } finally {
    removingQuestionId.value = null
  }
}

async function addQuestion() {
  const text = newQuestionText.value.trim()
  if (!text) {
    toastWarning('Enter question text.')
    return
  }
  addingQuestion.value = true
  try {
    await api.post('/api/questions', {
      questionText: text,
      helpText: newQuestionHelp.value.trim() || null,
      askOwner: newAskOwner.value
    })
    toastSuccess('Question created.')
    newQuestionText.value = ''
    newQuestionHelp.value = ''
    showAddQuestionModal.value = false
    await load()
  } catch (e) {
    toastError(e.response?.data?.message || e.message || 'Failed.')
  } finally {
    addingQuestion.value = false
  }
}

function onQuestionDragStart(evt, q) {
  draggingQuestionId.value = q.id
  evt.dataTransfer.setData(
    'application/json',
    JSON.stringify({
      id: q.id,
      questionText: q.questionText,
      helpText: q.helpText || '',
      askOwner: q.askOwner !== false
    })
  )
  evt.dataTransfer.effectAllowed = 'copy'
}

function onQuestionDragEnd() {
  draggingQuestionId.value = null
  dropHighlightId.value = null
}

function onControlDragOver(controlId) {
  dropHighlightId.value = controlId
}

function onControlDragLeave(controlId, evt) {
  const el = evt.currentTarget
  if (!el.contains(evt.relatedTarget) && dropHighlightId.value === controlId) dropHighlightId.value = null
}

async function onDropOnControl(evt, control) {
  dropHighlightId.value = null
  draggingQuestionId.value = null
  let payload
  try {
    payload = JSON.parse(evt.dataTransfer.getData('application/json') || '{}')
  } catch {
    return
  }
  if (!payload.questionText && payload.id == null) return
  const already = (control.questions || []).some((q) => sameEntityId(q.id, payload.id))
  if (already) {
    toastWarning('Already linked.')
    return
  }
  try {
    if (payload.id != null) {
      await api.post(`/api/controls/${control.id}/questions`, { questionId: payload.id })
    } else {
      await api.post(`/api/controls/${control.id}/questions`, {
        questionText: payload.questionText,
        helpText: payload.helpText || null,
        askOwner: payload.askOwner !== false
      })
    }
    toastSuccess(`Linked to ${control.controlId}.`)
    await load()
    if (payload.id != null) syncDetailQuestionAfterLoad(payload.id)
  } catch (e) {
    toastError(e.response?.data?.error || e.message || 'Failed.')
  }
}

function questionsFromControlPayload(raw) {
  const byQuestion = new Map()
  raw.forEach((control) => {
    ;(control.questions || []).forEach((q) => {
      const mapping = {
        controlDbId: control.id,
        controlId: control.controlId,
        name: control.name,
        description: (control.description || '').trim() || null,
        framework: control.framework
      }
      if (!byQuestion.has(q.id)) {
        byQuestion.set(q.id, {
          id: q.id,
          questionText: q.questionText,
          helpText: q.helpText || '',
          askOwner: q.askOwner !== false,
          displayOrder: q.displayOrder,
          mappings: [mapping]
        })
      } else {
        byQuestion.get(q.id).mappings.push(mapping)
      }
    })
  })
  return Array.from(byQuestion.values()).sort((a, b) => (a.questionText || '').localeCompare(b.questionText || ''))
}

async function load() {
  loading.value = true
  try {
    const ctrlRes = await api.get('/api/controls?includeQuestions=true')
    const raw = ctrlRes.data || []
    controls.value = raw.map((c) => ({
      id: c.id,
      controlId: c.controlId,
      name: c.name,
      description: (c.description || '').trim(),
      framework: c.framework,
      questions: [...(c.questions || [])].sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0))
    }))
    try {
      const libRes = await api.get('/api/questions')
      questions.value = (libRes.data || [])
        .map((row) => ({
          id: row.id,
          questionText: row.questionText,
          helpText: row.helpText || '',
          askOwner: row.askOwner !== false,
          displayOrder: row.displayOrder,
          mappings: row.mappings || []
        }))
        .sort((a, b) => (a.questionText || '').localeCompare(b.questionText || ''))
    } catch {
      questions.value = questionsFromControlPayload(raw)
      toastWarning('Question library API unavailable; showing questions from controls only.')
    }
  } catch (e) {
    controls.value = []
    questions.value = []
    toastError(e.response?.data?.message || e.message || 'Load failed.')
  } finally {
    loading.value = false
  }
}

load()
</script>

<style scoped>
.mapping-studio-scroll {
  max-height: min(70vh, 42rem);
  overflow-y: auto;
}
.mapping-studio-drag-handle {
  cursor: grab;
  padding: 0.15rem 0.25rem;
}
.mapping-studio-drag-grip {
  display: block;
  width: 0.65rem;
  height: 1.25rem;
  background: repeating-linear-gradient(to bottom, #adb5bd 0, #adb5bd 2px, transparent 2px, transparent 5px);
}
.mapping-studio-control--active {
  border: 2px dashed #0d6efd;
  box-shadow: 0 0 0 0.15rem rgba(13, 110, 253, 0.2);
}
.studio-question-row-body {
  cursor: pointer;
}
.studio-q-detail-trigger:hover {
  background-color: rgba(var(--bs-primary-rgb), 0.06);
}
</style>
