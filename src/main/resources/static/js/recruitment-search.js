import {api,h,state,main,showPage,icon,button,empty,navigate,ages,frequencies} from './ui.js';
export const readListing=()=>{try{return JSON.parse(sessionStorage.getItem('band-link:list-return')||'null');}catch{return null;}};
export const writeListing=value=>{try{sessionStorage.setItem('band-link:list-return',JSON.stringify(value));}catch{}};
const keys=[['prefectureIds','活動エリア','prefectures'],['partIds','パート','parts'],['genreIds','ジャンル','genres'],['stanceIds','活動スタンス','stances'],['ageRanges','希望年齢層',null],['activityFrequency','活動頻度',null]];
const values=(params,key)=>(params.get(key)||'').split(',').filter(Boolean);
const removeUrl=(params,key,value)=>{const next=new URLSearchParams(params);if(value){const rest=values(next,key).filter(x=>x!==value);rest.length?next.set(key,rest.join(',')):next.delete(key);}else next.delete(key);next.delete('cursor');return '/posts'+(next.size?'?'+next:'');};

export async function listing(renderCard){
 const params=new URLSearchParams(location.search);
 const masters=state.masters||(state.masters=await api('/api/masters'));
 const options=key=>{const source=keys.find(x=>x[0]===key)?.[2];return source?masters[source].map(x=>[String(x.id),x.name]):key==='ageRanges'?ages:frequencies;};
 const label=(key,value)=>options(key).find(x=>String(x[0])===String(value))?.[1]||value;
 const applied=keys.flatMap(([key,title])=>values(params,key).map(value=>({key,title,value,label:label(key,value)})));
 if(params.get('keyword'))applied.unshift({key:'keyword',title:'キーワード',value:null,label:params.get('keyword')});
 const typeLabels={'MEMBER_WANTED':'メンバー募集','WANTS_TO_JOIN':'参加希望'};
 const group=([key,title])=>`<details class="search-group" ${['prefectureIds','partIds'].includes(key)?'open':''}><summary><span>${title}</span><span data-choice-count="${key}">${values(params,key).length||'指定なし'}</span></summary><p class="choice-summary" data-choice-summary="${key}">${h(values(params,key).map(v=>label(key,v)).join('・'))}</p><div class="filter-options ${key==='prefectureIds'?'prefecture-options':''}">${options(key).map(([value,name])=>`<label class="filter-option"><input type="checkbox" name="${key}" value="${h(value)}" ${values(params,key).includes(String(value))?'checked':''}><span>${h(name)}</span></label>`).join('')}</div></details>`;
 const filterOpen=matchMedia('(min-width:1100px)').matches;
 showPage(`<div class="page discovery-page">
  <header class="board-heading"><div><h1>バンドメンバー募集</h1><p>メンバーを探す人と、バンドに参加したい人の掲示板。</p></div>${button('募集を掲載する',state.user?'/posts/new':'/register')}</header>
  <div class="board-layout">
   <aside class="search-rail" aria-label="募集の検索"><form id="search-form">
    <div class="search-keyword"><label for="keyword">キーワード</label><div class="search-bar"><input id="keyword" class="search-input" name="keyword" maxlength="100" placeholder="アーティスト・駅名など" value="${h(params.get('keyword'))}"><button type="submit" class="button primary" aria-label="キーワードで検索">${icon('search')}</button></div></div>
    <button class="button secondary filter-toggle" type="button" aria-controls="filters" aria-expanded="${filterOpen}">${icon('filter')}条件を絞る <span>${applied.length?applied.length+'項目選択中':''}</span></button>
    <section id="filters" ${filterOpen?'':'hidden'}><div class="search-guide"><h2>条件を絞る</h2><p>項目内はどれかに一致。<br>項目間はすべてに一致。</p></div>${keys.map(group).join('')}<div class="filter-actions"><p id="filter-draft" role="status">条件を選んで適用してください。</p><button type="submit" class="button primary full">この条件で検索</button><button type="button" class="button quiet full" id="reset-filters">選択をすべて解除</button></div></section>
   </form></aside>
   <section class="board-results" aria-label="募集一覧"><div class="discover-tabs"><div class="tabs" role="group" aria-label="募集の種類">${[['','すべて'],...Object.entries(typeLabels)].map(([value,title])=>`<button type="button" class="tab" data-type="${value}" aria-pressed="${(params.get('type')||'')===value}">${title}</button>`).join('')}</div></div>
    ${applied.length?`<div class="applied-conditions"><p>検索中の条件</p><div class="active-filters">${applied.map(x=>`<a class="active-filter" href="${h(removeUrl(params,x.key,x.value))}" aria-label="${h(x.title+'：'+x.label)}を外して検索"><span><small>${h(x.title)}</small>${h(x.label)}</span>${icon('close')}</a>`).join('')}<a class="clear-search" href="/posts">すべて解除</a></div></div>`:'<p class="browse-hint">パートや活動場所が合う募集から、内容を読んでみましょう。</p>'}
    <div class="results-heading"><strong id="result-count" role="status">募集を読み込み中…</strong><span>新しい掲載順</span></div>
    <div id="results" class="post-grid" aria-busy="true"></div><div class="load-more" id="load-sentinel"><p id="page-status" class="hint" role="status"></p><button class="button secondary" id="load-more" hidden>さらに表示</button></div>
    <p class="board-footnote">募集とプロフィールは登録なしで閲覧できます。連絡にはログインとメール確認が必要です。</p>
   </section>
  </div></div>`,'バンドメンバー募集');
 const form=main.querySelector('#search-form');let type=params.get('type')||'';
 const search=()=>{const fd=new FormData(form);const next=new URLSearchParams();for(const key of ['keyword',...keys.map(x=>x[0])]){const list=fd.getAll(key).map(v=>String(v).trim()).filter(Boolean);if(list.length)next.set(key,list.join(','));}if(type)next.set('type',type);navigate('/posts'+(next.size?'?'+next:''));};
 form.addEventListener('submit',event=>{event.preventDefault();search();});
 main.querySelectorAll('[data-type]').forEach(el=>el.onclick=()=>{type=el.dataset.type;search();});
 const toggle=main.querySelector('.filter-toggle');toggle.onclick=()=>{const filters=main.querySelector('#filters');filters.hidden=!filters.hidden;toggle.setAttribute('aria-expanded',String(!filters.hidden));};
 const updateSelections=()=>{let count=0;for(const [key] of keys){const list=[...form.querySelectorAll(`[name="${key}"]:checked`)].map(x=>x.value);count+=list.length;form.querySelector(`[data-choice-count="${key}"]`).textContent=list.length?list.length+'件':'指定なし';form.querySelector(`[data-choice-summary="${key}"]`).textContent=list.map(v=>label(key,v)).join('・');}form.querySelector('#filter-draft').textContent=`${count}項目選択中。「この条件で検索」で反映します。`;};
 form.addEventListener('change',updateSelections);
 main.querySelector('#reset-filters').onclick=()=>{form.querySelectorAll('input[type=checkbox]').forEach(x=>x.checked=false);updateSelections();};
 const results=main.querySelector('#results'),more=main.querySelector('#load-more'),status=main.querySelector('#page-status');
 const request=new URLSearchParams(params);request.delete('cursor');request.set('limit','12');
 let cursor=null,total=0,loading=false,hasNext=true,failed=false;const seen=new Set();
 const emptyResults=()=>{
  const relax=keys.filter(([key])=>params.has(key)).map(([key,title])=>`<a href="${h(removeUrl(params,key))}">${h(title)}の指定を外す ${icon('arrow')}</a>`).join('');
  return `<div class="search-empty"><h2>${applied.length||type?'条件に合う募集はありません':'公開中の募集はまだありません'}</h2><p>${applied.length||type?'条件を一つ広げると、活動できる相手が見つかるかもしれません。残したい条件はそのまま検索できます。':'メンバー募集だけでなく、参加したいパートや活動場所を書いて、自分から募集を出せます。'}</p>${relax?`<nav class="relax-search" aria-label="条件を広げて探す">${relax}</nav>`:''}<div class="row">${button('すべての募集を見る','/posts','secondary')}${button('自分の募集を書く',state.user?'/posts/new':'/register')}</div></div>`;
 };
 const append=async()=>{if(loading||!hasNext)return;loading=true;failed=false;more.disabled=true;results.setAttribute('aria-busy','true');status.textContent='募集を読み込んでいます…';try{
  if(cursor)request.set('cursor',cursor);const raw=await api('/api/posts/page?'+request);if(!Array.isArray(raw.items))throw new Error('募集の読み込みに失敗しました。');
  const items=raw.items.filter(p=>!seen.has(p.id));items.forEach(p=>seen.add(p.id));results.insertAdjacentHTML('beforeend',items.map(renderCard).join(''));total+=items.length;cursor=raw.nextCursor||null;hasNext=Boolean(raw.hasNext&&cursor);
  if(!total)results.innerHTML=emptyResults();main.querySelector('#result-count').textContent=hasNext?`${total}件を表示中`:`${total}件の募集`;status.textContent=hasNext?'スクロールで続きを表示':total?'すべての募集を表示しました':'';more.hidden=!hasNext;more.textContent='さらに表示';
 }catch(error){failed=true;status.textContent=error.message;more.hidden=false;more.textContent='もう一度読み込む';if(!total)main.querySelector('#result-count').textContent='募集を読み込めませんでした';}finally{loading=false;more.disabled=false;results.setAttribute('aria-busy','false');}};
 more.onclick=append;const restore=readListing();const currentUrl=location.pathname+location.search;
 results.addEventListener('click',event=>{if(event.target.closest('a')&&event.button===0&&!event.ctrlKey&&!event.metaKey&&!event.shiftKey)writeListing({url:currentUrl,count:total,y:scrollY,at:Date.now(),restore:true});});
 await append();if(restore?.restore&&restore.url===currentUrl&&Date.now()-restore.at<1800000){while(total<restore.count&&hasNext&&!failed)await append();scrollTo(0,restore.y);writeListing({...restore,restore:false});}
 const observer=new IntersectionObserver(entries=>{if(entries[0].isIntersecting&&!failed)void append();},{rootMargin:'240px'});observer.observe(main.querySelector('#load-sentinel'));window.addEventListener('pagehide',()=>observer.disconnect(),{once:true});
}
