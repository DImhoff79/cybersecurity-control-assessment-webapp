<template>
  <div>
    <h1 class="h3 mb-1">Questionnaire</h1>
    <p class="text-muted mb-3">
      Manage your control and question library, then publish governance snapshots for future audits.
    </p>

    <div class="row g-3">
      <div class="col-lg-6">
        <div class="card shadow-sm h-100">
          <div class="card-body">
            <h2 class="h5">Library Builder</h2>
            <p class="text-muted">
              Edit controls and questions in one place and keep mappings current.
            </p>
            <router-link class="btn btn-primary btn-sm" :to="{ name: 'AdminQuestionnaireBuilder' }">
              Open Builder
            </router-link>
          </div>
        </div>
      </div>

      <div class="col-lg-6">
        <div class="card shadow-sm h-100">
          <div class="card-body">
            <h2 class="h5">Governance Versions</h2>
            <p class="text-muted">
              Create working snapshots, compare versions, and publish the baseline for new audits.
            </p>
            <router-link
              class="btn btn-primary btn-sm"
              :to="{ name: 'AdminQuestionnaireTemplates' }"
            >
              Open Governance
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
