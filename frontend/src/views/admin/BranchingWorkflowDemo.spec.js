import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import api from '../../services/api'

const demoGraph = {
  versionId: 1,
  workflowId: 1,
  startNodeId: 10,
  nodes: [
    {
      id: 10,
      stableKey: 'q1',
      title: 'First question',
      body: 'Hint text',
      questionType: 'YES_NO',
      posX: 0,
      posY: 0
    },
    {
      id: 11,
      stableKey: 'end1',
      title: 'The end',
      body: '',
      questionType: 'END',
      posX: 200,
      posY: 0
    }
  ],
  edges: [
    {
      id: 1,
      fromNodeId: 10,
      toNodeId: 11,
      sortOrder: 0,
      conditionKind: 'YES',
      conditionValue: null
    },
    {
      id: 2,
      fromNodeId: 10,
      toNodeId: 11,
      sortOrder: 1,
      conditionKind: 'NO',
      conditionValue: null
    }
  ]
}

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ data: demoGraph })),
    put: vi.fn(() =>
      Promise.resolve({
        data: {
          ...demoGraph,
          versionId: 2
        }
      })
    ),
    post: vi.fn(() =>
      Promise.resolve({
        data: {
          finished: false,
          nextNode: {
            id: 11,
            title: 'The end',
            body: '',
            questionType: 'END'
          }
        }
      })
    )
  }
}))

/** Access the same mock instance the component uses (hoisted factory returns these fns). */
const apiMock = api

vi.mock('@vue-flow/core', () => ({
  VueFlow: {
    name: 'VueFlow',
    props: {
      nodes: { type: Array, default: () => [] },
      edges: { type: Array, default: () => [] },
      class: [String, Array, Object]
    },
    template: '<div class="vue-flow-stub"><slot /></div>'
  },
  MarkerType: { Arrow: 'arrow' },
  Panel: { template: '<div class="vue-flow-panel"><slot /></div>' },
  useVueFlow: () => ({
    fitView: vi.fn(),
    onNodesChange: vi.fn(),
    onEdgesChange: vi.fn()
  })
}))

vi.mock('@vue-flow/background', () => ({
  Background: { template: '<div />' }
}))
vi.mock('@vue-flow/controls', () => ({
  Controls: { template: '<div class="vue-flow-controls"><slot /></div>' }
}))
vi.mock('@vue-flow/minimap', () => ({
  MiniMap: { template: '<div />' }
}))

/** Renders in-document; mirrors BsModal by emitting `hidden` when v-model closes so parent can clear preview state. */
const BsModalStub = {
  name: 'BsModal',
  props: {
    modelValue: { type: Boolean, default: false },
    title: { type: String, default: '' },
    size: String,
    scrollable: Boolean
  },
  emits: ['update:modelValue', 'hidden'],
  watch: {
    modelValue(v, prev) {
      if (prev === true && v === false) {
        this.$emit('hidden')
      }
    }
  },
  template: `
    <div v-show="modelValue" class="bs-modal-stub" data-testid="preview-modal">
      <h2 class="modal-title-stub">{{ title }}</h2>
      <div class="modal-body-stub"><slot /></div>
      <div class="modal-footer-stub"><slot name="footer" /></div>
    </div>
  `
}

describe('BranchingWorkflowDemo', () => {
  let BranchingWorkflowDemo

  beforeEach(async () => {
    vi.resetModules()
    BranchingWorkflowDemo = (await import('./BranchingWorkflowDemo.vue')).default
  })

  afterEach(() => {
    document.body.innerHTML = ''
  })

  function mountDemo(overrides = {}) {
    return mount(BranchingWorkflowDemo, {
      attachTo: document.body,
      global: {
        stubs: {
          BsModal: BsModalStub,
          BranchingFlowNode: { template: '<div class="flow-node-stub" />' },
          BranchingFlowMapTools: { template: '<div class="map-tools-stub" />' },
          ...overrides.stubs
        }
      },
      ...overrides
    })
  }

  it('loads the demo graph and shows the sticky draft bar with preview and save actions', async () => {
    const wrapper = mountDemo()
    await flushPromises()
    await nextTick()

    expect(wrapper.text()).toMatch(/Branching workflow/)
    expect(wrapper.text()).toMatch(/Draft/)
    expect(wrapper.find('button').exists()).toBe(true)
    const buttons = wrapper.findAll('button')
    const labels = buttons.map((b) => b.text())
    expect(labels.some((t) => /Start preview/i.test(t))).toBe(true)
    expect(labels.some((t) => /Discard/i.test(t))).toBe(true)
    expect(labels.some((t) => /Save workflow/i.test(t))).toBe(true)
  })

  it('shows the flow map card and map footnote with Delete / Backspace key hints', async () => {
    const wrapper = mountDemo()
    await flushPromises()
    await nextTick()

    expect(wrapper.text()).toMatch(/Flow map/)
    expect(wrapper.text()).toMatch(/internal key/)
    expect(wrapper.text()).toMatch(/Delete/)
    expect(wrapper.text()).toMatch(/Backspace/)

    const kbds = wrapper.findAll('.branching-demo__map-footnote-kbd')
    expect(kbds.length).toBe(2)
    expect(kbds[0].text()).toBe('Delete')
    expect(kbds[1].text()).toBe('Backspace')
  })

  it('lists questions as collapsible details with a summary row per draft node', async () => {
    const wrapper = mountDemo()
    await flushPromises()
    await nextTick()

    const details = wrapper.findAll('details.branching-demo__question-details')
    expect(details.length).toBeGreaterThanOrEqual(1)
    expect(details[0].find('summary').text()).toMatch(/Question 1/)
  })

  it('opens the preview modal when Start preview succeeds', async () => {
    const wrapper = mountDemo()
    await flushPromises()
    await nextTick()

    const startBtn = wrapper.findAll('button').find((b) => b.text().includes('Start preview'))
    expect(startBtn).toBeTruthy()
    await startBtn.trigger('click')
    await nextTick()

    const modal = wrapper.find('[data-testid="preview-modal"]')
    expect(modal.isVisible()).toBe(true)
    expect(wrapper.find('.modal-title-stub').text()).toContain('Preview saved flow')
    expect(wrapper.text()).toMatch(/last saved/i)
  })

  it('activates map interactions after pointerdown on the flow wrap (hint disappears)', async () => {
    const wrapper = mountDemo()
    await flushPromises()
    await nextTick()

    const flowWrap = wrapper.get('.branching-demo__flow-wrap')
    const hint = wrapper.find('.branching-demo__map-activate-hint')
    expect(hint.exists()).toBe(true)

    await flowWrap.trigger('pointerdown')
    await nextTick()

    expect(wrapper.find('.branching-demo__map-activate-hint').exists()).toBe(false)
  })

  it('closes the preview modal via the footer Close button and shows Start preview again', async () => {
    const wrapper = mountDemo()
    await flushPromises()
    await nextTick()

    await wrapper.findAll('button').find((b) => b.text().includes('Start preview')).trigger('click')
    await nextTick()
    expect(wrapper.find('[data-testid="preview-modal"]').isVisible()).toBe(true)

    const closeBtn = wrapper.find('.modal-footer-stub button')
    expect(closeBtn.text()).toMatch(/Close/i)
    await closeBtn.trigger('click')
    await nextTick()

    expect(wrapper.find('[data-testid="preview-modal"]').isVisible()).toBe(false)
    const labels = wrapper.findAll('button').map((b) => b.text())
    expect(labels.some((t) => /Start preview/i.test(t))).toBe(true)
  })

  it('shows a load error when Reload graph fails', async () => {
    const wrapper = mountDemo()
    await flushPromises()
    await nextTick()

    apiMock.get.mockRejectedValueOnce(new Error('Network down'))
    const reloadBtn = wrapper.findAll('button').find((b) => /Reload graph/i.test(b.text()))
    expect(reloadBtn).toBeTruthy()
    await reloadBtn.trigger('click')
    await flushPromises()
    await nextTick()

    expect(wrapper.text()).toMatch(/Could not load demo workflow|network|Network/i)
  })

  it('shows save success after Save workflow succeeds', async () => {
    const wrapper = mountDemo()
    await flushPromises()
    await nextTick()

    const saveBtn = wrapper.findAll('button').find((b) => /Save workflow/i.test(b.text()))
    await saveBtn.trigger('click')
    await flushPromises()
    await nextTick()

    expect(apiMock.put).toHaveBeenCalled()
    expect(wrapper.text()).toMatch(/Saved/)
  })
})
