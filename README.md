# 思维星球 · 每日奥数

一款面向 1–6 年级孩子的轻量奥数学习计划 Web App 原型。

项目现在包含两种客户端：

- 根目录：网页版及可安装 PWA
- `android/`：Kotlin 编写的 Android 原生版

## 运行

无需安装依赖。直接双击 `index.html`，或在本目录启动任意静态文件服务器：

```powershell
python -m http.server 4173
```

然后打开 `http://localhost:4173`。

## 已实现

- 首次选择孩子名字、年级和开始日期
- 计划开始后锁定年级及日期
- 每天安排 3 个短视频和 3 道随堂题
- 完成视频后才解锁当日挑战
- 本周打卡、连续学习、后续课程日历
- 浏览器本地保存学习进度，刷新不丢失
- 桌面端及手机端自适应

## 接入真实视频和题库

当前 `app.js` 的 `topics`、`makePlan()`、`makeQuiz()` 是演示数据。接入正式内容时，建议把课程整理成 JSON：

```json
{
  "grade": 3,
  "title": "和差问题 · 认识方法",
  "videoUrl": "videos/grade-3/001.mp4",
  "duration": 7,
  "questions": [
    { "q": "题目", "opts": ["A", "B", "C"], "answer": 1, "explain": "解析" }
  ]
}
```

正式上线还应把计划和进度迁移到后端账号体系，家长端增加视频上传、题库编辑和学习报告。

Android 工程的打开、视频接入和打包说明见 `android/README.md`。
