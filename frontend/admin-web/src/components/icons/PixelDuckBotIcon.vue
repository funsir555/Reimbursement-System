<template>
  <svg
    viewBox="0 0 128 128"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    role="img"
    :aria-label="ariaLabel"
  >
    <rect x="30" y="24" width="12" height="12" fill="currentColor" opacity="0.16" />
    <rect x="42" y="24" width="12" height="12" fill="currentColor" opacity="0.28" />
    <rect x="54" y="24" width="12" height="12" fill="currentColor" />
    <rect x="66" y="24" width="12" height="12" fill="currentColor" opacity="0.82" />
    <rect x="78" y="24" width="12" height="12" fill="currentColor" opacity="0.42" />

    <rect x="18" y="36" width="12" height="12" fill="currentColor" opacity="0.2" />
    <rect x="30" y="36" width="12" height="12" :fill="palette.outline" />
    <rect x="42" y="36" width="48" height="12" :fill="palette.outline" />
    <rect x="90" y="36" width="12" height="12" :fill="palette.outline" />
    <rect x="102" y="36" width="12" height="12" fill="currentColor" opacity="0.2" />

    <rect x="18" y="48" width="12" height="12" :fill="palette.outline" />
    <rect x="30" y="48" width="72" height="12" :fill="palette.body" />
    <rect x="102" y="48" width="12" height="12" :fill="palette.outline" />

    <rect x="18" y="60" width="12" height="12" :fill="palette.outline" />
    <rect x="30" y="60" width="12" height="12" :fill="palette.body" />
    <rect x="42" y="60" width="12" height="12" :fill="palette.eyeHighlight" />
    <rect x="54" y="60" width="12" height="12" :fill="palette.outline" />
    <rect x="66" y="60" width="12" height="12" :fill="palette.body" />
    <rect x="78" y="60" width="12" height="12" :fill="palette.body" />
    <rect x="90" y="60" width="24" height="12" :fill="palette.beak" />

    <rect x="6" y="72" width="12" height="12" fill="currentColor" opacity="0.12" />
    <rect x="18" y="72" width="12" height="12" :fill="palette.outline" />
    <rect x="30" y="72" width="72" height="12" :fill="palette.body" />
    <rect x="102" y="72" width="12" height="12" :fill="palette.outline" />
    <rect x="114" y="72" width="8" height="12" fill="currentColor" opacity="0.16" />

    <rect x="18" y="84" width="12" height="12" :fill="palette.outline" />
    <rect x="30" y="84" width="48" height="12" :fill="palette.body" />
    <rect x="78" y="84" width="24" height="12" :fill="palette.wingHighlight" />
    <rect x="102" y="84" width="12" height="12" :fill="palette.outline" />

    <rect x="30" y="96" width="12" height="12" :fill="palette.outline" />
    <rect x="42" y="96" width="12" height="12" :fill="palette.body" />
    <rect x="54" y="96" width="12" height="12" :fill="palette.body" />
    <rect x="66" y="96" width="12" height="12" :fill="palette.outline" />
    <rect x="78" y="96" width="12" height="12" :fill="palette.outline" />

    <rect x="42" y="108" width="12" height="12" :fill="palette.outline" />
    <rect x="78" y="108" width="12" height="12" :fill="palette.outline" />

    <rect x="54" y="12" width="12" height="12" :fill="palette.outline" />
    <rect x="66" y="12" width="12" height="12" :fill="palette.accent" />
    <rect x="78" y="12" width="12" height="12" :fill="palette.outline" />
    <rect x="90" y="24" width="12" height="12" :fill="palette.accent" />
    <rect x="102" y="24" width="12" height="12" :fill="palette.outline" />

    <rect x="6" y="48" width="12" height="12" :fill="palette.accent" opacity="0.45" />
    <rect x="114" y="48" width="8" height="12" :fill="palette.accent" opacity="0.45" />
  </svg>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  variant?: 'default' | 'brand'
  ariaLabel?: string
}>(), {
  variant: 'default',
  ariaLabel: 'pixel duck icon'
})

const palette = computed(() => {
  if (props.variant === 'brand') {
    return {
      outline: '#1E3A5F',
      body: '#7CC6FF',
      wingHighlight: '#DDF1FF',
      eyeHighlight: '#F7FBFF',
      beak: '#3C97FF',
      accent: '#8DD7FF'
    }
  }

  return {
    outline: '#273047',
    body: '#FFD44D',
    wingHighlight: '#FFE17C',
    eyeHighlight: '#EAF2FF',
    beak: '#FF9C36',
    accent: '#59C3C3'
  }
})
</script>
