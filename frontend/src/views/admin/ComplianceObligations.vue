<template>
  <div>
    <div class="mb-3">
      <h1 class="h3 mb-1">Compliance Obligations</h1>
      <p class="text-muted mb-0">
        Manage regulations, requirements, and mappings to controls and policies.
      </p>
    </div>
    <div class="small text-muted mb-3">
      Keep requirement mappings current to maintain defensible compliance coverage metrics.
    </div>

    <div class="row g-3">
      <div class="col-lg-6">
        <div class="card shadow-sm h-100">
          <div class="card-body">
            <h2 class="h5 mb-3">Regulations</h2>
            <form class="row g-2 mb-3" @submit.prevent="createRegulation">
              <div class="col-md-4">
                <input v-model="regulationForm.code" class="form-control form-control-sm" placeholder="Code" required />
              </div>
              <div class="col-md-8">
                <input v-model="regulationForm.name" class="form-control form-control-sm" placeholder="Name" required />
              </div>
              <div class="col-md-12">
                <button class="btn btn-outline-primary btn-sm">Add Regulation</button>
              </div>
            </form>
            <div class="table-responsive">
              <table class="table table-sm table-striped mb-0">
                <thead><tr><th>Code</th><th>Name</th></tr></thead>
                <tbody>
                  <tr v-for="r in regulations" :key="r.id">
                    <td>{{ r.code }}</td>
                    <td>{{ r.name }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      <div class="col-lg-6">
        <div class="card shadow-sm h-100">
          <div class="card-body">
            <h2 class="h5 mb-3">Requirements</h2>
            <form class="row g-2 mb-3" @submit.prevent="createRequirement">
              <div class="col-md-4">
                <select v-model="requirementForm.regulationId" class="form-select form-select-sm" required>
                  <option disabled value="">Regulation</option>
                  <option v-for="r in regulations" :key="r.id" :value="r.id">{{ r.code }}</option>
                </select>
              </div>
              <div class="col-md-3">
                <input v-model="requirementForm.requirementCode" class="form-control form-control-sm" placeholder="Req Code" required />
              </div>
              <div class="col-md-5">
                <input v-model="requirementForm.title" class="form-control form-control-sm" placeholder="Title" required />
              </div>
              <div class="col-md-12">
                <button class="btn btn-outline-primary btn-sm">Add Requirement</button>
              </div>
            </form>
            <div class="table-responsive">
              <table class="table table-sm table-striped mb-0">
                <thead><tr><th>Requirement</th><th>Regulation</th></tr></thead>
                <tbody>
                  <tr v-for="r in requirements" :key="r.id">
                    <td>{{ r.requirementCode }} - {{ r.title }}</td>
                    <td>{{ r.regulationCode }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="card shadow-sm mt-3">
      <div class="card-body">
        <h2 class="h5 mb-3">Mappings</h2>
        <div class="row g-2 mb-3">
          <div class="col-md-4">
            <label class="small form-label mb-1">Requirement -> Control</label>
            <div class="input-group input-group-sm">
              <select v-model="mappingForm.requirementId" class="form-select">
                <option disabled value="">Requirement</option>
                <option v-for="r in requirements" :key="r.id" :value="r.id">{{ r.requirementCode }}</option>
              </select>
              <select v-model="mappingForm.controlId" class="form-select">
                <option disabled value="">Control</option>
                <option v-for="c in controls" :key="c.id" :value="c.id">{{ c.controlId }}</option>
              </select>
              <button class="btn btn-outline-secondary" @click="createRequirementControlMapping">Map</button>
            </div>
          </div>
          <div class="col-md-4">
            <label class="small form-label mb-1">Policy -> Requirement</label>
            <div class="input-group input-group-sm">
              <select v-model="mappingForm.policyId" class="form-select">
                <option disabled value="">Policy</option>
                <option v-for="p in policies" :key="p.id" :value="p.id">{{ p.code }}</option>
              </select>
              <select v-model="mappingForm.policyRequirementId" class="form-select">
                <option disabled value="">Requirement</option>
                <option v-for="r in requirements" :key="r.id" :value="r.id">{{ r.requirementCode }}</option>
              </select>
              <button class="btn btn-outline-secondary" @click="createPolicyRequirementMapping">Map</button>
            </div>
          </div>
        </div>

        <div class="row g-3">
          <div class="col-lg-6">
            <h3 class="h6">Requirement -> Control</h3>
            <div class="table-responsive">
              <table class="table table-sm table-striped mb-0">
                <thead><tr><th>Requirement</th><th>Control</th><th>Coverage</th></tr></thead>
                <tbody>
                  <tr v-for="row in requirementControlMappings" :key="row.id">
                    <td>{{ requirementCodeById(row.requirementId) }}</td>
                    <td>{{ row.controlControlId }} - {{ row.controlName }}</td>
                    <td>{{ row.coveragePct }}%</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
          <div class="col-lg-6">
            <h3 class="h6">Policy -> Requirement</h3>
            <div class="table-responsive">
              <table class="table table-sm table-striped mb-0">
                <thead><tr><th>Policy</th><th>Requirement</th></tr></thead>
                <tbody>
                  <tr v-for="row in policyRequirementMappings" :key="row.id">
                    <td>{{ row.policyCode }}</td>
                    <td>{{ row.requirementCode }} - {{ row.requirementTitle }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../../services/api'
import { toastError, toastSuccess } from '../../services/toast'

const regulations = ref([])
const requirements = ref([])
const controls = ref([])
const policies = ref([])
const requirementControlMappings = ref([])
const policyRequirementMappings = ref([])

const regulationForm = ref({ code: '', name: '' })
const requirementForm = ref({ regulationId: '', requirementCode: '', title: '' })
const mappingForm = ref({
  requirementId: '',
  controlId: '',
  policyId: '',
  policyRequirementId: ''
})

onMounted(async () => {
  await loadAll()
})

async function loadAll() {
  const [regulationsRes, requirementsRes, controlsRes, policiesRes, reqControlRes, policyReqRes] = await Promise.all([
    api.get('/api/compliance/regulations'),
    api.get('/api/compliance/requirements'),
    api.get('/api/controls'),
    api.get('/api/policies'),
    api.get('/api/compliance/requirement-control-mappings'),
    api.get('/api/compliance/policy-requirement-mappings')
  ])
  regulations.value = regulationsRes.data || []
  requirements.value = requirementsRes.data || []
  controls.value = controlsRes.data || []
  policies.value = policiesRes.data || []
  requirementControlMappings.value = reqControlRes.data || []
  policyRequirementMappings.value = policyReqRes.data || []
}

async function createRegulation() {
  try {
    await api.post('/api/compliance/regulations', regulationForm.value)
    regulationForm.value = { code: '', name: '' }
    await loadAll()
    toastSuccess('Regulation added.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to add regulation')
  }
}

async function createRequirement() {
  try {
    await api.post('/api/compliance/requirements', {
      ...requirementForm.value,
      regulationId: Number(requirementForm.value.regulationId)
    })
    requirementForm.value = { regulationId: '', requirementCode: '', title: '' }
    await loadAll()
    toastSuccess('Requirement added.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to add requirement')
  }
}

async function createRequirementControlMapping() {
  try {
    await api.post('/api/compliance/requirement-control-mappings', {
      requirementId: Number(mappingForm.value.requirementId),
      controlId: Number(mappingForm.value.controlId)
    })
    mappingForm.value.requirementId = ''
    mappingForm.value.controlId = ''
    await loadAll()
    toastSuccess('Requirement mapped to control.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to map requirement')
  }
}

async function createPolicyRequirementMapping() {
  try {
    await api.post('/api/compliance/policy-requirement-mappings', {
      policyId: Number(mappingForm.value.policyId),
      requirementId: Number(mappingForm.value.policyRequirementId)
    })
    mappingForm.value.policyId = ''
    mappingForm.value.policyRequirementId = ''
    await loadAll()
    toastSuccess('Policy mapped to requirement.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to map policy')
  }
}

function requirementCodeById(requirementId) {
  const row = requirements.value.find((it) => it.id === requirementId)
  return row?.requirementCode || `#${requirementId}`
}
</script>
