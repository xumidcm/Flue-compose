package com.flue.launcher.ui.navigation

/**
 * 启动器屏幕状态机，对应 state-face / state-apps / state-app 等状态。
 * 每个状态切换都会触发对应的 scale + blur + opacity 动画。
 */
enum class ScreenState {
    /** 表盘/时钟界面 */
    Face,

    /** 应用列表（蜂窝或列表视图） */
    Apps,

    /** 单个应用内部视图 */
    App,

    /** 智能侧屏 */
    Stack,

    /** 通知中心 */
    Notifications,

    /** 独立小组件页 */
    Widgets,

    /** 控制中心 */
    ControlCenter,

    /** 桌面设置 */
    Settings
}

/** 应用抽屉布局模式 */
enum class LayoutMode {
    Honeycomb,
    List
}
