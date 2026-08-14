const APP_KEY = 'mathPlanetStateV1';
const PLAN_DAYS = 40;
const REVIEW_GAPS = [
  {days:1,label:'隔天回顾'},
  {days:3,label:'三天巩固'},
  {days:7,label:'一周复习'},
  {days:14,label:'两周唤醒'}
];
const gradeNames = ['','一年级','二年级','三年级','四年级','五年级','六年级'];
const weekdayNames = ['日','一','二','三','四','五','六'];
const topics = {
  1:['数一数的秘密','巧填数字宫','图形变变变','排队问题','找规律','火柴棒游戏','比一比','简单移多补少','趣味钟表','数图形'],
  2:['数列找规律','巧算加减法','间隔问题','简单植树','图形计数','等量代换','一笔画','年龄趣题','合理安排','周期问题'],
  3:['和差问题','倍数问题','巧算乘除','盈亏问题','植树问题','周期问题','枚举法','方阵问题','重叠问题','归一问题'],
  4:['和倍问题','差倍问题','年龄问题','鸡兔同笼','平均数','行程初步','加法原理','乘法原理','图形面积','数阵图'],
  5:['牛吃草问题','流水行船','相遇追及','容斥原理','抽屉原理','组合图形','分数应用','工程问题','逻辑推理','数论初步'],
  6:['浓度问题','工程进阶','比例模型','圆与扇形','立体几何','排列组合','复杂行程','不定方程','最大最小','综合推理']
};
const thoughts=['数学不是算得快，而是想得巧。','复杂的问题，也可以从最简单的一步开始。','画一画、试一试，答案常常就藏在图里。','错误不是终点，而是思路转弯的路标。','把新问题变成见过的问题，就是聪明的办法。'];
const uploadedCurriculum = Array.isArray(window.MATH_PLANET_CURRICULUM) ? window.MATH_PLANET_CURRICULUM : [];
const uploadedQuestions = Array.isArray(window.MATH_PLANET_QUESTIONS) ? window.MATH_PLANET_QUESTIONS : [];
const questionsByVideo = new Map(uploadedQuestions.map(item => [item.videoId, item]));
const decompositionPlan = Array.isArray(window.MATH_DECOMPOSITION_PLAN) ? window.MATH_DECOMPOSITION_PLAN : [];

let state = loadState();
let activeLesson = null;
let videoCompletionUnlocked = false;
let selectedPlanDay = null;

const $ = (id) => document.getElementById(id);
const pad = n => String(n).padStart(2,'0');
const isoLocal = d => `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}`;
const parseDate = s => { const [y,m,d]=s.split('-').map(Number); return new Date(y,m-1,d); };
const addDays = (date,n) => { const d=new Date(date); d.setDate(d.getDate()+n); return d; };
const dayDiff = (a,b) => Math.floor((parseDate(a)-parseDate(b))/86400000);

function loadState(){ try{return JSON.parse(localStorage.getItem(APP_KEY))||null}catch{return null} }
function saveState(){ localStorage.setItem(APP_KEY,JSON.stringify(state)); }
function addReviewSchedule(days){
  days.forEach(day=>{
    day.reviews=REVIEW_GAPS.map((rule,index)=>{
      const source=days[day.day-rule.days];
      if(!source?.lessons.length)return null;
      return {lesson:source.lessons[(day.day+index)%source.lessons.length],gapDays:rule.days,label:rule.label};
    }).filter(Boolean);
  });
  return days;
}
function makePlan(grade){
  const realLessons=uploadedCurriculum.filter(item=>item.grade===grade).map(item=>({
    id:item.id,title:item.title,topic:item.topic,duration:item.durationMinutes||6,
    videoUrl:item.videoUrl,points:['观察条件','画图分析','举一反三']
  }));
  if(realLessons.length){
    const days=Array.from({length:PLAN_DAYS},(_,day)=>({day,lessons:[],quiz:[],reviews:[]}));
    realLessons.forEach((lesson,index)=>{
      const day=Math.floor(index*PLAN_DAYS/realLessons.length);
      days[day].lessons.push(lesson);
      const question=questionsByVideo.get(lesson.id);
      if(question) days[day].quiz.push(question);
    });
    return addReviewSchedule(days);
  }
  const list=topics[grade], lessons=[];
  for(let i=0;i<90;i++){
    const topic=list[Math.floor(i/3)%list.length];
    const part=(i%3)+1;
    lessons.push({id:`g${grade}-v${i+1}`,title:`${topic} · ${['认识方法','例题拆解','举一反三'][part-1]}`,topic,duration:[6,7,5,8][i%4],points:['观察条件','画图分析','举一反三'].slice(0,2+(i%2))});
  }
  const days=Array.from({length:PLAN_DAYS},(_,day)=>({day,lessons:[],quiz:makeQuiz(grade,list[day%list.length],day),reviews:[]}));
  lessons.forEach((lesson,index)=>days[Math.floor(index*PLAN_DAYS/lessons.length)].lessons.push(lesson));
  return addReviewSchedule(days);
}
function makeQuiz(grade,topic,day){
  const base=grade*2+day+3;
  return [
    {q:`学习“${topic}”后，先观察：${base}、${base+2}、${base+4}，下一个数是？`,opts:[base+5,base+6,base+7],answer:1,explain:'相邻两个数都增加 2。'},
    {q:`有 ${base} 颗星，又点亮了 ${grade+2} 颗，现在一共有多少颗？`,opts:[base+grade+1,base+grade+2,base*2],answer:1,explain:'把原来的星星和新点亮的星星相加。'},
    {q:'遇到一道暂时不会的奥数题，哪种方法更好？',opts:['立刻放弃','只猜一个答案','画图并从简单情况试起'],answer:2,explain:'画图和尝试简单情况，能帮我们发现规律。'}
  ];
}

function init(){
  $('startDate').min=isoLocal(new Date()); $('startDate').value=isoLocal(new Date());
  bindEvents(); render();
}
function bindEvents(){
  $('setupForm').addEventListener('submit',e=>{
    e.preventDefault(); const data=new FormData(e.currentTarget);
    state={name:data.get('childName').trim(),grade:Number(data.get('grade')),startDate:data.get('startDate'),completedLessons:{},completedQuestions:{},completedDays:{},dayRewards:{}};
    saveState(); render(); toast('计划已创建，开始日期已锁定 🔒');
  });
  $('taskList').addEventListener('click',e=>{const b=e.target.closest('[data-lesson]');if(b)openLesson(b.dataset.lesson)});
  $('reviewCard').addEventListener('click',e=>{const b=e.target.closest('[data-review-lesson]');if(b)openLesson(b.dataset.reviewLesson)});
  $('lessonVideo').addEventListener('play',()=>$('videoPlayerWrap').classList.add('started'));
  $('lessonVideo').addEventListener('ended',completeVideoPlayback);
  $('lessonVideo').addEventListener('timeupdate',checkVideoProgress);
  $('lessonVideo').addEventListener('error',()=>toast('视频无法播放，请检查文件是否完整'));
  $('lessonQuestion').addEventListener('click',handleLessonQuestionClick);
  $('dateStrip').addEventListener('click',e=>{const cell=e.target.closest('[data-plan-day]');if(!cell)return;selectedPlanDay=Number(cell.dataset.planDay);render()});
  document.querySelectorAll('[data-close]').forEach(b=>b.addEventListener('click',()=>$(b.dataset.close).close()));
  $('datePrev').addEventListener('click',()=>scrollDates(-1)); $('dateNext').addEventListener('click',()=>scrollDates(1));
}
function render(){
  const ready=!!state; $('setupView').hidden=ready; $('dashboardView').hidden=!ready;
  if(!ready)return;
  ensureRewards();
  const now=new Date(), today=isoLocal(now), idx=dayDiff(today,state.startDate), plan=makePlan(state.grade), current=Math.max(0,Math.min(idx,plan.length-1));
  if(selectedPlanDay===null||!plan[selectedPlanDay])selectedPlanDay=current;
  renderTasks(plan[selectedPlanDay]);
  renderPlanBoard(plan,idx,selectedPlanDay); $('totalStars').textContent=calculateTotalStars(plan);
}
function ensureRewards(){state.completedLessons=state.completedLessons||{};state.completedQuestions=state.completedQuestions||{};state.completedDays=state.completedDays||{};state.dayRewards=state.dayRewards||{}}
function renderPlanBoard(plan,idx,selectedDay){
  $('dateStrip').innerHTML=plan.map(day=>{
    const date=isoLocal(addDays(parseDate(state.startDate),day.day)),d=parseDate(date),done=!!state.completedDays[date],today=day.day===idx,past=day.day<idx&&!done,selected=day.day===selectedDay;
    const stateClass=[done?'done':today?'today':past?'makeup':'available',selected?'selected':''].join(' ');
    const visibleStatus=done?'✓':today?'今日':'';
    return `<div class="date-cell ${stateClass}" data-plan-day="${day.day}" role="button" tabindex="0" aria-label="${date}${done?'，已完成':today?'，今日':''}"><small>${weekdayNames[d.getDay()]}</small><strong>${d.getDate()}</strong>${visibleStatus?`<em>${visibleStatus}</em>`:''}</div>`
  }).join('');
  requestAnimationFrame(()=>document.querySelector('.date-cell.selected')?.scrollIntoView({behavior:'smooth',inline:'center',block:'nearest'}));
}
function scrollDates(direction){$('dateStrip').scrollBy({left:direction*420,behavior:'smooth'})}
function renderTasks(dayPlan){
  $('taskList').innerHTML=dayPlan.lessons.map((lesson,i)=>{
    const complete=!!state.completedLessons[lesson.id];
    return `<article class="task-card ${complete?'done':''}"><div class="task-index">${complete?'✓':pad(i+1)}</div><div class="task-meta"><strong>${lesson.title}</strong></div><button class="task-action" data-lesson="${lesson.id}">${complete?'再挑战':'开始挑战'}</button></article>`
  }).join('');
  renderReviews(dayPlan);
  renderDecomposition(dayPlan.day);
}
function renderReviews(dayPlan){
  const card=$('reviewCard'),reviews=dayPlan.reviews||[];
  card.innerHTML=`<div class="review-card-head"><span>复习站</span><div><strong>记忆能量补给</strong><small>先在脑中回想，记不清时再打开视频</small></div></div>${reviews.length?`<div class="review-list">${reviews.map(item=>`<button class="review-item" data-review-lesson="${item.lesson.id}"><b>${item.label}</b><span>${item.lesson.title}</span><em>回顾一下</em></button>`).join('')}</div>`:'<p class="review-empty">第一天先收集新知识，明天开启第一次记忆补给。</p>'}`;
}
function renderDecomposition(day){
  const task=decompositionPlan[day],card=$('decompositionCard');
  if(!task){card.hidden=true;return} card.hidden=false;
  const exercises=Array.isArray(task.exercises)?task.exercises:[{label:'拆题 1',focus:task.focus,problem:task.problem,parentPrompt:task.parentPrompt}];
  card.innerHTML=`<div class="decomposition-card-head"><div><span>THINKING BOSS</span><strong>思维挑战关</strong></div><em>第 ${task.day} 天 · ${task.stageTitle}</em></div><div class="decomposition-exercises">${exercises.map((exercise,index)=>`<section class="decomposition-exercise"><div class="exercise-label">${exercise.label||`拆题 ${index+1}`}</div><p class="training-focus">挑战能力：${exercise.focus}</p><h3>${exercise.problem}</h3><p class="parent-prompt"><b>陪练提示：</b>${exercise.parentPrompt}</p></section>`).join('')}</div><small class="no-submit">这一关只需要说出思路，不列式、不计算</small>`;
}
function openLesson(id){
  const lesson=makePlan(state.grade).flatMap(d=>d.lessons).find(v=>v.id===id); if(!lesson)return; activeLesson=lesson;
  $('lessonNumber').textContent=lesson.id.split('v')[1].padStart(2,'0'); $('lessonGradeBadge').textContent=`${gradeNames[state.grade]} · 思维训练`;
  $('lessonTitle').textContent=lesson.title; $('lessonSummary').textContent=`通过生活里的小故事认识“${lesson.topic}”，学会观察、尝试和验证。`;
  const learned=!!state.completedLessons[id], video=$('lessonVideo');
  video.pause(); videoCompletionUnlocked=learned; video.src=lesson.videoUrl||''; video.load(); $('videoPlayerWrap').classList.remove('started');
  $('lessonPoints').innerHTML=lesson.points.map(x=>`<span>✦ ${x}</span>`).join('');
  renderLessonQuestion(); $('lessonDialog').showModal();
}
function checkVideoProgress(){
  const video=$('lessonVideo');
  if(videoCompletionUnlocked||!Number.isFinite(video.duration)||video.duration<=0)return;
  const ratio=video.currentTime/video.duration,remaining=video.duration-video.currentTime;
  if(ratio>=.95||remaining<=2)completeVideoPlayback();
}
function completeVideoPlayback(){if(videoCompletionUnlocked)return;videoCompletionUnlocked=true;if(activeLesson)state.completedLessons[activeLesson.id]=isoLocal(new Date());saveState();renderLessonQuestion();render();toast('视频完成，下面这道题已经解锁啦 ✦')}
function activeQuestion(){return activeLesson?questionsByVideo.get(activeLesson.id):null}
function renderLessonQuestion(){
  const box=$('lessonQuestion'),question=activeQuestion(); if(!question){box.hidden=true;return} box.hidden=false;
  const learned=!!state.completedLessons[activeLesson.id],result=state.completedQuestions[activeLesson.id];
  if(!learned){box.className='lesson-question locked';box.innerHTML='<b>🔒 看完视频后，这道题会在这里解锁</b>';return}
  box.className='lesson-question';
  const feedback=result?`<p class="lesson-question-feedback ${result.correct?'correct':'wrong'}">${result.correct?'🌟 答对啦！认真思考的你真棒！':'💪 这次还没答对，看看解析，再试一次吧！'}<br>${question.explain}</p>`:'';
  box.innerHTML=`<span class="lesson-question-label">视频后的思维小题</span><h3>${question.q}</h3><div class="quiz-options">${question.opts.map((o,i)=>`<button class="quiz-option" data-lesson-option="${i}" ${result?.correct?'disabled':''}><b>${String.fromCharCode(65+i)}.</b> ${o}</button>`).join('')}</div>${feedback}`;
}
function handleLessonQuestionClick(e){const option=e.target.closest('[data-lesson-option]');if(!option||!activeLesson)return;submitLessonAnswer(Number(option.dataset.lessonOption))}
function submitLessonAnswer(selected){
  const question=activeQuestion(),plan=makePlan(state.grade),day=plan.findIndex(x=>x.lessons.some(v=>v.id===activeLesson.id));if(!question||day<0)return;
  const dayPlan=plan[day],date=isoLocal(addDays(parseDate(state.startDate),day)),previous=dayRewardStars(date,dayPlan),correct=selected===question.answer;
  const previousAnswer=state.completedQuestions[activeLesson.id],savedCorrect=previousAnswer?.correct||correct;
  state.completedQuestions[activeLesson.id]={correct:savedCorrect,everWrong:previousAnswer?.everWrong||!correct,answeredAt:new Date().toISOString()};
  const results=dayPlan.lessons.map(x=>state.completedQuestions[x.id]).filter(Boolean),questionTotal=Math.min(dayPlan.lessons.length,dayPlan.quiz.length);
  state.dayRewards[date]={answered:results.length,correct:results.filter(x=>x.correct).length,perfect:results.length===questionTotal&&results.every(x=>x.correct&&!x.everWrong)};
  if(results.length>=questionTotal&&dayPlan.lessons.every(x=>state.completedLessons[x.id]))state.completedDays[date]={score:results.filter(x=>x.correct).length,total:questionTotal,completedAt:new Date().toISOString()};
  const gained=Math.max(0,dayRewardStars(date,dayPlan)-previous)+awardNewMilestones(plan);saveState();renderLessonQuestion();render();
  toast(correct?`答对啦！${gained?`获得 ${gained} 颗星 ✦`:'继续保持 ✦'}`:'别灰心，看看解析再试一次 💪');
}
function dayRewardStars(date,dayPlan){const r=state.dayRewards?.[date]||{},videos=dayPlan.lessons.filter(x=>state.completedLessons[x.id]).length,pairs=Math.min(videos,r.answered||0);return (pairs>=3?5:pairs>=2?3:pairs>=1?1:0)+(r.perfect?1:0)}
function milestoneDays(total){return [...new Set([5,10,20,25,total].filter(n=>n>0&&n<=total))]}
function completedFromStart(plan){let count=0;for(const day of plan){const date=isoLocal(addDays(parseDate(state.startDate),day.day));if(!state.completedDays[date])break;count++}return count}
function awardNewMilestones(plan){let gained=0;state.milestones=state.milestones||{};const completed=completedFromStart(plan);for(const day of milestoneDays(plan.length)){if(completed>=day&&!state.milestones[day]){state.milestones[day]=true;gained+=5}}return gained}
function calculateTotalStars(plan){ensureRewards();let total=0;for(const day of plan){const date=isoLocal(addDays(parseDate(state.startDate),day.day));total+=dayRewardStars(date,day)}total+=Object.values(state.milestones||{}).filter(Boolean).length*5;return total}
function formatDate(s){const d=parseDate(s);return `${d.getFullYear()}年${d.getMonth()+1}月${d.getDate()}日`}
function formatShort(s){const d=parseDate(s);return `${d.getMonth()+1}月${d.getDate()}日`}
function toast(msg){const el=$('toast');el.textContent=msg;el.classList.add('show');clearTimeout(el.timer);el.timer=setTimeout(()=>el.classList.remove('show'),2200)}
init();
