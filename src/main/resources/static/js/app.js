import {api,state,h,icon,showPage,notice} from './ui.js';
import {discoveryPage} from './discovery.js';
import {accountPage} from './account.js';
import {communityPage} from './community.js';

const path=()=>location.pathname.replace(/\/+$/,'')||'/';
async function loadUser(){try{state.user=await api('/api/auth/me');}catch{state.user=null;}}
function header(){
 const el=document.querySelector('#header');const here=path();
 if(state.user?.status==='SUSPENDED'){el.innerHTML='<div class="header-inner"><a class="wordmark" href="/support" aria-label="Band Link">Band Link</a></div>';return;}
 el.innerHTML=`<div class="header-inner"><a class="wordmark" href="/posts" aria-label="Band Link ホーム">Band Link</a><nav class="main-nav" aria-label="メインナビゲーション"><a href="/posts" class="${here==='/'||here==='/posts'?'active':''}">仲間を探す</a>${state.user?`<a href="/my/posts" class="${here==='/my/posts'?'active':''}">自分の募集</a>`:''}</nav><div class="header-actions">${state.user?`<a class="icon-button" href="/notifications" aria-label="通知">${icon('bell')}<span data-unread-dot class="dot" hidden></span></a><a class="icon-button" href="/messages" aria-label="メッセージ">${icon('message')}</a><a class="button secondary header-profile" href="/users/${state.user.id}">プロフィール</a>`:`${here==='/login'?'':'<a class="button secondary" href="/login">ログイン</a>'}${here==='/register'?'':'<a class="button primary" href="/register">新規登録</a>'}`}</div></div>`;
 if(state.user){api('/api/notifications/unread-count').then(x=>{const d=el.querySelector('[data-unread-dot]');if(d)d.hidden=!(x?.count>0);}).catch(()=>{});}
}
// requirements 3章: while an account is suspended, the screen after login carries the notice and
// where to ask about it, and nothing else. Without this the app looked normal and the suspension
// only surfaced as a 409 on whatever the person tried to do. The support page keeps its own route
// so it stays reachable, and logout lives here because settings no longer does. The screen used to
// send people to a contact address that page never held (requirements 11章 lists 問い合わせ先 as
// undecided), so it now says plainly that the channel is not open yet — see docs/decisions/0006.
function suspendedScreen(){
 showPage(`<div class="page narrow">
   <div class="page-heading"><div><h1>アカウントの利用を停止しています</h1></div></div>
   ${notice('現在このアカウントでは、募集の掲載とメッセージの送信ができません。公開していた募集とプロフィールは非公開になっています。','error')}
   <p class="body-text">解除の手続きは運営が行います。この画面から解除を申し出る方法は、まだ用意できていません。停止中にできること・できないことはヘルプにまとめています。</p>
   <div class="row" style="margin-top:28px">
     <a class="button primary" href="/support">ヘルプを見る</a>
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
// Every link was a full document load: the shell has eight stylesheets and seven modules, so a
// click threw all of that away and fetched it again, with a blank frame in between. The router was
// already here — nothing was handing it a click. Same-origin links to routes the shell serves go
// through it now; everything else (uploads, /api, downloads, other tabs, modified clicks) is left
// to the browser. Kept to plain string checks so the list reads like PageController's.
const shellRoutes = new Set(['/', '/posts', '/posts/new', '/my/posts', '/settings', '/settings/profile',
  '/settings/blocks', '/login', '/register', '/verify-email', '/password-reset', '/password-reset/confirm',
  '/messages', '/notifications', '/blocks', '/admin', '/admin/reports', '/support']);
const digits = value => value.length > 0 && [...value].every(c => c >= '0' && c <= '9');
const withId = [['/posts/', ''], ['/posts/', '/edit'], ['/users/', ''], ['/messages/', '']];
const servedByShell = pathname => shellRoutes.has(pathname) || withId.some(([prefix, suffix]) =>
  pathname.startsWith(prefix) && pathname.endsWith(suffix) &&
  digits(pathname.slice(prefix.length, pathname.length - suffix.length)));

document.addEventListener('click', event => {
 if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
 const link = event.target.closest('a[href]');
 if (!link || link.target || link.hasAttribute('download') || link.getAttribute('rel')?.includes('external')) return;
 const url = new URL(link.getAttribute('href'), location.href);
 if (url.origin !== location.origin || !servedByShell(url.pathname)) return;
 event.preventDefault();
 if (url.href === location.href) return;
 history.pushState(null, '', url);
 route();
 scrollTo(0, 0);
});

window.addEventListener('popstate',route);window.addEventListener('DOMContentLoaded',route);
