import { useRouter } from 'vue-router'
import { twinQueryForAlarm } from '../utils/alarmNavigation'

/** 报警跳转孪生视图 */
export function useAlarmNavigation() {
  const router = useRouter()

  function navigateToAlarmTwin(alarm, equipmentList = [], fallbackLineId) {
    router.push(twinQueryForAlarm(alarm, equipmentList, fallbackLineId))
  }

  return { navigateToAlarmTwin }
}
