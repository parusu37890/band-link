import {postEditor} from './post-editor.js';
import {api,h,icon,avatar,state,main,showPage,notice,empty,button,toast,report,confirmAction,time,relativeTime,ages,frequencyLabel,chips,requireUser,verificationNotice} from './ui.js';
import {listing,readListing,writeListing} from './recruitment-search.js';
const labelNames = values => (values||[]).map(x=>x.name).join('・');
export function postCard(post) {
 const joining=post.type==='WANTS_TO_JOIN',own=state.user&&String(state.user.id)===String(post.userId);
 const contact=own?button('募集を管理','/my/posts','quiet small'):button('メッセージ',state.user?'/messages?to='+Number(post.userId):'/login?next='+encodeURIComponent('/messages?to='+Number(post.userId)),'secondary small');
 return `<article class="post-card" data-post-id="${Number(post.id)}">
  <a class="post-thumb" href="/users/${Number(post.userId)}" tabindex="-1" aria-hidden="true">${avatar({username:post.username,profileImageUrl:post.authorImageUrl})}</a>
  <div class="post-classification"><time class="post-age" datetime="${h(post.createdAt)}" title="${h(time(post.createdAt))}">投稿 ${h(relativeTime(post.createdAt))}</time></div>
  <div class="post-content"><p class="post-roles"><span>${joining?'担当':'募集'}</span>${h(labelNames(post.parts))}</p><h2><span class="post-mark ${joining?'join':''}" aria-hidden="true" title="${joining?'参加希望':'メンバー募集'}">${joining?'参':'募'}</span><a href="/posts/${Number(post.id)}"><span class="sr-only">${joining?'参加希望':'メンバー募集'}：</span>${h(post.title)}</a></h2><p class="post-location">${h(labelNames(post.prefectures))}${post.areaSub?' / '+h(post.areaSub):''}<span>${h(frequencyLabel(post.activityFrequency))}</span></p><p class="excerpt">${h(post.content)}</p>${chips(post.genres)}</div>
  <div class="post-person"><a href="/users/${Number(post.userId)}" class="person">${avatar({username:post.username,profileImageUrl:post.authorImageUrl})}<span><span class="person-name">${h(post.username)}</span>${post.authorAgeRange?`<span class="person-age">${h(post.authorAgeRange)}</span>`:''}${post.authorOnline?`<span class="person-online"><span class="online-dot" aria-hidden="true"></span>オンライン中</span>`:''}${post.authorActivity&&!post.authorOnline?`<span class="person-sub">${h(post.authorActivity)}</span>`:''}</span></a><div class="post-contact">${contact}<a class="read-post" href="/posts/${Number(post.id)}">詳細を読む ${icon('arrow')}</a></div></div>
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
 const gallery=images.length?`<section class="detail-section"><h2>募集の写真</h2><div class="post-image-grid">${images.map((image,index)=>image.imageUrl&&/^\/uploads\/[A-Za-z0-9/_.-]+$/.test(image.imageUrl)?`<a href="${h(image.imageUrl)}" target="_blank" rel="noopener" aria-label="${index+1}枚目の募集画像を大きく表示（新しいタブ）"><img src="${h(image.imageUrl)}" alt="募集画像 ${index+1}枚目" loading="lazy"></a>`:'').join('')}</div></section>`:'';
 showPage(`<div class="page post-detail-page"><a class="back-link" href="${h(backUrl)}">${icon('back')}募集一覧へ</a><div class="detail-layout"><article class="detail-article">
  <div class="detail-topline"><span class="post-type ${joining?'join':''}">${joining?'参加希望':'メンバー募集'}</span><span>投稿 ${h(relativeTime(p.createdAt))}</span>${p.status==='CLOSED'?'<strong>募集終了</strong>':''}</div>
  <h1 class="detail-title">${h(p.title)}</h1>
  ${p.status==='CLOSED'?notice('この募集は終了しました。投稿者のプロフィールから引き続き連絡できます。'):''}
  <section class="fit-summary" aria-label="主な募集条件"><div class="fit-part"><h2>${joining?'担当したいパート':'募集しているパート'}</h2><p>${h(labelNames(p.parts))}</p></div><div class="fit-practical"><div><h2>活動場所</h2><p>${h(area)}</p>${p.areaSub?`<span>${h(p.areaSub)}</span>`:''}</div><div><h2>活動頻度</h2><p>${h(frequencyLabel(p.activityFrequency))}</p></div></div></section>
  <section class="detail-section recruitment-story"><h2>募集について</h2><div class="body-text">${h(p.content)}</div></section>
  <section class="detail-section musical-conditions"><h2>音楽と活動の方向性</h2><div class="condition-columns"><div><h3>ジャンル</h3>${chips(p.genres)}</div><div><h3>活動スタンス</h3><p>${h(labelNames(p.stances))}</p></div>${ageText?`<div><h3>希望する年代</h3><p>${h(ageText)}</p></div>`:''}</div></section>${gallery}
  <footer class="detail-record"><p>投稿日 ${h(time(p.createdAt))}${p.expiresAt?' / 掲載期限 '+h(time(p.expiresAt)):''}</p>${state.user&&!own?'<button class="button quiet small" id="report-post">この募集を通報</button>':''}</footer>
 </article><aside class="sidebar author-contact"><h2>投稿者</h2><a class="detail-person" href="/users/${p.userId}">${avatar(author,true)}<span><strong>${h(p.username)}</strong>${p.authorActivity?`<small>${h(p.authorActivity)}</small>`:''}</span></a>${author.bio?`<p class="author-excerpt">${h(author.bio)}</p>`:''}<a class="read-post" href="/users/${p.userId}">プロフィールを読む ${icon('arrow')}</a><div class="author-message">${own?button('自分の募集を管理','/my/posts'):button('メッセージを送る',state.user?'/messages?to='+p.userId:'/login?next='+encodeURIComponent('/messages?to='+p.userId))}${own?'<p>編集・終了・再公開は自分の募集から行えます。</p>':''}</div></aside></div></div>`,p.title);
 main.querySelector('.back-link').onclick=()=>{if(previous)writeListing({...previous,restore:true});};main.querySelector('#report-post')?.addEventListener('click',()=>report('POST',id));
}
async function ownPosts(){
 const posts=await api('/api/posts/mine');showPage(`<div class="page own-posts-page"><div class="page-heading"><div><h1>自分の募集</h1><p>公開中の募集と、これまでの募集を管理します。</p></div>${button('新しい募集を作成','/posts/new')}</div><div class="stack">${verificationNotice()}${posts.length?posts.map(p=>`<article class="panel"><div class="row spread"><span class="badge">${p.status==='OPEN'?'公開中':'募集終了'}</span><span class="hint">掲載期限 ${h(time(p.expiresAt))}</span></div><h2 style="margin:20px 0">${h(p.title)}</h2><div class="row">${button('詳細を見る','/posts/'+p.id,'secondary')}${p.status==='OPEN'?`${button('編集する','/posts/'+p.id+'/edit','secondary')}<button class="button quiet" data-close="${p.id}">募集を終了</button>`:['MANUAL','EXPIRED'].includes(p.closedReason)?`<button class="button primary" data-reopen="${p.id}">再公開する</button>`:'<span class="hint">この募集は再公開できません。</span>'}</div></article>`).join(''):empty('募集を作成してみましょう','あなたの音楽の好みや活動条件を伝えると、仲間が見つけやすくなります。',button('募集を作成する','/posts/new'))}</div><p class="hint" style="margin-top:24px">同時に公開できる募集は1件です。新規投稿・編集後の12時間は、新規投稿・編集ができません。終了・再公開はいつでも操作できます。</p></div>`,'自分の募集');
 main.querySelectorAll('[data-close]').forEach(el=>el.onclick=()=>confirmAction('募集を終了しますか？','募集一覧から非表示になります。詳細と過去の会話は残り、あとから再公開できます。',async()=>{await api('/api/posts/'+el.dataset.close+'/close',{method:'PATCH'});await ownPosts();toast('募集を終了しました。');}));
 main.querySelectorAll('[data-reopen]').forEach(el=>el.onclick=async()=>{el.disabled=true;try{await api('/api/posts/'+el.dataset.reopen+'/reopen',{method:'PATCH'});await ownPosts();toast('募集を再公開しました。');}catch(e){toast(e.message);el.disabled=false;}});
}
