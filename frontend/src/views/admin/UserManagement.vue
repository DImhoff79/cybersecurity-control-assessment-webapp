<template>
  <div>
    <h1 class="h3 mb-3">User Management</h1>

    <section class="card shadow-sm mb-3">
      <div class="card-body">
        <h2 class="h5 mb-3">Pending Social Access Requests</h2>
        <p v-if="!pendingRequests.length" class="text-muted mb-0">No pending requests.</p>
        <div v-else class="table-responsive">
          <table class="table table-striped mb-0">
            <thead>
              <tr>
                <th>Email</th>
                <th>Name</th>
                <th>Provider</th>
                <th>Requested</th>
                <th>Role</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in pendingRequests" :key="r.id">
                <td>{{ r.email }}</td>
                <td>{{ r.displayName || '-' }}</td>
                <td>{{ r.provider }}</td>
                <td>{{ formatDate(r.requestedAt) }}</td>
                <td>
                  <select v-model="decisionRole[r.id]" class="form-select form-select-sm">
                    <option value="APPLICATION_OWNER">APPLICATION_OWNER</option>
                    <option value="ADMIN">ADMIN</option>
                  </select>
                </td>
                <td class="text-nowrap">
                  <button class="btn btn-success btn-sm me-2" @click="approve(r.id)">Approve</button>
                  <button class="btn btn-outline-danger btn-sm" @click="reject(r.id)">Reject</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>

    <section class="card shadow-sm">
      <div class="card-body">
        <h2 class="h5 mb-3">Users</h2>
        <div class="table-responsive">
          <table class="table table-striped mb-0">
            <thead>
              <tr>
                <th>Email</th>
                <th>Name</th>
                <th>Role</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="u in users" :key="u.id">
                <td>{{ u.email }}</td>
                <td>{{ u.displayName || '-' }}</td>
                <td>{{ u.role }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import api from '../../services/api'
import { toastError, toastSuccess } from '../../services/toast'

const users = ref([])
const pendingRequests = ref([])
const decisionRole = reactive({})

onMounted(load)

async function load() {
  const [usersRes, requestsRes] = await Promise.all([
    api.get('/api/users'),
    api.get('/api/admin/access-requests')
  ])
  users.value = usersRes.data || []
  pendingRequests.value = requestsRes.data || []
  for (const request of pendingRequests.value) {
    if (!decisionRole[request.id]) decisionRole[request.id] = 'APPLICATION_OWNER'
  }
}

async function approve(id) {
  try {
    await api.post(`/api/admin/access-requests/${id}/approve`, { role: decisionRole[id] || 'APPLICATION_OWNER' })
    toastSuccess('Access request approved.')
    await load()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to approve access request')
  }
}

async function reject(id) {
  try {
    await api.post(`/api/admin/access-requests/${id}/reject`, {})
    toastSuccess('Access request rejected.')
    await load()
  } catch (e) {
    toastError(e.response?.data?.error || 'Failed to reject access request')
  }
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}
</script>
