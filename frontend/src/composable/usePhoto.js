import { ref,  onUnmounted } from "vue";

export function usePhoto() {
    const photo = ref(null)
    const photoUrl = ref('')

    function choisirPhoto(event) {
        const fichier = event.target.files?.[0]

        if (!fichier) return

        if (!fichier.type.startsWith('image/')) {
            alert('Veuillez sélectionner une image.')
            event.target.value = ''
            return
        }

        if (photoUrl.value) {
            URL.revokeObjectURL(photoUrl.value)
        }

        photo.value = fichier
        photoUrl.value = URL.createObjectURL(fichier)
    }

    onUnmounted(() => {
        if (photoUrl.value) {
            URL.revokeObjectURL(photoUrl.value)
        }
    })

    return {
        photo,
        photoUrl,
        choisirPhoto
    }
}
