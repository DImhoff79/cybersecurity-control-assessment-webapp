import { OWNER_ANSWER_VALUE_KEYS } from './ownerAnswerOptions'

describe('ownerAnswerOptions', () => {
  it('defines the canonical stored value keys', () => {
    expect(OWNER_ANSWER_VALUE_KEYS).toEqual(['UNANSWERED', 'YES', 'PARTIAL', 'NO', 'NOT_APPLICABLE'])
  })
})
