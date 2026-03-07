<template>
  <div>
    <h1 class="page-title">My Audits</h1>
    <p v-if="!items.length && !loading">No audits assigned to you.</p>
    <div v-else class="card">
      <table>
        <thead>
          <tr>
            <th>Application</th>
            <th>Year</th>
            <th>Status</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="a in items" :key="a.id">
            <td>{{ a.applicationName }}</td>
            <td>{{ a.year }}</td>
            <td>{{ a.status }}</td>
            <td>
              <router-link :to="`/audits/${a.id}/respond`" class="btn btn-primary btn-sm">Fill out audit</router-link>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../../services/api'

const items = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await api.get('/api/my-audits')
    items.value = res.data || []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.btn-sm { padding: 0.35rem 0.75rem; font-size: 0.85rem; }
</style>
