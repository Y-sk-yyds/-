<!--
  MobileNav.vue — H5 底部导航（排班挂号 / 我的就诊）
  -----------------------------------------------------------
  用途：
    - 原先两个页面各写一份完全相同的底部导航（连样式都是重复的），
      本组件抽出来共用，改一次两处生效。

  用法：
    <MobileNav current="home" />      在 MobileHome.vue
    <MobileNav current="records" />   在 MobileRecords.vue

  props:
    current — 当前所在页，'home' | 'records'，用于高亮

  注意：
    - 用 fixed 定位 + max-width 居中，PC 上打开会居中显示成手机画布的样子
    - padding 叠加 env(safe-area-inset-bottom)：iPhone 底部有 Home Indicator，
      不加会被遮住；不支持 env() 的环境自动回退为 0
-->
<template>
  <div class="bottom-nav">
    <div class="nav-item" :class="{ active: current === 'home' }" @click="go('/mobile')">
      <el-icon><List /></el-icon>
      <span>排班挂号</span>
    </div>
    <div class="nav-item" :class="{ active: current === 'records' }" @click="go('/mobile/records')">
      <el-icon><Document /></el-icon>
      <span>我的就诊</span>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { List, Document } from '@element-plus/icons-vue'

const props = defineProps({
  current: { type: String, required: true }
})

const router = useRouter()

/** 点当前页不重复跳转（vue-router 对重复导航虽不报错，但会产生多余历史记录） */
const go = (path) => {
  if (router.currentRoute.value.path === path) return
  router.push(path)
}
</script>

<style scoped>
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  max-width: 480px;
  margin: 0 auto;
  display: flex;
  background: #fff;
  border-top: 1px solid #e4e7ed;
  padding: 8px 0 calc(8px + env(safe-area-inset-bottom));
}
.nav-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #909399;
  cursor: pointer;
}
.nav-item.active { color: #409EFF; }
</style>
