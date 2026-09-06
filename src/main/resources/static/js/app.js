import {api,state,h,icon} from './ui.js';
import {discoveryPage} from './discovery.js';
import {accountPage} from './account.js';
import {communityPage} from './community.js';

const path=()=>location.pathname.replace(/\/+$/,'')||'/';
async function loadUser(){try{state.user=await api('/api/auth/me');}catch{state.user=null;}}
function header(){
 const el=document.querySelector('#header');const here=path();
 el.innerHTML=`<div class="header-inner"><a class="wordmark" href="/posts" aria-label="Band Link ホーム">Band Link</a><nav class="main-nav" aria-label="メインナビゲーション"><a href="/posts" class="${here==='/'||here==='/posts'?'active':''}">仲間を探す</a>${state.user?`<a href="/my/posts" class="${here==='/my/posts'?'active':''}">自分の募集</a>`:''}</nav><div class="header-actions">${state.user?`<a class="icon-button" href="/notifications" aria-label="通知">${icon('bell')}<span data-unread-dot class="dot" hidden></span></a><a class="icon-button" href="/messages" aria-label="メッセージ">${icon('message')}</a><a class="button secondary header-profile" href="/users/${state.user.id}">プロフィール</a>`:`<a class="button secondary" href="/login">ログイン</a><a class="button primary" href="/register">新規登録</a>`}</div></div>`;
 if(state.user){api('/api/notifications/unread-count').then(x=>{const d=el.querySelector('[data-unread-dot]');if(d)d.hidden=!(x?.count>0);}).catch(()=>{});}
}
async function route(){
 await loadUser();header();
 const current=path();
 try {if(await accountPage(current))return;if(await communityPage(current))return;if(await discoveryPage(current))return;location.assign('/posts');}
 catch(error){const main=document.querySelector('#main');main.innerHTML=`<div class="page"> <div class="notice error" role="alert">${h(error.message||'ページを読み込めませんでした。')}</div><p style="margin-top:24px"><a class="button secondary" href="/posts">募集一覧へ戻る</a></p></div>`;}
}
window.addEventListener('popstate',route);window.addEventListener('DOMContentLoaded',route);
