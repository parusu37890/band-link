export const main = document.querySelector('#main');
export const state = { user: null, masters: null, csrf: null };
export const h = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const paths = {search:'<circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 4.5 4.5"/>',arrow:'<path d="M5 12h14m-6-6 6 6-6 6"/>','arrow-left':'<path d="M19 12H5m6-6-6 6 6 6"/>',back:'<path d="M19 12H5m6-6-6 6 6 6"/>',filter:'<path d="M4 7h16M4 17h16"/><circle cx="9" cy="7" r="2" fill="currentColor"/><circle cx="16" cy="17" r="2" fill="currentColor"/>',plus:'<path d="M12 5v14M5 12h14"/>',mail:'<rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 6 9 7 9-7"/>',message:'<path d="M20 11.5a7.5 7.5 0 0 1-8 7.5 8.5 8.5 0 0 1-4-.9L4 20l1.5-3.7A7.5 7.5 0 1 1 20 11.5Z"/>',send:'<path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/>',bell:'<path d="M6 8a6 6 0 0 1 12 0c0 8 3 8 3 10H3c0-2 3-2 3-10m4 13h4"/>',user:'<circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/>',pin:'<path d="M19 10c0 5-7 11-7 11S5 15 5 10a7 7 0 0 1 14 0Z"/><circle cx="12" cy="10" r="2"/>',music:'<path d="M9 18V5l11-2v13M9 9l11-2"/><ellipse cx="6" cy="18" rx="3" ry="2"/><ellipse cx="17" cy="16" rx="3" ry="2"/>',close:'<path d="m6 6 12 12M6 18 18 6"/>',check:'<path d="m5 12 4 4L19 6"/>',external:'<path d="M14 3h7v7m0-7L10 14M10 3H3v18h18v-7"/>',shield:'<path d="m12 2 9 4v6c0 5-9 10-9 10S3 17 3 12V6Z"/>'};
export const icon = name => `<svg class="icon" viewBox="0 0 24 24" aria-hidden="true">${paths[name] || paths.arrow}</svg>`;
export const button = (label, href, kind='primary') => `<a class="button ${h(kind)}" href="${h(href)}">${h(label)}</a>`;
export function safeUrl(value) { try { const u = new URL(value, location.origin); return ['http:','https:'].includes(u.protocol) ? u.href : ''; } catch { return ''; } }
export function avatar(user, large=false) { const url = user?.profileImageUrl; const local = url && /^\/(?:uploads|api\/images)\/[a-zA-Z0-9/_.-]+$/.test(url); return `<span class="avatar${large?' large':''}" aria-hidden="true">${local ? `<img src="${h(url)}" alt="" loading="lazy">` : h((user?.username || '♪').slice(0,1))}</span>`; }
export function time(value) { if (!value) return ''; const d=new Date(value); return Number.isNaN(d.valueOf())?'':new Intl.DateTimeFormat('ja-JP',{month:'short',day:'numeric',hour:'2-digit',minute:'2-digit'}).format(d); }
export const notice = (message,type='info') => `<div class="notice ${h(type)}"${type==='error'?' role="alert"':''}>${h(message)}</div>`;
export const empty = (title,body,actionHtml='') => `<div class="empty-state">${icon('music')}<h2>${h(title)}</h2><p>${h(body)}</p>${actionHtml}</div>`;
export function showPage(html,title) { main.innerHTML=html; document.title=`${title} — Band Link`; }
export const navigate = url => location.assign(url);
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
    const error=new Error(response.status===401?'ログインが必要です。ログインしてからもう一度お試しください。':data?.message || (response.status===404?'お探しの情報は見つかりませんでした。':response.status===403?'この操作は許可されていません。ページを再読み込みしてご確認ください。':'処理を完了できませんでした。時間をおいてもう一度お試しください。'));
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
  const dialog=document.querySelector('#dialog');dialog.innerHTML=`<h2 id="dialog-title">${h(title)}</h2><p>${h(description)}</p><form id="confirm-form"><div class="dialog-actions"><button type="button" class="button secondary" data-cancel>キャンセル</button><button type="submit" class="button primary">実行する</button></div></form>`;
  dialog.querySelector('[data-cancel]').onclick=()=>dialog.close();bindForm(dialog.querySelector('form'),async()=>{await handler();dialog.close();});dialog.showModal();
}
export function report(type,id) {
  const dialog=document.querySelector('#dialog');dialog.innerHTML=`<h2 id="dialog-title">運営に通報する</h2><p>困ったことや問題のある内容をお知らせください。相手に通報者の名前は表示されません。</p><form class="stack" style="margin-top:24px"><div class="form-field"><label for="reason">通報理由</label><textarea id="reason" name="reason" required maxlength="1000" placeholder="どのような問題があったか、具体的にご記入ください。"></textarea></div><div class="dialog-actions"><button type="button" class="button secondary" data-cancel>キャンセル</button><button type="submit" class="button primary">通報を送信</button></div></form>`;
  dialog.querySelector('[data-cancel]').onclick=()=>dialog.close();bindForm(dialog.querySelector('form'),async data=>{await api('/api/reports',{method:'POST',body:{targetType:type,targetId:Number(id),reason:data.get('reason')}});dialog.close();toast('通報を受け付けました。');});dialog.showModal();
}
export function poll(callback) {let busy=false;const run=async()=>{if(document.hidden||busy)return;busy=true;try{await callback();}catch{/* The screen owns its polling error feedback. */}finally{busy=false;}};const timer=setInterval(run,15000);const stop=()=>{clearInterval(timer);document.removeEventListener('visibilitychange',run);};document.addEventListener('visibilitychange',run);window.addEventListener('pagehide',stop,{once:true});return stop;}
export const frequencies=[['WEEKLY_2PLUS','週2回以上'],['WEEKLY_1','週1回程度'],['MONTHLY_2_3','月2〜3回'],['MONTHLY_1','月1回程度'],['IRREGULAR','不定期'],['NEGOTIABLE','相談して決める']];
export const ages=[['ANY','年齢不問'],['S10','10代'],['S20','20代'],['S30','30代'],['S40','40代'],['S50_PLUS','50代以上']];
export const frequencyLabel = value => frequencies.find(x=>x[0]===value)?.[1] || '相談して決める';
export function chips(values) {return `<div class="chips">${(values||[]).map(x=>`<span class="chip">${h(x.name ?? x)}</span>`).join('')}</div>`;}
export function choices(name,values,selected=[],single=false) {return `<div class="chips">${values.map(item=>{const [id,label]=Array.isArray(item)?item:[item.id,item.name];return `<label class="chip-select"><input type="${single?'radio':'checkbox'}" name="${h(name)}" value="${h(id)}" ${selected.map(String).includes(String(id))?'checked':''}><span>${h(label)}</span></label>`;}).join('')}</div>`;}
export function counter(form) {form.querySelectorAll('[maxlength]').forEach(el=>{const target=form.querySelector(`[data-count="${el.id}"]`);if(target){const update=()=>target.textContent=`${el.value.length} / ${el.maxLength}`;el.addEventListener('input',update);update();}});}
export function requireUser() {if(state.user)return true;location.replace('/login?next='+encodeURIComponent(location.pathname+location.search));return false;}
export function verificationNotice(){return state.user&&!state.user.emailVerified?notice('投稿・メッセージの送信にはメールアドレスの確認が必要です。メール確認画面で手続きを完了してください。'):'';}

