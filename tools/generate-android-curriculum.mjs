import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.dirname(scriptDir);
const grade = Number(process.argv[2] || 1);
const embedVideos = process.argv.includes('--embed');
const sourceRoot = path.join(projectRoot, `${grade}\u5e74\u7ea7\u5965\u6570`);
const output = path.join(projectRoot, 'android', 'app', 'src', 'main', 'assets', 'curriculum.json');
const webOutput = path.join(projectRoot, 'curriculum.js');
const androidVideosRoot = path.join(projectRoot, 'android', 'app', 'src', 'main', 'assets', 'videos');
const gradeAssetsRoot = path.join(androidVideosRoot, `grade${grade}`);

function walk(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
    const fullPath = path.join(directory, entry.name);
    return entry.isDirectory() ? walk(fullPath) : [fullPath];
  });
}

if (!fs.existsSync(sourceRoot)) throw new Error(`Video directory not found: ${sourceRoot}`);

const videos = walk(sourceRoot)
  .filter(file => path.extname(file).toLowerCase() === '.mp4')
  .map(file => {
    const baseName = path.basename(file, path.extname(file));
    const match = baseName.match(/^(\d+)-(\d+)\s*(.+)$/u);
    const categoryName = path.basename(path.dirname(file));
    const categoryMatch = categoryName.match(/^(\d+)\s*(.+)$/u);
    return {
      file,
      section: categoryMatch ? Number(categoryMatch[1]) : 999,
      order: match ? Number(match[2]) : 999,
      title: match ? match[3] : baseName,
      topic: categoryMatch ? categoryMatch[2] : categoryName
    };
  })
  .sort((a, b) => a.section - b.section || a.order - b.order);

if (embedVideos) {
  // The APK intentionally contains only one grade at a time. Rebuilding with
  // --embed removes the previously embedded grade before copying the new one.
  fs.rmSync(androidVideosRoot, { recursive: true, force: true });
  fs.mkdirSync(gradeAssetsRoot, { recursive: true });
}

const curriculum = videos.map((video, index) => {
  const id = `g${grade}-v${String(index + 1).padStart(3, '0')}`;
  if (embedVideos) fs.copyFileSync(video.file, path.join(gradeAssetsRoot, `${id}.mp4`));
  return {
    id,
    grade,
    title: video.title,
    topic: video.topic,
    durationMinutes: 6,
    videoUrl: embedVideos ? `asset://videos/grade${grade}/${id}.mp4` : ''
  };
});

const webCurriculum = curriculum.map((item, index) => ({
  ...item,
  videoUrl: path.relative(projectRoot, videos[index].file).split(path.sep).map(encodeURIComponent).join('/')
}));

function asciiJavaScript(value) {
  return JSON.stringify(value, null, 2).replace(/[\u007f-\uffff]/g, char =>
    `\\u${char.charCodeAt(0).toString(16).padStart(4, '0')}`
  );
}

fs.mkdirSync(path.dirname(output), { recursive: true });
fs.writeFileSync(output, `${JSON.stringify(curriculum, null, 2)}\n`, 'utf8');
fs.writeFileSync(webOutput, `window.MATH_PLANET_CURRICULUM = ${asciiJavaScript(webCurriculum)};\n`, 'ascii');
process.stdout.write(`Generated ${curriculum.length} grade-${grade} lessons${embedVideos ? ' with embedded videos' : ''}\n`);
