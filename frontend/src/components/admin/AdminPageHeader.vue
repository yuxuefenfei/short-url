<template>
  <div class="admin-page-header">
    <div class="title-group">
      <h1 class="page-title">{{ title }}</h1>
      <p v-if="subtitle" class="page-subtitle">{{ subtitle }}</p>
    </div>
    <div v-if="$slots.actions" class="actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  subtitle: {
    type: String,
    default: ''
  }
})
</script>

<style scoped>
.admin-page-header {
  margin-bottom: 24px;
  padding: 18px 22px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.title-group {
  min-width: 0;
}

.page-title {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 600;
  color: #262626;
}

.page-subtitle {
  margin: 0;
  color: #8c8c8c;
  font-size: 16px;
}

.actions {
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .admin-page-header {
    padding: 14px 16px;
    margin-bottom: 16px;
  }

  .page-title {
    font-size: 24px;
  }
}
</style>
