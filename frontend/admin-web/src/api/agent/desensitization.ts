import request, { buildQueryString } from '../modules/core'

/**
 * 脱敏管理 API
 */
export const desensitizationApi = {
  /**
   * 获取所有实体类型配置
   */
  getEntityTypes() {
    return request<any[]>('/auth/agent/desensitization/entity-types')
  },

  /**
   * 保存实体类型配置
   */
  saveEntityType(data: any) {
    return request<string>('/auth/agent/desensitization/entity-types', {
      method: 'POST',
      body: JSON.stringify(data)
    })
  },

  /**
   * 删除实体类型配置
   */
  deleteEntityType(code: string) {
    return request<string>(`/auth/agent/desensitization/entity-types/${encodeURIComponent(code)}`, {
      method: 'DELETE'
    })
  },

  /**
   * 更新实体类型启用状态
   */
  updateEntityTypeEnabled(code: string, enabled: boolean) {
    return request<string>(`/auth/agent/desensitization/entity-types/${encodeURIComponent(code)}/enabled${buildQueryString({ enabled })}`, {
      method: 'PUT'
    })
  },

  /**
   * 脱敏测试
   */
  test(data: { text: string; strategy: string }) {
    return request<any>('/auth/agent/desensitization/test', {
      method: 'POST',
      body: JSON.stringify(data)
    })
  },

  /**
   * 查询映射表列表
   */
  getMappings(params: { page?: number; pageSize?: number; sessionId?: string }) {
    return request<any>(`/auth/agent/desensitization/mappings${buildQueryString(params)}`)
  },

  /**
   * 新增映射关系
   */
  createMapping(data: { sessionId: string; mappings: Record<string, string>; ttlHours?: number }) {
    return request<any>('/auth/agent/desensitization/mappings', {
      method: 'POST',
      body: JSON.stringify(data)
    })
  },

  /**
   * 获取映射表统计
   */
  getMappingStats() {
    return request<any>('/auth/agent/desensitization/mappings/stats')
  },

  /**
   * 清理过期映射
   */
  cleanupMappings() {
    return request<string>('/auth/agent/desensitization/mappings/cleanup', {
      method: 'POST'
    })
  }
}
