export const main = document.querySelector('#main');
export const state = { user: null, masters: null, csrf: null };
export const h = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const paths = {search:'<circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 4.5 4.5"/>',arrow:'<path d="M5 12h14m-6-6 6 6-6 6"/>','arrow-left':'<path d="M19 12H5m6-6-6 6 6 6"/>',back:'<path d="M19 12H5m6-6-6 6 6 6"/>',filter:'<path d="M4 7h16M4 17h16"/><circle cx="9" cy="7" r="2" fill="currentColor"/><circle cx="16" cy="17" r="2" fill="currentColor"/>',plus:'<path d="M12 5v14M5 12h14"/>',mail:'<rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 6 9 7 9-7"/>',message:'<path d="M20 11.5a7.5 7.5 0 0 1-8 7.5 8.5 8.5 0 0 1-4-.9L4 20l1.5-3.7A7.5 7.5 0 1 1 20 11.5Z"/>',send:'<path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/>',bell:'<path d="M6 8a6 6 0 0 1 12 0c0 8 3 8 3 10H3c0-2 3-2 3-10m4 13h4"/>',user:'<circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/>',pin:'<path d="M19 10c0 5-7 11-7 11S5 15 5 10a7 7 0 0 1 14 0Z"/><circle cx="12" cy="10" r="2"/>',music:'<path d="M9 18V5l11-2v13M9 9l11-2"/><ellipse cx="6" cy="18" rx="3" ry="2"/><ellipse cx="17" cy="16" rx="3" ry="2"/>',close:'<path d="m6 6 12 12M6 18 18 6"/>',check:'<path d="m5 12 4 4L19 6"/>',external:'<path d="M14 3h7v7m0-7L10 14M10 3H3v18h18v-7"/>',shield:'<path d="m12 2 9 4v6c0 5-9 10-9 10S3 17 3 12V6Z"/>',eye:'<path d="M2.5 12s3.2-5 9.5-5 9.5 5 9.5 5-3.2 5-9.5 5-9.5-5-9.5-5Z"/><circle cx="12" cy="12" r="2.2"/>','eye-off':'<path d="m3 3 18 18M10.7 7.2A10.2 10.2 0 0 1 12 7c6.3 0 9.5 5 9.5 5a16.8 16.8 0 0 1-3.1 3.4M6.1 6.1C3.7 7.6 2.5 10 2.5 12c0 0 3.2 5 9.5 5 1 0 1.9-.1 2.7-.4"/>',x:'<path fill="currentColor" stroke="none" d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"/>'};
export const icon = name => `<svg class="icon" viewBox="0 0 24 24" aria-hidden="true">${paths[name] || paths.arrow}</svg>`;
export const button = (label, href, kind='primary') => `<a class="button ${h(kind)}" href="${h(href)}">${h(label)}</a>`;
export function safeUrl(value) { try { const u = new URL(value, location.origin); return ['http:','https:'].includes(u.protocol) ? u.href : ''; } catch { return ''; } }
export function avatar(user, large=false) { const url = user?.profileImageUrl; const local = url && /^\/(?:uploads|api\/images)\/[a-zA-Z0-9/_.-]+$/.test(url); return `<span class="avatar${large?' large':''}" aria-hidden="true">${local ? `<img src="${h(url)}" alt="" loading="lazy">` : h((user?.username || '♪').slice(0,1))}</span>`; }
// The server sends LocalDateTime values, which carry no zone by construction. They are always UTC
// (Clock.systemDefaultZone() in the container) and JacksonDateTimeConfig appends Z to say so, but
// this guards any timestamp that somehow arrives without one: a bare "2026-09-15T15:33:23" is
// ECMAScript-ambiguous and gets silently parsed as local time on a non-UTC system clock, which
// showed a brand-new post as "9時間前" on JST devices.
function parseServerDate(value) { return new Date(/[Zz]|[+-]\d\d:?\d\d$/.test(value) ? value : value + 'Z'); }
export function time(value) { if (!value) return ''; const d=parseServerDate(value); return Number.isNaN(d.valueOf())?'':new Intl.DateTimeFormat('ja-JP',{month:'short',day:'numeric',hour:'2-digit',minute:'2-digit'}).format(d); }
// Posts and login activity share the minute/hour/day/week buckets. Posts expire after 30 days,
// so their display stops at "4週間前"; login activity continues into month buckets.
function activityRelativeTime(value, post = false) {
  if (!value) return '—';
  const then = parseServerDate(value);
  if (Number.isNaN(then.valueOf())) return '—';
  const minutes = Math.max(0, Math.floor((Date.now() - then.getTime()) / 60000));
  if (minutes < 60) return `${Math.max(1, minutes)}分前`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}時間前`;
  const days = Math.floor(hours / 24);
  if (days < 7) return `${days}日前`;
  const weeks = Math.floor(days / 7);
  if (weeks < 4) return `${weeks}週間前`;
  if (post) return '4週間前';
  return `${Math.min(6, Math.max(1, Math.floor(days / 30)))}か月前`;
}
export function relativeTime(value) { return activityRelativeTime(value, true); }
export function loginRelativeTime(value, online = false) {
  return online ? 'オンライン中' : activityRelativeTime(value);
}
export const notice = (message,type='info') => `<div class="notice ${h(type)}"${type==='error'?' role="alert"':''}>${h(message)}</div>`;
export const empty = (title,body='',actionHtml='') => `<div class="empty-state">${icon('music')}<h2>${h(title)}</h2>${body?`<p>${h(body)}</p>`:''}${actionHtml}</div>`;
export function showPage(html,title) { main.innerHTML=html; document.title=`${title} — Band Link`; }
// Search and filter submits go through the router too rather than reloading the shell.
export const navigate = url => { history.pushState(null, '', url); dispatchEvent(new PopStateEvent('popstate')); scrollTo(0, 0); };
export function toast(message) { const el=document.querySelector('#toast');el.textContent=message;el.hidden=false;clearTimeout(toast.timer);toast.timer=setTimeout(()=>el.hidden=true,4500); }
export async function api(path,options={}) {
  const method=options.method || 'GET';
  if (!['GET','HEAD'].includes(method) && !state.csrf) state.csrf=await api('/api/csrf');
  const headers={Accept:'application/json',...options.headers};
  if (state.csrf && !['GET','HEAD'].includes(method)) headers[state.csrf.headerName]=state.csrf.token;
  let body=options.body;
  if (body && !(body instanceof FormData)) {headers['Content-Type']='application/json';body=JSON.stringify(body);}
  let response;
  try {response=await fetch(path,{...options,method,headers,body,credentials:'same-origin'});} catch {throw new Error('接続できませんでした。通信環境を確認して、もう一度お試しください。');}
  const type=response.headers.get('content-type') || '';
  const data=response.status===204?null:type.includes('json')?await response.json():await response.text();
  if(!response.ok || response.redirected || (response.status!==204&&!type.includes('json')&&method==='GET')) {
    // The server already tells 401s apart (wrong login credentials vs. no session at all)
    // with its own message, so prefer that over a one-size-fits-all client string — otherwise
    // a failed login attempt shows "please log in", which is nonsense on the login form itself.
    const error=new Error(data?.message || (response.status===401?'ログインが必要です。ログインしてからもう一度お試しください。':response.status===404?'お探しの情報は見つかりませんでした。':response.status===403?'この操作は許可されていません。ページを再読み込みしてご確認ください。':'処理を完了できませんでした。時間をおいてもう一度お試しください。'));
    error.status=response.status;error.code=data?.code;throw error;
  }
  return data;
}
export function bindForm(form,handler) {
  form.addEventListener('submit',async event=>{event.preventDefault();if(form.dataset.busy)return;if(!form.reportValidity())return;let error=form.querySelector('.form-error');if(!error){error=document.createElement('div');error.className='form-error';form.prepend(error);}error.innerHTML='';const submit=form.querySelector('[type=submit]');form.dataset.busy='true';if(submit){submit.disabled=true;submit.setAttribute('aria-busy','true');}
    try {await handler(new FormData(form),form);} catch(e){error.innerHTML=notice(e.message,'error');error.setAttribute('tabindex','-1');error.focus();}finally{delete form.dataset.busy;if(submit){submit.disabled=false;submit.removeAttribute('aria-busy');}}
  });
}
export function confirmAction(title,description,handler) {
  const dialog=document.querySelector('#dialog');dialog.innerHTML=`<h2 id="dialog-title">${h(title)}</h2>${description?`<p>${h(description)}</p>`:''}<form id="confirm-form"><div class="dialog-actions"><button type="button" class="button secondary" data-cancel>キャンセル</button><button type="submit" class="button primary">実行する</button></div></form>`;
  dialog.querySelector('[data-cancel]').onclick=()=>dialog.close();bindForm(dialog.querySelector('form'),async()=>{await handler();dialog.close();});dialog.showModal();
}
/** Opens an image at full size in the shared modal dialog, keeping focus in-page instead of
    handing the person off to a new tab. Closing (Esc, backdrop click, or the close button, all
    native <dialog> behaviour) returns focus to whatever thumbnail opened it. */
export function openImageViewer(url,alt) {
  const dialog=document.querySelector('#dialog');dialog.innerHTML=`<form method="dialog" class="image-dialog"><button type="submit" class="button secondary small">閉じる</button><img src="${h(url)}" alt="${h(alt||'拡大表示')}"></form>`;
  dialog.showModal();
}
/** Drag to reposition, slider to zoom, inside a circular guide matching how the avatar is shown
    everywhere. Exports a square JPEG so the existing `.avatar { object-fit: cover }` display never
    has to crop it further. Resolves with the cropped File, or null if the person cancels. */
export function openImageCropper(file) {
  const VP=280, OUT=480, MAX_ZOOM=3;
  return new Promise(resolve=>{
    const url=URL.createObjectURL(file);
    const dialog=document.querySelector('#dialog');
    dialog.innerHTML=`<h2 id="dialog-title">画像の位置を調整</h2><p>ドラッグで位置を、スライダーで拡大を調整できます。</p>
      <div class="crop-viewport"><img class="crop-image" src="${h(url)}" alt="" draggable="false"></div>
      <div class="form-field"><label for="crop-zoom">拡大</label><input id="crop-zoom" type="range" min="1" max="${MAX_ZOOM}" step="0.01" value="1"></div>
      <div class="dialog-actions"><button type="button" class="button secondary" data-cancel>キャンセル</button><button type="button" class="button primary" data-crop-confirm disabled>この位置で保存</button></div>`;
    const img=dialog.querySelector('.crop-image'),viewport=dialog.querySelector('.crop-viewport'),zoomInput=dialog.querySelector('#crop-zoom'),confirmButton=dialog.querySelector('[data-crop-confirm]');
    let baseScale=1,zoom=1,offsetX=0,offsetY=0,naturalWidth=0,naturalHeight=0;
    const clamp=(value,min,max)=>Math.min(max,Math.max(min,value));
    const apply=()=>{
      const scale=baseScale*zoom;
      const displayedWidth=naturalWidth*scale,displayedHeight=naturalHeight*scale;
      const maxOffsetX=Math.max(0,(displayedWidth-VP)/2),maxOffsetY=Math.max(0,(displayedHeight-VP)/2);
      offsetX=clamp(offsetX,-maxOffsetX,maxOffsetX);offsetY=clamp(offsetY,-maxOffsetY,maxOffsetY);
      img.style.width=`${displayedWidth}px`;img.style.height=`${displayedHeight}px`;
      img.style.transform=`translate(${(VP-displayedWidth)/2+offsetX}px, ${(VP-displayedHeight)/2+offsetY}px)`;
    };
    const finish=result=>{URL.revokeObjectURL(url);dialog.close();resolve(result);};
    img.onload=()=>{
      naturalWidth=img.naturalWidth;naturalHeight=img.naturalHeight;
      baseScale=Math.max(VP/naturalWidth,VP/naturalHeight);
      apply();confirmButton.disabled=false;
    };
    img.onerror=()=>finish(null);
    zoomInput.addEventListener('input',()=>{zoom=Number(zoomInput.value);apply();});
    let dragging=false,startX=0,startY=0,startOffsetX=0,startOffsetY=0;
    viewport.addEventListener('pointerdown',event=>{dragging=true;startX=event.clientX;startY=event.clientY;startOffsetX=offsetX;startOffsetY=offsetY;viewport.setPointerCapture(event.pointerId);});
    viewport.addEventListener('pointermove',event=>{if(!dragging)return;offsetX=startOffsetX+(event.clientX-startX);offsetY=startOffsetY+(event.clientY-startY);apply();});
    viewport.addEventListener('pointerup',()=>{dragging=false;});
    viewport.addEventListener('pointercancel',()=>{dragging=false;});
    dialog.querySelector('[data-cancel]').onclick=()=>finish(null);
    dialog.querySelector('[data-crop-confirm]').onclick=()=>{
      const scale=baseScale*zoom;
      const displayedWidth=naturalWidth*scale,displayedHeight=naturalHeight*scale;
      const imgLeft=(VP-displayedWidth)/2+offsetX,imgTop=(VP-displayedHeight)/2+offsetY;
      const canvas=document.createElement('canvas');canvas.width=OUT;canvas.height=OUT;
      const ctx=canvas.getContext('2d');
      ctx.drawImage(img,-imgLeft/scale,-imgTop/scale,VP/scale,VP/scale,0,0,OUT,OUT);
      canvas.toBlob(blob=>finish(blob?new File([blob],'avatar.jpg',{type:'image/jpeg'}):null),'image/jpeg',0.92);
    };
    dialog.addEventListener('close',()=>finish(null),{once:true});
    dialog.showModal();
  });
}
export function report(type,id) {
  const dialog=document.querySelector('#dialog');dialog.innerHTML=`<h2 id="dialog-title">運営に通報する</h2><p>困ったことや問題のある内容をお知らせください。相手に通報者の名前は表示されません。</p><form class="stack" style="margin-top:24px"><div class="form-field"><label for="reason">通報理由</label><textarea id="reason" name="reason" required maxlength="1000" placeholder="どのような問題があったか、具体的にご記入ください。"></textarea></div><div class="dialog-actions"><button type="button" class="button secondary" data-cancel>キャンセル</button><button type="submit" class="button primary">通報を送信</button></div></form>`;
  dialog.querySelector('[data-cancel]').onclick=()=>dialog.close();bindForm(dialog.querySelector('form'),async data=>{await api('/api/reports',{method:'POST',body:{targetType:type,targetId:Number(id),reason:data.get('reason')}});dialog.close();toast('通報を受け付けました。');});dialog.showModal();
}
export function poll(callback) {let busy=false;const run=async()=>{if(document.hidden||busy)return;busy=true;try{await callback();}catch{/* The screen owns its polling error feedback. */}finally{busy=false;}};const timer=setInterval(run,15000);const stop=()=>{clearInterval(timer);document.removeEventListener('visibilitychange',run);};document.addEventListener('visibilitychange',run);window.addEventListener('pagehide',stop,{once:true});return stop;}
export const frequencies=[['WEEKLY_2PLUS','週2回以上'],['WEEKLY_1','週1回程度'],['MONTHLY_2_3','月2〜3回'],['MONTHLY_1','月1回程度'],['BIMONTHLY_1','2か月に1回']];
export const ages=[['ANY','年齢不問'],['S10','10代'],['S20','20代'],['S30','30代'],['S40','40代'],['S50_PLUS','50代以上']];
export const frequencyLabel = value => frequencies.find(x=>x[0]===value)?.[1] || '月1回程度';
export function chips(values) {return `<div class="chips">${(values||[]).map(x=>`<span class="chip">${h(x.name ?? x)}</span>`).join('')}</div>`;}
export function choices(name,values,selected=[],single=false) {return `<div class="chips">${values.map(item=>{const [id,label]=Array.isArray(item)?item:[item.id,item.name];return `<label class="chip-select"><input type="${single?'radio':'checkbox'}" name="${h(name)}" value="${h(id)}" ${selected.map(String).includes(String(id))?'checked':''}><span>${h(label)}</span></label>`;}).join('')}</div>`;}
export function counter(form) {form.querySelectorAll('[maxlength]').forEach(el=>{const target=form.querySelector(`[data-count="${el.id}"]`);if(target){const update=()=>target.textContent=`${el.value.length} / ${el.maxLength}`;el.addEventListener('input',update);update();}});}
export function requireUser() {if(state.user)return true;location.replace('/login?next='+encodeURIComponent(location.pathname+location.search));return false;}
export function verificationNotice(){return state.user&&!state.user.emailVerified?notice('投稿・メッセージの送信にはメールアドレスの確認が必要です。メール確認画面で手続きを完了してください。'):'';}
