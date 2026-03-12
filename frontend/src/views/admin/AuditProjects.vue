<template>
  <div>
    <h1 class="h3 mb-3">Audit Projects</h1>
    <div class="alert alert-info py-2">
      Create a point-in-time audit project (for example, PCI 2026), choose all in-scope applications, and automatically create linked audits for the selected scope.
    </div>

    <section class="card shadow-sm mb-3" v-if="canManageProjects">
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
            <div class="alert alert-light border py-2 small mb-2">
              1) Search in <strong>Available applications</strong>, 2) check rows, 3) click <strong>Add checked</strong>.
              Use <strong>Remove checked</strong> on the right to take apps back out.
            </div>
            <div class="row g-3">
              <div class="col-lg-6">
                <div class="card border">
                  <div class="card-body p-3">
                    <div class="d-flex justify-content-between align-items-center mb-2">
                      <h3 class="h6 mb-0">Available applications</h3>
                      <span class="small text-muted">{{ availableApps.length }} available</span>
                    </div>
                    <input v-model.trim="appSearch" class="form-control form-control-sm mb-2" placeholder="Search by app name, owner, or scope..." />
                    <div class="d-flex gap-2 mb-2">
                      <button type="button" class="btn btn-outline-primary btn-sm" :disabled="!availableSelection.length" @click="addChecked">
                        Add checked
                      </button>
                      <button type="button" class="btn btn-outline-secondary btn-sm" :disabled="!availableApps.length" @click="addAllVisible">
                        Add all visible
                      </button>
                    </div>
                    <div class="transfer-list border rounded">
                      <table class="table table-sm align-middle mb-0">
                        <thead>
                          <tr>
                            <th style="width: 34px;"></th>
                            <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleAvailableSort()">Application {{ availableSortIndicator() }}</button></th>
                            <th style="width: 80px;"></th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr v-for="app in availableApps" :key="`available-${app.id}`">
                            <td><input v-model="availableSelection" class="form-check-input" type="checkbox" :value="app.id" /></td>
                            <td>
                              {{ app.name }}
                              <div class="small text-muted">{{ app.regulatoryScope || '-' }}</div>
                            </td>
                            <td><button type="button" class="btn btn-outline-primary btn-sm" @click="addApplication(app.id)">Add</button></td>
                          </tr>
                        </tbody>
                      </table>
                      <div v-if="!availableApps.length" class="text-muted small p-2">No available apps match your search.</div>
                    </div>
                  </div>
                </div>
              </div>
              <div class="col-lg-6">
                <div class="card border">
                  <div class="card-body p-3">
                    <div class="d-flex justify-content-between align-items-center mb-2">
                      <h3 class="h6 mb-0">In this project</h3>
                      <span class="small text-muted">{{ selectedApplications.length }} selected</span>
                    </div>
                    <div class="d-flex gap-2 mb-2">
                      <button type="button" class="btn btn-outline-danger btn-sm" :disabled="!selectedSelection.length" @click="removeChecked">
                        Remove checked
                      </button>
                      <button type="button" class="btn btn-outline-secondary btn-sm" :disabled="!selectedApplications.length" @click="form.applicationIds = []">
                        Remove all
                      </button>
                    </div>
                    <div class="transfer-list border rounded">
                      <table class="table table-sm align-middle mb-0">
                        <thead>
                          <tr>
                            <th style="width: 34px;"></th>
                            <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSelectedSort()">Application {{ selectedSortIndicator() }}</button></th>
                            <th style="width: 90px;"></th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr v-for="app in selectedApplications" :key="`selected-${app.id}`">
                            <td><input v-model="selectedSelection" class="form-check-input" type="checkbox" :value="app.id" /></td>
                            <td>
                              {{ app.name }}
                              <div class="small text-muted">{{ app.regulatoryScope || '-' }}</div>
                            </td>
                            <td><button type="button" class="btn btn-outline-danger btn-sm" @click="removeApplication(app.id)">Remove</button></td>
                          </tr>
                        </tbody>
                      </table>
                      <div v-if="!selectedApplications.length" class="text-muted small p-2">No apps in this project yet.</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="col-12">
            <div class="alert alert-secondary py-2 mb-2">
              <div class="fw-semibold small mb-1">Project scope summary before create</div>
              <div class="small">
                This project will include <strong>{{ selectedApplications.length }}</strong> application(s):
                <span v-if="selectedApplications.length">
                  {{ selectedPreview }}
                  <span v-if="selectedApplications.length > 5"> (+{{ selectedApplications.length - 5 }} more)</span>
                </span>
                <span v-else>No apps selected yet.</span>
              </div>
            </div>
            <button type="submit" class="btn btn-primary">Create project and audits</button>
          </div>
        </form>
      </div>
    </section>

    <section class="card shadow-sm mb-3" v-if="canManageProjects && editingProjectId">
      <div class="card-body">
        <h2 class="h5 mb-3">Edit project</h2>
        <form class="row g-3" @submit.prevent="saveEdit">
          <div class="col-md-4">
            <label class="form-label">Project name</label>
            <input v-model.trim="editForm.name" class="form-control" required />
          </div>
          <div class="col-md-2">
            <label class="form-label">Year</label>
            <input v-model.number="editForm.year" type="number" min="2020" max="2100" class="form-control" required />
          </div>
          <div class="col-md-3">
            <label class="form-label">Framework tag</label>
            <input v-model.trim="editForm.frameworkTag" class="form-control" placeholder="PCI, SOX, SOC2..." />
          </div>
          <div class="col-md-3">
            <label class="form-label">Due date</label>
            <input v-model="editForm.dueAt" type="date" class="form-control" />
          </div>
          <div class="col-12">
            <label class="form-label">Notes</label>
            <textarea v-model.trim="editForm.notes" class="form-control" rows="2" />
          </div>
          <div class="col-12">
            <label class="form-label">Applications in scope</label>
            <div class="alert alert-light border py-2 small mb-2">
              Update scope exactly the same way as create: move apps between Available and In this project, then save.
            </div>
            <div class="row g-3">
              <div class="col-lg-6">
                <div class="card border">
                  <div class="card-body p-3">
                    <div class="d-flex justify-content-between align-items-center mb-2">
                      <h3 class="h6 mb-0">Available applications</h3>
                      <span class="small text-muted">{{ editAvailableApps.length }} available</span>
                    </div>
                    <input v-model.trim="editAppSearch" class="form-control form-control-sm mb-2" placeholder="Search by app name, owner, or scope..." />
                    <div class="d-flex gap-2 mb-2">
                      <button type="button" class="btn btn-outline-primary btn-sm" :disabled="!editAvailableSelection.length" @click="addEditChecked">
                        Add checked
                      </button>
                      <button type="button" class="btn btn-outline-secondary btn-sm" :disabled="!editAvailableApps.length" @click="addEditAllVisible">
                        Add all visible
                      </button>
                    </div>
                    <div class="transfer-list border rounded">
                      <table class="table table-sm align-middle mb-0">
                        <thead>
                          <tr>
                            <th style="width: 34px;"></th>
                            <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleEditAvailableSort()">Application {{ editAvailableSortIndicator() }}</button></th>
                            <th style="width: 80px;"></th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr v-for="app in editAvailableApps" :key="`edit-available-${app.id}`">
                            <td><input v-model="editAvailableSelection" class="form-check-input" type="checkbox" :value="app.id" /></td>
                            <td>
                              {{ app.name }}
                              <div class="small text-muted">{{ app.regulatoryScope || '-' }}</div>
                            </td>
                            <td><button type="button" class="btn btn-outline-primary btn-sm" @click="addEditApplication(app.id)">Add</button></td>
                          </tr>
                        </tbody>
                      </table>
                      <div v-if="!editAvailableApps.length" class="text-muted small p-2">No available apps match your search.</div>
                    </div>
                  </div>
                </div>
              </div>
              <div class="col-lg-6">
                <div class="card border">
                  <div class="card-body p-3">
                    <div class="d-flex justify-content-between align-items-center mb-2">
                      <h3 class="h6 mb-0">In this project</h3>
                      <span class="small text-muted">{{ editSelectedApplications.length }} selected</span>
                    </div>
                    <div class="d-flex gap-2 mb-2">
                      <button type="button" class="btn btn-outline-danger btn-sm" :disabled="!editSelectedSelection.length" @click="removeEditChecked">
                        Remove checked
                      </button>
                      <button type="button" class="btn btn-outline-secondary btn-sm" :disabled="!editSelectedApplications.length" @click="editForm.applicationIds = []">
                        Remove all
                      </button>
                    </div>
                    <div class="transfer-list border rounded">
                      <table class="table table-sm align-middle mb-0">
                        <thead>
                          <tr>
                            <th style="width: 34px;"></th>
                            <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleEditSelectedSort()">Application {{ editSelectedSortIndicator() }}</button></th>
                            <th style="width: 90px;"></th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr v-for="app in editSelectedApplications" :key="`edit-selected-${app.id}`">
                            <td><input v-model="editSelectedSelection" class="form-check-input" type="checkbox" :value="app.id" /></td>
                            <td>
                              {{ app.name }}
                              <div class="small text-muted">{{ app.regulatoryScope || '-' }}</div>
                            </td>
                            <td><button type="button" class="btn btn-outline-danger btn-sm" @click="removeEditApplication(app.id)">Remove</button></td>
                          </tr>
                        </tbody>
                      </table>
                      <div v-if="!editSelectedApplications.length" class="text-muted small p-2">No apps in this project yet.</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="col-12 d-flex gap-2">
            <button type="submit" class="btn btn-primary">Save changes</button>
            <button type="button" class="btn btn-outline-secondary" @click="cancelEdit">Cancel</button>
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
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('name')">Name {{ sortIndicator('name') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('frameworkTag')">Framework {{ sortIndicator('frameworkTag') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('year')">Year {{ sortIndicator('year') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('scopedApplications')">Scoped Apps {{ sortIndicator('scopedApplications') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('totalAudits')">Audits {{ sortIndicator('totalAudits') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('completeAudits')">Completed {{ sortIndicator('completeAudits') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('createdAt')">Created {{ sortIndicator('createdAt') }}</button></th>
                <th v-if="canManageProjects"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="p in sortedProjects" :key="p.id">
                <td>
                  <div class="fw-semibold">{{ p.name }}</div>
                  <div class="small text-muted">{{ (p.scopedApplications || []).map((a) => a.name).join(', ') || 'No scoped apps' }}</div>
                </td>
                <td>{{ p.frameworkTag || '-' }}</td>
                <td>{{ p.year }}</td>
                <td>{{ (p.scopedApplications || []).length }}</td>
                <td>{{ p.totalAudits || 0 }}</td>
                <td>{{ p.completeAudits || 0 }}</td>
                <td>{{ formatDate(p.createdAt) }}</td>
                <td v-if="canManageProjects" class="text-nowrap">
                  <button class="btn btn-outline-primary btn-sm me-2" @click="startEdit(p)">Edit</button>
                  <button class="btn btn-outline-danger btn-sm" @click="deleteProject(p.id)">Delete</button>
                </td>
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
import { useTableSort } from '../../composables/useTableSort'

const applications = ref([])
const projects = ref([])
const appSearch = ref('')
const availableSelection = ref([])
const selectedSelection = ref([])
const availableSortDir = ref('asc')
const selectedSortDir = ref('asc')
const editAppSearch = ref('')
const editAvailableSelection = ref([])
const editSelectedSelection = ref([])
const editAvailableSortDir = ref('asc')
const editSelectedSortDir = ref('asc')
const editingProjectId = ref(null)
const editForm = reactive({
  name: '',
  frameworkTag: '',
  year: new Date().getFullYear(),
  dueAt: '',
  notes: '',
  applicationIds: []
})
const form = reactive({
  name: '',
  frameworkTag: '',
  year: new Date().getFullYear(),
  dueAt: '',
  notes: '',
  applicationIds: []
})

const availableApps = computed(() => {
  const term = appSearch.value.trim().toLowerCase()
  const selected = new Set(form.applicationIds)
  const rows = applications.value
    .filter((a) => !selected.has(a.id))
    .filter((a) => {
      if (!term) return true
      const hay = `${a.name} ${a.regulatoryScope || ''} ${a.ownerDisplayName || ''} ${a.ownerEmail || ''}`.toLowerCase()
      return hay.includes(term)
    })
  const ordered = rows.slice().sort((a, b) => a.name.localeCompare(b.name))
  return availableSortDir.value === 'asc' ? ordered : ordered.reverse()
})

const selectedApplications = computed(() => {
  const selected = new Set(form.applicationIds)
  const ordered = applications.value
    .filter((a) => selected.has(a.id))
    .slice()
    .sort((a, b) => a.name.localeCompare(b.name))
  return selectedSortDir.value === 'asc' ? ordered : ordered.reverse()
})

const editAvailableApps = computed(() => {
  const term = editAppSearch.value.trim().toLowerCase()
  const selected = new Set(editForm.applicationIds)
  const rows = applications.value
    .filter((a) => !selected.has(a.id))
    .filter((a) => {
      if (!term) return true
      const hay = `${a.name} ${a.regulatoryScope || ''} ${a.ownerDisplayName || ''} ${a.ownerEmail || ''}`.toLowerCase()
      return hay.includes(term)
    })
  const ordered = rows.slice().sort((a, b) => a.name.localeCompare(b.name))
  return editAvailableSortDir.value === 'asc' ? ordered : ordered.reverse()
})

const editSelectedApplications = computed(() => {
  const selected = new Set(editForm.applicationIds)
  const ordered = applications.value
    .filter((a) => selected.has(a.id))
    .slice()
    .sort((a, b) => a.name.localeCompare(b.name))
  return editSelectedSortDir.value === 'asc' ? ordered : ordered.reverse()
})

const selectedPreview = computed(() => selectedApplications.value.slice(0, 5).map((a) => a.name).join(', '))
const canManageProjects = computed(() => true)
const { sortedRows: sortedProjects, toggleSort, sortIndicator } = useTableSort(projects, {
  initialKey: 'createdAt',
  initialDirection: 'desc',
  valueGetters: {
    scopedApplications: (row) => (row.scopedApplications || []).length
  }
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

function addApplication(appId) {
  if (!form.applicationIds.includes(appId)) form.applicationIds.push(appId)
  availableSelection.value = availableSelection.value.filter((id) => id !== appId)
}

function addChecked() {
  if (!availableSelection.value.length) return
  form.applicationIds = Array.from(new Set([...form.applicationIds, ...availableSelection.value]))
  availableSelection.value = []
}

function addAllVisible() {
  form.applicationIds = Array.from(new Set([...form.applicationIds, ...availableApps.value.map((a) => a.id)]))
  availableSelection.value = []
}

function removeApplication(appId) {
  form.applicationIds = form.applicationIds.filter((id) => id !== appId)
  selectedSelection.value = selectedSelection.value.filter((id) => id !== appId)
}

function removeChecked() {
  if (!selectedSelection.value.length) return
  const toRemove = new Set(selectedSelection.value)
  form.applicationIds = form.applicationIds.filter((id) => !toRemove.has(id))
  selectedSelection.value = []
}

watch(selectedApplications, () => {
  const selected = new Set(form.applicationIds)
  availableSelection.value = availableSelection.value.filter((id) => !selected.has(id))
  selectedSelection.value = selectedSelection.value.filter((id) => selected.has(id))
})

watch(editSelectedApplications, () => {
  const selected = new Set(editForm.applicationIds)
  editAvailableSelection.value = editAvailableSelection.value.filter((id) => !selected.has(id))
  editSelectedSelection.value = editSelectedSelection.value.filter((id) => selected.has(id))
})

async function createProject() {
  if (!canManageProjects.value) {
    toastError('Only AUDIT_MANAGER can create audit projects')
    return
  }
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

function startEdit(project) {
  editingProjectId.value = project.id
  editForm.name = project.name || ''
  editForm.frameworkTag = project.frameworkTag || ''
  editForm.year = project.year || new Date().getFullYear()
  editForm.notes = project.notes || ''
  editForm.dueAt = project.dueAt ? new Date(project.dueAt).toISOString().slice(0, 10) : ''
  editForm.applicationIds = (project.scopedApplications || []).map((a) => a.id)
  editAppSearch.value = ''
  editAvailableSelection.value = []
  editSelectedSelection.value = []
}

function cancelEdit() {
  editingProjectId.value = null
}

function addEditApplication(appId) {
  if (!editForm.applicationIds.includes(appId)) editForm.applicationIds.push(appId)
  editAvailableSelection.value = editAvailableSelection.value.filter((id) => id !== appId)
}

function addEditChecked() {
  if (!editAvailableSelection.value.length) return
  editForm.applicationIds = Array.from(new Set([...editForm.applicationIds, ...editAvailableSelection.value]))
  editAvailableSelection.value = []
}

function addEditAllVisible() {
  editForm.applicationIds = Array.from(new Set([...editForm.applicationIds, ...editAvailableApps.value.map((a) => a.id)]))
  editAvailableSelection.value = []
}

function removeEditApplication(appId) {
  editForm.applicationIds = editForm.applicationIds.filter((id) => id !== appId)
  editSelectedSelection.value = editSelectedSelection.value.filter((id) => id !== appId)
}

function removeEditChecked() {
  if (!editSelectedSelection.value.length) return
  const toRemove = new Set(editSelectedSelection.value)
  editForm.applicationIds = editForm.applicationIds.filter((id) => !toRemove.has(id))
  editSelectedSelection.value = []
}

async function saveEdit() {
  if (!editingProjectId.value) return
  try {
    const payload = {
      name: editForm.name,
      frameworkTag: editForm.frameworkTag || null,
      year: editForm.year,
      notes: editForm.notes || null,
      applicationIds: editForm.applicationIds
    }
    if (editForm.dueAt) payload.dueAt = `${editForm.dueAt}T23:59:59Z`
    await api.put(`/api/audit-projects/${editingProjectId.value}`, payload)
    toastSuccess('Audit project updated.')
    editingProjectId.value = null
    await load()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to update audit project')
  }
}

async function deleteProject(projectId) {
  if (!window.confirm('Delete this audit project?')) return
  try {
    await api.delete(`/api/audit-projects/${projectId}`)
    toastSuccess('Audit project deleted.')
    if (editingProjectId.value === projectId) {
      editingProjectId.value = null
    }
    await load()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to delete audit project')
  }
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}

function toggleAvailableSort() {
  availableSortDir.value = availableSortDir.value === 'asc' ? 'desc' : 'asc'
}

function availableSortIndicator() {
  return availableSortDir.value === 'asc' ? '↑' : '↓'
}

function toggleSelectedSort() {
  selectedSortDir.value = selectedSortDir.value === 'asc' ? 'desc' : 'asc'
}

function selectedSortIndicator() {
  return selectedSortDir.value === 'asc' ? '↑' : '↓'
}

function toggleEditAvailableSort() {
  editAvailableSortDir.value = editAvailableSortDir.value === 'asc' ? 'desc' : 'asc'
}

function editAvailableSortIndicator() {
  return editAvailableSortDir.value === 'asc' ? '↑' : '↓'
}

function toggleEditSelectedSort() {
  editSelectedSortDir.value = editSelectedSortDir.value === 'asc' ? 'desc' : 'asc'
}

function editSelectedSortIndicator() {
  return editSelectedSortDir.value === 'asc' ? '↑' : '↓'
}
</script>

<style scoped>
.transfer-list {
  max-height: 420px;
  overflow-y: auto;
}
</style>
