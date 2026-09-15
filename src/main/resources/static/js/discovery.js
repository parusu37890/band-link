import {postEditor} from './post-editor.js';
import {api,h,icon,avatar,state,main,showPage,notice,empty,button,toast,report,confirmAction,time,relativeTime,loginRelativeTime,ages,frequencyLabel,chips,requireUser,verificationNotice,openImageViewer} from './ui.js';
import {listing,readListing,writeListing} from './recruitment-search.js';
const labelNames = values => (values||[]).map(x=>x.name).join('・');
export function postCard(post) {
 const joining=post.type==='WANTS_TO_JOIN',own=state.user&&String(state.user.id)===String(post.userId);
 const loginLabel=loginRelativeTime(post.authorLastLoginAt,post.authorOnline);
 const contact=own?button('募集を管理','/my/posts','quiet small'):button('メッセージ',state.user?'/messages?to='+Number(post.userId):'/login?next='+encodeURIComponent('/messages?to='+Number(post.userId)),'secondary small');
 const author={username:post.username,profileImageUrl:post.authorImageUrl};
 return `<article class="post-card" data-post-id="${Number(post.id)}">
  <div class="post-thumb">${avatar(author,true)}</div><div class="post-content"><h2><span class="post-mark ${joining?'join':''}" aria-hidden="true" title="${joining?'加入希望':'募集'}">${joining?'加':'募'}</span><a href="/posts/${Number(post.id)}"><span class="sr-only">${joining?'加入希望':'募集'}：</span>${h(post.title)}</a></h2><p class="post-roles"><span>${joining?'加入':'募集'}</span>${h(labelNames(post.parts))}</p><p class="post-location">${h(labelNames(post.prefectures))}</p><div class="post-person"><a href="/users/${Number(post.userId)}" class="person"><span><span class="person-name">${h(post.username)}</span>${post.authorAgeRange?`<span class="person-age">${h(post.authorAgeRange)}</span>`:''}${post.authorGender?`<span class="person-gender">${h(post.authorGender)}</span>`:''}</span></a><span class="post-times">最終ログイン ${h(loginLabel)}${post.authorOnline?'<span class="online-dot" aria-hidden="true"></span>':''}　投稿 ${h(relativeTime(post.createdAt))}</span><div class="post-contact">${contact}<a class="read-post" href="/posts/${Number(post.id)}">詳細を読む ${icon('arrow')}</a></div></div></div>
 </article>`;
}
export async function discoveryPage(path) {
 if(path==='/'||path==='/posts'){await listing(postCard);return true;}
 if(path==='/my/posts'){if(requireUser())await ownPosts();return true;}
 if(path==='/posts/new'||/^\/posts\/\d+\/edit$/.test(path)){if(requireUser())await postEditor(path.match(/\d+/)?.[0]);return true;}
 if(/^\/posts\/\d+$/.test(path)){await detail(path.split('/')[2]);return true;}
 return false;
}
async function detail(id){
 const previous=readListing();const backUrl=previous?.url&&/^\/posts(?:\?|$)/.test(previous.url)?previous.url:'/posts';
 const [p,images]=await Promise.all([api('/api/posts/'+id),api('/api/posts/'+id+'/images')]);
 const own=String(state.user?.id)===String(p.userId),joining=p.type==='WANTS_TO_JOIN';
 let author={username:p.username};try{author=await api('/api/users/'+p.userId);}catch{/* Public post remains readable if profile lookup fails. */}
 const area=labelNames(p.prefectures),ageText=(p.ageRanges||[]).map(x=>ages.find(a=>a[0]===x)?.[1]||x).join('・');
 const gallery=images.length?`<section class="detail-section"><h2>募集の写真</h2><div class="post-image-grid">${images.map((image,index)=>image.imageUrl&&/^\/uploads\/[A-Za-z0-9/_.-]+$/.test(image.imageUrl)?`<button type="button" class="post-image-button" data-expand-image="${h(image.imageUrl)}" aria-label="${index+1}枚目の募集画像を拡大表示"><img src="${h(image.imageUrl)}" alt="募集画像 ${index+1}枚目" loading="lazy"></button>`:'').join('')}</div></section>`:'';
 showPage(`<div class="page post-detail-page"><a class="back-link" href="${h(backUrl)}">${icon('back')}募集一覧へ</a><div class="detail-layout"><article class="detail-article">
  <div class="detail-topline"><span class="post-type ${joining?'join':''}">${joining?'加入希望':'募集'}</span><span>投稿 ${h(relativeTime(p.createdAt))}</span>${p.status==='CLOSED'?'<strong>募集終了</strong>':''}</div>
  <h1 class="detail-title">${h(p.title)}</h1>
  ${p.status==='CLOSED'?notice('この募集は終了しました。投稿者のプロフィールから引き続き連絡できます。'):''}
  <section class="fit-summary" aria-label="主な募集条件"><div class="fit-part"><h2>${joining?'担当したいパート':'募集しているパート'}</h2><p>${h(labelNames(p.parts))}</p></div><div class="fit-practical"><div><h2>活動場所</h2><p>${h(area)}</p></div><div><h2>活動頻度</h2><p>${h(frequencyLabel(p.activityFrequency))}</p></div></div></section>
  <section class="detail-section recruitment-story"><h2>募集について</h2><div class="body-text">${h(p.content)}</div></section>
  <section class="detail-section musical-conditions"><h2>音楽と活動の方向性</h2><div class="condition-columns"><div><h3>ジャンル</h3>${chips(p.genres)}</div><div><h3>活動スタンス</h3><p>${h(labelNames(p.stances))}</p></div>${ageText?`<div><h3>希望する年代</h3><p>${h(ageText)}</p></div>`:''}</div></section>${gallery}
  <footer class="detail-record"><p>投稿日 ${h(time(p.createdAt))}${p.expiresAt?' / 掲載期限 '+h(time(p.expiresAt)):''}</p>${state.user&&!own?'<button class="button quiet small" id="report-post">この募集を通報</button>':''}</footer>
 </article><aside class="sidebar author-contact"><h2>投稿者</h2><a class="detail-person" href="/users/${p.userId}">${avatar(author,true)}<span><strong>${h(p.username)}</strong>${p.authorActivity?`<small>${h(p.authorActivity)}</small>`:''}</span></a>${author.bio?`<p class="author-excerpt">${h(author.bio)}</p>`:''}<a class="read-post" href="/users/${p.userId}">プロフィールを読む ${icon('arrow')}</a><div class="author-message">${own?button('自分の募集を管理','/my/posts'):button('メッセージを送る',state.user?'/messages?to='+p.userId:'/login?next='+encodeURIComponent('/messages?to='+p.userId))}${own?'<p>編集・終了・再公開は自分の募集から行えます。</p>':''}</div></aside></div></div>`,p.title);
 main.querySelector('.back-link').onclick=()=>{if(previous)writeListing({...previous,restore:true});};main.querySelector('#report-post')?.addEventListener('click',()=>report('POST',id));
 main.querySelectorAll('[data-expand-image]').forEach(btn=>btn.addEventListener('click',()=>openImageViewer(btn.dataset.expandImage,btn.getAttribute('aria-label'))));
}
async function ownPosts(){
 const posts=await api('/api/posts/mine');showPage(`<div class="page own-posts-page"><div class="page-heading"><div><h1>自分の投稿</h1><p>公開中の募集と、これまでの投稿を管理します。</p></div>${button('新しい投稿を作成','/posts/new')}</div><div class="stack">${verificationNotice()}${posts.length?posts.map(p=>`<article class="panel"><div class="row spread"><span class="badge">${p.status==='OPEN'?'公開中':'募集終了'}</span><span class="hint">掲載期限 ${h(time(p.expiresAt))}</span></div><h2 style="margin:20px 0">${h(p.title)}</h2><div class="row">${button('詳細を見る','/posts/'+p.id,'quiet')}${p.status==='OPEN'?`${button('編集する','/posts/'+p.id+'/edit','quiet')}<button class="button quiet" data-boost="${p.id}">更新する</button>`:['MANUAL','EXPIRED'].includes(p.closedReason)?`<button class="button primary" data-reopen="${p.id}">再公開する</button>`:'<span class="hint">この投稿は再公開できません。</span>'}</div>${p.status==='OPEN'?`<div class="row" style="justify-content:center;padding-right:70px;margin-top:8px"><button class="button quiet" data-close="${p.id}">投稿を終了</button></div>`:''}</article>`).join(''):empty('投稿を作成してみましょう','募集または加入希望を投稿すると、音楽仲間が見つかりやすくなります。',button('投稿を作成する','/posts/new'))}</div><p class="hint" style="margin-top:24px">募集と加入は、それぞれ1件ずつ公開できます。新規投稿・編集に制限はありません。「更新する」で一覧の上位に上がりますが、12時間に1回までです。</p></div>`,'自分の投稿');
 main.querySelectorAll('[data-close]').forEach(el=>el.onclick=()=>confirmAction('募集を終了しますか？','募集一覧から非表示になります。詳細と過去の会話は残り、あとから再公開できます。',async()=>{await api('/api/posts/'+el.dataset.close+'/close',{method:'PATCH'});await ownPosts();toast('募集を終了しました。');}));
 main.querySelectorAll('[data-reopen]').forEach(el=>el.onclick=async()=>{el.disabled=true;try{await api('/api/posts/'+el.dataset.reopen+'/reopen',{method:'PATCH'});await ownPosts();toast('募集を再公開しました。');}catch(e){toast(e.message);el.disabled=false;}});
 main.querySelectorAll('[data-boost]').forEach(el=>el.onclick=async()=>{el.disabled=true;try{await api('/api/posts/'+el.dataset.boost+'/boost',{method:'PATCH'});await ownPosts();toast('募集を更新し、一覧の上位に表示されるようにしました。');}catch(e){toast(e.message);el.disabled=false;}});
}
