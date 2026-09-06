import {api,state,h,icon,showPage,notice} from './ui.js';
import {discoveryPage} from './discovery.js';
import {accountPage} from './account.js';
import {communityPage} from './community.js';

const path=()=>location.pathname.replace(/\/+$/,'')||'/';
async function loadUser(){try{state.user=await api('/api/auth/me');}catch{state.user=null;}}
function header(){
 const el=document.querySelector('#header');const here=path();
 if(state.user?.status==='SUSPENDED'){el.innerHTML='<div class="header-inner"><a class="wordmark" href="/support" aria-label="Band Link">Band Link</a></div>';return;}
 el.innerHTML=`<div class="header-inner"><a class="wordmark" href="/posts" aria-label="Band Link ホーム">Band Link</a><nav class="main-nav" aria-label="メインナビゲーション"><a href="/posts" class="${here==='/'||here==='/posts'?'active':''}">仲間を探す</a>${state.user?`<a href="/my/posts" class="${here==='/my/posts'?'active':''}">自分の募集</a>`:''}</nav><div class="header-actions">${state.user?`<a class="icon-button" href="/notifications" aria-label="通知">${icon('bell')}<span data-unread-dot class="dot" hidden></span></a><a class="icon-button" href="/messages" aria-label="メッセージ">${icon('message')}</a><a class="button secondary header-profile" href="/users/${state.user.id}">プロフィール</a>`:`<a class="button secondary" href="/login">ログイン</a><a class="button primary" href="/register">新規登録</a>`}</div></div>`;
 if(state.user){api('/api/notifications/unread-count').then(x=>{const d=el.querySelector('[data-unread-dot]');if(d)d.hidden=!(x?.count>0);}).catch(()=>{});}
}
// requirements 3章: while an account is suspended, the screen after login carries the notice and
// where to ask about it, and nothing else. Without this the app looked normal and the suspension
// only surfaced as a 409 on whatever the person tried to do. The support page keeps its own route
// so the contact details stay reachable, and logout lives here because settings no longer does.
function suspendedScreen(){
 showPage(`<div class="page narrow">
   <div class="page-heading"><div><h1>アカウントの利用を停止しています</h1></div></div>
   ${notice('現在このアカウントでは、募集の掲載とメッセージの送信ができません。公開していた募集とプロフィールは非公開になっています。','error')}
   <p class="body-text">解除の手続きは運営が行います。心当たりがない場合や解除を希望する場合は、お問い合わせ先からご連絡ください。</p>
   <div class="row" style="margin-top:28px">
     <a class="button primary" href="/support">お問い合わせ先を見る</a>
     <button type="button" class="button secondary" id="suspended-logout">ログアウト</button>
   </div>
 </div>`,'利用停止のお知らせ');
 document.querySelector('#suspended-logout').onclick=async()=>{try{await api('/api/auth/logout',{method:'POST'});}finally{location.assign('/login');}};
}
async function route(){
 await loadUser();header();
 const current=path();
 if(state.user?.status==='SUSPENDED'&&current!=='/support'){suspendedScreen();return;}
 try {if(await accountPage(current))return;if(await communityPage(current))return;if(await discoveryPage(current))return;location.assign('/posts');}
 catch(error){const main=document.querySelector('#main');main.innerHTML=`<div class="page"> <div class="notice error" role="alert">${h(error.message||'ページを読み込めませんでした。')}</div><p style="margin-top:24px"><a class="button secondary" href="/posts">募集一覧へ戻る</a></p></div>`;}
}
window.addEventListener('popstate',route);window.addEventListener('DOMContentLoaded',route);
