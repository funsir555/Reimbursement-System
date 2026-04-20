<template>
  <svg
    viewBox="0 0 128 128"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    role="img"
    :aria-label="ariaLabel"
  >
    <rect x="54" y="6" width="8" height="8" :fill="palette.spout" opacity="0.9" />
    <rect x="62" y="6" width="8" height="8" :fill="palette.spout" />
    <rect x="58" y="14" width="8" height="8" :fill="palette.spout" opacity="0.78" />

    <rect x="70" y="14" width="8" height="8" :fill="palette.outline" />
    <rect x="78" y="14" width="8" height="8" :fill="palette.body" />

    <rect x="38" y="22" width="8" height="8" :fill="palette.outline" opacity="0.72" />
    <rect x="46" y="22" width="32" height="8" :fill="palette.outline" />
    <rect x="78" y="22" width="16" height="8" :fill="palette.body" />
    <rect x="94" y="22" width="8" height="8" :fill="palette.outline" />

    <rect x="22" y="30" width="8" height="8" :fill="palette.outline" opacity="0.5" />
    <rect x="30" y="30" width="16" height="8" :fill="palette.outline" />
    <rect x="46" y="30" width="48" height="8" :fill="palette.body" />
    <rect x="94" y="30" width="16" height="8" :fill="palette.outline" />

    <rect x="14" y="38" width="8" height="8" :fill="palette.outline" opacity="0.38" />
    <rect x="22" y="38" width="8" height="8" :fill="palette.outline" />
    <rect x="30" y="38" width="72" height="8" :fill="palette.body" />
    <rect x="102" y="38" width="8" height="8" :fill="palette.outline" />
    <rect x="110" y="38" width="8" height="8" :fill="palette.tail" />

    <rect x="14" y="46" width="8" height="8" :fill="palette.outline" />
    <rect x="22" y="46" width="80" height="8" :fill="palette.body" />
    <rect x="102" y="46" width="8" height="8" :fill="palette.outline" />
    <rect x="110" y="46" width="8" height="8" :fill="palette.tail" />

    <rect x="14" y="54" width="8" height="8" :fill="palette.outline" />
    <rect x="22" y="54" width="16" height="8" :fill="palette.body" />
    <rect x="38" y="54" width="8" height="8" :fill="palette.eye" />
    <rect x="46" y="54" width="8" height="8" :fill="palette.outline" />
    <rect x="54" y="54" width="48" height="8" :fill="palette.body" />
    <rect x="102" y="54" width="8" height="8" :fill="palette.outline" />

    <rect x="6" y="62" width="8" height="8" :fill="palette.tail" opacity="0.7" />
    <rect x="14" y="62" width="8" height="8" :fill="palette.outline" />
    <rect x="22" y="62" width="72" height="8" :fill="palette.body" />
    <rect x="94" y="62" width="8" height="8" :fill="palette.bodyShade" />
    <rect x="102" y="62" width="8" height="8" :fill="palette.outline" />

    <rect x="14" y="70" width="8" height="8" :fill="palette.outline" />
    <rect x="22" y="70" width="16" height="8" :fill="palette.body" />
    <rect x="38" y="70" width="40" height="8" :fill="palette.belly" />
    <rect x="78" y="70" width="16" height="8" :fill="palette.bodyShade" />
    <rect x="94" y="70" width="8" height="8" :fill="palette.body" />
    <rect x="102" y="70" width="8" height="8" :fill="palette.outline" />

    <rect x="22" y="78" width="8" height="8" :fill="palette.outline" />
    <rect x="30" y="78" width="8" height="8" :fill="palette.body" />
    <rect x="38" y="78" width="32" height="8" :fill="palette.belly" />
    <rect x="70" y="78" width="16" height="8" :fill="palette.bodyShade" />
    <rect x="86" y="78" width="8" height="8" :fill="palette.outline" />

    <rect x="38" y="86" width="8" height="8" :fill="palette.outline" />
    <rect x="46" y="86" width="8" height="8" :fill="palette.fin" />
    <rect x="54" y="86" width="16" height="8" :fill="palette.fin" />
    <rect x="70" y="86" width="8" height="8" :fill="palette.outline" />

    <rect x="46" y="94" width="8" height="8" :fill="palette.fin" opacity="0.8" />
    <rect x="54" y="94" width="8" height="8" :fill="palette.outline" opacity="0.85" />
    <rect x="62" y="94" width="8" height="8" :fill="palette.fin" opacity="0.8" />

    <rect x="30" y="62" width="8" height="8" :fill="palette.cheek" opacity="0.65" />
  </svg>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  ariaLabel?: string
}>(), {
  ariaLabel: 'pixel whale icon'
})

const palette = {
  outline: '#15446E',
  body: '#78C8FF',
  bodyShade: '#4FA8F2',
  belly: '#EAF8FF',
  fin: '#91D8FF',
  tail: '#3D93DD',
  cheek: '#BDE6FF',
  eye: '#F9FDFF',
  spout: '#D8F2FF'
}
</script>
