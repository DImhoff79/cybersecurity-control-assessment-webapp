import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import BranchingFlowMapTools from './BranchingFlowMapTools.vue'

const fitView = vi.fn()

vi.mock('@vue-flow/core', () => ({
  Panel: {
    name: 'Panel',
    template: '<div class="panel-stub"><slot /></div>'
  },
  useVueFlow: () => ({
    fitView
  })
}))

describe('BranchingFlowMapTools', () => {
  it('emits add-step, auto-layout, and calls fitView when buttons are used', async () => {
    const wrapper = mount(BranchingFlowMapTools, {
      props: { layoutFitTick: 0 }
    })

    await wrapper.get('button').trigger('click')
    expect(wrapper.emitted('add-step')).toEqual([[]])

    const buttons = wrapper.findAll('button')
    expect(buttons.length).toBe(3)
    await buttons[1].trigger('click')
    expect(wrapper.emitted('auto-layout')).toEqual([[]])

    await buttons[2].trigger('click')
    expect(fitView).toHaveBeenCalledWith({ padding: 0.2, duration: 200 })
  })

  it('calls fitView when layoutFitTick increments', async () => {
    fitView.mockClear()
    const wrapper = mount(BranchingFlowMapTools, {
      props: { layoutFitTick: 0 }
    })
    await wrapper.setProps({ layoutFitTick: 1 })
    await wrapper.vm.$nextTick()
    expect(fitView).toHaveBeenCalledWith({ padding: 0.2, duration: 280 })
  })
})
