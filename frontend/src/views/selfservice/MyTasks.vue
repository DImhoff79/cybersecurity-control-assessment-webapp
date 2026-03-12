<template>
  <div>
    <h1 class="h3 mb-3">My Tasks</h1>
    <p v-if="!tasks.length && !loading" class="text-muted">No delegated control tasks assigned.</p>
    <div v-else class="card shadow-sm">
      <div class="card-body">
        <div v-if="loading" class="text-muted">Loading...</div>
        <div v-else class="table-responsive">
          <table class="table table-striped align-middle mb-0">
            <thead>
              <tr>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('applicationName')">Application {{ sortIndicator('applicationName') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('auditYear')">Audit {{ sortIndicator('auditYear') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('control')">Control {{ sortIndicator('control') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('assignmentRole')">Role {{ sortIndicator('assignmentRole') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('status')">Status {{ sortIndicator('status') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('dueAt')">Due {{ sortIndicator('dueAt') }}</button></th>
                <th><button class="btn btn-link btn-sm p-0 text-decoration-none" @click="toggleSort('notes')">Notes {{ sortIndicator('notes') }}</button></th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="t in sortedRows" :key="t.assignmentId">
                <td>{{ t.applicationName }}</td>
                <td>{{ t.auditYear }}</td>
                <td>{{ t.controlControlId }} - {{ t.controlName }}</td>
                <td><span class="badge text-bg-light border">{{ t.assignmentRole }}</span></td>
                <td>
                  <select :value="t.status" class="form-select form-select-sm" @change="updateTaskStatus(t, $event.target.value)">
                    <option value="NOT_STARTED">Not started</option>
                    <option value="IN_PROGRESS">In progress</option>
                    <option value="PASS">Pass</option>
                    <option value="FAIL">Fail</option>
                    <option value="NA">N/A</option>
                  </select>
                </td>
                <td>{{ formatDate(t.dueAt) }}</td>
                <td>
                  <textarea class="form-control form-control-sm" rows="2" :value="t.notes || ''" @blur="updateTaskNotes(t, $event.target.value)" />
                </td>
                <td class="text-nowrap">
                  <router-link :to="`/audits/${t.auditId}/respond`" class="btn btn-outline-primary btn-sm">Open Task</router-link>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../../services/api'
import { toastError, toastSuccess } from '../../services/toast'
import { useTableSort } from '../../composables/useTableSort'

const tasks = ref([])
const loading = ref(true)
const { sortedRows, toggleSort, sortIndicator } = useTableSort(tasks, {
  initialKey: 'dueAt',
  valueGetters: {
    control: (row) => `${row.controlControlId} ${row.controlName}`
  }
})

onMounted(load)

async function load() {
  loading.value = true
  try {
    const res = await api.get('/api/audit-controls/my-tasks')
    tasks.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function updateTaskStatus(task, status) {
  try {
    await api.put(`/api/audit-controls/${task.auditControlId}`, { status })
    task.status = status
    toastSuccess('Task status updated.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to update task status')
  }
}

async function updateTaskNotes(task, notes) {
  if ((task.notes || '') === (notes || '')) return
  try {
    await api.put(`/api/audit-controls/${task.auditControlId}`, { notes })
    task.notes = notes
    toastSuccess('Task notes updated.')
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to update task notes')
  }
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleDateString()
}
</script>
