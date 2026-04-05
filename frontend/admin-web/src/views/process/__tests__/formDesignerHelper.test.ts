import { describe, expect, it } from 'vitest'
import type { ProcessCustomArchiveSummary } from '@/api'
import {
  BUSINESS_COMPONENT_DEFINITIONS,
  CONTROL_PALETTE_ITEMS,
  buildBusinessComponentPaletteItems,
  buildSharedFieldPaletteItems,
  createBlockFromPaletteItem,
  createDefaultFieldPermission,
  createEmptyFormSchema,
  getBlockQuickActionStates,
  insertBlockAt,
  moveBlock,
  normalizeFormSchema,
  removeBlock,
  updateBlock
} from '@/views/process/formDesignerHelper'

function createArchive(archiveCode: string, archiveName: string): ProcessCustomArchiveSummary {
  return {
    id: Number(archiveCode.replace(/\D/g, '') || '1'),
    archiveCode,
    archiveName,
    archiveType: 'SELECT',
    archiveTypeLabel: '提供选择',
    formDescription: '',
    status: 1,
    itemCount: 3,
    updatedAt: '2026-03-28 12:00'
  }
}

describe('formDesignerHelper', () => {
  it('creates control blocks from palette defaults', () => {
    const block = createBlockFromPaletteItem(CONTROL_PALETTE_ITEMS[0])

    expect(block.kind).toBe('CONTROL')
    expect(block.label).toBe('单行文本')
    expect(block.props.controlType).toBe('TEXT')
    expect(block.permission.fixedStages.ARCHIVED).toBe('READONLY')
    expect(block.span).toBe(1)
  })

  it('creates business component blocks with registered component code', () => {
    const paletteItem = buildBusinessComponentPaletteItems()[0]
    const block = createBlockFromPaletteItem(paletteItem)

    expect(block.kind).toBe('BUSINESS_COMPONENT')
    expect(block.props.componentCode).toBe(BUSINESS_COMPONENT_DEFINITIONS[0].code)
    expect(block.span).toBe(1)
  })

  it('includes payment company in business component palette', () => {
    const paletteItem = buildBusinessComponentPaletteItems().find((item) => item.key === 'payment-company')

    expect(paletteItem).toBeTruthy()
    expect(paletteItem?.label).toBe('付款公司')
    expect(paletteItem?.props).toEqual({ componentCode: 'payment-company' })
  })

  it('includes related and writeoff document business components with default allowed types', () => {
    const businessItems = buildBusinessComponentPaletteItems()
    const relatedItem = businessItems.find((item) => item.key === 'related-document')
    const writeoffItem = businessItems.find((item) => item.key === 'writeoff-document')

    expect(relatedItem).toBeTruthy()
    expect(relatedItem?.props).toEqual({
      componentCode: 'related-document',
      allowedTemplateTypes: ['report', 'application', 'contract', 'loan']
    })

    expect(writeoffItem).toBeTruthy()
    expect(writeoffItem?.props).toEqual({
      componentCode: 'writeoff-document',
      allowedTemplateTypes: ['report', 'loan']
    })
  })

  it('builds shared field palette items that only persist archive code', () => {
    const items = buildSharedFieldPaletteItems([createArchive('CA001', '共享部门')])
    const block = createBlockFromPaletteItem(items[0])

    expect(block.kind).toBe('SHARED_FIELD')
    expect(block.props.archiveCode).toBe('CA001')
    expect(Object.keys(block.props)).toEqual(['archiveCode'])
  })

  it('inserts, moves and removes blocks without mutating schema shape', () => {
    const schema = createEmptyFormSchema()
    const withFirst = insertBlockAt(schema, CONTROL_PALETTE_ITEMS[0], 0)
    const withSecond = insertBlockAt(withFirst, CONTROL_PALETTE_ITEMS[1], 1)
    const moved = moveBlock(withSecond, 1, 0)
    const removed = removeBlock(moved, moved.blocks[0].blockId)

    expect(withSecond.blocks).toHaveLength(2)
    expect(moved.blocks[0].label).toBe('多行文本')
    expect(removed.blocks).toHaveLength(1)
    expect(removed.blocks[0].label).toBe('单行文本')
  })

  it('normalizes missing permission stages and deduplicates scene overrides', () => {
    const normalized = normalizeFormSchema({
      blocks: [
        {
          blockId: 'block-1',
          fieldKey: 'field-1',
          kind: 'CONTROL',
          label: '字段 1',
          span: 2,
          required: true,
          props: { controlType: 'TEXT' },
          permission: {
            fixedStages: {
              DRAFT_BEFORE_SUBMIT: 'EDITABLE',
              RESUBMIT_AFTER_RETURN: 'READONLY'
            } as ReturnType<typeof createDefaultFieldPermission>['fixedStages'],
            sceneOverrides: [
              { sceneId: 1, permission: 'READONLY' },
              { sceneId: 1, permission: 'HIDDEN' }
            ]
          }
        }
      ]
    })

    expect(normalized.blocks[0].permission.fixedStages.IN_APPROVAL).toBe('READONLY')
    expect(normalized.blocks[0].permission.sceneOverrides).toEqual([{ sceneId: 1, permission: 'READONLY' }])
  })

  it('updates option controls in place through updater helper', () => {
    const schema = insertBlockAt(createEmptyFormSchema(), CONTROL_PALETTE_ITEMS[6], 0)
    const next = updateBlock(schema, schema.blocks[0].blockId, (block) => ({
      ...block,
      label: '费用类型',
      props: {
        ...block.props,
        options: [{ label: '差旅', value: 'travel' }]
      }
    }))

    expect(next.blocks[0].label).toBe('费用类型')
    expect(next.blocks[0].props.options).toEqual([{ label: '差旅', value: 'travel' }])
  })

  it('computes quick action states for expandable and collapsible blocks', () => {
    const schema = normalizeFormSchema({
      layoutMode: 'TWO_COLUMN',
      blocks: [
        {
          blockId: 'left-alone',
          fieldKey: 'field-1',
          kind: 'CONTROL',
          label: '字段 1',
          span: 1,
          required: false,
          helpText: '',
          props: { controlType: 'TEXT' },
          permission: createDefaultFieldPermission()
        },
        {
          blockId: 'full-width',
          fieldKey: 'field-2',
          kind: 'CONTROL',
          label: '字段 2',
          span: 2,
          required: false,
          helpText: '',
          props: { controlType: 'TEXTAREA' },
          permission: createDefaultFieldPermission()
        },
        {
          blockId: 'paired-left',
          fieldKey: 'field-3',
          kind: 'CONTROL',
          label: '字段 3',
          span: 1,
          required: false,
          helpText: '',
          props: { controlType: 'TEXT' },
          permission: createDefaultFieldPermission()
        },
        {
          blockId: 'paired-right',
          fieldKey: 'field-4',
          kind: 'CONTROL',
          label: '字段 4',
          span: 1,
          required: false,
          helpText: '',
          props: { controlType: 'TEXT' },
          permission: createDefaultFieldPermission()
        },
        {
          blockId: 'tail-left',
          fieldKey: 'field-5',
          kind: 'CONTROL',
          label: '字段 5',
          span: 1,
          required: false,
          helpText: '',
          props: { controlType: 'TEXT' },
          permission: createDefaultFieldPermission()
        }
      ]
    })

    expect(getBlockQuickActionStates(schema)).toEqual({
      'left-alone': 'expandable',
      'full-width': 'collapsible',
      'paired-left': 'hidden',
      'paired-right': 'hidden',
      'tail-left': 'expandable'
    })
  })
})
