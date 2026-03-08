<template>
  <div>
    <h1 class="h3 mb-3">Audit Projects</h1>
    <div class="alert alert-info py-2">
      Create a point-in-time audit project (for example, PCI 2026), choose all in-scope applications, and automatically create linked audits for the selected scope.
    </div>

    <section class="card shadow-sm mb-3">
      <div class="card-body">
        <h2 class="h5 mb-3">Create project</h2>
        <form class="row g-3" @submit.prevent="createProject">
          <div class="col-md-4">
            <label class="form-label">Project name</label>
            <input v-model.trim="form.name" class="form-control" required />
          </div>
          <div class="col-md-2">
            <label class="form-label">Year</label>
            <input v-model.number="form.year" type="number" min="2020" max="2100" class="form-control" required />
          </div>
          <div class="col-md-3">
            <label class="form-label">Framework tag</label>
            <input v-model.trim="form.frameworkTag" class="form-control" placeholder="PCI, SOX, SOC2..." />
          </div>
          <div class="col-md-3">
            <label class="form-label">Due date</label>
            <input v-model="form.dueAt" type="date" class="form-control" />
          </div>
          <div class="col-12">
            <label class="form-label">Notes</label>
            <textarea v-model.trim="form.notes" class="form-control" rows="2" />
          </div>
          <div class="col-12">
            <label class="form-label">Applications in scope</label>
            <div class="d-flex justify-content-between align-items-center mb-2">
              <small class="text-muted">
                Selected {{ selectedApplications.length }} of {{ applications.length }}
              </small>
              <button type="button" class="btn btn-link btn-sm p-0" @click="form.applicationIds = []">Clear all selected</button>
            </div>
            <div class="row g-2 align-items-center">
              <div class="col-md-5">
                <input v-model.trim="appSearch" class="form-control form-control-sm" placeholder="Search by app name, owner, or scope..." />
              </div>
              <div class="col-md-7 d-flex flex-wrap gap-2">
                <button type="button" class="btn btn-outline-secondary btn-sm me-2" @click="selectFiltered(true)">Select filtered</button>
                <button type="button" class="btn btn-outline-secondary btn-sm" @click="selectFiltered(false)">Clear filtered</button>
                <button type="button" class="btn btn-outline-secondary btn-sm" :disabled="!form.frameworkTag" @click="selectByFrameworkTag">
                  Select matching framework tag
                </button>
                <div class="form-check ms-2">
                  <input id="showSelectedOnly" v-model="showSelectedOnly" class="form-check-input" type="checkbox" />
                  <label for="showSelectedOnly" class="form-check-label small">Show selected only</label>
                </div>
              </div>
            </div>
            <div class="row g-2 align-items-center mt-1">
              <div class="col-md-3">
                <label class="form-label small mb-1">Rows per page</label>
                <select v-model.number="pageSize" class="form-select form-select-sm">
                  <option :value="10">10</option>
                  <option :value="25">25</option>
                  <option :value="50">50</option>
                </select>
              </div>
              <div class="col-md-9 d-flex flex-wrap gap-2 align-items-end">
                <button type="button" class="btn btn-outline-secondary btn-sm" @click="selectCurrentPage(true)">Select page</button>
                <button type="button" class="btn btn-outline-secondary btn-sm" @click="selectCurrentPage(false)">Clear page</button>
                <button type="button" class="btn btn-outline-secondary btn-sm" @click="selectFiltered(true)">
                  Select all results ({{ filteredApplications.length }})
                </button>
                <button type="button" class="btn btn-outline-secondary btn-sm" @click="selectFiltered(false)">Clear all results</button>
              </div>
            </div>
            <div v-if="selectedApplications.length" class="selected-chips mt-2">
              <span v-for="app in selectedApplications" :key="`chip-${app.id}`" class="badge text-bg-primary me-1 mb-1">
                {{ app.name }}
              </span>
            </div>
            <div class="scope-list border rounded mt-2">
              <table class="table table-sm align-middle mb-0">
                <thead>
                  <tr>
                    <th style="width: 40px;"></th>
                    <th>Application</th>
                    <th>Scope</th>
                    <th>Owner</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="app in pagedApplications" :key="app.id">
                    <td>
                      <input
                        :id="`scope-app-${app.id}`"
                        :checked="isSelected(app.id)"
                        class="form-check-input"
                        type="checkbox"
                        @change="toggleApplication(app.id, $event.target.checked)"
                      />
                    </td>
                    <td>
                      <label :for="`scope-app-${app.id}`" class="form-check-label">
                        {{ app.name }}
                      </label>
                    </td>
                    <td>{{ app.regulatoryScope || '-' }}</td>
                    <td>{{ app.ownerDisplayName || app.ownerEmail || '-' }}</td>
                  </tr>
                </tbody>
              </table>
              <div v-if="!filteredApplications.length" class="text-muted small p-2">
                No applications match the current search/filter.
              </div>
            </div>
            <div v-if="filteredApplications.length" class="d-flex justify-content-between align-items-center mt-2 small text-muted">
              <span>Showing {{ pageStart }}-{{ pageEnd }} of {{ filteredApplications.length }}</span>
              <div class="d-flex gap-2 align-items-center">
                <button type="button" class="btn btn-outline-secondary btn-sm" :disabled="page <= 1" @click="page--">Previous</button>
                <span>Page {{ page }} of {{ totalPages }}</span>
                <button type="button" class="btn btn-outline-secondary btn-sm" :disabled="page >= totalPages" @click="page++">Next</button>
              </div>
            </div>
          </div>
          <div class="col-12">
            <button type="submit" class="btn btn-primary">Create project and audits</button>
          </div>
        </form>
      </div>
    </section>

    <section class="card shadow-sm">
      <div class="card-body">
        <h2 class="h5 mb-3">Existing projects</h2>
        <div class="table-responsive">
          <table class="table table-striped mb-0">
            <thead>
              <tr>
                <th>Name</th>
                <th>Framework</th>
                <th>Year</th>
                <th>Scoped Apps</th>
                <th>Audits</th>
                <th>Completed</th>
                <th>Created</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="p in projects" :key="p.id">
                <td>{{ p.name }}</td>
                <td>{{ p.frameworkTag || '-' }}</td>
                <td>{{ p.year }}</td>
                <td>{{ (p.scopedApplications || []).length }}</td>
                <td>{{ p.totalAudits || 0 }}</td>
                <td>{{ p.completeAudits || 0 }}</td>
                <td>{{ formatDate(p.createdAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import api from '../../services/api'
import { toastError, toastSuccess } from '../../services/toast'

const applications = ref([])
const projects = ref([])
const appSearch = ref('')
const showSelectedOnly = ref(false)
const page = ref(1)
const pageSize = ref(10)
const form = reactive({
  name: '',
  frameworkTag: '',
  year: new Date().getFullYear(),
  dueAt: '',
  notes: '',
  applicationIds: []
})

const filteredApplications = computed(() => {
  const term = appSearch.value.trim().toLowerCase()
  let rows = applications.value
  if (term) {
    rows = rows.filter((a) => {
      const hay = `${a.name} ${a.regulatoryScope || ''} ${a.ownerDisplayName || ''} ${a.ownerEmail || ''}`.toLowerCase()
      return hay.includes(term)
    })
  }
  if (showSelectedOnly.value) {
    const selected = new Set(form.applicationIds)
    rows = rows.filter((a) => selected.has(a.id))
  }
  return rows.slice().sort((a, b) => a.name.localeCompare(b.name))
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredApplications.value.length / pageSize.value)))

const pagedApplications = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredApplications.value.slice(start, start + pageSize.value)
})

const pageStart = computed(() => {
  if (!filteredApplications.value.length) return 0
  return (page.value - 1) * pageSize.value + 1
})

const pageEnd = computed(() => Math.min(page.value * pageSize.value, filteredApplications.value.length))

const selectedApplications = computed(() => {
  const selected = new Set(form.applicationIds)
  return applications.value.filter((a) => selected.has(a.id)).sort((a, b) => a.name.localeCompare(b.name))
})

onMounted(load)

async function load() {
  const [appsRes, projectsRes] = await Promise.all([
    api.get('/api/applications'),
    api.get('/api/audit-projects')
  ])
  applications.value = appsRes.data || []
  projects.value = projectsRes.data || []
}

function selectFiltered(checked) {
  const ids = filteredApplications.value.map((a) => a.id)
  if (checked) {
    form.applicationIds = Array.from(new Set([...form.applicationIds, ...ids]))
  } else {
    form.applicationIds = form.applicationIds.filter((id) => !ids.includes(id))
  }
}

function selectCurrentPage(checked) {
  const ids = pagedApplications.value.map((a) => a.id)
  if (checked) {
    form.applicationIds = Array.from(new Set([...form.applicationIds, ...ids]))
  } else {
    form.applicationIds = form.applicationIds.filter((id) => !ids.includes(id))
  }
}

function isSelected(appId) {
  return form.applicationIds.includes(appId)
}

function toggleApplication(appId, checked) {
  if (checked) {
    if (!form.applicationIds.includes(appId)) form.applicationIds.push(appId)
    return
  }
  form.applicationIds = form.applicationIds.filter((id) => id !== appId)
}

function selectByFrameworkTag() {
  const tag = form.frameworkTag.trim().toLowerCase()
  if (!tag) return
  const matches = applications.value
    .filter((a) => String(a.regulatoryScope || '').toLowerCase().includes(tag))
    .map((a) => a.id)
  form.applicationIds = Array.from(new Set([...form.applicationIds, ...matches]))
}

watch([appSearch, showSelectedOnly, pageSize], () => {
  page.value = 1
})

watch(filteredApplications, () => {
  if (page.value > totalPages.value) page.value = totalPages.value
})

async function createProject() {
  try {
    const payload = {
      name: form.name,
      frameworkTag: form.frameworkTag || null,
      year: form.year,
      notes: form.notes || null,
      applicationIds: form.applicationIds
    }
    if (form.dueAt) payload.dueAt = `${form.dueAt}T23:59:59Z`
    await api.post('/api/audit-projects', payload)
    toastSuccess('Audit project created.')
    form.name = ''
    form.frameworkTag = ''
    form.notes = ''
    form.applicationIds = []
    await load()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to create audit project')
  }
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}
</script>

<style scoped>
.scope-list {
  max-height: 260px;
  overflow-y: auto;
}
</style>
