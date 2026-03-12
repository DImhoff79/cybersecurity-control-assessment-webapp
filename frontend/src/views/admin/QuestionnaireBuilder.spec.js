import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import QuestionnaireBuilder from './QuestionnaireBuilder.vue'

const replaceMock = vi.fn()
const routeMock = { query: { tab: 'controls', from: 'governance' } }

vi.mock('vue-router', () => ({
  useRoute: () => routeMock,
  useRouter: () => ({ replace: replaceMock })
}))

describe('QuestionnaireBuilder', () => {
  it('switches tabs and updates route query', async () => {
    const wrapper = mount(QuestionnaireBuilder, {
      global: {
        stubs: {
          RouterLink: true,
          ControlCatalog: { template: '<div>Controls panel</div>' },
          QuestionManager: { template: '<div>Questions panel</div>' }
        }
      }
    })

    const tabButtons = wrapper.findAll('button')
    const questionTab = tabButtons.find((b) => b.text() === 'Questions')
    await questionTab.trigger('click')

    expect(replaceMock).toHaveBeenCalledWith({
      name: 'AdminQuestionnaireBuilder',
      query: { tab: 'questions', from: 'governance' }
    })
  })
})
