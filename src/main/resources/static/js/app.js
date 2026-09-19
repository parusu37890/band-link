import {api,state,h,icon,showPage,notice} from './ui.js?v=20260919-3';
import {discoveryPage} from './discovery.js?v=20260919-4';
import {accountPage} from './account.js?v=20260919-6';
import {communityPage} from './community.js?v=20260919-3';

const path=()=>location.pathname.replace(/\/+$/,'')||'/';
async function loadUser(){try{state.user=await api('/api/auth/me');}catch{state.user=null;}}
function header(){
 const el=document.querySelector('#header');const here=path();
 const brand=(tag,attrs='')=>`<${tag} class="wordmark" ${attrs}><img class="brand-mark" src="/assets/mark.svg?v=20260913-1" alt=""><span>Band Link</span></${tag}>`;
 if(state.user?.status==='SUSPENDED'){el.innerHTML=`<div class="header-inner">${brand('a','href="/support" aria-label="Band Link"')}</div>`;return;}
 if(state.user && !state.user.emailVerified){
  // Unverified is no longer confined to /verify-email alone (route() below now permits the same
  // read-only board an anonymous visitor can see), so this keeps a working way back to /posts
  // instead of the plain unclickable wordmark it used to be - everything else (messages, my
  // posts, profile) still isn't reachable, so those links stay off this header.
  el.innerHTML=`<div class="header-inner">${brand('a','href="/posts" aria-label="Band Link ホーム"')}<nav class="main-nav" aria-label="メインナビゲーション"><a href="/posts" class="${here==='/'||here==='/posts'?'active':''}">仲間を探す</a></nav><button type="button" class="button quiet" id="verification-logout">ログアウト</button></div>`;
  el.querySelector('#verification-logout').onclick=async()=>{try{await api('/api/auth/logout',{method:'POST'});}finally{location.assign('/login');}};
  return;
 }
 const feedbackLinks=state.user?`<a href="/contact" class="${here==='/contact'?'active':''}">お問い合わせ</a><a href="/feature-request" class="${here==='/feature-request'?'active':''}">機能要望</a>`:'';
 el.innerHTML=`<div class="header-inner">${brand('a','href="/posts" aria-label="Band Link ホーム"')}<nav class="main-nav" aria-label="メインナビゲーション"><a href="/posts" class="${here==='/'||here==='/posts'?'active':''}">仲間を探す</a>${state.user?`<a href="/my/posts" class="${here==='/my/posts'?'active':''}">自分の投稿</a>`:''}${feedbackLinks}</nav><div class="header-actions">${state.user?`<a class="icon-button" data-message-link href="/messages" aria-label="メッセージ">${icon('message')}<span data-unread-badge class="badge-count" hidden></span></a><a class="button secondary header-profile" href="/users/${state.user.id}">プロフィール</a>`:`${here==='/login'?'':'<a class="button secondary" href="/login">ログイン</a>'}${here==='/register'?'':'<a class="button primary register-cta" href="/register">新規登録</a>'}`}</div></div>`;
 if(state.user)refreshUnreadBadge();
}
// community.js dispatches this right after marking a conversation read, so the badge catches up
// immediately instead of only on the next full route() - a plain DOM event rather than an import,
// since app.js already imports communityPage and a reverse import would be circular.
function refreshUnreadBadge(){
 const el=document.querySelector('#header');
 if(!state.user||!el)return;
 // The unread count is a colour-only cue for sighted users; without also updating the link's
 // accessible name, assistive tech announces a plain "メッセージ" even when there is something new
 // (NFT-010: state must reach assistive tech, not just be shown as a colour/shape).
 api('/api/notifications/unread-count?type=NEW_MESSAGE').then(x=>{const count=x?.count||0;const b=el.querySelector('[data-unread-badge]');if(b){b.hidden=!count;b.textContent=count>99?'99+':String(count);}const link=el.querySelector('[data-message-link]');if(link)link.setAttribute('aria-label',count?`メッセージ（未読${count}件）`:'メッセージ');}).catch(()=>{});
}
window.addEventListener('messages-read',refreshUnreadBadge);
function footer(){
 const btn=document.querySelector('#footer-logout');
 if(!btn)return;
 btn.style.marginLeft='';
 btn.hidden=!state.user;
 // Align to where the release date starts on the row below rather than the row's own right
 // edge - the tagline text doesn't reach that edge, so a plain margin-left:auto overshot it.
 const day=document.querySelector('#footer-day');
 if(state.user&&day){
  const delta=day.getBoundingClientRect().left-btn.getBoundingClientRect().left-10;
  btn.style.marginLeft=Math.max(0,delta)+'px';
 }
 btn.onclick=async()=>{try{await api('/api/auth/logout',{method:'POST'});}finally{location.assign('/login');}};
}
// requirements 3章: while an account is suspended, the screen after login carries the notice and
// where to ask about it, and nothing else. Without this the app looked normal and the suspension
// only surfaced as a 409 on whatever the person tried to do. The support page keeps its own route
// so it stays reachable, and logout lives here because settings no longer does. The screen used to
// send people to the help page, which now carries separate inquiry and feature-request forms.
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
 await loadUser();header();footer();
 const current=path();
 if(state.user?.status==='SUSPENDED'&&!['/support','/contact','/feature-request'].includes(current)){suspendedScreen();return;}
 // Mirrors EmailVerificationGateFilter's own allowlist: the same read-only board an anonymous
 // visitor can already see, everything else still bounces to /verify-email.
 const boardBrowsingAllowed=p=>p==='/'||p==='/posts'||/^\/posts\/\d+$/.test(p)||/^\/users\/\d+$/.test(p);
 if(state.user && !state.user.emailVerified && current!=='/verify-email' && !boardBrowsingAllowed(current)){
  history.replaceState(null,'','/verify-email');
  await accountPage('/verify-email');
  return;
 }
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
  '/messages', '/notifications', '/blocks', '/admin', '/admin/reports', '/support', '/contact', '/feature-request', '/privacy']);
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

// The shared #dialog (confirm prompts, the report form, image lightboxes) is a native <dialog>:
// Escape already closes it for free, but a click on its own ::backdrop is reported as a click on the
// dialog element itself, with no built-in behaviour attached - none of the callers close it, so
// clicking outside the dialog's content silently did nothing.
document.querySelector('#dialog')?.addEventListener('click', event => {
  if (event.target.id === 'dialog') event.target.close();
});

window.addEventListener('popstate',route);window.addEventListener('DOMContentLoaded',route);
