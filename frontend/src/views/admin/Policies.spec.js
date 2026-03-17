import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import Policies from './Policies.vue'
import api from '../../services/api'

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn()
  }
}))

vi.mock('quill', () => ({
  default: vi.fn().mockImplementation(function MockQuill(container) {
    this.container = container
    this.root = container
    this._lastHtml = ''
    this.clipboard = {
      convert: ({ html }) => html,
      dangerouslyPasteHTML: (html) => {
        this.root.innerHTML = html
      }
    }
    this.setText = () => {
      this.root.innerHTML = ''
    }
    this.setContents = (delta) => {
      this._lastHtml = delta || ''
      this.root.innerHTML = this._lastHtml
    }
  })
}))

describe('Policies', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockResolvedValue({
      data: [
        {
          id: 1,
          code: 'POL-1',
          name: 'Access Policy',
          status: 'DRAFT',
          versions: [{ id: 12, versionNumber: 1, status: 'DRAFT', title: 'v1', bodyMarkdown: '<p>Body</p>' }]
        }
      ]
    })
    api.post.mockResolvedValue({ data: {} })
    api.put.mockResolvedValue({ data: {} })
  })

  it('opens add policy editor and creates policy', async () => {
    const wrapper = mount(Policies)
    await flushPromises()
    expect(wrapper.text()).toContain('Access Policy')

    const addPolicyBtn = wrapper.findAll('button').find((b) => b.text().includes('Add Policy'))
    await addPolicyBtn.trigger('click')
    await flushPromises()

    const codeInput = wrapper.find('input[placeholder="Code (POL-001)"]')
    const nameInput = wrapper.find('input[placeholder="Policy name"]')
    await codeInput.setValue('POL-500')
    await nameInput.setValue('Encryption Policy')

    const editable = wrapper.find('.rich-editor')
    editable.element.innerHTML = '<h2>Policy</h2><p>Encryption standards</p>'
    const createBtn = wrapper.findAll('button').find((b) => b.text().includes('Create Policy'))
    await createBtn.trigger('click')
    await flushPromises()

    expect(api.post).toHaveBeenCalledWith('/api/policies', {
      code: 'POL-500',
      name: 'Encryption Policy',
      description: null,
      initialVersionTitle: 'Policy v1',
      initialBodyMarkdown: '<h2>Policy</h2><p>Encryption standards</p>',
      csfFunctions: []
    })
  })

  it('opens rich editor and saves draft updates', async () => {
    const wrapper = mount(Policies)
    await flushPromises()

    const editBtn = wrapper.findAll('button').find((b) => b.text().includes('Edit'))
    await editBtn.trigger('click')
    await flushPromises()

    const editable = wrapper.find('.rich-editor')
    editable.element.innerHTML = '<h2>Updated</h2><p>Policy body</p>'
    const saveBtn = wrapper.findAll('button').find((b) => b.text() === 'Save')
    await saveBtn.trigger('click')
    await flushPromises()

    expect(api.put).toHaveBeenCalledWith('/api/policies/1/versions/12', {
      title: 'v1',
      bodyMarkdown: '<h2>Updated</h2><p>Policy body</p>'
    })
  })

  it('reopens add policy modal with populated large editor', async () => {
    const wrapper = mount(Policies)
    await flushPromises()

    const addPolicyBtn = wrapper.findAll('button').find((b) => b.text().includes('Add Policy'))
    await addPolicyBtn.trigger('click')
    await flushPromises()

    const cancelBtn = wrapper.findAll('button').find((b) => b.text() === 'Cancel')
    await cancelBtn.trigger('click')
    await flushPromises()

    const addPolicyBtnAgain = wrapper.findAll('button').find((b) => b.text().includes('Add Policy'))
    await addPolicyBtnAgain.trigger('click')
    await flushPromises()

    const editor = wrapper.find('.rich-editor')
    expect(editor.exists()).toBe(true)
    expect(editor.classes()).toContain('rich-editor-create')
    expect(editor.html()).toContain('Purpose')
  })
})
