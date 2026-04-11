# API modules 基线说明

当前 `src/api/modules/` 是前端 API 契约与类型 owner 的主目录。

## 当前规则

- 新的 API contract、request wrapper、类型 owner 按领域放在本目录下
- `src/api/index.ts` 继续保留为兼容导出层，不再回灌新的领域真相
- 若存在跨域共享类型，优先放到明确 owner 或 `core`/共享基础文件，而不是重新制造新的 mega shared 文件

## 当前状态

- 前端验证基线：`211/211`
- 当前阶段：跟随后端 `backend residual hotspot second-wave`
- 进度与优先级以 `C:\Users\funsir\Desktop\报销系统\执行记录\报销系统治理落地方案.md` 为准

## 维护建议

- 改动某一业务域接口时，优先在对应分域文件补测试或类型断言
- 不要把新接口组重新塞回 `src/api/index.ts`
- 如果某个分域文件再次膨胀，再在域内二次拆分，不回退到共享大文件
