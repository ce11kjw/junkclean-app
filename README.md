# JunkClean App

Android 垃圾清理 App，纯 Java 原生 View 构建（无 XML 布局、无 Gradle、无第三方依赖）。深空玻璃 UI，root 自动适配。

对应的 Magisk / KernelSU / APatch 模块版：[ce11kjw/junkclean](https://github.com/ce11kjw/junkclean)

## 功能

### 首页
- **8 分类扫描** — 应用缓存 / WebView / 日志 / 临时文件 / 缩略图 / 冗余安装包 / 空文件 / 应用残留
- 分类卡可展开明细、逐项勾选，**长按条目加入白名单**
- 「谨慎」「需 root」徽章标注，谨慎项默认不勾
- 清理前二次确认（含谨慎项额外提示），清理后显示**可用空间前后对比**
- 一键全清安全项（自动跳过谨慎分类）
- **目录体积排行** — sdcard 一级目录占用条形图，找出空间大户
- 60 秒扫描缓存，可点击强制重扫

### 工具箱（9 张卡片）
| 卡片 | 子功能 |
|------|--------|
| 📁 大文件 | 7 类型筛选（图片/视频/音频/文档/压缩/安装包）· 4 种排序 · 时间过滤（7/30/90/180 天前）· 全选 · 移入回收站 / 彻底删除 |
| 🔁 重复文件 | 大小分桶 + 内容哈希精确比对 · 3 种保留策略（最新/最旧/路径最短）· 分组展示 · 保留项标注 |
| 🫙 空文件 | 空文件 + 空目录（可开关）· 全选 · 批量清理 |
| 📱 应用缓存 | 按体积排行 + 条形图 · 全选/全不选 · 批量清理 · 长按加白名单 |
| 📥 安装包管理 | 扫描 apk 并标注**已装/未装/未知** · 一键只选已安装 · 删除前对未安装项额外警告 |
| 🖼 缩略图 | 10 个厂商缓存目录覆盖（MIUI/Google Photos/微信等）· 勾选清理 |
| 🗂 整理中心 | 多规则管理 · 源/目标目录可编辑 · 处理子目录 + 完整性检测开关 · **干跑预览**移动清单 · 自定义扩展名映射 · **整理历史与一键还原** |
| 🗑 回收站 | 剩余保留天数徽章 · 恢复到原位 · 彻底删除 · 长按确认清空 |
| ⚡ fstrim | /data /cache /system 分区 TRIM（需 root） |

### 设置
- **运行环境** — root 检测 + 管理器识别（KernelSU / APatch / Magisk）· 一键测试授权
- **外观** — 3 主题（深色 / OLED 纯黑 / 浅色）× 4 强调色（青绿 / 紫罗兰 / 蓝 / 粉），切换即时重建
- **清理行为** — 先入回收站开关 · 保留天数（永久/3/7/14/30 天）· 自定义扫描根目录 · 清除扫描缓存 · 立即清理过期项
- **扫描分类** — 8 个分类独立开关
- **白名单** — 多行编辑 / 保存 / 清空
- **统计** — 累计项数与释放量 · **最近 7 天柱状图** · 复制文本报告 · 重置

## 安全设计

- 路径白名单：仅允许 sdcard 与已知系统缓存目录，拒绝 `..` 遍历，禁止操作 `/`、`/data`、`/data/data` 等根路径
- 只删扫描结果中已勾选的项，不做模糊匹配
- sdcard 用户文件默认先入回收站，系统缓存直接删除
- 危险操作二次确认，清空回收站需长按

## 技术栈

- 纯 Java + 原生 View，代码构建 UI，自绘存储条与柱状图
- minSdk 26 / targetSdk 34
- 编译链：aapt2 + javac + d8 + zipalign + apksigner
- 4300+ 行源码 → 174 类 / 978 方法 → APK 73 KB

## 目录结构

```
src/com/ce11kjw/junkclean/
├── Theme.java            主题配色（3 主题 × 4 强调色）+ drawable 工厂
├── UI.java               视图工厂（card/chip/badge/switch/dialog/fileRow）
├── Shell.java            root 探测 / 管理器识别 / su 执行 / du / fstrim
├── Util.java             体积与时间格式化 / 目录大小 / 快速哈希 / 移动 / 文本读写
├── Store.java            SharedPreferences 持久化（白名单/主题/规则/统计/开关）
├── JunkItem.java         数据模型：单项
├── JunkCategory.java     数据模型：分类
├── ScanEngine.java       8 分类扫描 + 60s 缓存 + 分类开关
├── CleanEngine.java      清理 + 路径安全校验 + 回收站集成
├── Finder.java           大文件 / 目录排行 / 空项 / 重复 / 缩略图 / APK / 应用缓存
├── Organize.java         整理规则 / 干跑预览 / 执行 / 历史还原
├── Trash.java            回收站：移入 / 恢复 / 删除 / 过期自动清理
├── StorageBarView.java   自绘条形进度条（渐变 + ease-out + 阈值变色）
├── StatsChartView.java   自绘 7 天柱状图
├── MainActivity.java     三 Tab 导航 + 主题重建 + 权限申请
├── HomePage.java         存储 / 扫描 / 分类卡 / 清理 / 目录排行
├── ToolsPage.java        9 张工具卡片
└── SettingsPage.java     环境 / 外观 / 行为 / 分类 / 白名单 / 统计 / 关于
```

## 构建

需要 Android SDK（build-tools 34.0.0 + platform 34）与 JDK 17。

```bash
# 首次生成签名库
keytool -genkeypair -keystore junkclean.keystore -alias junkclean \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass junkclean123 -keypass junkclean123 \
  -dname "CN=JunkClean, O=ce11kjw, C=CN"

./build.sh
# 产物：out/JunkClean-v2.0.0.apk
```

> JDK 17 下 `-source 8` 配合 `-bootclasspath` 不支持 lambda，代码统一使用匿名内部类。
> `build.sh` 会在 javac 报错时终止构建，不会产出残缺 APK。

## 权限

| 权限 | 用途 |
|------|------|
| `QUERY_ALL_PACKAGES` | 枚举已安装应用，识别残留、缓存、apk 安装状态 |
| `MANAGE_EXTERNAL_STORAGE` | 扫描与清理 sdcard（Android 11+） |
| `READ/WRITE_EXTERNAL_STORAGE` | Android 10 及以下存储访问 |

首次启动会引导开启「所有文件访问权限」。root 用户另需在设置页点「测试 root 权限」授权。

## 使用

1. 授予存储权限；有 root 则进「设置」点「测试 root 权限」
2. 「首页」扫描 → 展开分类核对 → 勾选后清理（长按条目可加白名单）
3. 「工具箱」按需使用专项功能；删除默认先入回收站，可在回收站恢复
4. 「设置」调整主题、清理行为、分类开关，查看 7 天统计

## License

MIT
