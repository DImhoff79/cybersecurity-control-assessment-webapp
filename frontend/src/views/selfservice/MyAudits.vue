<template>
  <div>
    <h1 class="h3 mb-3">My Audits</h1>
    <p v-if="!items.length && !loading" class="text-muted">No audits assigned to you.</p>
    <div v-else class="card shadow-sm">
      <div class="card-body">
        <div class="table-responsive">
          <table class="table table-striped table-hover align-middle mb-0">
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
