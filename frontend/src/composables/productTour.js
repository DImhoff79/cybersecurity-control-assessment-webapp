import { driver } from 'driver.js'
import 'driver.js/dist/driver.css'

const base = {
  showProgress: true,
  animate: true,
  allowClose: true,
  overlayOpacity: 0.72,
  stagePadding: 10,
  stageRadius: 8,
  nextBtnText: 'Next',
  prevBtnText: 'Back',
  doneBtnText: 'Done',
  progressText: '{{current}} of {{total}}'
}

function safeDrive(steps) {
  const resolved = steps.filter((s) => {
    if (!s.element) return true
    const el = typeof s.element === 'string' ? document.querySelector(s.element) : s.element
    return !!el
  })
  if (!resolved.length) return
  const d = driver({ ...base, steps: resolved })
  d.drive()
}

/**
 * Application owners: audits, questionnaire workflow, and workspace shortcuts.
 * Shown when role is APPLICATION_OWNER in the self-service shell.
 */
export function startApplicationOwnerWorkspaceTour() {
  safeDrive([
    {
      popover: {
        title: 'Your role: application owner',
        description:
          'As an application owner, you complete cybersecurity assessments for applications assigned to you. An audit ties your application to a program year and project, and asks structured questions mapped to security controls so risk and compliance can be reviewed consistently.',
        side: 'over',
        align: 'center'
      }
    },
    {
      popover: {
        title: 'How you complete an assessment',
        description:
          'From My Audits, choose Start or Resume to open the questionnaire. For each topic, answer the questions (for example: fully in place, partial, not yet, or not applicable). Expand Notes & supporting files to add context or upload evidence for auditors. Use Save draft anytime. Move through sections with Next section or Continue to additional questions, then Submit for review when everything is complete. If work is split across people, use My Tasks for control-level updates.',
        side: 'over',
        align: 'center'
      }
    },
    {
      element: '#tour-nav-my-audits',
      popover: {
        title: 'My Audits',
        description:
          'This is your home for every assessment assigned to you. Open it anytime to see status, completion, and the button to start, resume, or view a submission.',
        side: 'right',
        align: 'start'
      }
    },
    {
      element: '#tour-my-audits-panel',
      popover: {
        title: 'Your audit list',
        description:
          'Each row is an assignment: application, year, project, status, and how far along you are. Use the action on the right to open the response experience (or view a completed submission).',
        side: 'bottom',
        align: 'center'
      }
    },
    {
      element: '#tour-nav-my-tasks',
      popover: {
        title: 'My Tasks',
        description:
          'When controls are delegated to you, they appear here. Update status and notes, or use Open Task to jump into the same assessment with that control in context.',
        side: 'right',
        align: 'start'
      }
    },
    {
      element: '#tour-nav-my-policies',
      popover: {
        title: 'My Policies',
        description:
          'Use this for policy-related attestations your organization may require outside of a specific audit questionnaire.',
        side: 'right',
        align: 'start'
      }
    },
    {
      element: '#tour-workspace-header',
      popover: {
        title: 'Notifications & session',
        description:
          'Watch for updates from your audit program. Log out when you are done on a shared device.',
        side: 'bottom',
        align: 'end'
      }
    }
  ])
}

/** Self-service shell (My Audits, Tasks, etc.) — non–application-owner roles */
export function startWorkspaceTour() {
  safeDrive([
    {
      popover: {
        title: 'Welcome',
        description:
          'This short tour shows where to find your work. Use Next to continue, or Close any time.',
        side: 'over',
        align: 'center'
      }
    },
    {
      element: '#tour-workspace-sidebar',
      popover: {
        title: 'Workspace menu',
        description:
          'Use My Audits for assessments assigned to you, My Tasks for control work, and My Policies for attestations. Profile and account settings are here too.',
        side: 'right',
        align: 'start'
      }
    },
    {
      element: '#tour-workspace-header',
      popover: {
        title: 'Notifications & session',
        description:
          'Check notifications for updates. Use Log out when you are finished on a shared computer.',
        side: 'bottom',
        align: 'end'
      }
    },
    {
      element: '#tour-workspace-content',
      popover: {
        title: 'Main page',
        description:
          'The content you are working on—such as an audit or task list—appears here. This tour will show again the next time you load the app.',
        side: 'top',
        align: 'center'
      }
    }
  ])
}

/** Admin layout (when you open /admin) */
export function startAdminTour() {
  safeDrive([
    {
      popover: {
        title: 'Admin workspace',
        description:
          'This tour highlights the admin layout. Use Next to continue or Close to skip.',
        side: 'over',
        align: 'center'
      }
    },
    {
      element: '#admin-sidebar-nav',
      popover: {
        title: 'Navigation',
        description:
          'Sidebar groups program areas: workspace shortcuts, audit program, governance, risk, reporting, and admin ops. Your access depends on your role.',
        side: 'right',
        align: 'start'
      }
    },
    {
      element: '#tour-admin-toolbar',
      popover: {
        title: 'Where you are',
        description:
          'Breadcrumbs show the section and page. Notifications and Account are on the right.',
        side: 'bottom',
        align: 'start'
      }
    },
    {
      element: '#tour-admin-content',
      popover: {
        title: 'Page content',
        description:
          'Lists, forms, and reports for the page you selected are shown here. On small screens, use Menu to open the sidebar.',
        side: 'top',
        align: 'center'
      }
    }
  ])
}
