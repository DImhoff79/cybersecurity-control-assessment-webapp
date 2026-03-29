<script setup>
import { computed, inject, nextTick, ref, watch } from 'vue'
import { Handle, Position } from '@vue-flow/core'

const props = defineProps({
  data: {
    type: Object,
    required: true
  }
})

const ctx = inject('branchingMapContext', null)

const editOpen = ref(false)
/** Set when opening edit; used by cancel to restore draft + outgoing edges. */
const editSnapshot = ref(null)
const titleInputRef = ref(null)

const draftNode = computed(() => ctx?.getDraftNode?.(props.data.stableKey) ?? null)
const qt = computed(() => String(draftNode.value?.questionType ?? 'TEXT').trim().toUpperCase())

watch(
  () => draftNode.value,
  (n) => {
    if (editOpen.value && !n) {
      editOpen.value = false
      editSnapshot.value = null
      ctx?.setEditingCardStableKey?.('')
    }
  }
)

watch(
  () => ctx?.editingCardStableKey?.value,
  (k) => {
    if (editOpen.value && k && k !== props.data.stableKey) {
      editOpen.value = false
      editSnapshot.value = null
    }
  }
)

function openEdit() {
  editSnapshot.value = ctx?.captureEditSnapshot?.(props.data.stableKey) ?? null
  ctx?.setEditingCardStableKey?.(props.data.stableKey)
  editOpen.value = true
  nextTick(() => titleInputRef.value?.focus())
}

function saveAndClose() {
  editSnapshot.value = null
  editOpen.value = false
  ctx?.setEditingCardStableKey?.('')
}

function cancelEdit() {
  ctx?.restoreEditSnapshot?.(props.data.stableKey, editSnapshot.value)
  editSnapshot.value = null
  editOpen.value = false
  ctx?.setEditingCardStableKey?.('')
}

/** While editing, stop clicks from bubbling to Vue Flow (pane / selection) and avoid accidental close behavior. */
function onCardRootClick(e) {
  if (editOpen.value) {
    e.stopPropagation()
  }
}
</script>

<template>
  <div
    class="branching-flow-node rounded border bg-body p-2 shadow-sm small user-select-none"
    :class="{
      'branching-flow-node--start': data.isStart,
      'branching-flow-node--selected': data.isSelected,
      'branching-flow-node--run': data.isRun,
      'branching-flow-node--editing': editOpen,
      nodrag: editOpen
    }"
    :style="
      editOpen
        ? { minWidth: '280px', maxWidth: 'min(420px, 100%)' }
        : { minWidth: '148px', maxWidth: '240px' }
    "
    @click="onCardRootClick"
  >
    <Handle type="target" :position="Position.Left" class="branching-flow-node__handle" />
    <div class="d-flex align-items-start justify-content-between gap-1 mb-1">
      <span class="badge rounded-pill text-bg-secondary">Q{{ data.num }}</span>
      <div class="d-flex align-items-center gap-1 flex-shrink-0">
        <span v-if="data.isStart" class="badge rounded-pill text-bg-success" style="font-size: 0.6rem">Start</span>
        <button
          v-if="!editOpen"
          type="button"
          class="btn btn-link btn-sm p-0 nodrag branching-flow-node__icon-btn"
          title="Edit on card"
          aria-label="Edit on card"
          @click.stop="openEdit"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" fill="currentColor" viewBox="0 0 16 16" aria-hidden="true">
            <path
              d="M12.146.146a.5.5 0 0 1 .708 0l3 3a.5.5 0 0 1 0 .708l-10 10a.5.5 0 0 1-.168.11l-5 2a.5.5 0 0 1-.65-.65l2-5a.5.5 0 0 1 .11-.168l10-10zM11.207 2.5 13.5 4.793 14.793 3.5 12.5 1.207 11.207 2.5zm1.586 3L10.5 3.207 4 9.707V10h.5a.5.5 0 0 1 .5.5v.5h.5a.5.5 0 0 1 .5.5V12h.5a.5.5 0 0 1 .5.5v.5h.293l6.5-6.5zm-9.761 5.175-.106.106-1.528 3.821 3.821-1.528.106-.106A.5.5 0 0 1 5 12.5V12h-.5a.5.5 0 0 1-.5-.5V11h-.5a.5.5 0 0 1-.468-.325z"
            />
          </svg>
        </button>
        <template v-else>
          <button
            type="button"
            class="btn btn-link btn-sm p-0 nodrag branching-flow-node__icon-btn branching-flow-node__icon-btn--cancel"
            title="Discard changes"
            aria-label="Discard changes"
            @click.stop="cancelEdit"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="currentColor" viewBox="0 0 16 16" aria-hidden="true">
              <path
                d="M4.646 4.646a.5.5 0 0 1 .708 0L8 7.293l2.646-2.647a.5.5 0 0 1 .708.708L8.707 8l2.647 2.646a.5.5 0 0 1-.708.708L8 8.707l-2.646 2.647a.5.5 0 0 1-.708-.708L7.293 8 4.646 5.354a.5.5 0 0 1 0-.708z"
              />
            </svg>
          </button>
          <button
            type="button"
            class="btn btn-link btn-sm p-0 nodrag branching-flow-node__icon-btn branching-flow-node__icon-btn--done"
            title="Save and close"
            aria-label="Save and close"
            @click.stop="saveAndClose"
          >
            <svg
              class="branching-flow-node__check-svg"
              xmlns="http://www.w3.org/2000/svg"
              width="24"
              height="24"
              viewBox="0 0 16 16"
              aria-hidden="true"
            >
              <path
                fill="none"
                stroke="currentColor"
                stroke-width="3"
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M3.1 8.4 7.05 12.35 13.9 4.2"
              />
            </svg>
          </button>
        </template>
      </div>
    </div>
    <div class="text-muted text-uppercase mb-1" style="font-size: 0.6rem; letter-spacing: 0.03em">{{ data.typeLabel }}</div>

    <template v-if="!editOpen">
      <div class="fw-semibold text-break">{{ data.title || 'Untitled' }}</div>
    </template>

    <div v-else-if="ctx && draftNode" class="branching-flow-node__edit nodrag" @mousedown.stop>
      <label class="form-label mb-0 small" :for="'bf-title-' + data.stableKey">Question</label>
      <input
        :id="'bf-title-' + data.stableKey"
        ref="titleInputRef"
        v-model="draftNode.title"
        type="text"
        class="form-control form-control-sm branching-flow-node__field fw-semibold mb-2"
        placeholder="Question title"
        autocomplete="off"
      />
      <label class="form-label mb-0 small" :for="'bf-body-' + data.stableKey">Hint (optional)</label>
      <textarea
        :id="'bf-body-' + data.stableKey"
        v-model="draftNode.body"
        rows="2"
        class="form-control form-control-sm branching-flow-node__field branching-flow-node__body mb-2"
        placeholder="Shown under the question"
      />
      <label class="form-label mb-0 small" :for="'bf-qt-' + data.stableKey">How they answer</label>
      <select
        :id="'bf-qt-' + data.stableKey"
        v-model="draftNode.questionType"
        class="form-select form-select-sm mb-2"
        @change="ctx.onQuestionTypeChange(draftNode)"
      >
        <option v-for="t in ctx.questionTypes" :key="'bfqt-' + t" :value="t">{{ ctx.questionTypeLabel(t) }}</option>
      </select>

      <template v-if="qt === 'CHOICE'">
        <div class="d-flex justify-content-between align-items-center mb-1">
          <span class="text-muted fw-semibold" style="font-size: 0.65rem">Answers</span>
          <button type="button" class="btn btn-outline-secondary btn-sm py-0" @click="ctx.addChoiceRow(draftNode)">
            Add
          </button>
        </div>
        <div
          v-for="(row, ridx) in draftNode.choiceRows || []"
          :key="'bfc-' + row.id + '-' + ridx"
          class="input-group input-group-sm mb-1"
        >
          <input v-model="row.label" type="text" class="form-control" placeholder="Answer text" />
          <button
            type="button"
            class="btn btn-outline-secondary"
            title="Remove"
            @click="ctx.removeChoiceRow(draftNode, ridx)"
          >
            ×
          </button>
        </div>
        <div class="branching-flow-node__routing pt-2 mt-1 border-top border-secondary-subtle">
          <div class="text-muted fw-semibold mb-1" style="font-size: 0.65rem">Then go to…</div>
          <p v-if="!(draftNode.choiceRows || []).length" class="small text-warning mb-1">Add answers first.</p>
          <div v-for="row in draftNode.choiceRows || []" :key="'bfb-' + row.id" class="mb-2">
            <label class="form-label mb-0" style="font-size: 0.65rem">After “{{ row.label || 'this answer' }}”</label>
            <select
              class="form-select form-select-sm"
              :value="ctx.choiceTarget(draftNode.stableKey, row.id)"
              @change="ctx.setChoiceTarget(draftNode.stableKey, row.id, $event.target.value)"
            >
              <option value="">Choose…</option>
              <option v-for="k in ctx.branchTargetOptions(draftNode.stableKey)" :key="'bfch-' + row.id + k" :value="k">
                {{ ctx.stepSelectLabel(k) }}
              </option>
            </select>
          </div>
        </div>
      </template>

      <template v-if="qt === 'YES_NO'">
        <div class="branching-flow-node__routing pt-2 mt-1 border-top border-secondary-subtle">
          <div class="text-muted fw-semibold mb-1" style="font-size: 0.65rem">Then go to…</div>
          <div class="row g-2">
            <div class="col-12">
              <label class="form-label mb-0" style="font-size: 0.65rem">If Yes</label>
              <select
                class="form-select form-select-sm"
                :value="ctx.yesTarget(draftNode.stableKey)"
                @change="ctx.setYesNoTarget(draftNode.stableKey, 'yes', $event.target.value)"
              >
                <option value="">Choose…</option>
                <option v-for="k in ctx.branchTargetOptions(draftNode.stableKey)" :key="'bfyny-' + k" :value="k">
                  {{ ctx.stepSelectLabel(k) }}
                </option>
              </select>
            </div>
            <div class="col-12">
              <label class="form-label mb-0" style="font-size: 0.65rem">If No</label>
              <select
                class="form-select form-select-sm"
                :value="ctx.yesTargetNo(draftNode.stableKey)"
                @change="ctx.setYesNoTarget(draftNode.stableKey, 'no', $event.target.value)"
              >
                <option value="">Choose…</option>
                <option v-for="k in ctx.branchTargetOptions(draftNode.stableKey)" :key="'bfynn-' + k" :value="k">
                  {{ ctx.stepSelectLabel(k) }}
                </option>
              </select>
            </div>
          </div>
        </div>
      </template>

      <template v-if="qt === 'TEXT'">
        <div class="branching-flow-node__routing pt-2 mt-1 border-top border-secondary-subtle">
          <div class="text-muted fw-semibold mb-1" style="font-size: 0.65rem">Then go to…</div>
          <label class="form-label mb-0" style="font-size: 0.65rem">After they continue</label>
          <select
            class="form-select form-select-sm"
            :value="ctx.textNext(draftNode.stableKey)"
            @change="ctx.setTextNext(draftNode.stableKey, $event.target.value)"
          >
            <option value="">Choose…</option>
            <option v-for="k in ctx.branchTargetOptions(draftNode.stableKey)" :key="'bftx-' + k" :value="k">
              {{ ctx.stepSelectLabel(k) }}
            </option>
          </select>
        </div>
      </template>

      <template v-if="qt === 'END'">
        <p class="small text-muted mb-0 pt-1">End screen — no outgoing branches.</p>
      </template>
    </div>

    <div
      class="font-monospace text-muted text-truncate mt-1"
      style="font-size: 0.65rem"
      :title="data.stableKey"
    >
      {{ data.stableKey }}
    </div>
    <Handle
      v-if="!data.hideSourceHandle"
      type="source"
      :position="Position.Right"
      class="branching-flow-node__handle"
    />
  </div>
</template>

<style scoped>
.branching-flow-node--selected {
  outline: 2px solid var(--bs-primary);
  outline-offset: 2px;
}
.branching-flow-node--run {
  outline: 2px solid var(--bs-success, #198754);
  outline-offset: 1px;
}
.branching-flow-node__handle {
  width: 8px;
  height: 8px;
  border: 1px solid var(--bs-secondary);
  background: var(--bs-body-bg);
}
.branching-flow-node__icon-btn {
  line-height: 1;
  color: var(--bs-primary, #0d6efd);
  text-decoration: none;
  opacity: 1;
}
.branching-flow-node__icon-btn:not(.branching-flow-node__icon-btn--done):not(.branching-flow-node__icon-btn--cancel):hover {
  color: var(--bs-primary-text-emphasis, #052c65);
  filter: brightness(0.92);
}
.branching-flow-node__icon-btn--cancel {
  color: #6c757d;
  font-weight: 600;
}
.branching-flow-node__icon-btn--cancel:hover {
  color: #b02a37;
}
.branching-flow-node__icon-btn--done {
  color: #198754;
  font-weight: 700;
}
.branching-flow-node__check-svg {
  display: block;
  flex-shrink: 0;
}
.branching-flow-node__icon-btn--done:hover {
  color: #146c43;
}
.branching-flow-node__field {
  width: 100%;
  min-width: 0;
  font-size: 0.8125rem;
  user-select: text;
}
.branching-flow-node__body {
  resize: vertical;
  min-height: 2.5rem;
  max-height: 6rem;
  font-size: 0.75rem;
  line-height: 1.35;
}
.branching-flow-node__edit :deep(.form-label),
.branching-flow-node__edit :deep(.form-select),
.branching-flow-node__edit :deep(.form-control) {
  user-select: text;
}
.branching-flow-node__edit :deep(.form-select),
.branching-flow-node__edit :deep(.form-control) {
  font-size: 0.75rem;
}
</style>
