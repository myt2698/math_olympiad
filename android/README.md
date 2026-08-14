# 思维星球 Android 原生版

这是网页版配套的 Android 原生 Kotlin 工程，不是 WebView 套壳。

## 技术结构

- `MainActivity.kt`：首次计划设置、开始日期锁定、每日课程与学习足迹
- `LessonActivity.kt`：Android 原生 `VideoView` 视频播放页
- `LessonActivity.kt`：视频播放、对应随堂题与完成结果
- `ProgressStore.kt`：用 `SharedPreferences` 保存计划及学习进度
- `CourseRepository.kt`：读取统一的 JSON 课程清单
- `assets/curriculum.json`：课程、视频地址和知识点数据

课程会均匀分布到 40 天，并按学后第 1、3、7、14 天自动安排轻量复习；网页与 Android 原生版使用相同的计划规则。

最低支持 Android 8.0（API 26），目标 API 36。

## 用 Android Studio 打开

1. 在 Android Studio 中选择 **Open**。
2. 打开本目录 `android`，而不是外层网页目录。
3. 等待 Gradle Sync 完成。
4. 选择手机或模拟器运行。

本工程代码使用 Android Gradle Plugin 9.2.0、JDK 17。仓库没有附带 SDK，也没有在当前电脑安装 SDK。

当某个年级的视频文件已经放到外层项目的“1年级奥数”这类目录时，可以运行：

```powershell
node ..\tools\generate-android-curriculum.mjs 1
```

它会按章节号和视频序号生成 UTF-8 课程清单；参数 `1` 可替换为其他年级。

如需把该年级所有视频直接内置到 APK：

```powershell
node ..\tools\generate-android-curriculum.mjs 1 --embed
```

`--embed` 会先删除 `assets/videos` 中之前内置的年级，再复制当前年级。因此以后上传二年级后，执行参数 `2 --embed`，一年级视频会自动从 Android 工程移除。

## 接入视频

编辑 `app/src/main/assets/curriculum.json` 的 `videoUrl`：

- 在线视频：`https://cdn.example.com/grade1/001.mp4`
- 内置小视频：`asset://videos/grade1/001.mp4`
- 本地绝对路径：`file:///data/user/0/.../001.mp4`

当前交付方案按要求一次只内置一个年级。一个年级的 APK 仍可能达到数百 MB，比较适合直接分发 APK。

若确实需要内置视频，把文件放入：

`app/src/main/assets/videos/grade1/`

然后把对应地址改为 `asset://videos/grade1/文件名.mp4`。

## 日期锁定

`ProgressStore.createPlan()` 在已有学习计划时会直接返回 `false`，所以开始日期和年级不能通过普通界面再次修改。卸载应用或清除应用数据才会移除本地计划。

正式上线后应将这一规则同时放在后端，避免用户换设备或清除本地数据后绕过锁定。
