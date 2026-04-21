<template>
  <svg
    viewBox="0 0 128 128"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    role="img"
    :aria-label="ariaLabel"
  >
    <rect x="38" y="8" width="8" height="8" :fill="palette.spout" opacity="0.82" />
    <rect x="46" y="4" width="8" height="8" :fill="palette.spout" />
    <rect x="54" y="8" width="8" height="8" :fill="palette.spout" opacity="0.72" />

    <rect x="54" y="16" width="8" height="8" :fill="palette.outline" />
    <rect x="62" y="16" width="8" height="8" :fill="palette.body" />

    <rect x="22" y="24" width="8" height="8" :fill="palette.outline" opacity="0.5" />
    <rect x="30" y="24" width="40" height="8" :fill="palette.outline" />
    <rect x="70" y="24" width="16" height="8" :fill="palette.body" />
    <rect x="86" y="24" width="8" height="8" :fill="palette.outline" />

    <rect x="14" y="32" width="8" height="8" :fill="palette.outline" opacity="0.32" />
    <rect x="22" y="32" width="8" height="8" :fill="palette.outline" />
    <rect x="30" y="32" width="56" height="8" :fill="palette.body" />
    <rect x="86" y="32" width="8" height="8" :fill="palette.outline" />
    <rect x="94" y="32" width="8" height="8" :fill="palette.bodyShade" />

    <rect x="14" y="40" width="8" height="8" :fill="palette.outline" />
    <rect x="22" y="40" width="64" height="8" :fill="palette.body" />
    <rect x="86" y="40" width="8" height="8" :fill="palette.outline" />
    <rect x="94" y="40" width="8" height="8" :fill="palette.bodyShade" />
    <rect x="102" y="40" width="8" height="8" :fill="palette.tail" />

    <rect x="14" y="48" width="8" height="8" :fill="palette.outline" />
    <rect x="22" y="48" width="16" height="8" :fill="palette.body" />
    <rect x="38" y="48" width="8" height="8" :fill="palette.eye" />
    <rect x="46" y="48" width="8" height="8" :fill="palette.outline" />
    <rect x="54" y="48" width="32" height="8" :fill="palette.body" />
    <rect x="86" y="48" width="8" height="8" :fill="palette.outline" />
    <rect x="94" y="48" width="16" height="8" :fill="palette.tail" />

    <rect x="6" y="56" width="8" height="8" :fill="palette.outline" opacity="0.24" />
    <rect x="14" y="56" width="8" height="8" :fill="palette.outline" />
    <rect x="22" y="56" width="56" height="8" :fill="palette.body" />
    <rect x="78" y="56" width="8" height="8" :fill="palette.bodyShade" />
    <rect x="86" y="56" width="8" height="8" :fill="palette.outline" />
    <rect x="94" y="56" width="8" height="8" :fill="palette.tail" />
    <rect x="102" y="56" width="8" height="8" :fill="palette.outline" />
    <rect x="110" y="56" width="8" height="8" :fill="palette.tail" />

    <rect x="14" y="64" width="8" height="8" :fill="palette.outline" />
    <rect x="22" y="64" width="16" height="8" :fill="palette.body" />
    <rect x="38" y="64" width="24" height="8" :fill="palette.belly" />
    <rect x="62" y="64" width="16" height="8" :fill="palette.bodyShade" />
    <rect x="78" y="64" width="8" height="8" :fill="palette.body" />
    <rect x="86" y="64" width="8" height="8" :fill="palette.outline" />
    <rect x="94" y="64" width="8" height="8" :fill="palette.tail" />
    <rect x="102" y="64" width="16" height="8" :fill="palette.outline" />

    <rect x="22" y="72" width="8" height="8" :fill="palette.outline" />
    <rect x="30" y="72" width="8" height="8" :fill="palette.body" />
    <rect x="38" y="72" width="16" height="8" :fill="palette.belly" />
    <rect x="54" y="72" width="16" height="8" :fill="palette.bodyShade" />
    <rect x="70" y="72" width="8" height="8" :fill="palette.outline" />
    <rect x="94" y="72" width="8" height="8" :fill="palette.outline" />
    <rect x="102" y="72" width="8" height="8" :fill="palette.tail" />
    <rect x="110" y="72" width="8" height="8" :fill="palette.outline" />

    <rect x="94" y="48" width="8" height="8" :fill="palette.outline" opacity="0.98" />
    <rect x="102" y="48" width="8" height="8" :fill="palette.tail" />
    <rect x="110" y="48" width="8" height="8" :fill="palette.outline" />

    <rect x="102" y="40" width="8" height="8" :fill="palette.outline" />
    <rect x="110" y="40" width="8" height="8" :fill="palette.tail" />

    <rect x="102" y="32" width="8" height="8" :fill="palette.tail" />
    <rect x="110" y="32" width="8" height="8" :fill="palette.outline" opacity="0.92" />

    <rect x="38" y="80" width="8" height="8" :fill="palette.outline" />
    <rect x="46" y="80" width="8" height="8" :fill="palette.fin" />
    <rect x="54" y="80" width="16" height="8" :fill="palette.fin" />
    <rect x="70" y="80" width="8" height="8" :fill="palette.outline" />

    <rect x="46" y="88" width="8" height="8" :fill="palette.fin" opacity="0.8" />
    <rect x="54" y="88" width="8" height="8" :fill="palette.outline" opacity="0.85" />
    <rect x="62" y="88" width="8" height="8" :fill="palette.fin" opacity="0.8" />

    <rect x="30" y="56" width="8" height="8" :fill="palette.cheek" opacity="0.65" />
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
