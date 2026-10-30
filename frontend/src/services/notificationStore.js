import { computed, ref } from 'vue'
import api from '@/services/api.js'

export const notifications = ref([])
export const nombreNotificationsNonLues = computed(
  () => notifications.value.filter((notification) => !notification.dateLecture).length,
)

export async function chargerNotifications() {
  const { data } = await api.get('/notifications')
  notifications.value = data
}

export async function marquerNotificationCommeLue(id) {
  const { data } = await api.patch(`/notifications/${id}/lecture`)
  notifications.value = notifications.value.map((notification) =>
    notification.id === id ? data : notification,
  )
}
