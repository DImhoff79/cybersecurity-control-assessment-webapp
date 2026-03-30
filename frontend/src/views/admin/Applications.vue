<template>
  <div>
    <div class="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-3">
      <div>
        <h1 class="h3 mb-1">Applications</h1>
        <p class="text-muted mb-0">
          Manage the application inventory, ownership, and lifecycle metadata used across audits.
        </p>
      </div>
      <button class="btn btn-primary btn-sm" @click="openModal()">Add application</button>
    </div>
    <div class="card workspace-card border-0 shadow-sm mb-3">
      <div class="card-body">
        <div class="small text-muted mb-3">
          Tip: keep owner and lifecycle values current so assignments and reporting stay accurate.
        </div>
        <div class="table-responsive">
          <table class="table workspace-table align-middle mb-0">
            <thead>
              <tr>
                <th><button type="button" class="workspace-table-sort" @click="toggleSort('name')">Name {{ sortIndicator('name') }}</button></th>
                <th><button type="button" class="workspace-table-sort" @click="toggleSort('description')">Description {{ sortIndicator('description') }}</button></th>
                <th><button type="button" class="workspace-table-sort" @click="toggleSort('owner')">Owner {{ sortIndicator('owner') }}</button></th>
                <th><button type="button" class="workspace-table-sort" @click="toggleSort('criticality')">Criticality {{ sortIndicator('criticality') }}</button></th>
                <th><button type="button" class="workspace-table-sort" @click="toggleSort('lifecycle')">Lifecycle {{ sortIndicator('lifecycle') }}</button></th>
                <th>Regulatory data types</th>
                <th>Security arch. review</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="a in sortedRows" :key="a.id">
                <td>{{ a.name }}</td>
                <td>{{ a.description || '-' }}</td>
                <td>{{ a.ownerDisplayName || a.ownerEmail || '-' }}</td>
                <td>{{ a.criticality || '-' }}</td>
                <td>{{ a.lifecycleStatus || '-' }}</td>
                <td class="small text-nowrap">
                  <span v-if="a.dataScopePii" class="badge text-bg-secondary me-1">PII</span>
                  <span v-if="a.dataScopePci" class="badge text-bg-secondary me-1">PCI</span>
                  <span v-if="a.dataScopeSox" class="badge text-bg-secondary me-1">SOX</span>
                  <span v-if="a.dataScopeHipaa ?? a.dataScopePhi" class="badge text-bg-secondary me-1">HIPAA</span>
                  <span
                    v-if="
                      !a.dataScopePii &&
                      !a.dataScopePci &&
                      !a.dataScopeSox &&
                      !(a.dataScopeHipaa ?? a.dataScopePhi)
                    "
                    class="text-muted"
                    >—</span
                  >
                </td>
                <td class="small">
                  <span
                    v-if="a.securityArchitectureReview"
                    class="badge me-1"
                    :class="secReviewBadgeClass(a.securityArchitectureReview.status)"
                  >
                    {{ secReviewLabel(a.securityArchitectureReview.status) }}
                  </span>
                  <span v-else class="text-muted">—</span>
                  <button type="button" class="btn btn-link btn-sm p-0 ms-1 align-baseline" @click="openReviewModal(a)">
                    Update
                  </button>
                </td>
                <td class="text-nowrap">
                  <button class="btn btn-secondary btn-sm me-2" @click="openModal(a)">Edit</button>
                  <button class="btn btn-danger btn-sm" @click="remove(a.id)">Delete</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <BsModal v-model="showModal" :title="`${editing?.id ? 'Edit' : 'Add'} Application`">
      <form id="application-form" @submit.prevent="save">
        <div class="mb-3">
          <label class="form-label">Name</label>
          <input v-model="form.name" required class="form-control" />
        </div>
        <div class="mb-3">
          <label class="form-label">Description</label>
          <textarea v-model="form.description" class="form-control" />
        </div>
        <div class="mb-3">
          <label class="form-label">Owner</label>
          <select v-model="form.ownerId" class="form-select">
            <option :value="null">- Select -</option>
            <option v-for="u in users" :key="u.id" :value="u.id">{{ u.displayName || u.email }}</option>
          </select>
        </div>
        <div class="row g-3">
          <div class="col-md-6">
            <label class="form-label">Criticality</label>
            <select v-model="form.criticality" class="form-select">
              <option :value="null">- Select -</option>
              <option value="LOW">LOW</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="HIGH">HIGH</option>
              <option value="CRITICAL">CRITICAL</option>
            </select>
          </div>
          <div class="col-md-6">
            <label class="form-label">Data Classification</label>
            <select v-model="form.dataClassification" class="form-select">
              <option :value="null">- Select -</option>
              <option value="PUBLIC">PUBLIC</option>
              <option value="INTERNAL">INTERNAL</option>
              <option value="CONFIDENTIAL">CONFIDENTIAL</option>
              <option value="RESTRICTED">RESTRICTED</option>
            </select>
          </div>
          <div class="col-md-6">
            <label class="form-label">Lifecycle Status</label>
            <select v-model="form.lifecycleStatus" class="form-select">
              <option :value="null">- Select -</option>
              <option value="PLANNED">PLANNED</option>
              <option value="ACTIVE">ACTIVE</option>
              <option value="SUNSETTING">SUNSETTING</option>
              <option value="RETIRED">RETIRED</option>
            </select>
          </div>
          <div class="col-md-6">
            <label class="form-label">Notes (optional)</label>
            <input v-model="form.regulatoryScope" class="form-control" placeholder="Operational context only" />
            <div class="form-text">Do not use this field for PII, PCI, SOX, or HIPAA—use the checkboxes under regulatory data types.</div>
          </div>
          <div class="col-12">
            <hr class="my-2" />
            <div class="fw-semibold mb-2">Owner triage</div>
            <p class="small text-muted mb-3">
              Plain-language routing: helps prioritize assessments. Not a full control attestation.
            </p>
          </div>
          <div class="col-md-6">
            <label class="form-label">Primary purpose</label>
            <select v-model="form.applicationPurpose" class="form-select">
              <option :value="null">— Select —</option>
              <option value="INTERNAL_OPERATIONAL">Internal / back-office</option>
              <option value="CUSTOMER_FACING">Customer or shopper-facing</option>
              <option value="PARTNER_FACING">Partner / B2B</option>
              <option value="MIXED">Mixed</option>
            </select>
          </div>
          <div class="col-md-6">
            <label class="form-label">Where it runs</label>
            <select v-model="form.hostingModel" class="form-select">
              <option :value="null">— Select —</option>
              <option value="KROGER_MANAGED">Enterprise-managed (data center / cloud)</option>
              <option value="VENDOR_SAAS">Vendor SaaS / vendor-hosted</option>
              <option value="HYBRID">Hybrid</option>
            </select>
          </div>
          <div class="col-md-6">
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
          <div class="col-md-6">
            <label class="form-label">Integrations &amp; data flows</label>
            <select v-model="form.integrationScope" class="form-select">
              <option :value="null">— Select —</option>
              <option value="STANDALONE">Mostly standalone</option>
              <option value="KROGER_INTEGRATED">Connects to other enterprise systems</option>
              <option value="EXTERNAL_EXCHANGE">Sends/receives data with external orgs</option>
              <option value="BOTH">Enterprise and external</option>
            </select>
          </div>
          <div class="col-12">
            <label class="form-label d-block">Regulatory data types in scope</label>
            <p class="small text-muted mb-2">
              Structured flags for reporting and filtering. Select each type this application stores, processes, or transmits.
            </p>
            <div class="form-check">
              <input id="scope-pii" v-model="form.dataScopePii" class="form-check-input" type="checkbox" />
              <label class="form-check-label" for="scope-pii">
                Personal information about individuals (names, contact info, loyalty IDs, employee identifiers, etc.) — <strong>PII</strong>
              </label>
            </div>
            <div class="form-check">
              <input id="scope-pci" v-model="form.dataScopePci" class="form-check-input" type="checkbox" />
              <label class="form-check-label" for="scope-pci">
                Payment card data (card numbers, full magnetic stripe, CVV, PIN) — <strong>PCI</strong> scope
              </label>
            </div>
            <div class="form-check">
              <input id="scope-sox" v-model="form.dataScopeSox" class="form-check-input" type="checkbox" />
              <label class="form-check-label" for="scope-sox">
                SOX-relevant financial reporting, disclosure controls, or IT general controls — <strong>SOX</strong>
              </label>
            </div>
            <div class="form-check">
              <input id="scope-hipaa" v-model="form.dataScopeHipaa" class="form-check-input" type="checkbox" />
              <label class="form-check-label" for="scope-hipaa">
                HIPAA-regulated health information (PHI) — <strong>HIPAA</strong>
              </label>
            </div>
          </div>
          <div class="col-md-6">
            <label class="form-label">Business Owner Name</label>
            <input v-model="form.businessOwnerName" class="form-control" />
          </div>
          <div class="col-md-6">
            <label class="form-label">Technical Owner Name</label>
            <input v-model="form.technicalOwnerName" class="form-control" />
          </div>
        </div>
      </form>
      <template #footer>
        <button type="button" class="btn btn-secondary" @click="showModal = false">Cancel</button>
        <button type="submit" form="application-form" class="btn btn-primary">Save</button>
      </template>
    </BsModal>

    <BsModal v-model="showReviewModal" title="Security architecture review">
      <form id="review-form" @submit.prevent="saveReview">
        <p class="small text-muted mb-3">
          Parallel to control attestation—updating this does not block the owner assessment.
        </p>
        <div class="mb-3">
          <label class="form-label">Status</label>
          <select v-model="reviewForm.status" class="form-select" required>
            <option value="NOT_STARTED">Not started</option>
            <option value="IN_REVIEW">In review</option>
            <option value="APPROVED">Approved</option>
            <option value="CHANGES_REQUESTED">Changes requested</option>
          </select>
        </div>
        <div class="mb-0">
          <label class="form-label">Notes</label>
          <textarea v-model="reviewForm.notes" class="form-control" rows="4" placeholder="Optional feedback for the owner" />
        </div>
      </form>
      <template #footer>
        <button type="button" class="btn btn-secondary" @click="showReviewModal = false">Cancel</button>
        <button type="submit" form="review-form" class="btn btn-primary">Save</button>
      </template>
    </BsModal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import BsModal from '../../components/BsModal.vue'
import api from '../../services/api'
import { toastError, toastSuccess } from '../../services/toast'
import { useTableSort } from '../../composables/useTableSort'

const applications = ref([])
const users = ref([])
const showModal = ref(false)
const showReviewModal = ref(false)
const reviewApp = ref(null)
const reviewForm = reactive({
  status: 'NOT_STARTED',
  notes: ''
})
const editing = ref(null)
const form = reactive({
  name: '',
  description: '',
  ownerId: null,
  criticality: null,
  dataClassification: null,
  regulatoryScope: '',
  businessOwnerName: '',
  technicalOwnerName: '',
  lifecycleStatus: null,
  applicationPurpose: null,
  hostingModel: null,
  userAudienceScope: null,
  integrationScope: null,
  dataScopePii: false,
  dataScopePci: false,
  dataScopeSox: false,
  dataScopeHipaa: false
})

const sortableApplications = computed(() => applications.value)
const { sortedRows, toggleSort, sortIndicator } = useTableSort(sortableApplications, {
  initialKey: 'name',
  valueGetters: {
    owner: (row) => row.ownerDisplayName || row.ownerEmail || '',
    lifecycle: (row) => row.lifecycleStatus || ''
  }
})

onMounted(load)

async function load() {
  const [appsRes, usersRes] = await Promise.all([
    api.get('/api/applications'),
    api.get('/api/users')
  ])
  applications.value = appsRes.data || []
  users.value = usersRes.data || []
}

function openModal(app = null) {
  editing.value = app
  form.name = app?.name ?? ''
  form.description = app?.description ?? ''
  form.ownerId = app?.ownerId ?? null
  form.criticality = app?.criticality ?? null
  form.dataClassification = app?.dataClassification ?? null
  form.regulatoryScope = app?.regulatoryScope ?? ''
  form.businessOwnerName = app?.businessOwnerName ?? ''
  form.technicalOwnerName = app?.technicalOwnerName ?? ''
  form.lifecycleStatus = app?.lifecycleStatus ?? null
  form.applicationPurpose = app?.applicationPurpose ?? null
  form.hostingModel = app?.hostingModel ?? null
  form.userAudienceScope = app?.userAudienceScope ?? null
  form.integrationScope = app?.integrationScope ?? null
  form.dataScopePii = !!app?.dataScopePii
  form.dataScopePci = !!app?.dataScopePci
  form.dataScopeSox = !!app?.dataScopeSox
  form.dataScopeHipaa = !!(app?.dataScopeHipaa ?? app?.dataScopePhi)
  showModal.value = true
}

async function save() {
  try {
    if (editing.value?.id) {
      await api.put(`/api/applications/${editing.value.id}`, form)
      toastSuccess('Application updated.')
    } else {
      await api.post('/api/applications', form)
      toastSuccess('Application created.')
    }
    showModal.value = false
    load()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to save')
  }
}

async function remove(id) {
  if (!confirm('Delete this application?')) return
  try {
    await api.delete(`/api/applications/${id}`)
    toastSuccess('Application deleted.')
    load()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to delete')
  }
}

function secReviewLabel(status) {
  const m = {
    NOT_STARTED: 'Not started',
    IN_REVIEW: 'In review',
    APPROVED: 'Approved',
    CHANGES_REQUESTED: 'Changes requested'
  }
  return m[status] || status || '—'
}

function secReviewBadgeClass(status) {
  if (status === 'APPROVED') return 'text-bg-success'
  if (status === 'CHANGES_REQUESTED') return 'text-bg-warning text-dark'
  if (status === 'IN_REVIEW') return 'text-bg-info text-dark'
  return 'text-bg-secondary'
}

function openReviewModal(app) {
  reviewApp.value = app
  reviewForm.status = app.securityArchitectureReview?.status || 'NOT_STARTED'
  reviewForm.notes = app.securityArchitectureReview?.notes || ''
  showReviewModal.value = true
}

async function saveReview() {
  if (!reviewApp.value?.id) return
  try {
    await api.patch(`/api/applications/${reviewApp.value.id}/security-architecture-review`, {
      status: reviewForm.status,
      notes: reviewForm.notes || null
    })
    toastSuccess('Security review updated.')
    showReviewModal.value = false
    load()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to save review')
  }
}
</script>
