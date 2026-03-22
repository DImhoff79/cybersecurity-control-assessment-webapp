import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import QuestionnaireHub from './QuestionnaireHub.vue'

describe('QuestionnaireHub', () => {
  it('renders builder and governance entry points', () => {
    const wrapper = mount(QuestionnaireHub, {
      global: {
        stubs: {
          RouterLink: {
            props: ['to'],
            template: '<a class="router-link-stub"><slot /></a>'
          }
        }
      }
    })

    expect(wrapper.text()).toContain('Questionnaire')
    expect(wrapper.text()).toContain('Library Builder')
    expect(wrapper.text()).toContain('Governance Versions')
    expect(wrapper.text()).toContain('Open Builder')
    expect(wrapper.text()).toContain('Open Governance')
    const links = wrapper.findAll('.router-link-stub')
    expect(links.length).toBeGreaterThanOrEqual(2)
  })
})
