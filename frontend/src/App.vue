<!--
  App.vue — 应用根组件
  --------------------------------------------------------
  用途：
    - 整个 Vue 应用的顶层入口组件
    - 通过 <router-view /> 渲染匹配当前路由的页面组件
    - 定义全局 CSS 样式（重置、字体、通用卡片/表格/按钮等）
  
  模板结构：
    - <router-view />：Vue Router 的路由出口，根据 URL 动态切换页面内容
  
  样式策略：
    - 全局重置：清除浏览器默认 margin/padding，统一盒模型
    - 全局字体：Inter（英文）→ PingFang SC（中文 macOS）→ Microsoft YaHei（中文 Windows）
    - 全局工具类：.page-card（卡片容器）、.page-toolbar（标题栏）、.search-bar（搜索栏）
    - 组件覆盖：美化 Element Plus 的 el-table、el-button、el-dialog、el-pagination
    - 使用 CSS 变量 --clinic-primary-dark 保持主题色一致性
    - 表格采用 table-layout: fixed 保证列宽均匀
-->
<template>
  <!-- Vue Router 的动态视图出口：根据当前 URL 路径渲染对应的页面组件 -->
  <router-view />
</template>

<script setup>
// App.vue 为纯布局/样式容器，不包含业务逻辑
// 所有全局状态（用户认证等）由 Pinia store 和路由守卫管理
</script>

<style>
/* ================================================================
   全局基础样式
   ================================================================ */

/* 全局 CSS 重置：清除浏览器默认边距，统一盒模型为 border-box */
* { margin: 0; padding: 0; box-sizing: border-box; }

/* 根元素占满全屏，设置全局字体栈和抗锯齿渲染 */
html, body, #app {
  height: 100%;
  /*
    字体优先级：
    1. Inter — 现代西文字体
    2. PingFang SC — macOS 苹方中文字体
    3. Microsoft YaHei — Windows 微软雅黑
    4. sans-serif — 系统兜底无衬线字体
  */
  font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;    /* macOS/WebKit 字体平滑 */
}

/* ================================================================
   全局工具类（页面通用组件样式）
   ================================================================ */

/* ---- 页面卡片容器 ---- */
/*
  用途：包裹页面主要内容区域
  特点：白色背景、圆角、轻微阴影，营造层次感
*/
.page-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

/* ---- 页面标题栏 ---- */
/*
  用途：页面顶部区域，左侧标题 + 右侧操作按钮
  布局：flex 两端对齐
*/
.page-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
/* 标题栏中的 h3 标题样式 */
.page-toolbar h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;       /* 深色标题色 */
}

/* ---- 搜索栏 ---- */
/*
  用途：表格页面的筛选搜索区域
  特点：flex 横向排列，自动换行，12px 间距
*/
.search-bar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

/* ================================================================
   Element Plus 组件样式覆盖
   目的：统一项目视觉风格，覆盖 Element Plus 默认样式
   ================================================================ */

/* ---- 表格全局美化 ---- */
.el-table {
  --el-table-border-color: #f0f0f0;   /* 浅灰色表格边框 */
  width: 100% !important;
}
/* 确保表格 wrapper 和内部 table 元素始终占满宽度 */
.el-table__wrapper { width: 100% !important; }
.el-table table {
  width: 100% !important;
  table-layout: fixed;   /* 固定列宽算法，防止列宽因内容撑开 */
}
/* 表头样式：浅灰背景、深灰文字、加粗 */
.el-table th.el-table__cell {
  background: #fafbfc;
  color: #606266;
  font-weight: 600;
  font-size: 13px;
}
/* 行悬浮效果：浅蓝色背景高亮当前行 */
.el-table .el-table__row:hover > td {
  background: #f5f8ff;
}

/* ---- 按钮主题色覆盖 ---- */
/*
  Element Plus 按钮 hover 状态使用项目主题色变量
  --clinic-primary-dark 在 theme.css 中定义
*/
.el-button--primary {
  --el-button-hover-bg-color: var(--clinic-primary-dark);
  --el-button-hover-border-color: var(--clinic-primary-dark);
}

/* ---- 对话框全局美化 ---- */
.el-dialog { border-radius: 12px; }                     /* 对话框圆角 */
.el-dialog__header { padding: 20px 24px 0; }           /* 标题区内边距 */
.el-dialog__body { padding: 20px 24px; }               /* 内容区内边距 */
.el-dialog__footer { padding: 12px 24px 20px; }        /* 底部按钮区内边距 */

/* ---- 分页组件对齐 ---- */
.el-pagination {
  margin-top: 16px;
  justify-content: flex-end;     /* 分页靠右对齐 */
}
</style>
