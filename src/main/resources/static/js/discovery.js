import {api,h,icon,avatar,state,main,showPage,notice,empty,button,toast,bindForm,report,confirmAction,time,navigate,frequencies,ages,frequencyLabel,chips,choices,counter,requireUser,verificationNotice} from './ui.js';
const savedListingKey='band-link:list-return';
const readListing=()=>{try{return JSON.parse(sessionStorage.getItem(savedListingKey)||'null');}catch{return null;}};
const writeListing=value=>{try{sessionStorage.setItem(savedListingKey,JSON.stringify(value));}catch{/* Browsing remains available when storage is disabled. */}};
const masterFields=[['prefectureIds','活動エリア','prefectures'],['partIds','パート','parts'],['genreIds','ジャンル','genres'],['stanceIds','活動スタンス','stances']];
const selected=(p,key)=>(p.get(key)||'').split(',').filter(Boolean);
const labelNames = values => (values||[]).map(x=>x.name).join('・');
export function postCard(post) {
  const area = labelNames(post.prefectures) || '活動エリア未設定';
  const joining = post.type === 'WANTS_TO_JOIN';
  return `<article class="post-card" data-post-id="${Number(post.id)}">
    <div class="card-top"><span class="post-type ${joining?'join':''}">${joining?'参加希望':'メンバー募集'}</span><span class="post-number">No. ${String(Number(post.id)).padStart(4,'0')}</span></div>
    <h2><a href="/posts/${Number(post.id)}">${h(post.title)}</a></h2>
    <p class="post-location">${h(area)}${post.areaSub?' · '+h(post.areaSub):''}</p>
    <p class="excerpt">${h(post.content)}</p>
    <p class="post-roles"><span>${joining?'担当パート':'募集パート'}</span>${h(labelNames(post.parts))}</p>
    ${chips(post.genres)}
    <div class="card-bottom"><a href="/users/${Number(post.userId)}" class="person">${avatar({username:post.username})}<div><div class="person-name">${h(post.username)}</div><div class="person-sub">${h(frequencyLabel(post.activityFrequency))}</div></div></a><a href="/posts/${Number(post.id)}" class="icon-button" aria-label="${h(post.title)}の詳細">${icon('arrow')}</a></div>
  </article>`;
}
async function masters(){ if(!state.masters)state.masters=await api('/api/masters');return state.masters; }
export async function discoveryPage(path) {
 if(path==='/'||path==='/posts'){await listing();return true;}
 if(path==='/my/posts'){if(requireUser())await ownPosts();return true;}
 if(path==='/posts/new'||/^\/posts\/\d+\/edit$/.test(path)){if(requireUser())await editor(path.match(/\d+/)?.[0]);return true;}
 if(/^\/posts\/\d+$/.test(path)){await detail(path.split('/')[2]);return true;}
 return false;
}
async function listing(){
 const params=new URLSearchParams(location.search);const data=await masters();const activeCount=[...params].filter(([k,v])=>v&&k!=='type'&&k!=='keyword').length;
 const quickPartUrl=id=>{const next=new URLSearchParams(params);next.set('partIds',id);next.delete('cursor');return '/posts?'+next;};
 showPage(`<div class="page discovery-page"><section class="discovery-intro"><div><p class="eyebrow">バンドメンバー募集・参加希望 <span class="edition">JAPAN / BAND LINK</span></p><h1>一緒に鳴らす、<br><span>仲間を探そう。</span></h1></div><div class="discovery-note"><p>週末のスタジオも、次のライブも。<br>エリアとパートから、自分に合う募集を。</p><a href="${state.user?'/posts/new':'/register'}">自分の募集を掲載する ${icon('arrow')}</a></div></section><form id="search-form"><div class="search-bar">${icon('search')}<label for="keyword" class="sr-only">キーワードで募集を検索</label><input id="keyword" name="keyword" class="search-input" maxlength="100" placeholder="ジャンル・アーティスト名など" value="${h(params.get('keyword'))}"><button type="submit" class="button primary">検索</button></div><nav class="quick-parts" aria-label="パートから探す"><span>パートから</span>${data.parts.slice(0,5).map(part=>`<a href="${h(quickPartUrl(part.id))}">${h(part.name)}</a>`).join('')}</nav><div class="discover-tabs"><div class="tabs" role="group" aria-label="募集の種類">${[['','すべて'],['MEMBER_WANTED','メンバー募集'],['WANTS_TO_JOIN','参加希望']].map(([value,label])=>`<button type="button" class="tab" aria-pressed="${(params.get('type')||'')===value}" data-type="${value}">${label}</button>`).join('')}</div><button type="button" class="button secondary filter-toggle" aria-expanded="${activeCount>0}" aria-controls="filters">${icon('filter')}絞り込み${activeCount?` (${activeCount})`:''}</button></div><section id="filters" class="filter-panel" ${activeCount?'':'hidden'}><div class="filter-grid">${masterFields.map(([key,label,source])=>`<fieldset class="filter-group"><legend>${label}</legend>${source==='prefectures'?`<label for="filter-area" class="sr-only">活動エリア（複数選択できます）</label><select id="filter-area" name="${key}" multiple size="5">${data[source].map(x=>`<option value="${x.id}" ${selected(params,key).includes(String(x.id))?'selected':''}>${h(x.name)}</option>`).join('')}</select><p class="hint">パソコンではCtrl / ⌘を押しながら複数選択できます。</p>`:choices(key,data[source],selected(params,key))}</fieldset>`).join('')}<fieldset class="filter-group"><legend>希望年齢層</legend>${choices('ageRanges',ages,selected(params,'ageRanges'))}</fieldset><fieldset class="filter-group"><legend>活動頻度</legend>${choices('activityFrequency',frequencies,selected(params,'activityFrequency'))}</fieldset></div><div class="filter-actions"><p class="hint">同じ項目は「どれかに一致」で検索します。</p><div class="row"><button type="button" class="button quiet" id="reset-filters">クリア</button><button type="submit" class="button primary">条件を適用</button></div></div></section></form><div class="results-heading"><strong id="result-count" role="status">募集を読み込み中…</strong><span>掲載順</span></div><div id="results" class="post-grid" aria-busy="true"></div><div class="load-more" id="load-sentinel"><p id="page-status" class="hint" role="status"></p><button class="button secondary" id="load-more" hidden>さらに表示</button></div><aside class="join-banner"><div><h2>あなたのバンドにも、新しい仲間を。</h2><p>活動場所、好きな音楽、練習のペースを募集に書いてみましょう。</p></div>${button('募集を掲載する',state.user?'/posts/new':'/register')}</aside></div>`,'仲間を探す');
 const form=main.querySelector('#search-form');let type=params.get('type')||'';
 const search=()=>{const fd=new FormData(form);const next=new URLSearchParams();for(const key of ['keyword',...masterFields.map(x=>x[0]),'ageRanges','activityFrequency']){const values=fd.getAll(key).map(String).filter(x=>x.trim());if(values.length)next.set(key,values.join(','));}if(type)next.set('type',type);navigate('/posts'+(next.size?'?'+next:''));};
 form.addEventListener('submit',e=>{e.preventDefault();search();});main.querySelectorAll('[data-type]').forEach(el=>el.onclick=()=>{type=el.dataset.type;search();});
 main.querySelector('.filter-toggle').onclick=e=>{const target=main.querySelector('#filters');target.hidden=!target.hidden;e.currentTarget.setAttribute('aria-expanded',String(!target.hidden));};
 main.querySelector('#reset-filters').onclick=()=>{form.querySelectorAll('#filters input').forEach(x=>x.checked=false);form.querySelectorAll('#filters option').forEach(x=>x.selected=false);};
 const results=main.querySelector('#results');
 const more=main.querySelector('#load-more');
 const pageStatus=main.querySelector('#page-status');
 const request=new URLSearchParams(params);request.delete('cursor');request.set('limit','12');
 let cursor=null,total=0,loading=false,hasNext=true,failed=false;
 const seen=new Set();
 const append=async()=>{
   if(loading||!hasNext)return;
   loading=true;failed=false;more.disabled=true;results.setAttribute('aria-busy','true');pageStatus.textContent='募集を読み込んでいます…';
   try {
     if(cursor)request.set('cursor',cursor);
     const raw=await api('/api/posts/page?'+request);
     if(!Array.isArray(raw.items))throw new Error('募集の読み込みに失敗しました。');
     const items=raw.items.filter(p=>!seen.has(p.id));items.forEach(p=>seen.add(p.id));
     results.insertAdjacentHTML('beforeend',items.map(postCard).join(''));total+=items.length;
     cursor=raw.nextCursor||null;hasNext=Boolean(raw.hasNext&&cursor);
     if(!total)results.innerHTML=empty('条件に合う募集はありません','地域やパートの条件を広げて探してみてください。',button('すべての募集を見る','/posts','secondary'));
     main.querySelector('#result-count').textContent=hasNext?`${total}件を表示中`:`${total}件の募集`;
     pageStatus.textContent=hasNext?'スクロールで続きを表示します':total?'すべての募集を表示しました':'';
     more.hidden=!hasNext;more.textContent='さらに表示';
   } catch(error) {
     failed=true;pageStatus.textContent=error.message;more.hidden=false;more.textContent='もう一度読み込む';
     if(!total)main.querySelector('#result-count').textContent='募集を読み込めませんでした';
   } finally {loading=false;more.disabled=false;results.setAttribute('aria-busy','false');}
 };
 more.onclick=append;
 const restore=readListing();
 const currentUrl=location.pathname+location.search;
 const remember=()=>writeListing({url:currentUrl,count:total,y:scrollY,at:Date.now(),restore:true});
 results.addEventListener('click',event=>{const link=event.target.closest('a');if(link&&event.button===0&&!event.ctrlKey&&!event.metaKey&&!event.shiftKey)remember();});
 await append();
 if(restore?.restore&&restore.url===currentUrl&&Date.now()-restore.at<30*60*1000){
   while(total<restore.count&&hasNext&&!failed)await append();
   window.scrollTo(0,restore.y);writeListing({...restore,restore:false});
 }

 const observer=new IntersectionObserver(entries=>{if(entries[0].isIntersecting&&!failed)void append();},{rootMargin:'240px'});
 observer.observe(main.querySelector('#load-sentinel'));window.addEventListener('pagehide',()=>observer.disconnect(),{once:true});

}
async function detail(id){
 const previous=readListing();
 const backUrl=previous?.url&&/^\/posts(?:\?|$)/.test(previous.url)?previous.url:'/posts';
 const [p,images]=await Promise.all([api('/api/posts/'+id),api('/api/posts/'+id+'/images')]);const own=state.user?.id===p.userId;const gallery=images.length?`<section class="detail-section"><h2>募集画像</h2><div class="post-image-grid">${images.map(image=>{const url=image.imageUrl&&/^\/uploads\/[A-Za-z0-9\/_.-]+$/.test(image.imageUrl);return url?`<a href="${h(image.imageUrl)}" target="_blank" rel="noopener" aria-label="募集画像を大きく表示（新しいタブ）"><img src="${h(image.imageUrl)}" alt="募集画像" loading="lazy"></a>`:''}).join('')}</div></section>`:'';
 let author={username:p.username};try{author=await api('/api/users/'+p.userId);}catch{/* The recruitment remains readable if its profile is unavailable. */}
 showPage(`<div class="page post-detail-page"><a class="back-link" href="${h(backUrl)}">${icon('back')}募集一覧へ</a><div class="detail-layout"><article><span class="post-type ${p.type==='WANTS_TO_JOIN'?'join':''}">${p.type==='WANTS_TO_JOIN'?'参加希望':'メンバー募集'}</span><h1 class="detail-title">${h(p.title)}</h1><p class="muted small">${icon('pin')} ${h(labelNames(p.prefectures))}${p.areaSub?' / '+h(p.areaSub):''}</p>${p.status==='CLOSED'?`<div style="margin-top:24px">${notice('この募集は終了しました。投稿者のプロフィールから引き続き連絡できます。')}</div>`:''}<section class="detail-section"><h2>一緒にやりたいこと</h2><div class="body-text">${h(p.content)}</div></section>${gallery}<section class="detail-section"><h2>募集条件</h2><dl class="facts"><dt>パート</dt><dd>${chips(p.parts)}</dd><dt>ジャンル</dt><dd>${chips(p.genres)}</dd><dt>スタンス</dt><dd>${h(labelNames(p.stances))}</dd><dt>希望年齢層</dt><dd>${h((p.ageRanges||[]).map(x=>ages.find(a=>a[0]===x)?.[1]||x).join('・'))}</dd><dt>活動頻度</dt><dd>${h(frequencyLabel(p.activityFrequency))}</dd><dt>掲載期限</dt><dd>${h(time(p.expiresAt))}</dd></dl></section><section class="detail-section row spread"><span class="hint">公開 ${h(time(p.createdAt))}</span>${state.user&&!own?'<button class="button quiet small" id="report-post">この募集を通報する</button>':''}</section></article><aside class="panel sidebar stack"><div><span class="eyebrow">この募集の投稿者</span>${avatar(author,true)}<h2>${h(p.username)}</h2><p class="muted small" style="margin-top:8px">${h(labelNames(p.prefectures))}</p></div>${button('プロフィールを見る','/users/'+p.userId,'secondary')}${own?button('自分の募集を管理','/my/posts'):button('メッセージを送る',state.user?'/messages?to='+p.userId:'/login?next='+encodeURIComponent('/messages?to='+p.userId))}<p class="hint">${own?'募集の編集・終了・再公開は管理画面から行えます。':'まずは、好きな音楽や活動のイメージを気軽に話してみましょう。'}</p></aside></div></div>`,p.title);
 main.querySelector('.back-link').addEventListener('click',()=>{if(previous)writeListing({...previous,restore:true});});
 main.querySelector('#report-post')?.addEventListener('click',()=>report('POST',id));
}
async function ownPosts(){
 const posts=await api('/api/posts/mine');showPage(`<div class="page own-posts-page"><div class="page-heading"><div><h1>自分の募集</h1><p>公開中の募集と、これまでの募集を管理します。</p></div>${button('新しい募集を作成','/posts/new')}</div><div class="stack">${verificationNotice()}${posts.length?posts.map(p=>`<article class="panel"><div class="row spread"><span class="badge">${p.status==='OPEN'?'公開中':'募集終了'}</span><span class="hint">掲載期限 ${h(time(p.expiresAt))}</span></div><h2 style="margin:20px 0">${h(p.title)}</h2><div class="row">${button('詳細を見る','/posts/'+p.id,'secondary')}${p.status==='OPEN'?`${button('編集する','/posts/'+p.id+'/edit','secondary')}<button class="button quiet" data-close="${p.id}">募集を終了</button>`:['MANUAL','EXPIRED'].includes(p.closedReason)?`<button class="button primary" data-reopen="${p.id}">再公開する</button>`:'<span class="hint">この募集は再公開できません。</span>'}</div></article>`).join(''):empty('募集を作成してみましょう','あなたの音楽の好みや活動条件を伝えると、仲間が見つけやすくなります。',button('募集を作成する','/posts/new'))}</div><p class="hint" style="margin-top:24px">同時に公開できる募集は1件です。新規投稿・編集後の12時間は、新規投稿・編集ができません。終了・再公開はいつでも操作できます。</p></div>`,'自分の募集');
 main.querySelectorAll('[data-close]').forEach(el=>el.onclick=()=>confirmAction('募集を終了しますか？','募集一覧から非表示になります。詳細と過去の会話は残り、あとから再公開できます。',async()=>{await api('/api/posts/'+el.dataset.close+'/close',{method:'PATCH'});await ownPosts();toast('募集を終了しました。');}));
 main.querySelectorAll('[data-reopen]').forEach(el=>el.onclick=async()=>{el.disabled=true;try{await api('/api/posts/'+el.dataset.reopen+'/reopen',{method:'PATCH'});await ownPosts();toast('募集を再公開しました。');}catch(e){toast(e.message);el.disabled=false;}});
}
async function editor(id){
 const [m,p]=await Promise.all([masters(),id?api('/api/posts/'+id):Promise.resolve(null)]);if(p&&p.userId!==state.user.id)throw new Error('この募集を編集できるのは投稿者本人だけです。');
 const section=(key,label,source)=>`<div class="form-field"><span class="form-label">${label} <span class="optional">必須${source==='prefectures'?'・3つまで':''}</span></span>${source==='prefectures'?`<label class="sr-only" for="post-area">活動エリア</label><select id="post-area" name="prefectureIds" multiple required size="6">${m.prefectures.map(x=>`<option value="${x.id}" ${p?.prefectures?.some(y=>y.id===x.id)?'selected':''}>${h(x.name)}</option>`).join('')}</select><p class="hint">Ctrl / ⌘を押しながら、複数の都道府県を選択できます。</p>`:choices(key,m[source],p?.[source]?.map(x=>x.id)||[])}</div>`;
 showPage(`<div class="page post-editor-page"><a class="back-link" href="/my/posts">${icon('back')}自分の募集へ</a><div class="page-heading"><div><h1>${id?'募集を編集する':'バンドの募集を掲載する'}</h1><p>活動場所や練習のペースを、具体的に伝えましょう。</p></div></div><div class="editor-layout"><aside class="editor-guide"><strong>募集に書くこと</strong><ol><li>募集の内容</li><li>エリア・パート</li><li>活動のペース</li></ol><p>公開後の編集は12時間に1回です。画像も含めて確認してください。</p></aside><div>${verificationNotice()}<form id="post-form" style="margin-top:28px"><fieldset class="form-section"><legend>01　どんな仲間を探していますか？</legend><div class="stack">${!id?`<div class="form-field"><span class="form-label">募集の種類</span>${choices('type',[['MEMBER_WANTED','メンバーを募集したい'],['WANTS_TO_JOIN','バンドに参加したい']],['MEMBER_WANTED'],true)}</div>`:''}<div class="form-field"><label for="title">募集タイトル <span class="optional">必須</span></label><input id="title" name="title" required maxlength="100" placeholder="例：週末に一緒に音を鳴らす、ギター仲間を募集" value="${h(p?.title)}"><span class="hint" data-count="title"></span></div><div class="form-field"><label for="content">募集の本文 <span class="optional">必須</span></label><textarea id="content" name="content" required maxlength="2000" rows="9" placeholder="やりたい音楽、好きなアーティスト、活動の目標など。具体的に書くと、相性のよい仲間に伝わりやすくなります。">${h(p?.content)}</textarea><span class="hint" data-count="content"></span></div><div class="form-field"><label for="images">募集画像 <span class="optional">任意・5枚まで、1枚5MB</span></label><input class="input" id="images" name="images" type="file" accept="image/jpeg,image/png,image/webp" multiple><span class="hint">jpg / png / webp。選択した画像は保存すると公開されます。</span><div id="image-selection" class="image-selection" aria-live="polite"></div></div></div></fieldset><fieldset class="form-section"><legend>02　活動エリアと音楽の好み</legend><div class="stack">${section('prefectureIds','活動エリア','prefectures')}<div class="form-field"><label for="areaSub">市区町村・駅など <span class="optional">任意</span></label><input id="areaSub" name="areaSub" maxlength="100" placeholder="例：下北沢、新宿周辺のスタジオ" value="${h(p?.areaSub)}"></div>${section('partIds','パート','parts')}${section('genreIds','ジャンル','genres')}${section('stanceIds','活動スタンス','stances')}</div></fieldset><fieldset class="form-section"><legend>03　活動のペース</legend><div class="stack"><div class="form-field"><span class="form-label">希望年齢層</span>${choices('ageRanges',ages,p?.ageRanges||['ANY'])}</div><div class="form-field"><label for="frequency">活動頻度</label><select id="frequency" name="activityFrequency">${frequencies.map(([v,l])=>`<option value="${v}" ${(p?.activityFrequency||'NEGOTIABLE')===v?'selected':''}>${l}</option>`).join('')}</select></div></div></fieldset>${notice('保存後12時間は、新規投稿・編集ができません。公開期間は30日です。内容を確認してから保存してください。')}<div class="sticky-actions">${button('キャンセル','/my/posts','secondary')}<button type="submit" class="button primary" ${!state.user.emailVerified?'disabled':''}>${id?'変更を保存する':'募集を公開する'}</button></div></form></div></div></div>`,id?'募集の編集':'募集の作成');
 const form=main.querySelector('form');counter(form);
 let imagePreviews=[];
 const clearPreviews=()=>{imagePreviews.forEach(URL.revokeObjectURL);imagePreviews=[];};
 form.elements.images.addEventListener('change',()=>{clearPreviews();const files=[...form.elements.images.files];const container=main.querySelector('#image-selection');container.innerHTML=files.slice(0,5).map(file=>{const url=URL.createObjectURL(file);imagePreviews.push(url);return `<figure><img src="${h(url)}" alt="選択した画像"><figcaption>${h(file.name)}</figcaption></figure>`;}).join('')+(files.length>5?notice('画像は5枚までです。選び直してください。','error'):'');});
 window.addEventListener('pagehide',clearPreviews,{once:true});
 form.querySelectorAll('[name=ageRanges]').forEach(el=>el.onchange=()=>{if(el.checked)form.querySelectorAll('[name=ageRanges]').forEach(other=>{if(other!==el&&(el.value==='ANY'||other.value==='ANY'))other.checked=false;});});
 bindForm(form,async fd=>{const body={title:fd.get('title').trim(),content:fd.get('content').trim(),areaSub:fd.get('areaSub').trim(),activityFrequency:fd.get('activityFrequency'),ageRanges:fd.getAll('ageRanges')};for(const [key,label] of masterFields){body[key]=fd.getAll(key).map(Number);if(!body[key].length)throw new Error(label+'を1つ以上選択してください。');}if(body.prefectureIds.length>3)throw new Error('活動エリアは3つまで選択できます。');if(!body.ageRanges.length)throw new Error('希望年齢層を選択してください。');if(!id)body.type=fd.get('type');const files=fd.getAll('images').filter(file=>file instanceof File&&file.size);if(files.length>5)throw new Error('募集画像は5枚まで選択してください。');if(files.some(file=>file.size>5*1024*1024||!['image/jpeg','image/png','image/webp'].includes(file.type)))throw new Error('画像はjpg/png/webp、1枚5MBまでです。');const saved=await api('/api/posts'+(id?'/'+id:''),{method:id?'PUT':'POST',body});if(files.length){const imageBody=new FormData();files.forEach(file=>imageBody.append('files',file));await api('/api/posts/'+saved.id+'/images/batch',{method:'POST',body:imageBody});}navigate('/posts/'+saved.id);});
}
