import type { TagTooltipProps } from 'element-plus/es/components/select/src/select'

export type GlobalCollapseTagTooltipProps = TagTooltipProps

export const GLOBAL_COLLAPSED_TAG_TOOLTIP_CLASS = 'global-collapsed-tag-tooltip'

const collapsedTagTooltipProps: GlobalCollapseTagTooltipProps = {
  placement: 'bottom',
  fallbackPlacements: ['top'],
  popperClass: GLOBAL_COLLAPSED_TAG_TOOLTIP_CLASS
}

export const globalCollapseTagTooltipProps = Object.freeze(collapsedTagTooltipProps)
