<template>
  <div class="role-hub mx-auto" style="max-width: 56rem">
    <div class="mb-4">
      <h1 class="h3 mb-2">What would you like to do?</h1>
      <p class="text-muted mb-0">
        Signed in as <strong>{{ authStore.user?.displayName || authStore.user?.email }}</strong>
        <span v-if="roleLabel" class="text-muted"> · {{ roleLabel }}</span>
      </p>
    </div>

    <!-- Application owner: plain-language journey -->
    <div v-if="showOwnerJourney" class="card border-0 shadow-sm mb-4">
      <div class="card-body">
        <h2 class="h6 text-uppercase text-muted mb-3">Your path to go-live</h2>
        <ol class="mb-0 ps-3 owner-steps">
          <li class="mb-2">
            <strong>Register your application</strong> — Tell us what you are building and how it runs (no security jargon required on the intake).
            <router-link to="/start/new-application" class="ms-1">Start intake</router-link>
          </li>
          <li class="mb-2">
            <strong>Complete the assessment</strong> — Answer plain-language questions mapped to our control framework; attach evidence when helpful.
            <router-link to="/my-audits" class="ms-1">My assessments</router-link>
          </li>
          <li class="mb-2">
            <strong>Handle gaps</strong> — Request time-limited exceptions for controls you cannot meet yet, and track approval.
            <router-link to="/my-exceptions" class="ms-1">Control exceptions</router-link>
          </li>
        </ol>
        <div v-if="myApplications.length" class="mt-3 pt-3 border-top">
          <div class="small text-muted mb-2">Your applications</div>
          <ul class="mb-0 list-unstyled">
            <li v-for="app in myApplications" :key="app.id" class="mb-1">
              <span class="fw-medium">{{ app.name }}</span>
              <span v-if="app.description" class="text-muted small"> — {{ truncate(app.description, 80) }}</span>
            </li>
          </ul>
        </div>
        <p v-else-if="!appsLoading" class="text-muted small mb-0 mt-3 pt-3 border-top">No applications yet — start with Register a new application above.</p>
      </div>
    </div>

    <!-- Administrator quick checklist -->
    <div v-if="authStore.user?.role === 'ADMIN'" class="card border-0 shadow-sm mb-4">
      <div class="card-body">
        <h2 class="h6 text-uppercase text-muted mb-2">Admin checklist</h2>
        <ul class="mb-0 small ps-3">
          <li><router-link to="/admin/users">Users &amp; roles</router-link> — access and escalations</li>
          <li><router-link to="/admin/questionnaire">Questionnaire &amp; controls</router-link> — content health</li>
          <li><router-link to="/admin/applications">Applications</router-link> — inventory (if you use central onboarding)</li>
        </ul>
      </div>
    </div>

    <div class="row g-3">
      <div v-for="card in cards" :key="card.key" class="col-12 col-md-6">
        <button
          type="button"
          class="card role-hub-card h-100 w-100 text-start border-0 shadow-sm p-0"
          @click="router.push(card.to)"
        >
          <div class="card-body p-4">
            <div class="fw-semibold mb-1">{{ card.title }}</div>
            <p class="text-muted small mb-0">{{ card.description }}</p>
          </div>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import api from '../services/api'

const router = useRouter()
const authStore = useAuthStore()
const myApplications = ref([])
const appsLoading = ref(false)

const showOwnerJourney = computed(
  () => !authStore.canAccessAdmin && authStore.user?.role === 'APPLICATION_OWNER'
)

onMounted(async () => {
  if (!showOwnerJourney.value) return
  appsLoading.value = true
  try {
    const res = await api.get('/api/applications')
    const uid = authStore.user?.id
    myApplications.value = (res.data || []).filter((a) => a.ownerId === uid || a.ownerEmail === authStore.user?.email)
  } catch {
    myApplications.value = []
  } finally {
    appsLoading.value = false
  }
})

function truncate(s, n) {
  if (!s || s.length <= n) return s
  return `${s.slice(0, n)}…`
}

const roleLabel = computed(() => {
  const r = authStore.user?.role
  if (!r) return ''
  const labels = {
    ADMIN: 'Administrator',
    AUDIT_MANAGER: 'Audit manager',
    AUDITOR: 'Auditor',
    APPLICATION_OWNER: 'Application owner'
  }
  return labels[r] || r
})

const cards = computed(() => {
  const role = authStore.user?.role
  const hp = (p) => authStore.hasPermission(p)

  if (!authStore.canAccessAdmin) {
    const base = [
      {
        key: 'my-audits',
        title: 'Work on my assessments',
        description: 'Your assessments—status, progress, and what is left before approval.',
        to: '/my-audits'
      }
    ]
    if (role === 'APPLICATION_OWNER') {
      base.unshift({
        key: 'new-app',
        title: 'Register a new application',
        description: 'Short intake, then the common controls assessment for your app.',
        to: '/start/new-application'
      })
    }
    base.push({
      key: 'my-exceptions',
      title: 'Control exceptions',
      description: 'Request and track exceptions when a control cannot be met on time.',
      to: '/my-exceptions'
    })
    return base
  }

  const out = []

  if (hp('REPORT_VIEW')) {
    out.push(
      {
        key: 'program-home',
        title: 'Audit program home',
        description: 'Snapshot of applications, audits, findings, and risks—then drill into each area.',
        to: '/admin/program-home'
      },
      {
        key: 'queue',
        title: 'Review audits assigned to you',
        description: 'Prioritized queue, assignments, and reviews ready for action.',
        to: '/admin/operations'
      },
      {
        key: 'projects',
        title: 'Audit projects',
        description: 'Programs, scoped applications, and audits.',
        to: '/admin/audit-projects'
      },
      {
        key: 'reports',
        title: 'Reports & metrics',
        description: 'Statuses, trends, and exports.',
        to: '/admin/reports'
      },
      {
        key: 'workspace',
        title: 'My workspace',
        description: 'Assessments and shortcuts in the admin shell.',
        to: '/admin/my-audits'
      }
    )
  }

  if (role === 'AUDITOR') {
    out.push({
      key: 'exceptions-scoped',
      title: 'Control exceptions (my audits)',
      description: 'Exceptions for audits you can access—review when you are the approver.',
      to: '/admin/workspace-exceptions'
    })
  }

  if (hp('AUDIT_MANAGEMENT')) {
    out.push(
      {
        key: 'exceptions',
        title: 'Control exceptions (program)',
        description: 'All exception requests—approve, reject, and track SLAs.',
        to: '/admin/control-exceptions'
      },
      {
        key: 'questionnaire',
        title: 'Questionnaire & controls',
        description: 'Controls, questions, and questionnaire hub.',
        to: '/admin/questionnaire'
      }
    )
  }

  if (hp('POLICY_MANAGEMENT')) {
    out.push({
      key: 'policies',
      title: 'Policies & procedures',
      description: 'Published policies, versions, and governance.',
      to: '/admin/policies'
    })
  }

  if (hp('USER_MANAGEMENT')) {
    out.push({
      key: 'users',
      title: 'User management',
      description: 'Create users, assign roles, and manage access.',
      to: '/admin/users'
    })
  }

  if (role === 'ADMIN' || role === 'AUDIT_MANAGER') {
    out.push({
      key: 'approval-delegates',
      title: 'Approval delegates',
      description: 'Who may approve control exceptions besides audit managers.',
      to: '/admin/approval-delegates'
    })
  }

  return out
})
</script>

<style scoped>
.role-hub-card {
  background: #fff;
  border-radius: 0.5rem;
  transition: box-shadow 0.15s ease, transform 0.15s ease;
  cursor: pointer;
}

.role-hub-card:hover {
  box-shadow: 0 0.35rem 1rem rgba(11, 35, 64, 0.12) !important;
  transform: translateY(-1px);
}

.role-hub-card:focus {
  outline: 2px solid #0d6efd;
  outline-offset: 2px;
}

.owner-steps li {
  line-height: 1.5;
}
</style>
