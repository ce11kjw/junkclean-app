# JunkClean App

Android 垃圾清理 App，纯 Java 原生 View 构建（无 XML 布局、无 Gradle、无第三方依赖）。深空玻璃 UI，root 自动适配。

对应的 Magisk / KernelSU / APatch 模块版：[ce11kjw/junkclean](https://github.com/ce11kjw/junkclean)

## 特性

- **root 自动适配** — 有 root 扫 `/data/data/*/cache` 与系统日志；无 root 降级到公共目录、外部缓存、自身缓存，设置页明示当前模式
- **6 大分类扫描** — 应用缓存 / WebView / 日志 / 临时文件 / 缩略图 / 应用残留（已卸载）
- **工具箱** — 大文件（5 类型筛选）、空文件与空目录、重复文件、应用缓存排行、缩略图
- **长按加白名单** — 首页明细长按即加入白名单并取消勾选
- **路径安全** — 拒绝 `..` 路径遍历，清理范围限定在 sdcard 与已知系统缓存目录
- **谨慎分类** — 应用残留标红「谨慎」徽章，默认不勾选
- **深空玻璃 UI** — 自绘条形存储进度条（渐变 + ease-out 动画 + 阈值变色）

## 技术栈

- 纯 Java + 原生 View，代码构建 UI
- minSdk 26 / targetSdk 34
- 编译链：aapt2 + javac + d8 + zipalign + apksigner
- APK 约 41 KB

## 目录结构

```
src/com/ce11kjw/junkclean/
├── Theme.java            深空玻璃配色 + drawable 工厂
├── UI.java               视图工厂（card/chip/badge/input/button）
├── Shell.java            root 探测 + su/sh 执行 + 降级
├── Util.java             体积格式化 / 目录大小 / rm -rf / sdcard 根
├── Store.java            SharedPreferences 持久化
├── JunkItem.java         数据模型：单条垃圾项
├── JunkCategory.java     数据模型：分类
├── ScanEngine.java       6 分类扫描
├── CleanEngine.java      清理 + 路径安全校验
├── StorageBarView.java   自绘条形进度条
├── MainActivity.java     三 Tab 导航 + 权限申请
├── HomePage.java         存储条 / 扫描 / 分类卡 / 清理
├── ToolsPage.java        5 张工具大卡片
└── SettingsPage.java     root 状态 / 白名单 / 统计 / 关于
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
# 产物：out/JunkClean-v1.0.0.apk
```

> JDK 17 下 `-source 8` 配合 `-bootclasspath` 不支持 lambda，代码统一使用匿名内部类。

## 权限

| 权限 | 用途 |
|------|------|
| `QUERY_ALL_PACKAGES` | 枚举已安装应用，识别残留与缓存 |
| `MANAGE_EXTERNAL_STORAGE` | 无 root 时扫描 sdcard（Android 11+） |
| `READ/WRITE_EXTERNAL_STORAGE` | Android 10 及以下存储访问 |

首次启动若无 root，会引导开启「所有文件访问权限」；有 root 时跳过该申请。

## 使用

1. 安装后进入「设置」→ 点「测试 root 权限」授权（无 root 可跳过）
2. 回「首页」→ 开始扫描 → 点分类卡展开明细 → 勾选后清理
3. 明细项长按即加入白名单，后续扫描跳过
4. 「工具箱」按需使用各专项清理

## License

MIT
