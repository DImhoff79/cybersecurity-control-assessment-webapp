<template>
  <div class="mx-auto" style="max-width: 40rem">
    <h1 class="h3 mb-2">New application assessment</h1>
    <p class="text-muted mb-4">
      Tell us about the application you are building. Your answers set scope for the Kroger Common Controls Framework (CCF)
      assessment that follows—plain language first, then control attestation.
    </p>

    <div v-if="wizardLoading" class="text-muted py-4">Loading intake steps…</div>

    <template v-else>
      <div class="mb-3">
        <div class="progress" style="height: 6px">
          <div class="progress-bar" role="progressbar" :style="{ width: `${((step + 1) / totalSteps) * 100}%` }" />
        </div>
        <div class="small text-muted mt-1">Step {{ step + 1 }} of {{ totalSteps }}</div>
      </div>

      <!-- Library-driven wizard (from questions.intake_step_key + optional canonical flow order) -->
      <div v-if="useWizard" class="card border-0 shadow-sm">
        <div class="card-body p-4">
          <h2 class="h5 mb-2">{{ currentWizardStep?.questionText }}</h2>
          <p v-if="currentWizardStep?.helpText" class="text-muted small mb-3">{{ currentWizardStep.helpText }}</p>

          <div v-if="currentWizardStep?.inputType === 'TEXT'" class="mb-0">
            <input
              v-model="wizAnswers[currentWizardStep.questionId]"
              class="form-control"
              :placeholder="currentWizardStep.stepKey === 'app_name' ? 'e.g. Loyalty checkout API' : ''"
            />
          </div>

          <div v-else-if="currentWizardStep?.inputType === 'CHOICE'" class="mb-0">
            <select v-model="wizAnswers[currentWizardStep.questionId]" class="form-select">
              <option value="">— Select —</option>
              <option v-for="ch in choicesForStep(currentWizardStep)" :key="ch.id" :value="ch.id">
                {{ ch.label }}
              </option>
            </select>
          </div>

          <div v-else-if="currentWizardStep?.inputType === 'YES_NO'" class="mb-0">
            <select v-model="wizAnswers[currentWizardStep.questionId]" class="form-select">
              <option value="">— Select —</option>
              <option value="true">Yes</option>
              <option value="false">No</option>
            </select>
          </div>
        </div>
      </div>

      <!-- Fallback: static 3-step form when no library steps are configured -->
      <template v-else>
        <div v-if="step === 0" class="card border-0 shadow-sm">
          <div class="card-body p-4">
            <h2 class="h5 mb-3">Application</h2>
            <div class="mb-3">
              <label class="form-label" for="app-name">Application name</label>
              <input id="app-name" v-model="form.name" class="form-control" required placeholder="e.g. Loyalty checkout API" />
            </div>
            <div class="mb-0">
              <label class="form-label" for="app-desc">Short description</label>
              <textarea id="app-desc" v-model="form.description" class="form-control" rows="3" placeholder="What it does for the business" />
            </div>
          </div>
        </div>

        <div v-else-if="step === 1" class="card border-0 shadow-sm">
          <div class="card-body p-4">
            <h2 class="h5 mb-3">How it fits</h2>
            <div class="mb-3">
              <label class="form-label">Primary purpose</label>
              <select v-model="form.applicationPurpose" class="form-select">
                <option :value="null">— Select —</option>
                <option value="INTERNAL_OPERATIONAL">Internal / back-office</option>
                <option value="CUSTOMER_FACING">Customer or shopper-facing</option>
                <option value="PARTNER_FACING">Partner / B2B</option>
                <option value="MIXED">Mixed</option>
              </select>
            </div>
            <div class="mb-3">
              <label class="form-label">Where it runs</label>
              <select v-model="form.hostingModel" class="form-select">
                <option :value="null">— Select —</option>
                <option value="KROGER_MANAGED">Enterprise-managed (data center / cloud)</option>
                <option value="VENDOR_SAAS">Vendor SaaS / vendor-hosted</option>
                <option value="HYBRID">Hybrid</option>
              </select>
            </div>
            <div class="mb-3">
              <label class="form-label">Who uses it</label>
              <select v-model="form.userAudienceScope" class="form-select">
                <option :value="null">— Select —</option>
                <option value="WORKFORCE_ONLY">Workforce only</option>
                <option value="WORKFORCE_AND_CONTRACTORS">Workforce and contractors</option>
                <option value="CUSTOMER_OR_PUBLIC">Customers or the public</option>
                <option value="BUSINESS_PARTNERS">External business partners</option>
                <option value="MIXED">Mixed audiences</option>
              </select>
            </div>
            <div class="mb-0">
              <label class="form-label">Integrations &amp; data flows</label>
              <select v-model="form.integrationScope" class="form-select">
                <option :value="null">— Select —</option>
                <option value="STANDALONE">Mostly standalone</option>
                <option value="KROGER_INTEGRATED">Connects to other enterprise systems</option>
                <option value="EXTERNAL_EXCHANGE">Sends/receives data with external orgs</option>
                <option value="BOTH">Enterprise and external</option>
              </select>
            </div>
          </div>
        </div>

        <div v-else class="card border-0 shadow-sm">
          <div class="card-body p-4">
            <h2 class="h5 mb-3">Data &amp; sensitivity</h2>
            <div class="mb-3">
              <label class="form-label">Data classification (highest level handled)</label>
              <select v-model="form.dataClassification" class="form-select">
                <option :value="null">— Select —</option>
                <option value="PUBLIC">PUBLIC</option>
                <option value="INTERNAL">INTERNAL</option>
                <option value="CONFIDENTIAL">CONFIDENTIAL</option>
                <option value="RESTRICTED">RESTRICTED</option>
              </select>
            </div>
            <label class="form-label d-block">Regulatory data types in scope</label>
            <p class="small text-muted mb-2">
              Select each type this application stores, processes, or transmits. These values are stored as structured fields for dashboards and reports (not free text).
            </p>
            <div class="form-check mb-2">
              <input id="pii" v-model="form.dataScopePii" class="form-check-input" type="checkbox" />
              <label class="form-check-label" for="pii">Personal information about individuals (PII)</label>
            </div>
            <div class="form-check mb-2">
              <input id="pci" v-model="form.dataScopePci" class="form-check-input" type="checkbox" />
              <label class="form-check-label" for="pci">Payment card data (PCI DSS scope)</label>
            </div>
            <div class="form-check mb-2">
              <input id="sox" v-model="form.dataScopeSox" class="form-check-input" type="checkbox" />
              <label class="form-check-label" for="sox">SOX-relevant financial reporting or IT general controls</label>
            </div>
            <div class="form-check mb-0">
              <input id="hipaa" v-model="form.dataScopeHipaa" class="form-check-input" type="checkbox" />
              <label class="form-check-label" for="hipaa">HIPAA-regulated health information (PHI)</label>
            </div>
          </div>
        </div>
      </template>

      <div class="d-flex justify-content-between mt-4">
        <button type="button" class="btn btn-outline-secondary" :disabled="step === 0 || submitting" @click="step--">Back</button>
        <div class="d-flex gap-2">
          <button v-if="step < totalSteps - 1" type="button" class="btn btn-primary" @click="next">Continue</button>
          <button
            v-else
            type="button"
            class="btn btn-primary"
            :disabled="submitting || !canSubmitFinal"
            @click="submit"
          >
            {{ submitting ? 'Starting…' : 'Start assessment' }}
          </button>
        </div>
      </div>
    </template>
    <p v-if="error" class="text-danger small mt-3 mb-0">{{ error }}</p>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../../services/api'
import { toastError, toastSuccess } from '../../services/toast'

const router = useRouter()
const wizardSteps = ref([])
const wizardLoading = ref(true)
const step = ref(0)
const submitting = ref(false)
const error = ref('')
const wizAnswers = reactive({})

const useWizard = computed(() => wizardSteps.value.length > 0)
const totalSteps = computed(() => (useWizard.value ? wizardSteps.value.length : 3))

const currentWizardStep = computed(() => (useWizard.value ? wizardSteps.value[step.value] : null))

const form = reactive({
  name: '',
  description: '',
  applicationPurpose: null,
  hostingModel: null,
  userAudienceScope: null,
  integrationScope: null,
  dataClassification: null,
  dataScopePii: false,
  dataScopePci: false,
  dataScopeSox: false,
  dataScopeHipaa: false,
  lifecycleStatus: 'ACTIVE'
})

watch(
  wizardSteps,
  (steps) => {
    for (const s of steps) {
      if (wizAnswers[s.questionId] === undefined) {
        wizAnswers[s.questionId] = ''
      }
    }
  },
  { immediate: true }
)

const canSubmitFinal = computed(() => {
  if (useWizard.value) {
    const last = wizardSteps.value[wizardSteps.value.length - 1]
    if (!last) return false
    const nameStep = wizardSteps.value.find((x) => x.stepKey === 'app_name')
    const nameVal = nameStep ? String(wizAnswers[nameStep.questionId] || '').trim() : ''
    return !!nameVal
  }
  return !!form.name?.trim()
})

onMounted(async () => {
  wizardLoading.value = true
  try {
    const res = await api.get('/api/application-intake/wizard')
    wizardSteps.value = Array.isArray(res.data) ? res.data : []
  } catch {
    wizardSteps.value = []
  } finally {
    wizardLoading.value = false
  }
})

function choicesForStep(s) {
  if (!s?.choicesJson) return []
  try {
    const arr = JSON.parse(s.choicesJson)
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}

function next() {
  error.value = ''
  if (useWizard.value) {
    const cur = wizardSteps.value[step.value]
    if (!cur) return
    const v = wizAnswers[cur.questionId]
    if (cur.inputType === 'TEXT' && (!v || !String(v).trim())) {
      error.value = 'This field is required.'
      return
    }
    if ((cur.inputType === 'CHOICE' || cur.inputType === 'YES_NO') && (v === '' || v == null)) {
      error.value = 'Please make a selection.'
      return
    }
  } else if (step.value === 0 && !form.name?.trim()) {
    error.value = 'Enter an application name.'
    return
  }
  step.value++
}

function mapWizardToPayload() {
  const body = { lifecycleStatus: 'ACTIVE' }
  const intakeAnswers = []
  for (const s of wizardSteps.value) {
    const raw = wizAnswers[s.questionId]
    const answerText = raw == null ? '' : String(raw)
    intakeAnswers.push({ questionId: s.questionId, answerText: answerText || null })
    switch (s.stepKey) {
      case 'app_name':
        body.name = answerText.trim()
        break
      case 'app_description':
        body.description = answerText.trim() || null
        break
      case 'purpose':
        body.applicationPurpose = answerText || null
        break
      case 'hosting':
        body.hostingModel = answerText || null
        break
      case 'audience':
        body.userAudienceScope = answerText || null
        break
      case 'integration':
        body.integrationScope = answerText || null
        break
      case 'classification':
        body.dataClassification = answerText || null
        break
      case 'scope_pii':
        body.dataScopePii = answerText === 'true'
        break
      case 'scope_pci':
        body.dataScopePci = answerText === 'true'
        break
      case 'scope_sox':
        body.dataScopeSox = answerText === 'true'
        break
      case 'scope_hipaa':
        body.dataScopeHipaa = answerText === 'true'
        break
      default:
        break
    }
  }
  body.intakeAnswers = intakeAnswers
  return body
}

async function submit() {
  error.value = ''
  submitting.value = true
  try {
    const payload = useWizard.value ? mapWizardToPayload() : { ...form }
    if (useWizard.value && !payload.name?.trim()) {
      error.value = 'Enter an application name.'
      submitting.value = false
      return
    }
    const res = await api.post('/api/application-intake', payload)
    const auditId = res.data?.audit?.id
    toastSuccess('Your assessment is ready. You can answer CCF questions next.')
    if (auditId) {
      router.replace({ name: 'AuditRespond', params: { auditId: String(auditId) } })
    } else {
      router.push({ name: 'MyAudits' })
    }
  } catch (e) {
    error.value = e.response?.data?.error || e.message || 'Could not start assessment.'
    toastError(error.value)
  } finally {
    submitting.value = false
  }
}
</script>
