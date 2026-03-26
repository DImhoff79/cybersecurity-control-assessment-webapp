<template>
  <div class="role-hub mx-auto" style="max-width: 56rem">
    <div class="mb-4">
      <h1 class="h3 mb-2">What would you like to do?</h1>
      <p class="text-muted mb-0">
        Signed in as <strong>{{ authStore.user?.displayName || authStore.user?.email }}</strong>
        <span v-if="roleLabel" class="text-muted"> · {{ roleLabel }}</span>
      </p>
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
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

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
    return [
      {
        key: 'my-audits',
        title: 'Work on my assessments',
        description: 'Audits assigned to you—status, progress, and the questionnaire.',
        to: '/my-audits'
      },
      {
        key: 'my-tasks',
        title: 'My delegated tasks',
        description: 'Control tasks delegated to you with status and notes.',
        to: '/my-tasks'
      },
      {
        key: 'my-policies',
        title: 'Policy attestations',
        description: 'Policies you may need to attest to.',
        to: '/my-policies'
      },
      {
        key: 'my-exceptions',
        title: 'Control exceptions',
        description: 'Request and track exceptions for controls on your audits.',
        to: '/my-exceptions'
      }
    ]
  }

  const out = []

  if (hp('REPORT_VIEW')) {
    out.push(
      {
        key: 'queue',
        title: 'Audit queue & triage',
        description: 'Prioritized audits, filters, assignments, and submitted reviews.',
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
        description: 'My Audits, Tasks, and Policies in the admin shell.',
        to: '/admin/my-audits'
      }
    )
  }

  if (role === 'AUDITOR') {
    out.push({
      key: 'exceptions-scoped',
      title: 'Control exceptions (my audits)',
      description: 'Exceptions for audits you are assigned to—review and approve when authorized.',
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
      description: 'Designate who may approve control exceptions besides audit managers.',
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
</style>
