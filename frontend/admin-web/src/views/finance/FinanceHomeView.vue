<template>
  <section class="finance-home-view">
    <header class="finance-home-hero">
      <div class="finance-home-hero__content">
        <p class="finance-home-eyebrow">FINANCE DESK</p>
        <h1>财务管理</h1>
        <p class="finance-home-description">
          把常用功能整理成一页清爽入口，最近访问优先呈现，帮助你更快回到正在处理的业务。
        </p>
        <div class="finance-home-hero__chips">
          <span class="finance-home-chip">最近使用优先</span>
          <span class="finance-home-chip">最多展示 10 个</span>
          <span class="finance-home-chip">点击即可直达</span>
        </div>
      </div>

      <div class="finance-home-hero__figure" aria-hidden="true">
        <div class="finance-home-ledger-scene">
          <span class="finance-home-ledger-scene__halo"></span>
          <span class="finance-home-ledger-scene__sheet finance-home-ledger-scene__sheet-back"></span>
          <span class="finance-home-ledger-scene__sheet finance-home-ledger-scene__sheet-front"></span>
          <span class="finance-home-ledger-scene__binding"></span>
          <span class="finance-home-ledger-scene__mark finance-home-ledger-scene__mark-a"></span>
          <span class="finance-home-ledger-scene__mark finance-home-ledger-scene__mark-b"></span>
        </div>
      </div>
    </header>

    <section class="finance-home-strip">
      <div class="finance-home-strip__meta">
        <span class="finance-home-strip__title">常用功能</span>
        <span class="finance-home-strip__subtitle">当前展示 {{ visibleModuleCount }} 个入口</span>
      </div>
    </section>

    <section class="finance-home-modules">
      <button
        v-for="(item, index) in modules"
        :key="item.path"
        type="button"
        class="finance-home-module"
        :data-module-path="item.path"
        :style="buildModuleCardStyle(index)"
        @click="openModule(item.path)"
      >
        <span class="finance-home-module__tag">{{ index < 3 ? '最近使用' : '常用功能' }}</span>
        <span class="finance-home-module__icon" aria-hidden="true">
          <span class="finance-home-book-icon">
            <span class="finance-home-book-icon__back"></span>
            <span class="finance-home-book-icon__cover"></span>
            <span class="finance-home-book-icon__pages"></span>
            <span class="finance-home-book-icon__spine"></span>
            <span class="finance-home-book-icon__line finance-home-book-icon__line-a"></span>
            <span class="finance-home-book-icon__line finance-home-book-icon__line-b"></span>
          </span>
        </span>
        <span class="finance-home-module__label">{{ item.label }}</span>
        <span class="finance-home-module__caption">{{ index < 3 ? '从最近访问继续' : '一键打开功能页' }}</span>
      </button>

      <div v-if="!modules.length" class="finance-home-empty">
        当前没有可展示的财务功能入口。
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onActivated, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { type FinanceHomeModuleItem, resolveFinanceHomeModules } from '@/utils/financeHomeModules'
import { readStoredUser } from '@/utils/permissions'

const router = useRouter()
const modules = ref<FinanceHomeModuleItem[]>([])

const currentUser = computed(() => readStoredUser())
const visibleModuleCount = computed(() => modules.value.length)

onMounted(refreshModules)
onActivated(refreshModules)

function refreshModules() {
  modules.value = resolveFinanceHomeModules(currentUser.value)
}

function openModule(path: string) {
  void router.push(path)
}

function buildModuleCardStyle(index: number) {
  const defaultPalette = { accent: '#6f8d7a', accentSoft: '#edf4ef', accentStrong: '#5b7566' }
  const palettes = [
    defaultPalette,
    { accent: '#8e7b63', accentSoft: '#f5efe7', accentStrong: '#75624b' },
    { accent: '#7088a3', accentSoft: '#eef3f8', accentStrong: '#58708b' },
    { accent: '#7e8d6c', accentSoft: '#f1f4eb', accentStrong: '#667453' }
  ]
  const palette = palettes[index % palettes.length] ?? defaultPalette
  return {
    '--finance-home-accent': palette.accent,
    '--finance-home-accent-soft': palette.accentSoft,
    '--finance-home-accent-strong': palette.accentStrong
  }
}
</script>

<style scoped>
.finance-home-view {
  font-family: "Avenir Next", "PingFang SC", "Microsoft YaHei", sans-serif;
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 100%;
  color: #243746;
}

.finance-home-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(220px, 0.78fr);
  align-items: stretch;
  gap: 20px;
  border: 1px solid rgba(178, 191, 202, 0.72);
  border-radius: 30px;
  background:
    radial-gradient(circle at top left, rgba(255, 255, 255, 0.92), transparent 38%),
    radial-gradient(circle at bottom right, rgba(192, 212, 194, 0.28), transparent 32%),
    linear-gradient(135deg, #fbfaf6 0%, #f2f6f2 48%, #eef3f7 100%);
  padding: 28px 30px;
  box-shadow: 0 20px 50px rgba(36, 55, 70, 0.07);
}

.finance-home-hero__content {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.finance-home-hero__figure {
  display: flex;
  align-items: center;
  justify-content: center;
}

.finance-home-eyebrow {
  margin: 0 0 10px;
  color: #738473;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.24em;
  text-transform: uppercase;
}

.finance-home-hero h1 {
  margin: 0;
  color: #203240;
  font-size: 34px;
  font-weight: 700;
  letter-spacing: 0.04em;
  line-height: 1.15;
}

.finance-home-description {
  max-width: 540px;
  margin: 14px 0 0;
  color: #5b6f78;
  font-size: 14px;
  line-height: 1.75;
}

.finance-home-hero__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.finance-home-chip {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  border: 1px solid rgba(157, 173, 152, 0.5);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.68);
  color: #51665b;
  padding: 0 14px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.finance-home-ledger-scene {
  position: relative;
  width: 220px;
  height: 170px;
}

.finance-home-ledger-scene__halo {
  position: absolute;
  inset: 18px 16px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(222, 231, 220, 0.78) 0%, rgba(222, 231, 220, 0.08) 68%, transparent 100%);
  filter: blur(2px);
}

.finance-home-ledger-scene__sheet {
  position: absolute;
  inset: 30px auto auto 54px;
  width: 120px;
  height: 90px;
  border-radius: 18px;
  background: linear-gradient(180deg, #fffdfa 0%, #f4f0e7 100%);
  border: 1px solid rgba(188, 176, 155, 0.56);
  box-shadow: 0 14px 30px rgba(65, 81, 89, 0.08);
}

.finance-home-ledger-scene__sheet-back {
  transform: rotate(-8deg) translate(-14px, 6px);
  opacity: 0.78;
}

.finance-home-ledger-scene__sheet-front {
  transform: rotate(8deg);
}

.finance-home-ledger-scene__binding {
  position: absolute;
  inset: 48px auto auto 140px;
  width: 18px;
  height: 72px;
  border-radius: 999px;
  background: linear-gradient(180deg, #70857a 0%, #5c7065 100%);
  box-shadow: 0 10px 18px rgba(58, 76, 66, 0.18);
}

.finance-home-ledger-scene__mark {
  position: absolute;
  display: block;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(110, 133, 121, 0.24) 0%, rgba(110, 133, 121, 0.08) 100%);
}

.finance-home-ledger-scene__mark-a {
  inset: 58px auto auto 82px;
  width: 56px;
  height: 7px;
}

.finance-home-ledger-scene__mark-b {
  inset: 76px auto auto 82px;
  width: 42px;
  height: 7px;
}

.finance-home-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.finance-home-strip__meta {
  display: flex;
  align-items: baseline;
  gap: 12px;
  min-width: 0;
}

.finance-home-strip__title {
  color: #223645;
  font-size: 18px;
  font-weight: 700;
}

.finance-home-strip__subtitle {
  color: #6d8088;
  font-size: 13px;
}

.finance-home-modules {
  display: flex;
  gap: 14px;
  overflow-x: auto;
  padding: 4px 2px 10px;
  scrollbar-gutter: stable;
}

.finance-home-module {
  --finance-home-accent: #6f8d7a;
  --finance-home-accent-soft: #edf4ef;
  --finance-home-accent-strong: #5b7566;
  display: inline-flex;
  position: relative;
  flex: 0 0 156px;
  flex-direction: column;
  align-items: flex-start;
  gap: 14px;
  border: 1px solid rgba(192, 201, 206, 0.84);
  border-radius: 26px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(248, 247, 243, 0.96) 100%);
  box-shadow: 0 16px 34px rgba(36, 55, 70, 0.06);
  cursor: pointer;
  min-height: 184px;
  padding: 18px 16px 18px;
  text-align: left;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease,
    background 0.18s ease;
}

.finance-home-module:hover {
  border-color: color-mix(in srgb, var(--finance-home-accent) 48%, #c8d3d9);
  background:
    linear-gradient(180deg, #ffffff 0%, color-mix(in srgb, var(--finance-home-accent-soft) 42%, #faf8f4) 100%);
  box-shadow: 0 22px 40px rgba(36, 55, 70, 0.1);
  transform: translateY(-3px);
}

.finance-home-module__tag {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  border-radius: 999px;
  background: var(--finance-home-accent-soft);
  color: var(--finance-home-accent-strong);
  padding: 0 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.finance-home-module__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  border-radius: 22px;
  background:
    radial-gradient(circle at top left, rgba(255, 255, 255, 0.9), transparent 42%),
    linear-gradient(180deg, color-mix(in srgb, var(--finance-home-accent-soft) 74%, #ffffff) 0%, var(--finance-home-accent-soft) 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.86);
}

.finance-home-book-icon {
  position: relative;
  display: inline-flex;
  width: 40px;
  height: 34px;
}

.finance-home-book-icon__back,
.finance-home-book-icon__cover {
  position: absolute;
  inset: 0;
  border-radius: 8px 12px 12px 8px;
}

.finance-home-book-icon__back {
  transform: translate(-4px, 4px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.86) 0%, rgba(230, 234, 232, 0.82) 100%);
  border: 1px solid rgba(140, 154, 145, 0.28);
}

.finance-home-book-icon__cover {
  background: linear-gradient(180deg, #fffef9 0%, #f6f1e7 100%);
  border: 1px solid color-mix(in srgb, var(--finance-home-accent) 56%, #b8c1c6);
  box-shadow:
    inset -8px 0 0 rgba(255, 255, 255, 0.58),
    0 8px 16px rgba(36, 55, 70, 0.08);
}

.finance-home-book-icon__pages {
  position: absolute;
  inset: 7px 8px auto auto;
  width: 16px;
  height: 14px;
  border-top: 2px solid rgba(98, 116, 122, 0.42);
  border-bottom: 2px solid rgba(98, 116, 122, 0.2);
}

.finance-home-book-icon__spine {
  position: absolute;
  inset: 3px auto 3px 5px;
  width: 5px;
  border-radius: 999px;
  background: linear-gradient(180deg, var(--finance-home-accent) 0%, var(--finance-home-accent-strong) 100%);
}

.finance-home-book-icon__line {
  position: absolute;
  right: 8px;
  height: 2px;
  border-radius: 999px;
  background: rgba(103, 120, 128, 0.28);
}

.finance-home-book-icon__line-a {
  bottom: 11px;
  width: 16px;
}

.finance-home-book-icon__line-b {
  bottom: 7px;
  width: 12px;
}

.finance-home-module__label {
  color: #243746;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.5;
  word-break: break-word;
}

.finance-home-module__caption {
  color: #6a7d83;
  font-size: 12px;
  line-height: 1.6;
}

.finance-home-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 168px;
  min-width: 240px;
  border: 1px dashed rgba(174, 185, 188, 0.88);
  border-radius: 26px;
  color: #60757b;
  background: linear-gradient(180deg, #faf9f4 0%, #f4f6f2 100%);
  padding: 24px;
}

@media (max-width: 768px) {
  .finance-home-view {
    gap: 16px;
  }

  .finance-home-hero {
    grid-template-columns: 1fr;
    padding: 22px 20px;
  }

  .finance-home-hero h1 {
    font-size: 28px;
  }

  .finance-home-hero__figure {
    justify-content: flex-start;
  }

  .finance-home-ledger-scene {
    width: 180px;
    height: 132px;
  }

  .finance-home-strip {
    flex-direction: column;
    align-items: flex-start;
  }

  .finance-home-module {
    flex-basis: 144px;
    min-height: 172px;
  }
}
</style>
