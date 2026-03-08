import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import BsModal from './BsModal.vue'

const show = vi.fn()
const hide = vi.fn()
const dispose = vi.fn()

vi.mock('bootstrap/js/dist/modal', () => {
  class ModalMock {
    constructor() {}
    show = show
    hide = hide
    dispose = dispose
  }
  return {
    default: ModalMock
  }
})

describe('BsModal', () => {
  it('shows, hides, emits, and disposes bootstrap modal', async () => {
    const wrapper = mount(BsModal, {
      props: {
        modelValue: true,
        title: 'Test Modal',
        size: 'lg'
      },
      slots: {
        default: '<div>Body</div>',
        footer: '<button>Footer</button>'
      },
      attachTo: document.body
    })

    await wrapper.vm.$nextTick()
    expect(show).toHaveBeenCalled()

    await wrapper.setProps({ modelValue: false })
    expect(hide).toHaveBeenCalled()

    const modalEl = document.body.querySelector('.modal')
    modalEl.dispatchEvent(new Event('hidden.bs.modal'))
    expect(wrapper.emitted('update:modelValue')).toBeTruthy()

    await wrapper.unmount()
    expect(dispose).toHaveBeenCalled()
  })
})
