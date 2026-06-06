import { ref, computed } from 'vue'
import { LINE_ID } from '../api'

const ROLE_KEY = 'jqhc-user-role'
const LINE_KEY = 'jqhc-last-line'

export const USER_ROLES = {
  director: { id: 'director', label: '厂长', home: '/factory' },
  supervisor: { id: 'supervisor', label: '线长', home: null }
}

function readRole() {
  const v = localStorage.getItem(ROLE_KEY)
  return v === 'supervisor' ? 'supervisor' : 'director'
}

function readLastLine() {
  return localStorage.getItem(LINE_KEY) || LINE_ID
}

export function getDefaultHomePath() {
  const role = readRole()
  if (role === 'supervisor') {
    return `/lines/${readLastLine()}`
  }
  return '/factory'
}

export function rememberLine(lineId) {
  if (lineId) localStorage.setItem(LINE_KEY, lineId)
}

const roleRef = ref(readRole())

export function useUserRole() {
  const role = computed({
    get: () => roleRef.value,
    set: (v) => {
      roleRef.value = v === 'supervisor' ? 'supervisor' : 'director'
      localStorage.setItem(ROLE_KEY, roleRef.value)
    }
  })

  const roleMeta = computed(() => USER_ROLES[role.value] || USER_ROLES.director)
  const defaultHome = computed(() =>
    role.value === 'supervisor' ? `/lines/${readLastLine()}` : '/factory'
  )

  function setRole(next) {
    role.value = next
  }

  return { role, roleMeta, defaultHome, setRole, rememberLine }
}
