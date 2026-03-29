<template>
  <div class="branching-demo branching-demo--wide branching-demo--shell">
    <header class="branching-demo__page-head d-flex flex-wrap gap-3 justify-content-between align-items-start mb-3">
      <div>
        <p class="branching-demo__eyebrow text-uppercase small fw-semibold mb-1">Demo workspace</p>
        <h1 class="h3 mb-0 branching-demo__title">Branching workflow</h1>
        <p class="text-body-secondary small mb-0 mt-2 branching-demo__lede">
          Design steps on the map, wire branches, and edit details in the list. Open preview from the bar to walk the
          <strong>last saved</strong> version in a modal.
        </p>
      </div>
      <button
        type="button"
        class="btn btn-outline-secondary btn-sm align-self-start rounded-pill px-3"
        :disabled="loading"
        @click="loadGraph"
      >
        Reload graph
      </button>
    </header>

    <div class="branching-demo__intro callout small mb-4">
      <p class="mb-0">
        Use the <strong>map</strong> to add steps and draw branches; the question list stays in sync. For Yes/No and list
        choices, a dialog picks which branch a new link uses.
        <strong>Preview</strong> opens in a modal from <strong>Start preview</strong> in the sticky bar. Save drafts with the same bar.
      </p>
    </div>

    <div v-if="loadError" class="alert alert-warning border-0 shadow-sm rounded-3">
      <div class="fw-semibold mb-1">Could not load demo workflow</div>
      <div class="small mb-0">{{ loadError }}</div>
    </div>
    <div v-else-if="loading" class="branching-demo__loading text-body-secondary py-5 text-center rounded-3">
      <span class="branching-demo__spinner d-inline-block rounded-circle me-2" aria-hidden="true" />
      Loading workflow…
    </div>

    <div v-else-if="hasGraph" class="d-flex flex-column gap-4 branching-demo__main">
      <div
        class="branching-demo__save-bar sticky-top d-flex flex-wrap align-items-center gap-3 py-3 px-3 px-md-4 rounded-3"
      >
        <div class="d-flex align-items-center gap-2 me-auto">
          <span class="branching-demo__save-bar-icon rounded-2 d-none d-sm-flex" aria-hidden="true" />
          <div class="d-flex flex-column lh-sm">
            <span class="small fw-semibold text-body">Draft</span>
            <span class="text-body-secondary" style="font-size: 0.75rem">Unsaved map and list edits</span>
          </div>
        </div>
        <div class="d-flex flex-wrap align-items-center gap-2 gap-md-3 ms-sm-auto">
          <div v-if="!runStarted" class="d-flex flex-wrap align-items-center gap-2 branching-demo__save-bar-preview">
            <span class="small text-body-secondary d-none d-md-inline">Preview <span class="text-muted">(saved)</span></span>
            <button type="button" class="btn btn-primary btn-sm rounded-pill px-3" @click="startRun">Start preview</button>
            <p v-if="startRunError" class="small text-danger mb-0">{{ startRunError }}</p>
          </div>
          <span v-if="saveSuccess" class="small text-success mb-0">{{ saveSuccess }}</span>
          <span v-if="saveError" class="small text-danger mb-0">{{ saveError }}</span>
          <div class="vr d-none d-md-block text-muted opacity-25 align-self-stretch my-1" />
          <div class="d-flex flex-wrap gap-2">
            <button type="button" class="btn btn-outline-secondary btn-sm" :disabled="saving" @click="resetDraft">
              Discard
            </button>
            <button type="button" class="btn btn-primary branching-demo__save-btn px-4" :disabled="saving" @click="saveWorkflow">
              {{ saving ? 'Saving…' : 'Save workflow' }}
            </button>
          </div>
        </div>
      </div>

      <div class="row g-4">
        <div class="col-12">
          <div class="card branching-demo__surface border-0 shadow-sm h-100">
            <div
              class="card-header py-3 px-3 px-md-4 branching-demo__card-head d-flex flex-wrap gap-2 justify-content-between align-items-center border-0"
            >
              <div class="d-flex align-items-center gap-2">
                <span class="branching-demo__section-dot rounded-circle flex-shrink-0" aria-hidden="true" />
                <span class="fw-semibold">Flow map</span>
              </div>
              <span v-if="!draftStartStableKey" class="badge rounded-pill text-bg-warning text-dark">Pick a start step below</span>
            </div>
            <div class="card-body p-0">
              <div
                ref="flowWrapRef"
                class="branching-demo__flow-wrap border-0 rounded-0 position-relative"
                @pointerdown.capture="onFlowWrapPointerDownCapture"
              >
                <div
                  v-if="!flowMapActivated"
                  class="branching-demo__map-activate-hint position-absolute top-0 start-50 translate-middle-x mt-3 px-3 py-2 rounded-pill small shadow"
                >
                  Click the map to pan, zoom, and drag steps
                </div>
                <VueFlow
                  v-model:nodes="flowVueNodes"
                  v-model:edges="flowVueEdges"
                  :node-types="nodeTypes"
                  :default-edge-options="{ type: 'smoothstep' }"
                  :fit-view-on-init="true"
                  :min-zoom="0.15"
                  :max-zoom="2"
                  :is-valid-connection="isFlowValidConnection"
                  :zoom-on-scroll="flowMapActivated"
                  :pan-on-drag="flowMapActivated"
                  :pan-on-scroll="flowMapActivated"
                  :zoom-on-pinch="flowMapActivated"
                  :zoom-on-double-click="flowMapActivated"
                  :nodes-draggable="flowMapActivated"
                  :nodes-connectable="flowMapActivated"
                  :class="[
                    'branching-demo__vue-flow',
                    { 'branching-demo--card-editing': !!editingCardStableKey },
                    { 'branching-demo--map-inactive': !flowMapActivated }
                  ]"
                  @node-drag-start="onFlowDragStart"
                  @node-drag-stop="onFlowDragStop"
                  @node-click="onFlowNodeClick"
                  @pane-click="onFlowPaneClick"
                  @connect="onFlowConnect"
                  @edges-change="onFlowEdgesChange"
                >
                  <Background variant="dots" :gap="14" color="#94a3b8" />
                  <Controls position="top-left" :show-interactive="false">
                    <template #icon-zoom-in>
                      <span class="branching-demo__map-control" title="Zoom in — enlarge the map">
                        <span class="visually-hidden">Zoom in</span>
                        <span class="branching-demo__map-control-glyph" aria-hidden="true">+</span>
                        <span class="branching-demo__map-control-label" aria-hidden="true">Zoom in</span>
                      </span>
                    </template>
                    <template #icon-zoom-out>
                      <span class="branching-demo__map-control" title="Zoom out — shrink the map">
                        <span class="visually-hidden">Zoom out</span>
                        <span class="branching-demo__map-control-glyph" aria-hidden="true">−</span>
                        <span class="branching-demo__map-control-label" aria-hidden="true">Zoom out</span>
                      </span>
                    </template>
                    <template #icon-fit-view>
                      <span class="branching-demo__map-control branching-demo__map-control--fit" title="Fit view — show all steps">
                        <span class="visually-hidden">Fit all steps in view</span>
                        <span class="branching-demo__map-control-glyph branching-demo__map-control-glyph--fit" aria-hidden="true">⤢</span>
                        <span class="branching-demo__map-control-label" aria-hidden="true">Fit all</span>
                      </span>
                    </template>
                  </Controls>
                  <MiniMap
                    :pannable="flowMapActivated"
                    :zoomable="flowMapActivated"
                    class="branching-demo__minimap"
                  />
                  <BranchingFlowMapTools
                    :layout-fit-tick="layoutFitTick"
                    @add-step="addDraftNode"
                    @auto-layout="runAutoLayout"
                  />
                </VueFlow>
              </div>
              <p class="branching-demo__map-footnote small text-body-secondary mb-0 px-3 py-3">
                <span class="text-body fw-medium">Q1, Q2…</span> matches list order. The id on each box is the
                <strong>internal key</strong>. Click the map once to enable pan, zoom, and handles; click outside to
                disable. Select an arrow and press <kbd class="branching-demo__map-footnote-kbd">Delete</kbd> or
                <kbd class="branching-demo__map-footnote-kbd">Backspace</kbd> to remove it.
              </p>
              <div ref="mapEditorEl" class="branching-demo__map-editor px-3 px-md-4 py-4 small border-top">
                <div class="row g-2 align-items-end mb-3">
                  <div class="col-md-6 col-lg-5">
                    <label class="form-label mb-0">Start of flow</label>
                    <select v-model="draftStartStableKey" class="form-select form-select-sm">
                      <option v-for="k in draftStableKeyOptions" :key="'map-start-' + k" :value="k">{{ stepSelectLabel(k) }}</option>
                    </select>
                  </div>
                  <div class="col-md-6 col-lg-7 d-flex flex-wrap gap-2 align-items-end">
                    <button type="button" class="btn btn-outline-primary btn-sm" @click="addDraftNode">Add question</button>
                  </div>
                </div>

                <template v-if="selectedDraftNode">
                  <div class="d-flex flex-wrap justify-content-between align-items-start gap-2 mb-2 pb-2 border-bottom border-secondary-subtle">
                    <span class="fw-semibold">
                      Selected: Question {{ selectedDraftIndex + 1 }}
                      <span class="text-muted fw-normal">(Q{{ selectedDraftIndex + 1 }} on map)</span>
                    </span>
                    <button
                      v-if="selectedDraftIndex >= 0"
                      type="button"
                      class="btn btn-outline-danger btn-sm py-0"
                      @click="removeDraftNode(selectedDraftIndex)"
                    >
                      Remove step
                    </button>
                  </div>
                  <div class="row g-3">
                    <div class="col-12">
                      <label class="form-label mb-0">What to ask</label>
                      <input
                        v-model="selectedDraftNode.title"
                        type="text"
                        class="form-control form-control-sm"
                        placeholder="Question title"
                      />
                    </div>
                    <div class="col-md-6">
                      <label class="form-label mb-0">How they answer</label>
                      <select
                        v-model="selectedDraftNode.questionType"
                        class="form-select form-select-sm"
                        @change="onQuestionTypeChange(selectedDraftNode)"
                      >
                        <option v-for="t in questionTypes" :key="'mapt-' + t" :value="t">{{ questionTypeLabel(t) }}</option>
                      </select>
                    </div>
                    <div class="col-12">
                      <label class="form-label mb-0">Extra hint <span class="text-muted fw-normal">(optional)</span></label>
                      <textarea
                        v-model="selectedDraftNode.body"
                        class="form-control form-control-sm"
                        rows="2"
                        placeholder="Shown under the question"
                      />
                    </div>

                    <template v-if="selectedDraftNode.questionType === 'CHOICE'">
                      <div class="col-12">
                        <div class="d-flex justify-content-between align-items-center mb-2">
                          <span class="form-label mb-0">Answers</span>
                          <button type="button" class="btn btn-outline-secondary btn-sm py-0" @click="addChoiceRow(selectedDraftNode)">
                            Add answer
                          </button>
                        </div>
                        <div
                          v-for="(row, ridx) in selectedDraftNode.choiceRows || []"
                          :key="'mapcr-' + row.id + '-' + ridx"
                          class="input-group input-group-sm mb-2"
                        >
                          <input v-model="row.label" type="text" class="form-control" placeholder="Answer text" />
                          <button
                            type="button"
                            class="btn btn-outline-secondary"
                            title="Remove"
                            @click="removeChoiceRow(selectedDraftNode, ridx)"
                          >
                            ×
                          </button>
                        </div>
                      </div>
                    </template>

                    <div v-if="selectedDraftNode.questionType === 'YES_NO'" class="col-12 pt-2 border-top border-secondary-subtle">
                      <div class="fw-semibold mb-2">Then go to…</div>
                      <div class="row g-2">
                        <div class="col-md-6">
                          <label class="form-label mb-0">If Yes</label>
                          <select
                            class="form-select form-select-sm"
                            :value="yesTarget(selectedDraftNode.stableKey)"
                            @change="setYesNoTarget(selectedDraftNode.stableKey, 'yes', $event.target.value)"
                          >
                            <option value="">Choose…</option>
                            <option
                              v-for="k in branchTargetOptions(selectedDraftNode.stableKey)"
                              :key="'mapyny-' + k"
                              :value="k"
                            >
                              {{ stepSelectLabel(k) }}
                            </option>
                          </select>
                        </div>
                        <div class="col-md-6">
                          <label class="form-label mb-0">If No</label>
                          <select
                            class="form-select form-select-sm"
                            :value="yesTargetNo(selectedDraftNode.stableKey)"
                            @change="setYesNoTarget(selectedDraftNode.stableKey, 'no', $event.target.value)"
                          >
                            <option value="">Choose…</option>
                            <option
                              v-for="k in branchTargetOptions(selectedDraftNode.stableKey)"
                              :key="'mapynn-' + k"
                              :value="k"
                            >
                              {{ stepSelectLabel(k) }}
                            </option>
                          </select>
                        </div>
                      </div>
                    </div>

                    <div
                      v-else-if="selectedDraftNode.questionType === 'CHOICE'"
                      class="col-12 pt-2 border-top border-secondary-subtle"
                    >
                      <div class="fw-semibold mb-2">Then go to…</div>
                      <p v-if="!selectedDraftNode.choiceRows?.length" class="small text-warning mb-2">Add answers above, then link each one.</p>
                      <div v-for="row in selectedDraftNode.choiceRows || []" :key="'mapbr-' + row.id" class="mb-2">
                        <label class="form-label mb-0 small">After “{{ row.label || 'this answer' }}”</label>
                        <select
                          class="form-select form-select-sm"
                          :value="choiceTarget(selectedDraftNode.stableKey, row.id)"
                          @change="setChoiceTarget(selectedDraftNode.stableKey, row.id, $event.target.value)"
                        >
                          <option value="">Choose…</option>
                          <option
                            v-for="k in branchTargetOptions(selectedDraftNode.stableKey)"
                            :key="'mapch-' + row.id + k"
                            :value="k"
                          >
                            {{ stepSelectLabel(k) }}
                          </option>
                        </select>
                      </div>
                    </div>

                    <div v-else-if="selectedDraftNode.questionType === 'TEXT'" class="col-12 pt-2 border-top border-secondary-subtle">
                      <div class="fw-semibold mb-2">Then go to…</div>
                      <label class="form-label mb-0">After they continue</label>
                      <select
                        class="form-select form-select-sm"
                        :value="textNext(selectedDraftNode.stableKey)"
                        @change="setTextNext(selectedDraftNode.stableKey, $event.target.value)"
                      >
                        <option value="">Choose…</option>
                        <option v-for="k in branchTargetOptions(selectedDraftNode.stableKey)" :key="'maptx-' + k" :value="k">
                          {{ stepSelectLabel(k) }}
                        </option>
                      </select>
                    </div>

                    <details class="col-12">
                      <summary class="text-muted" style="cursor: pointer">Technical details</summary>
                      <div class="row g-2 mt-2 pt-2 border-top border-secondary-subtle">
                        <div class="col-md-6">
                          <label class="form-label mb-0">Internal id</label>
                          <input
                            v-model="selectedDraftNode.stableKey"
                            type="text"
                            class="form-control form-control-sm font-monospace"
                            autocomplete="off"
                          />
                        </div>
                        <div class="col-md-3">
                          <label class="form-label mb-0">Map X</label>
                          <input v-model.number="selectedDraftNode.posX" type="number" class="form-control form-control-sm" />
                        </div>
                        <div class="col-md-3">
                          <label class="form-label mb-0">Map Y</label>
                          <input v-model.number="selectedDraftNode.posY" type="number" class="form-control form-control-sm" />
                        </div>
                      </div>
                    </details>
                  </div>
                </template>
                <p v-else class="text-muted mb-0">Click a box on the map to edit it here (title, answers, and routing).</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="card branching-demo__surface border-0 shadow-sm">
        <div class="card-header py-3 px-3 px-md-4 branching-demo__card-head border-0">
          <div class="d-flex flex-wrap align-items-center gap-2 mb-1">
            <span class="branching-demo__section-dot rounded-circle flex-shrink-0" aria-hidden="true" />
            <span class="fw-semibold">Questions</span>
          </div>
          <p class="text-body-secondary small mb-1 fw-normal">
            Expand a row to edit wording, answers, and branching. Order matches Q1, Q2… on the map.
          </p>
          <p class="text-body-secondary mb-0" style="font-size: 0.8rem">Save from the sticky bar at the top.</p>
        </div>
        <div class="card-body px-3 px-md-4 pb-4">
          <div class="vstack gap-3 mb-3">
            <details
              v-for="(node, idx) in draftNodes"
              :key="'dn' + idx"
              :id="'branching-q-' + idx"
              class="border rounded-3 bg-body-tertiary branching-demo__question-card branching-demo__question-details"
              :class="{ 'branching-demo__question-card--active': selectedStableKey === String(node.stableKey ?? '').trim() }"
              @click="selectQuestionForMap(node)"
            >
              <summary class="branching-demo__question-summary d-flex justify-content-between align-items-center gap-2 px-3 py-2 small rounded-top">
                <div class="d-flex flex-wrap align-items-center gap-2 min-w-0 flex-grow-1">
                  <span class="fw-semibold text-nowrap">Question {{ idx + 1 }}</span>
                  <span class="text-muted">·</span>
                  <span class="text-truncate" :title="(node.title || '').trim() || 'Untitled'">
                    {{ (node.title || '').trim() || 'Untitled' }}
                  </span>
                  <span class="badge text-bg-secondary text-truncate" style="max-width: 12rem">{{ questionTypeLabel(node.questionType) }}</span>
                  <span class="text-muted fw-normal small text-nowrap">(Q{{ idx + 1 }} on map)</span>
                </div>
                <button
                  type="button"
                  class="btn btn-outline-danger btn-sm py-0 flex-shrink-0"
                  @click.stop="removeDraftNode(idx)"
                >
                  Remove
                </button>
              </summary>
              <div class="px-3 pb-3 pt-0 small">
                <div class="row g-3 pt-3">
                <div class="col-12">
                  <label class="form-label mb-0">What to ask</label>
                  <input v-model="node.title" type="text" class="form-control form-control-sm" placeholder="Question title" />
                </div>
                <div class="col-md-6">
                  <label class="form-label mb-0">How they answer</label>
                  <select v-model="node.questionType" class="form-select form-select-sm" @change="onQuestionTypeChange(node)">
                    <option v-for="t in questionTypes" :key="t" :value="t">{{ questionTypeLabel(t) }}</option>
                  </select>
                </div>
                <div class="col-12">
                  <label class="form-label mb-0">Extra hint <span class="text-muted fw-normal">(optional)</span></label>
                  <textarea v-model="node.body" class="form-control form-control-sm" rows="2" placeholder="Shown under the question" />
                </div>

                <template v-if="node.questionType === 'CHOICE'">
                  <div class="col-12">
                    <div class="d-flex justify-content-between align-items-center mb-2">
                      <span class="form-label mb-0">Answers</span>
                      <button type="button" class="btn btn-outline-secondary btn-sm py-0" @click="addChoiceRow(node)">Add answer</button>
                    </div>
                    <div v-for="(row, ridx) in node.choiceRows || []" :key="row.id + '-' + ridx" class="input-group input-group-sm mb-2">
                      <input v-model="row.label" type="text" class="form-control" placeholder="Answer text" />
                      <button type="button" class="btn btn-outline-secondary" title="Remove" @click="removeChoiceRow(node, ridx)">×</button>
                    </div>
                  </div>
                </template>

                <div v-if="node.questionType === 'YES_NO'" class="col-12 pt-2 border-top border-secondary-subtle">
                  <div class="fw-semibold mb-2 small">Then go to…</div>
                  <div class="row g-2">
                    <div class="col-md-6">
                      <label class="form-label mb-0">If Yes</label>
                      <select
                        class="form-select form-select-sm"
                        :value="yesTarget(node.stableKey)"
                        @change="setYesNoTarget(node.stableKey, 'yes', $event.target.value)"
                      >
                        <option value="">Choose…</option>
                        <option v-for="k in branchTargetOptions(node.stableKey)" :key="'yny-' + k" :value="k">{{ stepSelectLabel(k) }}</option>
                      </select>
                    </div>
                    <div class="col-md-6">
                      <label class="form-label mb-0">If No</label>
                      <select
                        class="form-select form-select-sm"
                        :value="yesTargetNo(node.stableKey)"
                        @change="setYesNoTarget(node.stableKey, 'no', $event.target.value)"
                      >
                        <option value="">Choose…</option>
                        <option v-for="k in branchTargetOptions(node.stableKey)" :key="'ynn-' + k" :value="k">{{ stepSelectLabel(k) }}</option>
                      </select>
                    </div>
                  </div>
                </div>

                <div v-else-if="node.questionType === 'CHOICE'" class="col-12 pt-2 border-top border-secondary-subtle">
                  <div class="fw-semibold mb-2 small">Then go to…</div>
                  <p v-if="!node.choiceRows?.length" class="small text-warning mb-2">Add answers above, then link each one.</p>
                  <div v-for="row in node.choiceRows || []" :key="'br-' + row.id" class="mb-2">
                    <label class="form-label mb-0 small">After “{{ row.label || 'this answer' }}”</label>
                    <select
                      class="form-select form-select-sm"
                      :value="choiceTarget(node.stableKey, row.id)"
                      @change="setChoiceTarget(node.stableKey, row.id, $event.target.value)"
                    >
                      <option value="">Choose…</option>
                      <option v-for="k in branchTargetOptions(node.stableKey)" :key="'ch-' + row.id + k" :value="k">{{ stepSelectLabel(k) }}</option>
                    </select>
                  </div>
                </div>

                <div v-else-if="node.questionType === 'TEXT'" class="col-12 pt-2 border-top border-secondary-subtle">
                  <div class="fw-semibold mb-2 small">Then go to…</div>
                  <label class="form-label mb-0">After they continue</label>
                  <select
                    class="form-select form-select-sm"
                    :value="textNext(node.stableKey)"
                    @change="setTextNext(node.stableKey, $event.target.value)"
                  >
                    <option value="">Choose…</option>
                    <option v-for="k in branchTargetOptions(node.stableKey)" :key="'tx-' + k" :value="k">{{ stepSelectLabel(k) }}</option>
                  </select>
                </div>

                <details class="col-12 small">
                  <summary class="text-muted" style="cursor: pointer">Technical details</summary>
                  <div class="row g-2 mt-2 pt-2 border-top border-secondary-subtle">
                    <div class="col-md-6">
                      <label class="form-label mb-0">Internal id</label>
                      <input v-model="node.stableKey" type="text" class="form-control form-control-sm font-monospace" autocomplete="off" />
                    </div>
                    <div class="col-md-3">
                      <label class="form-label mb-0">Map X</label>
                      <input v-model.number="node.posX" type="number" class="form-control form-control-sm" />
                    </div>
                    <div class="col-md-3">
                      <label class="form-label mb-0">Map Y</label>
                      <input v-model.number="node.posY" type="number" class="form-control form-control-sm" />
                    </div>
                  </div>
                </details>
              </div>
            </div>
            </details>
          </div>

          <details class="small">
            <summary class="text-muted mb-2" style="cursor: pointer">Advanced — manual transitions</summary>
            <p class="text-muted mb-2">
              Only if you need extra links (for example a fallback). Most flows work without this.
            </p>
            <div class="d-flex gap-2 mb-2">
              <button type="button" class="btn btn-outline-secondary btn-sm" @click="addDraftEdge">Add row</button>
            </div>
            <div class="table-responsive">
              <table class="table table-sm align-middle mb-0">
                <thead>
                  <tr>
                    <th>From</th>
                    <th>To</th>
                    <th>Order</th>
                    <th>Rule</th>
                    <th>Value</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="row in manualEdgeRows" :key="'de' + row.idx">
                    <td>
                      <select v-model="row.edge.fromStableKey" class="form-select form-select-sm">
                        <option value="">—</option>
                        <option v-for="k in draftStableKeyOptions" :key="'f' + row.idx + k" :value="k">{{ stepSelectLabel(k) }}</option>
                      </select>
                    </td>
                    <td>
                      <select v-model="row.edge.toStableKey" class="form-select form-select-sm">
                        <option value="">—</option>
                        <option v-for="k in draftStableKeyOptions" :key="'t' + row.idx + k" :value="k">{{ stepSelectLabel(k) }}</option>
                      </select>
                    </td>
                    <td style="width: 4.5rem">
                      <input v-model.number="row.edge.sortOrder" type="number" class="form-control form-control-sm" />
                    </td>
                    <td>
                      <select v-model="row.edge.conditionKind" class="form-select form-select-sm">
                        <option v-for="c in conditionKinds" :key="c" :value="c">{{ c }}</option>
                      </select>
                    </td>
                    <td>
                      <input
                        v-model="row.edge.conditionValue"
                        type="text"
                        class="form-control form-control-sm font-monospace"
                        :disabled="row.edge.conditionKind !== 'OPTION'"
                        placeholder="if needed"
                      />
                    </td>
                    <td class="text-end">
                      <button type="button" class="btn btn-outline-danger btn-sm py-0" @click="removeDraftEdge(row.edge)">×</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <p v-if="!draftEdges.length" class="small text-muted mb-0 mt-2">No extra rows.</p>
            <p v-else-if="!manualEdgeRows.length" class="small text-muted mb-0 mt-2">Nothing extra — all links are set above.</p>
          </details>
        </div>
      </div>
    </div>

    <div v-else class="alert alert-secondary small mb-0">
      No graph to display. If this persists after Reload, open DevTools → Network and check
      <code>GET /api/demo/branching-workflows/graph</code> (expect 200 and a JSON body with
      <code>nodes</code>).
    </div>

    <div
      v-if="connectModalOpen"
      ref="connectModalEl"
      class="modal fade show d-block branching-demo__connect-modal"
      tabindex="-1"
      role="dialog"
      aria-modal="true"
      aria-labelledby="branching-connect-modal-title"
      @click.self="cancelConnectModal"
    >
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content shadow" @click.stop>
          <div class="modal-header py-2">
            <h2 id="branching-connect-modal-title" class="modal-title h6 mb-0">New branch</h2>
            <button type="button" class="btn-close" aria-label="Close" @click="cancelConnectModal" />
          </div>
          <div class="modal-body small">
            <p v-if="connectModalSummary" class="text-muted mb-3 mb-md-2">{{ connectModalSummary }}</p>

            <template v-if="connectModalQuestionType === 'YES_NO'">
              <p class="fw-medium mb-2">Which answer does this arrow represent?</p>
              <div class="vstack gap-2">
                <label class="d-flex align-items-center gap-2 mb-0">
                  <input v-model="connectModalYesNoKind" type="radio" class="form-check-input mt-0" value="YES" />
                  <span>Yes</span>
                </label>
                <label class="d-flex align-items-center gap-2 mb-0">
                  <input v-model="connectModalYesNoKind" type="radio" class="form-check-input mt-0" value="NO" />
                  <span>No</span>
                </label>
                <label class="d-flex align-items-center gap-2 mb-0">
                  <input v-model="connectModalYesNoKind" type="radio" class="form-check-input mt-0" value="ALWAYS" />
                  <span>Always <span class="text-muted fw-normal">(fallback — use only if you need an extra link)</span></span>
                </label>
              </div>
            </template>

            <template v-else-if="connectModalQuestionType === 'CHOICE'">
              <p class="fw-medium mb-2">After which answer does this path apply?</p>
              <div v-if="connectModalChoiceRows.length" class="vstack gap-2">
                <label
                  v-for="row in connectModalChoiceRows"
                  :key="'cm-' + row.id"
                  class="d-flex align-items-start gap-2 mb-0"
                >
                  <input
                    v-model="connectModalChoiceValue"
                    type="radio"
                    class="form-check-input mt-1"
                    :value="String(row.id ?? '')"
                  />
                  <span>{{ row.label?.trim() ? row.label : row.id }}</span>
                </label>
                <label class="d-flex align-items-center gap-2 mb-0">
                  <input v-model="connectModalChoiceValue" type="radio" class="form-check-input mt-0" value="__ALWAYS__" />
                  <span>Always <span class="text-muted fw-normal">(fallback)</span></span>
                </label>
              </div>
              <p v-else class="text-warning mb-0">Add at least one answer option to this question in the list below, then try again.</p>
            </template>
          </div>
          <div class="modal-footer py-2">
            <button type="button" class="btn btn-outline-secondary btn-sm" @click="cancelConnectModal">Cancel</button>
            <button
              type="button"
              class="btn btn-primary btn-sm"
              :disabled="connectModalConfirmDisabled"
              @click="confirmConnectModal"
            >
              Add branch
            </button>
          </div>
        </div>
      </div>
    </div>

    <BsModal
      v-model="previewModalOpen"
      title="Preview saved flow"
      size="lg"
      scrollable
      @hidden="onPreviewModalHidden"
    >
      <div>
      <p class="small text-muted mb-3 mb-md-4">
        Walks through the <strong>last saved</strong> version from the server — not your unsaved draft.
      </p>
      <div v-if="runFinished" class="alert alert-success small mb-0">
        <div class="fw-semibold">{{ endTitle }}</div>
        <div v-if="endBody" class="mt-1">{{ endBody }}</div>
        <button type="button" class="btn btn-outline-secondary btn-sm mt-2" @click="closePreviewModal">Start over</button>
      </div>
      <template v-else-if="currentRunNode">
        <p class="fw-medium mb-1">{{ currentRunNode.title }}</p>
        <p v-if="currentRunNode.body" class="small text-muted mb-3">{{ currentRunNode.body }}</p>
        <div v-if="currentRunNode.questionType === 'YES_NO'" class="d-flex gap-2 flex-wrap">
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
          <p v-else class="small text-warning mb-0">No answers configured for this question.</p>
        </div>
        <div v-else-if="currentRunNode.questionType === 'TEXT'">
          <textarea v-model="textAnswer" class="form-control form-control-sm mb-2" rows="3" placeholder="Type something…" />
          <button type="button" class="btn btn-primary btn-sm" :disabled="resolving" @click="submitText">Continue</button>
        </div>
        <div v-else class="alert alert-warning small py-2 mb-0">This step type cannot be previewed here.</div>
        <p v-if="resolveError" class="small text-danger mt-2 mb-0">{{ resolveError }}</p>
        <button type="button" class="btn btn-link btn-sm px-0 mt-2" @click="closePreviewModal">Cancel</button>
      </template>
      <div v-if="runStarted && pathLabels.length" class="mt-3 pt-3 border-top border-secondary-subtle small text-body-secondary">
        <div class="text-uppercase mb-2 branching-demo__path-label">Path so far</div>
        <ol class="mb-0 ps-3">
          <li v-for="(p, i) in pathLabels" :key="i">{{ p }}</li>
        </ol>
      </div>
      </div>
      <template #footer>
        <button type="button" class="btn btn-outline-secondary btn-sm" @click="closePreviewModal">Close</button>
      </template>
    </BsModal>
  </div>
</template>

<script setup>
import { MarkerType, VueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { MiniMap } from '@vue-flow/minimap'
import { computed, markRaw, nextTick, onMounted, onUnmounted, provide, ref, watch } from 'vue'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/minimap/dist/style.css'
import dagre from '@dagrejs/dagre'
import api from '../../services/api'
import { formatLoadError } from '../../utils/loadErrorFormat'
import BsModal from '../../components/BsModal.vue'
import BranchingFlowNode from './BranchingFlowNode.vue'
import BranchingFlowMapTools from './BranchingFlowMapTools.vue'

const questionTypes = ['YES_NO', 'TEXT', 'CHOICE', 'END']
const conditionKinds = ['ALWAYS', 'YES', 'NO', 'OPTION']

const questionTypeLabels = {
  YES_NO: 'Yes / No',
  CHOICE: 'Pick one (list)',
  TEXT: 'Free text',
  END: 'End screen'
}

function questionTypeLabel(t) {
  return questionTypeLabels[t] || t
}

/** Label for branch dropdowns: question title, or title + id if duplicate titles */
function stepSelectLabel(key) {
  const k = String(key ?? '').trim()
  const n = draftNodes.value.find((x) => x.stableKey?.trim() === k)
  if (!n) return k
  const title = (n.title || '').trim()
  if (!title) return k
  const dup = draftNodes.value.filter((x) => (x.title || '').trim() === title).length
  return dup > 1 ? `${title} · ${k}` : title
}

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
/** @type {import('vue').Ref<Array<{ stableKey: string, title: string, body: string, questionType: string, choicesJson: string, choiceRows: Array<{ id: string, label: string }>, posX: number, posY: number }>>} */
const draftNodes = ref([])
/** @type {import('vue').Ref<Array<{ fromStableKey: string, toStableKey: string, sortOrder: number, conditionKind: string, conditionValue: string }>>} */
const draftEdges = ref([])

/** Apply field updates from map cards; mutates draft so the flow list and save payload stay in sync. */
function patchDraftNode(stableKey, patch) {
  const k = String(stableKey ?? '').trim()
  if (!k || !patch || typeof patch !== 'object') return
  const n = draftNodes.value.find((x) => String(x.stableKey ?? '').trim() === k)
  if (!n) return
  Object.assign(n, patch)
}

const saving = ref(false)
const saveError = ref('')
const saveSuccess = ref('')

const flowVueNodes = ref([])
const flowVueEdges = ref([])
const selectedStableKey = ref('')
const isFlowDragging = ref(false)
/** Incremented after auto-layout so the map toolbar can fit the view to the new positions. */
const layoutFitTick = ref(0)
/** Map card inline editor: which step is being edited (elevates node, sends panels behind canvas). */
const editingCardStableKey = ref('')

const connectModalOpen = ref(false)
/** @type {import('vue').Ref<{ fk: string, tk: string } | null>} */
const pendingConnect = ref(null)
const connectModalYesNoKind = ref('YES')
const connectModalChoiceValue = ref('')
const connectModalEl = ref(null)
/** Map column "Selected step" panel — scroll into view on map click, not the list below. */
const mapEditorEl = ref(null)
/** Saved-flow preview modal (Bootstrap modal via BsModal). */
const previewModalOpen = ref(false)
/** Bordered map area (Vue Flow); pan/zoom/minimap controls require a click inside here first. */
const flowWrapRef = ref(null)
const flowMapActivated = ref(false)

const nodeTypes = { branching: markRaw(BranchingFlowNode) }

const hasGraph = computed(() => {
  const g = graph.value
  if (!g || !Array.isArray(g.nodes)) return false
  const vid = g.versionId
  return vid != null && Number.isFinite(Number(vid))
})
const savedGraphNodes = computed(() => graph.value?.nodes || [])

const nodeById = computed(() => {
  const m = new Map()
  savedGraphNodes.value.forEach((n) => m.set(n.id, n))
  return m
})

const currentRunNode = computed(() => {
  if (currentNodeId.value == null) return null
  return nodeById.value.get(currentNodeId.value)
})

/** Stable key of the node highlighted during Preview (saved graph ids). */
const runHighlightStableKey = computed(() => {
  if (!runStarted.value || runFinished.value || !graph.value) return null
  const cur = savedGraphNodes.value.find((n) => n.id === currentNodeId.value)
  return cur?.stableKey ?? null
})

const draftStableKeyOptions = computed(() =>
  draftNodes.value.map((n) => n.stableKey?.trim()).filter(Boolean)
)

const connectModalQuestionType = computed(() => {
  const fk = pendingConnect.value?.fk
  if (!fk) return ''
  const n = draftNodes.value.find((x) => x.stableKey?.trim() === fk)
  return String(n?.questionType ?? '').trim().toUpperCase()
})

const connectModalChoiceRows = computed(() => {
  const fk = pendingConnect.value?.fk
  if (!fk) return []
  const n = draftNodes.value.find((x) => x.stableKey?.trim() === fk)
  return n?.choiceRows || []
})

const connectModalSummary = computed(() => {
  const p = pendingConnect.value
  if (!p?.fk || !p?.tk) return ''
  return `From ${stepSelectLabel(p.fk)} → ${stepSelectLabel(p.tk)}`
})

const connectModalConfirmDisabled = computed(() => {
  if (!connectModalOpen.value) return false
  if (connectModalQuestionType.value !== 'CHOICE') return false
  return connectModalChoiceRows.value.length === 0
})

const selectedDraftNode = computed(() => {
  const k = String(selectedStableKey.value ?? '').trim()
  if (!k) return null
  return draftNodes.value.find((n) => String(n.stableKey ?? '').trim() === k) ?? null
})

const selectedDraftIndex = computed(() => {
  if (!selectedDraftNode.value) return -1
  return draftNodes.value.indexOf(selectedDraftNode.value)
})

function edgeRouteLabel(e) {
  const ck = String(e.conditionKind ?? '').trim().toUpperCase()
  if (ck === 'YES') return 'Yes'
  if (ck === 'NO') return 'No'
  if (ck === 'OPTION') {
    const v = String(e.conditionValue ?? '').trim()
    const from = String(e.fromStableKey ?? '').trim()
    const node = draftNodes.value.find((n) => n.stableKey?.trim() === from)
    const choice = node?.choiceRows?.find((r) => String(r.id).trim() === v)
    if (choice?.label?.trim()) return `"${choice.label.trim()}"`
    return v || 'Option'
  }
  if (ck === 'ALWAYS') return 'Always'
  return ck || '—'
}

/** Distinct stroke colors for draft edges (deterministic from endpoints + index). */
const BRANCHING_EDGE_COLORS = [
  '#2563eb',
  '#dc2626',
  '#059669',
  '#d97706',
  '#7c3aed',
  '#db2777',
  '#0891b2',
  '#ca8a04',
  '#4f46e5',
  '#ea580c',
  '#0d9488',
  '#be185d'
]

function hashStringToSeed(str) {
  let h = 0
  for (let i = 0; i < str.length; i++) {
    h = (Math.imul(31, h) + str.charCodeAt(i)) | 0
  }
  return Math.abs(h)
}

function strokeColorForDraftEdge(i, fk, tk) {
  const seed = hashStringToSeed(`${fk}\0${tk}\0${i}`)
  return BRANCHING_EDGE_COLORS[seed % BRANCHING_EDGE_COLORS.length]
}

function rebuildFlowFromDraft() {
  const editingSk = String(editingCardStableKey.value ?? '').trim()
  const nodes = draftNodes.value
    .map((n, idx) => {
      const sk = String(n.stableKey ?? '').trim()
      if (!sk) return null
      const start = String(draftStartStableKey.value ?? '').trim()
      const isCardEditing = editingSk !== '' && editingSk === sk
      return {
        id: sk,
        type: 'branching',
        position: { x: Number(n.posX) || 0, y: Number(n.posY) || 0 },
        zIndex: isCardEditing ? 50000 : undefined,
        class: isCardEditing ? 'branching-vue-node--card-editing' : undefined,
        data: {
          num: idx + 1,
          title: (n.title || '').trim() || 'Untitled',
          typeLabel: questionTypeLabel(String(n.questionType ?? '').trim()),
          stableKey: sk,
          isStart: start === sk,
          isSelected: selectedStableKey.value === sk,
          isRun: runHighlightStableKey.value === sk,
          hideSourceHandle: String(n.questionType ?? '').toUpperCase() === 'END'
        }
      }
    })
    .filter(Boolean)

  const edges = []
  draftEdges.value.forEach((e, i) => {
    const fk = String(e.fromStableKey ?? '').trim()
    const tk = String(e.toStableKey ?? '').trim()
    if (!fk || !tk) return
    const stroke = strokeColorForDraftEdge(i, fk, tk)
    edges.push({
      id: `draftedge-${i}`,
      source: fk,
      target: tk,
      label: edgeRouteLabel(e),
      type: 'smoothstep',
      style: (ed) => ({
        stroke,
        strokeWidth: ed.selected ? 6 : 2.5,
        strokeLinecap: 'round',
        strokeLinejoin: 'round'
      }),
      markerEnd: {
        type: MarkerType.ArrowClosed,
        color: stroke,
        width: 22,
        height: 22
      }
    })
  })
  flowVueNodes.value = nodes
  flowVueEdges.value = edges
}

watch(
  [draftNodes, draftEdges, draftStartStableKey, selectedStableKey, runHighlightStableKey],
  () => {
    if (isFlowDragging.value) return
    rebuildFlowFromDraft()
  },
  { deep: true, immediate: true }
)


function onFlowDragStart() {
  isFlowDragging.value = true
}

function onFlowDragStop({ node }) {
  const n = draftNodes.value.find((x) => x.stableKey?.trim() === node.id)
  if (n) {
    n.posX = Math.round(node.position.x)
    n.posY = Math.round(node.position.y)
  }
  isFlowDragging.value = false
}

function onFlowNodeClick({ node, event }) {
  event?.stopPropagation?.()
  if (!node?.id) return
  flowMapActivated.value = true
  selectedStableKey.value = String(node.id)
  nextTick(() => {
    mapEditorEl.value?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  })
}

function onFlowPaneClick() {
  flowMapActivated.value = true
  selectedStableKey.value = ''
}

function onFlowWrapPointerDownCapture() {
  flowMapActivated.value = true
}

function onDocumentPointerDownDeactivateMap(e) {
  const el = flowWrapRef.value
  const t = e.target
  if (!(el instanceof HTMLElement) || !(t instanceof Node)) return
  if (!el.contains(t)) {
    flowMapActivated.value = false
  }
}

function isFlowValidConnection(connection) {
  const s = String(connection?.source ?? '').trim()
  const t = String(connection?.target ?? '').trim()
  if (!s || !t || s === t) return false
  const fromNode = draftNodes.value.find((n) => n.stableKey?.trim() === s)
  if (!fromNode) return false
  const qt = String(fromNode.questionType ?? '').toUpperCase()
  if (qt === 'END') return false
  return true
}

function edgeAlreadyExists(fk, tk, kind, value) {
  const ck = String(kind ?? '').toUpperCase()
  const cv = String(value ?? '').trim()
  return draftEdges.value.some(
    (e) =>
      e.fromStableKey === fk &&
      e.toStableKey === tk &&
      String(e.conditionKind ?? '').toUpperCase() === ck &&
      String(e.conditionValue ?? '').trim() === cv
  )
}

/** Used for non-modal connects (e.g. TEXT). Yes/No and CHOICE use the connect dialog. */
function inferConnectBranch(fk) {
  const fromNode = draftNodes.value.find((n) => n.stableKey?.trim() === fk)
  if (!fromNode) return null
  const qt = String(fromNode.questionType ?? '').toUpperCase()
  if (qt === 'END') return null
  if (qt === 'TEXT') {
    return { kind: 'ALWAYS', value: '', sortOrder: 0 }
  }
  return { kind: 'ALWAYS', value: '', sortOrder: draftEdges.value.length }
}

function applyDraftEdgeForConnect(fk, tk, inf) {
  const ck = String(inf.kind ?? '').toUpperCase()
  const cv = String(inf.value ?? '').trim()

  if (edgeAlreadyExists(fk, tk, ck, cv)) return

  if (ck === 'YES') {
    draftEdges.value = draftEdges.value.filter(
      (e) => !(e.fromStableKey === fk && String(e.conditionKind ?? '').toUpperCase() === 'YES')
    )
  } else if (ck === 'NO') {
    draftEdges.value = draftEdges.value.filter(
      (e) => !(e.fromStableKey === fk && String(e.conditionKind ?? '').toUpperCase() === 'NO')
    )
  } else if (ck === 'OPTION') {
    draftEdges.value = draftEdges.value.filter(
      (e) =>
        !(
          e.fromStableKey === fk &&
          String(e.conditionKind ?? '').toUpperCase() === 'OPTION' &&
          String(e.conditionValue ?? '').trim() === cv
        )
    )
  } else if (ck === 'ALWAYS' && questionTypeForFromKey(fk) === 'TEXT') {
    draftEdges.value = draftEdges.value.filter(
      (e) => !(e.fromStableKey === fk && String(e.conditionKind ?? '').toUpperCase() === 'ALWAYS')
    )
  } else if (ck === 'ALWAYS' && questionTypeForFromKey(fk) === 'CHOICE') {
    draftEdges.value = draftEdges.value.filter(
      (e) => !(e.fromStableKey === fk && String(e.conditionKind ?? '').toUpperCase() === 'ALWAYS')
    )
  }

  draftEdges.value.push({
    fromStableKey: fk,
    toStableKey: tk,
    sortOrder: inf.sortOrder ?? 0,
    conditionKind: ck,
    conditionValue: ck === 'OPTION' ? cv : ''
  })
}

function openConnectModalForEdge(fk, tk) {
  const fromNode = draftNodes.value.find((n) => n.stableKey?.trim() === fk)
  if (!fromNode) return
  const qt = String(fromNode.questionType ?? '').toUpperCase()
  pendingConnect.value = { fk, tk }
  if (qt === 'YES_NO') {
    const hasYes = draftEdges.value.some(
      (e) => e.fromStableKey === fk && String(e.conditionKind ?? '').toUpperCase() === 'YES'
    )
    const hasNo = draftEdges.value.some(
      (e) => e.fromStableKey === fk && String(e.conditionKind ?? '').toUpperCase() === 'NO'
    )
    if (!hasYes) connectModalYesNoKind.value = 'YES'
    else if (!hasNo) connectModalYesNoKind.value = 'NO'
    else connectModalYesNoKind.value = 'ALWAYS'
  } else if (qt === 'CHOICE') {
    const rows = fromNode.choiceRows || []
    const used = new Set(
      draftEdges.value
        .filter((e) => e.fromStableKey === fk && String(e.conditionKind ?? '').toUpperCase() === 'OPTION')
        .map((e) => String(e.conditionValue ?? '').trim())
    )
    const firstUnused = rows.find((r) => {
      const id = String(r.id ?? '').trim()
      return id && !used.has(id)
    })
    connectModalChoiceValue.value = firstUnused ? String(firstUnused.id).trim() : '__ALWAYS__'
  }
  connectModalOpen.value = true
  nextTick(() => {
    rebuildFlowFromDraft()
    connectModalEl.value?.focus?.()
  })
}

function confirmConnectModal() {
  const p = pendingConnect.value
  if (!p) return
  const fromNode = draftNodes.value.find((n) => n.stableKey?.trim() === p.fk)
  const qt = String(fromNode?.questionType ?? '').toUpperCase()
  let inf = null
  if (qt === 'YES_NO') {
    const kind = String(connectModalYesNoKind.value ?? '').toUpperCase()
    const sortOrder =
      kind === 'YES' ? 0 : kind === 'NO' ? 1 : draftEdges.value.filter((e) => e.fromStableKey === p.fk).length
    inf = { kind, value: '', sortOrder }
  } else if (qt === 'CHOICE') {
    if (connectModalChoiceValue.value === '__ALWAYS__') {
      inf = {
        kind: 'ALWAYS',
        value: '',
        sortOrder: draftEdges.value.filter((e) => e.fromStableKey === p.fk).length
      }
    } else {
      const cid = String(connectModalChoiceValue.value ?? '').trim()
      const rows = fromNode.choiceRows || []
      const sortIdx = Math.max(0, rows.findIndex((r) => String(r.id).trim() === cid))
      inf = { kind: 'OPTION', value: cid, sortOrder: sortIdx * 10 }
    }
  }
  connectModalOpen.value = false
  pendingConnect.value = null
  if (inf) applyDraftEdgeForConnect(p.fk, p.tk, inf)
}

function cancelConnectModal() {
  connectModalOpen.value = false
  pendingConnect.value = null
  nextTick(() => rebuildFlowFromDraft())
}

function onConnectModalEscape(e) {
  if (e.key === 'Escape' && connectModalOpen.value) {
    e.preventDefault()
    cancelConnectModal()
  }
}

onMounted(() => {
  window.addEventListener('keydown', onConnectModalEscape)
  document.addEventListener('pointerdown', onDocumentPointerDownDeactivateMap, true)
})
onUnmounted(() => {
  window.removeEventListener('keydown', onConnectModalEscape)
  document.removeEventListener('pointerdown', onDocumentPointerDownDeactivateMap, true)
})

function onFlowConnect(connection) {
  const fk = String(connection?.source ?? '').trim()
  const tk = String(connection?.target ?? '').trim()
  const fromNode = draftNodes.value.find((n) => n.stableKey?.trim() === fk)
  if (!fromNode) return
  const qt = String(fromNode.questionType ?? '').toUpperCase()
  if (qt === 'YES_NO' || qt === 'CHOICE') {
    openConnectModalForEdge(fk, tk)
    return
  }
  const inf = inferConnectBranch(fk)
  if (!inf) return
  applyDraftEdgeForConnect(fk, tk, inf)
}

function parseDraftEdgeIndex(edgeId) {
  const m = /^draftedge-(\d+)$/.exec(String(edgeId ?? ''))
  if (!m) return null
  const idx = Number(m[1])
  return Number.isFinite(idx) ? idx : null
}

function onFlowEdgesChange(changes) {
  if (!Array.isArray(changes) || !changes.length) return
  const idxs = []
  for (const ch of changes) {
    if (ch.type !== 'remove') continue
    const idx = parseDraftEdgeIndex(ch.id)
    if (idx != null) idxs.push(idx)
  }
  if (!idxs.length) return
  idxs.sort((a, b) => b - a)
  for (const idx of idxs) {
    if (idx >= 0 && idx < draftEdges.value.length) draftEdges.value.splice(idx, 1)
  }
}

function selectQuestionForMap(node) {
  const k = String(node.stableKey ?? '').trim()
  if (!k) return
  selectedStableKey.value = k
}

function questionTypeForFromKey(fromKey) {
  const fk = String(fromKey ?? '').trim()
  const n = draftNodes.value.find((x) => x.stableKey?.trim() === fk)
  return String(n?.questionType ?? '').trim().toUpperCase()
}

function isManagedEdge(edge) {
  const fk = String(edge.fromStableKey ?? '').trim()
  const qt = questionTypeForFromKey(fk)
  const ck = String(edge.conditionKind ?? '').trim().toUpperCase()
  if (qt === 'YES_NO' && (ck === 'YES' || ck === 'NO')) return true
  if (qt === 'CHOICE' && ck === 'OPTION') return true
  if (qt === 'TEXT' && ck === 'ALWAYS') return true
  return false
}

const manualEdgeRows = computed(() =>
  draftEdges.value.map((edge, idx) => ({ edge, idx })).filter((row) => !isManagedEdge(row.edge))
)

function branchTargetOptions(nodeKey) {
  const exclude = String(nodeKey ?? '').trim()
  return draftStableKeyOptions.value.filter((k) => k !== exclude)
}

function yesTarget(nodeKey) {
  const k = String(nodeKey ?? '').trim()
  const e = draftEdges.value.find((x) => x.fromStableKey === k && String(x.conditionKind).toUpperCase() === 'YES')
  return e?.toStableKey ?? ''
}

function yesTargetNo(nodeKey) {
  const k = String(nodeKey ?? '').trim()
  const e = draftEdges.value.find((x) => x.fromStableKey === k && String(x.conditionKind).toUpperCase() === 'NO')
  return e?.toStableKey ?? ''
}

function setYesNoTarget(nodeKey, branch, toKey) {
  const nk = String(nodeKey ?? '').trim()
  if (!nk) return
  const kind = branch === 'yes' ? 'YES' : 'NO'
  const order = branch === 'yes' ? 0 : 1
  draftEdges.value = draftEdges.value.filter((e) => !(e.fromStableKey === nk && String(e.conditionKind).toUpperCase() === kind))
  if (String(toKey ?? '').trim()) {
    draftEdges.value.push({
      fromStableKey: nk,
      toStableKey: String(toKey).trim(),
      sortOrder: order,
      conditionKind: kind,
      conditionValue: ''
    })
  }
}

function choiceTarget(nodeKey, choiceId) {
  const nk = String(nodeKey ?? '').trim()
  const cid = String(choiceId ?? '').trim()
  const e = draftEdges.value.find(
    (x) =>
      x.fromStableKey === nk &&
      String(x.conditionKind).toUpperCase() === 'OPTION' &&
      String(x.conditionValue ?? '').trim() === cid
  )
  return e?.toStableKey ?? ''
}

function setChoiceTarget(nodeKey, choiceId, toKey) {
  const nk = String(nodeKey ?? '').trim()
  const cid = String(choiceId ?? '').trim()
  if (!nk || !cid) return
  draftEdges.value = draftEdges.value.filter(
    (e) =>
      !(
        e.fromStableKey === nk &&
        String(e.conditionKind).toUpperCase() === 'OPTION' &&
        String(e.conditionValue ?? '').trim() === cid
      )
  )
  const node = draftNodes.value.find((n) => n.stableKey?.trim() === nk)
  const sortIdx = Math.max(0, node?.choiceRows?.findIndex((r) => String(r.id).trim() === cid) ?? 0)
  if (String(toKey ?? '').trim()) {
    draftEdges.value.push({
      fromStableKey: nk,
      toStableKey: String(toKey).trim(),
      sortOrder: sortIdx * 10,
      conditionKind: 'OPTION',
      conditionValue: cid
    })
  }
}

function textNext(nodeKey) {
  const k = String(nodeKey ?? '').trim()
  const e = draftEdges.value.find((x) => x.fromStableKey === k && String(x.conditionKind).toUpperCase() === 'ALWAYS')
  return e?.toStableKey ?? ''
}

function setTextNext(nodeKey, toKey) {
  const nk = String(nodeKey ?? '').trim()
  if (!nk) return
  draftEdges.value = draftEdges.value.filter(
    (e) => !(e.fromStableKey === nk && String(e.conditionKind).toUpperCase() === 'ALWAYS')
  )
  if (String(toKey ?? '').trim()) {
    draftEdges.value.push({
      fromStableKey: nk,
      toStableKey: String(toKey).trim(),
      sortOrder: 0,
      conditionKind: 'ALWAYS',
      conditionValue: ''
    })
  }
}

function addChoiceRow(node) {
  if (!node.choiceRows) node.choiceRows = []
  node.choiceRows.push({ id: `c_${Date.now()}`, label: '' })
}

function removeChoiceRow(node, ridx) {
  const row = node.choiceRows?.[ridx]
  if (row?.id) {
    const nk = node.stableKey?.trim()
    const cid = String(row.id).trim()
    draftEdges.value = draftEdges.value.filter(
      (e) =>
        !(
          e.fromStableKey === nk &&
          String(e.conditionKind).toUpperCase() === 'OPTION' &&
          String(e.conditionValue ?? '').trim() === cid
        )
    )
  }
  node.choiceRows?.splice(ridx, 1)
}

function removeDraftEdge(edge) {
  const i = draftEdges.value.indexOf(edge)
  if (i >= 0) draftEdges.value.splice(i, 1)
}

function onQuestionTypeChange(node) {
  const k = node.stableKey?.trim()
  if (!k) return
  draftEdges.value = draftEdges.value.filter((e) => e.fromStableKey !== k)
  const t = String(node.questionType ?? '').trim().toUpperCase()
  if (t === 'CHOICE') {
    if (!node.choiceRows?.length) {
      node.choiceRows = [{ id: `opt_${Date.now()}`, label: 'Option 1' }]
    }
  } else {
    node.choiceRows = []
  }
}

function getDraftNode(stableKey) {
  const k = String(stableKey ?? '').trim()
  if (!k) return null
  return draftNodes.value.find((n) => String(n.stableKey ?? '').trim() === k) ?? null
}

function setEditingCardStableKey(sk) {
  editingCardStableKey.value = String(sk ?? '').trim()
  const k = editingCardStableKey.value
  const nodes = flowVueNodes.value
  if (!nodes.length) return
  /* Patch z-index/class only — do not run a full draft rebuild here, or the custom node remounts and loses local edit state. */
  flowVueNodes.value = nodes.map((node) => {
    const is = Boolean(k) && String(node.id) === k
    return {
      ...node,
      zIndex: is ? 50000 : undefined,
      class: is ? 'branching-vue-node--card-editing' : undefined
    }
  })
}

/** Snapshot draft fields + outgoing edges for inline card edit cancel. */
function captureEditSnapshot(stableKey) {
  const n = getDraftNode(stableKey)
  if (!n) return null
  const sk = String(stableKey ?? '').trim()
  return {
    node: {
      title: n.title,
      body: n.body,
      questionType: n.questionType,
      choicesJson: n.choicesJson,
      choiceRows: JSON.parse(JSON.stringify(n.choiceRows || []))
    },
    outgoingEdges: draftEdges.value
      .filter((e) => String(e.fromStableKey ?? '').trim() === sk)
      .map((e) => ({
        fromStableKey: e.fromStableKey,
        toStableKey: e.toStableKey,
        sortOrder: e.sortOrder,
        conditionKind: e.conditionKind,
        conditionValue: e.conditionValue != null ? String(e.conditionValue) : ''
      }))
  }
}

function restoreEditSnapshot(stableKey, snapshot) {
  if (!snapshot) return
  const n = getDraftNode(stableKey)
  if (!n) return
  const sk = String(stableKey ?? '').trim()
  n.title = snapshot.node.title
  n.body = snapshot.node.body
  n.questionType = snapshot.node.questionType
  n.choicesJson = snapshot.node.choicesJson
  n.choiceRows = JSON.parse(JSON.stringify(snapshot.node.choiceRows || []))
  draftEdges.value = draftEdges.value.filter((e) => String(e.fromStableKey ?? '').trim() !== sk)
  for (const e of snapshot.outgoingEdges) {
    draftEdges.value.push({ ...e })
  }
}

provide('branchingMapContext', {
  getDraftNode,
  patchDraftNode,
  questionTypes,
  questionTypeLabel,
  stepSelectLabel,
  branchTargetOptions,
  yesTarget,
  yesTargetNo,
  setYesNoTarget,
  choiceTarget,
  setChoiceTarget,
  textNext,
  setTextNext,
  addChoiceRow,
  removeChoiceRow,
  onQuestionTypeChange,
  editingCardStableKey,
  setEditingCardStableKey,
  captureEditSnapshot,
  restoreEditSnapshot
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
  draftNodes.value = g.nodes.map((n) => {
    const qt = String(n.questionType ?? 'TEXT').trim().toUpperCase()
    let choiceRows = []
    if (qt === 'CHOICE') {
      if (Array.isArray(n.choices) && n.choices.length) {
        choiceRows = n.choices
          .map((c) => ({
            id: String(c.id != null ? c.id : '').trim(),
            label: String(c.label != null ? c.label : c.id != null ? c.id : '').trim()
          }))
          .filter((c) => c.id)
      } else {
        choiceRows = parseChoicesLocal(choicesToJson(n.choices))
      }
    }
    return {
      stableKey: n.stableKey,
      title: n.title || '',
      body: n.body || '',
      questionType: n.questionType || 'TEXT',
      choicesJson: choicesToJson(n.choices),
      choiceRows: qt === 'CHOICE' ? choiceRows : [],
      posX: n.posX ?? 0,
      posY: n.posY ?? 0
    }
  })
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
  resetRun()
  initDraftFromGraph()
}

function addDraftNode() {
  const nodes = draftNodes.value
  let posX = 120
  let posY = 120
  if (nodes.length) {
    const maxX = Math.max(...nodes.map((n) => Number(n.posX) || 0))
    const sumY = nodes.reduce((s, n) => s + (Number(n.posY) || 0), 0)
    posX = maxX + 220
    posY = Math.round(sumY / nodes.length)
  }
  const key = `step_${Date.now()}`
  draftNodes.value.push({
    stableKey: key,
    title: 'New step',
    body: '',
    questionType: 'TEXT',
    choicesJson: '',
    choiceRows: [],
    posX,
    posY
  })
  if (draftNodes.value.length === 1) {
    draftStartStableKey.value = key
  }
  selectedStableKey.value = key
  nextTick(() => {
    mapEditorEl.value?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  })
}

/** Approximate Vue Flow node size for BranchingFlowNode (dagre uses center coords; we convert to top-left). */
const AUTO_LAYOUT_NODE_W = 200
const AUTO_LAYOUT_NODE_H = 76

function gridLayoutFallback(keys) {
  const cols = Math.max(1, Math.ceil(Math.sqrt(keys.length)))
  keys.forEach((k, i) => {
    const n = draftNodes.value.find((x) => String(x.stableKey ?? '').trim() === k)
    if (!n) return
    const col = i % cols
    const row = Math.floor(i / cols)
    n.posX = 48 + col * 260
    n.posY = 48 + row * 140
  })
}

function runAutoLayout() {
  const nodes = draftNodes.value
  if (!nodes.length) return

  const keyList = []
  const seen = new Set()
  for (const n of nodes) {
    const k = String(n.stableKey ?? '').trim()
    if (!k || seen.has(k)) continue
    seen.add(k)
    keyList.push(k)
  }
  if (!keyList.length) return

  const keySet = new Set(keyList)
  const g = new dagre.graphlib.Graph()
  g.setDefaultEdgeLabel(() => ({}))
  g.setGraph({
    rankdir: 'LR',
    align: 'UL',
    nodesep: 56,
    ranksep: 88,
    marginx: 32,
    marginy: 32,
    ranker: 'network-simplex'
  })

  for (const k of keySet) {
    g.setNode(k, { width: AUTO_LAYOUT_NODE_W, height: AUTO_LAYOUT_NODE_H })
  }

  const edgePairs = new Set()
  for (const e of draftEdges.value) {
    const f = String(e.fromStableKey ?? '').trim()
    const t = String(e.toStableKey ?? '').trim()
    if (!f || !t || f === t || !keySet.has(f) || !keySet.has(t)) continue
    const pk = `${f}\0${t}`
    if (edgePairs.has(pk)) continue
    edgePairs.add(pk)
    g.setEdge(f, t)
  }

  try {
    dagre.layout(g)
  } catch (err) {
    console.warn('Auto layout failed, using grid fallback', err)
    gridLayoutFallback(keyList)
    layoutFitTick.value++
    return
  }

  for (const n of nodes) {
    const k = String(n.stableKey ?? '').trim()
    if (!k || !keySet.has(k)) continue
    const pos = g.node(k)
    if (pos && typeof pos.x === 'number' && typeof pos.y === 'number') {
      n.posX = Math.round(pos.x - AUTO_LAYOUT_NODE_W / 2)
      n.posY = Math.round(pos.y - AUTO_LAYOUT_NODE_H / 2)
    }
  }

  layoutFitTick.value++
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
  if (key && selectedStableKey.value === key) {
    selectedStableKey.value = ''
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
  for (const n of draftNodes.value) {
    const sk = String(n.stableKey ?? '').trim()
    const qt = String(n.questionType ?? 'TEXT').trim().toUpperCase()
    if (qt === 'CHOICE') {
      if (!n.choiceRows?.length) return `Step "${sk}": add at least one choice option.`
      for (const row of n.choiceRows) {
        const id = String(row.id ?? '').trim()
        if (!id) return `Step "${sk}": each choice needs an option id.`
        const ok = draftEdges.value.some(
          (e) =>
            e.fromStableKey === sk &&
            String(e.conditionKind).toUpperCase() === 'OPTION' &&
            String(e.conditionValue ?? '').trim() === id &&
            String(e.toStableKey ?? '').trim()
        )
        if (!ok) return `Step "${sk}": choose a next step for choice "${row.label || id}".`
      }
    }
    if (qt === 'YES_NO') {
      const yes = draftEdges.value.find((e) => e.fromStableKey === sk && String(e.conditionKind).toUpperCase() === 'YES')
      const no = draftEdges.value.find((e) => e.fromStableKey === sk && String(e.conditionKind).toUpperCase() === 'NO')
      if (!yes?.toStableKey || !no?.toStableKey) {
        return `Step "${sk}": set both If Yes and If No next steps.`
      }
    }
    if (qt === 'TEXT') {
      const ok = draftEdges.value.some(
        (e) =>
          e.fromStableKey === sk &&
          String(e.conditionKind).toUpperCase() === 'ALWAYS' &&
          String(e.toStableKey ?? '').trim()
      )
      if (!ok) return `Step "${sk}": choose a next step after the text prompt.`
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
  const nodes = draftNodes.value.map((n) => {
    const qt = String(n.questionType ?? 'TEXT').trim().toUpperCase()
    let choicesJson = null
    if (qt === 'CHOICE' && Array.isArray(n.choiceRows) && n.choiceRows.length) {
      const rows = n.choiceRows
        .map((r) => ({
          id: String(r.id ?? '').trim(),
          label: String(r.label ?? r.id ?? '').trim() || String(r.id ?? '').trim()
        }))
        .filter((r) => r.id)
      if (rows.length) choicesJson = JSON.stringify(rows)
    }
    return {
      stableKey: String(n.stableKey).trim(),
      title: n.title ?? '',
      body: String(n.body ?? '').trim() === '' ? null : String(n.body),
      questionType: String(n.questionType ?? 'TEXT').trim(),
      choicesJson,
      posX: Number(n.posX) || 0,
      posY: Number(n.posY) || 0
    }
  })
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
      saveSuccess.value = 'Saved.'
    }
  } catch (e) {
    saveError.value = formatSaveError(e)
  } finally {
    saving.value = false
  }
}

function formatResolveError(err) {
  const d = err.response?.data
  if (d && typeof d === 'object') {
    const parts = [d.message, d.detail, d.error].filter(Boolean)
    if (parts.length) return parts.join(' — ')
  }
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
      resetRun()
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
  previewModalOpen.value = true
}

function finishAtEnd(n) {
  runFinished.value = true
  endTitle.value = n.title || 'Done'
  endBody.value = n.body || ''
}

function clearPreviewSessionState() {
  runStarted.value = false
  runFinished.value = false
  currentNodeId.value = null
  pathLabels.value = []
  resolveError.value = ''
  startRunError.value = ''
  textAnswer.value = ''
}

function resetRun() {
  clearPreviewSessionState()
  previewModalOpen.value = false
}

function closePreviewModal() {
  previewModalOpen.value = false
}

function onPreviewModalHidden() {
  clearPreviewSessionState()
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

<style scoped>
.branching-demo--wide {
  margin-left: 0;
  margin-right: 0;
  width: 100%;
  max-width: none;
}

.branching-demo--shell {
  --bd-canvas: #e4e8f0;
  --bd-canvas-deep: #d7dde8;
  padding-bottom: 2.5rem;
}

.branching-demo__page-head {
  padding-bottom: 0.25rem;
  border-bottom: 1px solid var(--bs-border-color-translucent);
}

.branching-demo__eyebrow {
  letter-spacing: 0.06em;
  color: var(--bs-secondary-color);
  font-size: 0.7rem;
}

.branching-demo__title {
  letter-spacing: -0.02em;
  color: var(--bs-emphasis-color);
}

.branching-demo__lede {
  max-width: 42rem;
  line-height: 1.5;
}

.branching-demo__intro {
  border-left: 3px solid var(--bs-primary);
  padding: 1rem 1.25rem;
  background: var(--bs-primary-bg-subtle);
  border-radius: 0 0.5rem 0.5rem 0;
  color: var(--bs-body-color);
  line-height: 1.55;
}

.branching-demo__loading {
  background: var(--bs-secondary-bg);
  border: 1px dashed var(--bs-border-color);
}

.branching-demo__spinner {
  width: 0.65rem;
  height: 0.65rem;
  vertical-align: middle;
  background: var(--bs-primary);
  animation: branching-demo-pulse 0.9s ease-in-out infinite alternate;
}

@keyframes branching-demo-pulse {
  from {
    opacity: 0.35;
    transform: scale(0.85);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.branching-demo__main {
  gap: 1.75rem !important;
}

.branching-demo__save-bar {
  z-index: 1040;
  background: linear-gradient(180deg, var(--bs-body-bg) 0%, color-mix(in srgb, var(--bs-body-bg) 92%, var(--bs-secondary-bg)) 100%);
  border: 1px solid var(--bs-border-color-translucent);
  box-shadow:
    0 1px 2px rgba(15, 23, 42, 0.04),
    0 8px 24px rgba(15, 23, 42, 0.06);
}

.branching-demo__save-bar-icon {
  width: 2.25rem;
  height: 2.25rem;
  background: linear-gradient(135deg, var(--bs-primary) 0%, color-mix(in srgb, var(--bs-primary) 70%, var(--bs-primary-bg-subtle)) 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.branching-demo__save-btn {
  min-width: 10.5rem;
  font-weight: 600;
  letter-spacing: 0.01em;
}

.branching-demo__surface {
  border-radius: 0.75rem;
  overflow: hidden;
}

.branching-demo__card-head {
  background: linear-gradient(180deg, var(--bs-body-bg) 0%, var(--bs-secondary-bg) 100%);
  border-bottom: 1px solid var(--bs-border-color-translucent) !important;
}

.branching-demo__card-head--muted {
  background: color-mix(in srgb, var(--bs-secondary-bg) 88%, var(--bs-body-bg));
}

.branching-demo__section-dot {
  width: 0.5rem;
  height: 0.5rem;
  background: var(--bs-primary);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--bs-primary) 22%, transparent);
}

.branching-demo__section-dot--muted {
  background: var(--bs-secondary-color);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--bs-secondary-color) 22%, transparent);
}

.branching-demo__flow-wrap {
  width: 100%;
  margin-left: auto;
  margin-right: auto;
  height: min(62vh, 880px);
  min-height: min(48vh, 448px);
  padding: 0.75rem 1rem 1rem;
  box-sizing: border-box;
  background: linear-gradient(180deg, var(--bd-canvas) 0%, var(--bd-canvas-deep) 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.35);
}

@media (min-width: 768px) {
  .branching-demo__flow-wrap {
    padding: 1rem 1.5rem 1.25rem;
  }
}

.branching-demo__vue-flow {
  width: 100%;
  height: 100%;
  min-height: min(46vh, 416px);
  border-radius: 0.5rem;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.08);
}

.branching-demo__minimap {
  bottom: 0.5rem;
  right: 0.5rem;
}

.branching-demo__map-activate-hint {
  z-index: 9;
  pointer-events: none;
  background: var(--bs-body-bg);
  border: 1px solid var(--bs-border-color);
  color: var(--bs-secondary-color);
  white-space: nowrap;
  max-width: calc(100% - 2rem);
  text-align: center;
}

.branching-demo__vue-flow.branching-demo--map-inactive .vue-flow__controls,
.branching-demo__vue-flow.branching-demo--map-inactive .vue-flow__minimap {
  pointer-events: none;
  opacity: 0.48;
}

.branching-demo__map-footnote {
  background: color-mix(in srgb, var(--bs-secondary-bg) 65%, var(--bs-body-bg));
  border-top: 1px solid var(--bs-border-color-translucent) !important;
  line-height: 1.55;
}

/* Bootstrap’s default kbd is light-on-dark; we use a light keycap here — force readable (dark) label text. */
.branching-demo__map-footnote-kbd {
  padding: 0.1rem 0.35rem;
  font-size: 0.8em;
  color: var(--bs-body-color);
  background-color: var(--bs-secondary-bg);
  border: 1px solid var(--bs-border-color);
  border-radius: 0.25rem;
}

.branching-demo__map-editor {
  max-height: min(32vh, 416px);
  overflow-y: auto;
  background: var(--bs-body-bg);
}

.branching-demo__empty-hint {
  border: 1px dashed var(--bs-border-color);
  background: var(--bs-secondary-bg);
  line-height: 1.55;
}

.branching-demo__path-label {
  font-size: 0.65rem;
  letter-spacing: 0.06em;
  font-weight: 600;
  color: var(--bs-secondary-color);
}
.branching-demo__connect-modal {
  z-index: 1060;
  background: rgba(33, 37, 41, 0.45);
}
.branching-demo__question-card {
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
  border-color: var(--bs-border-color-translucent) !important;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}
.branching-demo__question-details > summary {
  list-style: none;
}
.branching-demo__question-details > summary::-webkit-details-marker {
  display: none;
}
.branching-demo__question-details > summary::before {
  content: '▸';
  display: inline-block;
  margin-right: 0.5rem;
  font-size: 2rem;
  font-weight: 700;
  line-height: 1;
  vertical-align: middle;
  color: var(--bs-primary);
  transition: transform 0.2s ease, color 0.15s ease;
  flex-shrink: 0;
}
.branching-demo__question-details[open] > summary::before {
  transform: rotate(90deg);
}
.branching-demo__question-card:hover .branching-demo__question-details > summary::before {
  color: var(--bs-primary-text-emphasis, var(--bs-primary));
}
.branching-demo__question-summary {
  user-select: none;
  background-color: var(--bs-secondary-bg);
  border-bottom: 1px solid var(--bs-border-color);
}
.branching-demo__question-details[open] > .branching-demo__question-summary {
  border-bottom-color: var(--bs-border-color-translucent);
}
.branching-demo__question-card:hover {
  border-color: var(--bs-border-color) !important;
  box-shadow: 0 0.125rem 0.25rem rgba(0, 0, 0, 0.06);
}
.branching-demo__question-card--active {
  border-color: var(--bs-primary) !important;
  box-shadow: 0 0 0 1px var(--bs-primary);
}
</style>

<style>
/* Main map canvas background (Vue Flow fills this area; keep in sync with .branching-demo__flow-wrap) */
.branching-demo__vue-flow.vue-flow {
  background-color: #e4e8f0;
}

/* While a map card is in inline-edit mode, panels (controls, minimap, tools) sit above the viewport by default; send them behind so the node stays unobstructed. */
.branching-demo__vue-flow.branching-demo--card-editing .vue-flow__panel {
  z-index: 0 !important;
  opacity: 0.35;
  pointer-events: none;
  transition: opacity 0.15s ease;
}
.branching-demo__vue-flow.branching-demo--card-editing .vue-flow__connectionline {
  z-index: 0 !important;
}

.branching-demo__vue-flow .vue-flow__node.branching-vue-node--card-editing {
  z-index: 50000 !important;
}

/* Vue Flow edges (unscoped: SVG under .branching-demo__vue-flow). Stroke colors come from edge style(). */
.branching-demo__vue-flow .vue-flow__edge-path {
  transition: stroke-width 0.12s ease, filter 0.12s ease;
}
.branching-demo__vue-flow .vue-flow__edge.selected .vue-flow__edge-path,
.branching-demo__vue-flow .vue-flow__edge:focus .vue-flow__edge-path,
.branching-demo__vue-flow .vue-flow__edge:focus-visible .vue-flow__edge-path {
  filter: drop-shadow(0 0 5px rgba(0, 0, 0, 0.22));
}
.branching-demo__vue-flow .vue-flow__edge.selected .vue-flow__edge-text,
.branching-demo__vue-flow .vue-flow__edge:focus .vue-flow__edge-text {
  fill: #1a1d26;
  font-weight: 800;
  font-size: 0.82rem;
}
.branching-demo__vue-flow .vue-flow__edge.selected .vue-flow__edge-textbg,
.branching-demo__vue-flow .vue-flow__edge:focus .vue-flow__edge-textbg {
  fill: rgba(255, 255, 255, 0.97);
  stroke: rgba(0, 0, 0, 0.12);
  stroke-width: 2;
}
.branching-demo__vue-flow .vue-flow__edge-text {
  font-size: 0.7rem;
  font-weight: 600;
}

/* Vue Flow default control buttons are 16×16px — expand and label them. */
.branching-demo__vue-flow .vue-flow__controls {
  box-shadow: 0 0.125rem 0.5rem rgba(0, 0, 0, 0.12);
  border-radius: 0.375rem;
  overflow: hidden;
  border: 1px solid var(--bs-border-color, #dee2e6);
  background: var(--bs-body-bg, #fff);
}
.branching-demo__vue-flow .vue-flow__controls-button {
  width: auto !important;
  min-width: 4.5rem;
  height: auto !important;
  min-height: 2.75rem;
  padding: 0.35rem 0.5rem !important;
  border-bottom: 1px solid var(--bs-border-color, #eee) !important;
  background: var(--bs-body-bg, #fefefe) !important;
}
.branching-demo__vue-flow .vue-flow__controls-button:last-child {
  border-bottom: none !important;
}
.branching-demo__vue-flow .vue-flow__controls-button:hover {
  background: var(--bs-secondary-bg, #f4f4f4) !important;
}
.branching-demo__vue-flow .vue-flow__controls-button:disabled .branching-demo__map-control-label,
.branching-demo__vue-flow .vue-flow__controls-button:disabled .branching-demo__map-control-glyph {
  opacity: 0.4;
}
.branching-demo__map-control {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.15rem;
  line-height: 1.05;
  text-align: center;
}
.branching-demo__map-control-glyph {
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--bs-body-color, #212529);
}
.branching-demo__map-control-glyph--fit {
  font-size: 1.05rem;
  font-weight: 600;
}
.branching-demo__map-control-label {
  font-size: 0.6rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--bs-secondary, #6c757d);
  max-width: 4.25rem;
  line-height: 1.15;
}
.branching-demo__map-control--fit .branching-demo__map-control-label {
  max-width: none;
}
</style>
