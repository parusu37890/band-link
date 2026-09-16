import {mediaHref,mediaEmbed,mediaProvider} from './media-embed.js?v=20260916-1';
import {api,h,icon,avatar,state,main,showPage,notice,empty,button,toast,bindForm,confirmAction,report,choices,counter,requireUser,verificationNotice,openImageCropper,loginRelativeTime} from './ui.js';

const fields=[['prefectureIds','活動エリア','prefectures'],['partIds','パート','parts'],['genreIds','ジャンル','genres'],['stanceIds','活動スタンス','stances']];

const names = items => (items||[]).map(x=>x.name).join('・');
// Showing every field with 未設定 filled the screen with absences instead of the person.
// Only filled rows render; an empty profile says so once.
const factRows = (p, own) => [
  ['担当パート', names(p.parts)],
  ['好きなジャンル', names(p.genres)],
  ['活動エリア', names(p.prefectures)],
  ['活動スタンス', names(p.stances)],
  ['経験年数', p.experienceYears == null ? '' : p.experienceYears + '年'],
  ['年齢', p.ageRange || ''],
  ['性別', p.gender || ''],
  ['最近の活動', p.online || p.lastLoginAt ? loginRelativeTime(p.lastLoginAt, p.online) : ''],
  // Only the account itself sees this - p (ProfileResponse) never carries email, so this reads
  // state.user, which is only this same person's data when own is true.
  ['メールアドレス', own ? (state.user.email || '') : '']
].filter(([, value]) => value);
const facts = (p, own) => {
  const rows = factRows(p, own);
  if (!rows.length) return `<section class="detail-section profile-facts"><h2>音楽と活動</h2><p class="muted">まだ登録されていません。</p></section>`;
  return `<section class="detail-section profile-facts"><h2>音楽と活動</h2><dl class="facts">${rows.map(([label, value]) => `<dt>${h(label)}</dt><dd>${h(value)}</dd>`).join('')}</dl></section>`;
};
export async function accountPage(path){
  if(path==='/login'||path==='/register'||path==='/verify-email'||path==='/password-reset'||path==='/password-reset/confirm'){await authPage(path);return true;}
  if(path==='/settings'||path==='/settings/profile'){if(requireUser()) await profileEdit();return true;}
  if(path==='/settings/blocks') return false;
  if(path==='/contact'||path==='/feature-request'){if(requireUser()) await feedbackPage(path==='/feature-request'?'FEATURE_REQUEST':'CONTACT');return true;}
  if(path==='/support'){await supportPage();return true;}
  if(path==='/privacy'){await privacyPage();return true;}
  if(/^\/users\/\d+$/.test(path)){await profilePage(path.split('/')[2]);return true;}
  return false;
}
async function authPage(path){
  const requestedNext=new URLSearchParams(location.search).get('next');
  let next='/posts';
  try { const destination=new URL(requestedNext||'/posts',location.origin);if(destination.origin===location.origin)next=destination.pathname+destination.search+destination.hash; } catch { /* Keep the local default. */ }
  const config={
    '/login':['ログイン',''],
    '/register':['アカウントを作成',''],
    '/verify-email':['メールアドレスを確認',''],
    '/password-reset':['パスワードを再設定',''],
    '/password-reset/confirm':['新しいパスワードを設定','']
  }[path];
  const register=path==='/register', verify=path==='/verify-email', reset=path==='/password-reset', confirm=path==='/password-reset/confirm';
  const verificationToken=verify?new URLSearchParams(location.search).get('token'):'';
  const lineError=new URLSearchParams(location.search).get('lineError');
  const lineEnabled=path==='/login'||register ? await api('/api/auth/line/enabled').then(value=>Boolean(value?.enabled)).catch(()=>false) : false;
  const registrationMasters=register ? await api('/api/masters').catch(()=>null) : null;
  const registrationField=(key,label,source)=>{
    if(!registrationMasters)return '';
    const body=source==='prefectures'
      ? `<details class="editor-area-options"><summary>都道府県を選ぶ</summary><div class="editor-choices">${registrationMasters[source].map(x=>`<label class="editor-choice"><input type="checkbox" name="${key}" value="${x.id}"><span>${h(x.name)}</span></label>`).join('')}</div></details>`
      : choices(key,registrationMasters[source]);
    return `<fieldset class="form-section registration-choice"><legend>${label} <span class="required">必須</span></legend>${body}</fieldset>`;
  };
  const registrationFields=registrationMasters?fields.map(([key,label,source])=>registrationField(key,label,source)).join(''):'';
  let form='';
  const passwordField=(id,autocomplete)=>`<div class="password-field"><input class="input" id="${id}" name="${id==='newPassword'?'newPassword':'password'}" type="password" autocomplete="${autocomplete}" minlength="8" maxlength="128" required><button type="button" class="password-toggle" data-password-toggle="${id}" aria-label="パスワードを表示">${icon('eye')}</button></div>`;
  if(path==='/login') form=`<form id="auth-form"><div class="form-field"><label for="email">メールアドレス</label><input class="input" id="email" name="email" type="email" autocomplete="email" required></div><div class="form-field"><label for="password">パスワード</label>${passwordField('password','current-password')}</div><button class="button primary full" type="submit">ログイン</button></form>${lineEnabled?`<div class="auth-divider"><span>または</span></div><a class="button line-login full" href="/api/auth/line/start">LINEでログイン</a>`:''}`;
  if(register) form=`<form id="auth-form"><div class="form-field"><label for="username">表示名</label><input class="input" id="username" name="username" maxlength="80" autocomplete="nickname" required placeholder="活動名やニックネーム"></div><div class="form-field"><label for="email">メールアドレス</label><input class="input" id="email" name="email" type="email" maxlength="320" autocomplete="email" required></div><div class="form-field"><label for="password">パスワード</label>${passwordField('password','new-password')}<span class="hint">8文字以上で設定してください。</span></div><fieldset class="form-section"><legend>基本情報</legend><div class="form-grid"><div class="form-field"><label for="age">年齢 <span class="required">必須</span></label><input class="input" id="age" name="age" type="number" min="0" max="120" required></div><div class="form-field"><label for="experienceYears">経験年数 <span class="required">必須</span></label><input class="input" id="experienceYears" name="experienceYears" type="number" min="0" max="100" required></div></div><div class="form-field"><span class="form-label">性別 <span class="required">必須</span></span><div class="chips gender-choices"><label class="chip-select"><input type="radio" name="gender" value="男性" required><span>男性</span></label><label class="chip-select"><input type="radio" name="gender" value="女性"><span>女性</span></label></div></div></fieldset>${registrationFields}<button class="button primary full" type="submit">アカウントを作成</button></form>${lineEnabled?`<div class="auth-divider"><span>または</span></div><a class="button line-login full" href="/api/auth/line/start">LINEでアカウントを作成</a>`:''}`;
  if(verify) form=verificationToken
    ? `<div class="verify-link-state"><p class="muted">メール内のリンクを確認しています…</p></div>`
    : `<div class="verify-waiting"><p>登録時に送信した確認メールを開き、本文のリンクをタップしてください。</p><p class="hint">メールが見つからない場合は、迷惑メールフォルダも確認してください。</p>${state.user&&!state.user.emailVerified?'<button type="button" class="button secondary full" id="resend-verification">確認メールを再送する</button>':''}</div>`;
  if(reset) form=`<form id="auth-form"><div class="form-field"><label for="email">メールアドレス</label><input class="input" id="email" name="email" type="email" autocomplete="email" required></div><button class="button primary full" type="submit">再設定メールを送る</button></form>`;
  if(confirm) form=`<form id="auth-form"><div class="form-field"><label for="token">再設定トークン</label><input class="input" id="token" name="token" maxlength="100" required></div><div class="form-field"><label for="newPassword">新しいパスワード</label>${passwordField('newPassword','new-password')}</div><button class="button primary full" type="submit">パスワードを更新</button></form>`;
  const footer=path==='/login'
    ? `アカウントをお持ちでない方は <a href="/register">新規登録</a><br><a href="/password-reset">パスワードを忘れた方</a>`
    : register ? `すでに登録済みの方は <a href="/login">ログイン</a>`
    : verify ? ''
    : `<a href="/login">ログインへ戻る</a>`;
  const initialMessage=lineError==='cancelled'?'LINEログインをキャンセルしました。':lineError==='failed'?'LINEログインに失敗しました。もう一度お試しください。':lineError==='unavailable'?'LINEログインは現在利用できません。':'';
  const login=path==='/login';
  const authMark=(login||register)?'':`<a class="auth-mark" href="/" aria-label="Band Link ホーム"><img src="/assets/mark.svg?v=20260913-1" alt=""> <span>Band Link</span></a>`;
  showPage(`<div class="page auth-page${register?' register-page':''}${login?' login-page':''}"><section class="auth-panel">${authMark}<h1>${h(config[0])}</h1><div id="auth-message" aria-live="polite">${initialMessage?notice(initialMessage,'error'):''}</div>${form}${footer?`<div class="auth-footer">${footer}</div>`:''}</section></div>`,config[0]);
  const authForm=main.querySelector('#auth-form');
  if(authForm) bindForm(authForm,async fd=>{
    let response;
    if(path==='/login'){
      const email=fd.get('email');
      try { response=await api('/api/auth/login',{method:'POST',body:{email,password:fd.get('password')}}); }
      catch(e){
        if(e.code!=='EMAIL_NOT_VERIFIED') throw e;
        main.querySelector('#auth-message').innerHTML=notice(e.message,'error')+`<button type="button" class="button secondary full" id="resend-verification-anon">確認メールを再送する</button>`;
        main.querySelector('#resend-verification-anon').addEventListener('click',async event=>{
          event.currentTarget.disabled=true;
          try { await api('/api/auth/verify-email/resend-request',{method:'POST',body:{email}}); toast('確認メールを再送しました。メールをご確認ください。'); }
          catch { toast('再送に失敗しました。時間をおいてもう一度お試しください。'); event.currentTarget.disabled=false; }
        });
        return;
      }
    }
    else if(register){
      const body={username:fd.get('username'),email:fd.get('email'),password:fd.get('password'),age:Number(fd.get('age')),experienceYears:Number(fd.get('experienceYears')),gender:fd.get('gender'),partIds:fd.getAll('partIds').map(Number),genreIds:fd.getAll('genreIds').map(Number),stanceIds:fd.getAll('stanceIds').map(Number),prefectureIds:fd.getAll('prefectureIds').map(Number)};
      const labels={partIds:'担当パート',genreIds:'好きなジャンル',stanceIds:'活動スタンス',prefectureIds:'活動エリア'};
      for(const [key,label] of Object.entries(labels))if(!body[key].length)throw new Error(`${label}を1つ以上選択してください。`);
      if(body.prefectureIds.length>3)throw new Error('活動エリアは3つまで選択できます。');
      response=await api('/api/auth/register',{method:'POST',body});
    }
    else if(reset){await api('/api/auth/password-reset/request',{method:'POST',body:{email:fd.get('email')}});main.querySelector('#auth-message').innerHTML=notice('再設定の案内を送信しました。メールをご確認ください。','success');return;}
    else {await api('/api/auth/password-reset/confirm',{method:'POST',body:{token:fd.get('token'),newPassword:fd.get('newPassword')}});main.querySelector('#auth-message').innerHTML=notice('パスワードを更新しました。ログインしてください。','success');return;}
    if(response) {state.user=response;toast(register?'アカウントを作成しました。':'ログインしました。');location.assign(register?'/verify-email':next);}
  });
  main.querySelectorAll('[data-password-toggle]').forEach(toggle=>toggle.addEventListener('click',()=>{
    const input=main.querySelector('#'+toggle.dataset.passwordToggle); if(!input)return;
    const visible=input.type==='text'; input.type=visible?'password':'text';
    toggle.setAttribute('aria-label',visible?'パスワードを表示':'パスワードを隠す');
    toggle.innerHTML=icon(visible?'eye':'eye-off');
  }));
  main.querySelector('#resend-verification')?.addEventListener('click',async event=>{
    event.currentTarget.disabled=true;
    try { await api('/api/auth/verify-email/resend',{method:'POST'}); main.querySelector('#auth-message').innerHTML=notice('確認メールを再送しました。メールをご確認ください。','success'); }
    catch(e){ main.querySelector('#auth-message').innerHTML=notice(e.message,'error'); }
    finally { event.currentTarget.disabled=false; }
  });
  if(verify&&verificationToken){
    try {
      await api('/api/auth/verify-email',{method:'POST',body:{token:verificationToken}});
      if(state.user) state.user.emailVerified=true;
      history.replaceState({},'', '/verify-email');
      const destination=state.user?'/posts/new':'/login';
      main.querySelector('#auth-message').innerHTML=notice(
        state.user?'本登録が完了しました。募集を投稿したり、メッセージを送ったりできます。':'本登録が完了しました。ログインすると募集を投稿できます。',
        'success'
      )+`<div class="auth-link-action">${button(state.user?'募集を作成する':'ログインする',destination,'primary')}</div>`;
      main.querySelector('.verify-link-state')?.remove();
    } catch(e) {
      main.querySelector('#auth-message').innerHTML=notice(e.message||'確認リンクを確認できませんでした。','error');
      main.querySelector('.verify-link-state')?.replaceWith(document.createRange().createContextualFragment(`<p class="hint">登録時の確認メールを開き直すか、リンクの有効期限を確認してください。</p>`));
    }
  }
}
async function profilePage(id){
  try {
    const p=await api('/api/users/'+Number(id));
    const own=state.user&&String(state.user.id)===String(p.id);
    const mediaItems=[p.youtubeUrl,p.tiktokUrl,p.soundcloudUrl,p.spotifyUrl,p.appleMusicUrl,p.videoUrl].filter(url=>mediaHref(url));
    // The label shown is always the service the URL's own host resolves to (never the name of the
    // field it happened to be typed into): pasting a non-Spotify link into "Spotify URL" must not
    // render as a trusted-looking "Spotifyで開く" link to somewhere else.
    const mediaMarkup=mediaItems.length?`<section class="detail-section profile-video"><h2>演奏動画・音源</h2><div class="profile-media-list">${mediaItems.map(url=>{const label=mediaProvider(url)||'リンク';const embed=mediaEmbed(url);return `<article class="profile-media-item"><h3>${h(label)}</h3>${embed?`<iframe class="media-frame" style="${embed.ratio?`aspect-ratio:${embed.ratio}`:`height:${Number(embed.height)}px`}${embed.width?`;max-width:${Number(embed.width)}px`:''}" src="${h(embed.src)}" title="${h(p.username)}の${h(label)}" loading="lazy" allow="encrypted-media; fullscreen; clipboard-write" allowfullscreen></iframe>`:''}<a class="row media-link" href="${h(mediaHref(url))}" target="_blank" rel="noopener noreferrer">${icon('external')}${h(label)}で開く</a></article>`}).join('')}</div></section>`:'';
    const contact=own?button('プロフィールを編集','/settings/profile','secondary'):button('メッセージを送る',state.user?'/messages?to='+p.id:'/login?next='+encodeURIComponent('/messages?to='+p.id));
    showPage(`<div class="page profile-page">
      <a class="back-link" href="/posts">${icon('back')}募集一覧へ</a>
      <div class="profile-layout">
        <aside class="profile-identity">
          ${avatar(p,true)}
          <h1>${h(p.username)}</h1>
          <p class="profile-part">${h(names(p.parts)||'パート未設定')}</p>
          <p class="muted">${h(names(p.prefectures)||'活動エリア未設定')}</p>
          <div class="profile-contact">${contact}</div>
          ${!own&&state.user?'<div class="profile-guard"><button class="button quiet small" id="block-user">この人をブロック</button><button class="button quiet small" id="report-user">プロフィールを通報</button></div>':''}
        </aside>
        <article class="profile-story">
          <section class="profile-intro"><h2>自己紹介</h2><p class="body-text">${h(p.bio||'自己紹介はまだ登録されていません。')}</p></section>
          ${facts(p,own)}
          ${mediaMarkup}
        </article>
      </div>
    </div>`,p.username);
    main.querySelector('#report-user')?.addEventListener('click',()=>report('USER',id));
    // Blocking had no entry point in the UI at all, so the feature was unreachable: the blocks page
    // could only list and undo blocks that never had a way to be created.
    main.querySelector('#block-user')?.addEventListener('click',()=>confirmAction(
      'この人をブロックしますか？',
      'ブロックすると、お互いの募集が一覧に表示されなくなり、メッセージも送れなくなります。過去の会話は残ります。設定のブロック一覧からいつでも解除できます。',
      async()=>{await api('/api/blocks?userId='+encodeURIComponent(id),{method:'POST'});toast('ブロックしました。');location.assign('/settings/blocks');}));
  } catch(e){showPage(`<div class="page">${empty('プロフィールを表示できません。',e.message,button('募集を探す','/posts','secondary'))}</div>`,'プロフィール');}
}
async function profileEdit(){
  let p,m;
  try { [p,m]=await Promise.all([api('/api/users/me'),api('/api/masters')]); }
  catch(e){showPage(`<div class="page">${empty('設定を読み込めませんでした',e.message,button('再読み込み','/settings/profile','secondary'))}</div>`,'プロフィール編集');return;}
  // 47 prefectures do not fit the chip row the other three fields use, and the multiple-select
  // they replaced hid the choice behind Ctrl-click — the control DESIGN.md lists for removal.
  // Same disclosure the post editor already uses for this field, so picking an area looks and
  // behaves the same in both places and a long form is not made longer by 47 open checkboxes.
  const section=(key,label,source)=>{
    const selected=(p[source]||[]).map(x=>x.id);
    const field=body=>`<div class="form-field"><span class="form-label">${label} <span class="required">必須</span></span>${body}</div>`;
    if(source!=='prefectures')return field(choices(key,m[source],selected));
    return field(`<p class="editor-selection-status" id="${key}-status" aria-live="polite"></p><details class="editor-area-options"><summary>都道府県を選ぶ・変更する</summary><div class="editor-choices">${m[source].map(x=>`<label class="editor-choice"><input type="checkbox" name="${key}" value="${x.id}" ${selected.includes(x.id)?'checked':''}><span>${h(x.name)}</span></label>`).join('')}</div></details>`);
  };
  showPage(`<div class="page settings-page">
    <a class="back-link" href="/users/${p.id}">${icon('back')}公開プロフィールへ</a>
    <div class="page-heading"><div><h1>プロフィール・設定</h1><p>一緒に演奏する相手へ、あなたの音楽や活動のことを伝えましょう。</p></div></div>
    <div class="settings-layout">
      <nav class="settings-nav" aria-label="設定メニュー"><a href="#profile-form" aria-current="page">プロフィール</a><a href="/my/posts">自分の募集</a><a href="/settings/blocks">ブロック管理</a></nav>
      <div class="settings-content">${verificationNotice()}
        <form id="profile-form">
          <fieldset class="form-section"><legend>プロフィール画像</legend><div class="profile-image-editor"><div id="profile-image-preview">${avatar(p,true)}</div><div class="stack"><input id="profileImage" name="profileImage" type="file" accept="image/jpeg,image/png,image/webp" hidden><div class="row"><button type="button" class="button secondary" id="choose-profile-image">画像を選ぶ</button><button type="button" class="button quiet small" id="clear-profile-selection" hidden>選択を取り消す</button></div><p class="hint" id="profile-image-name" aria-live="polite">JPEG・PNG・WebP / 5MBまで</p>${p.profileImageUrl?'<button type="button" class="button quiet small" id="remove-profile-image">現在の画像を削除</button>':''}</div></div></fieldset>
          <fieldset class="form-section"><legend>自己紹介</legend><div class="stack"><div class="form-field"><label for="username">表示名 <span class="required">必須</span></label><input class="input" id="username" name="username" maxlength="80" autocomplete="nickname" required value="${h(p.username)}"></div><div class="form-field"><label for="bio">自己紹介 <span class="optional">任意</span></label><textarea class="input" id="bio" name="bio" maxlength="500" rows="7" placeholder="好きなアーティスト、これまでの活動、これからやりたい音楽など。">${h(p.bio)}</textarea><span class="hint" data-count="bio"></span></div></div></fieldset>
          <fieldset class="form-section"><legend>音楽と活動エリア</legend><div class="stack">${fields.map(x=>section(...x)).join('')}</div></fieldset>
          <fieldset class="form-section"><legend>基本情報</legend><div class="stack"><div class="form-grid"><div class="form-field"><label for="age">年齢 <span class="required">必須</span></label><input class="input" id="age" name="age" type="number" min="0" max="120" value="${p.age??''}" required></div><div class="form-field"><label for="experienceYears">経験年数 <span class="required">必須</span></label><input class="input" id="experienceYears" name="experienceYears" type="number" min="0" max="100" value="${p.experienceYears??''}" required></div></div><div class="form-field"><span class="form-label">性別 <span class="required">必須</span></span><div class="chips gender-choices"><label class="chip-select"><input type="radio" name="gender" value="男性" ${p.gender==='男性'?'checked':''} required><span>男性</span></label><label class="chip-select"><input type="radio" name="gender" value="女性" ${p.gender==='女性'?'checked':''}><span>女性</span></label></div></div></div></fieldset>
          <fieldset class="form-section"><legend>演奏動画・音源</legend><div class="stack"><div class="form-field"><label for="youtubeUrl">YouTube URL <span class="optional">任意</span></label><input class="input" id="youtubeUrl" name="youtubeUrl" type="url" maxlength="1000" value="${h(p.youtubeUrl||'')}" placeholder="https://youtu.be/..."></div><div class="form-field"><label for="tiktokUrl">TikTok URL <span class="optional">任意</span></label><input class="input" id="tiktokUrl" name="tiktokUrl" type="url" maxlength="1000" value="${h(p.tiktokUrl||'')}" placeholder="https://www.tiktok.com/@..."></div><div class="form-field"><label for="soundcloudUrl">SoundCloud URL <span class="optional">任意</span></label><input class="input" id="soundcloudUrl" name="soundcloudUrl" type="url" maxlength="1000" value="${h(p.soundcloudUrl||'')}" placeholder="https://soundcloud.com/..."></div><div class="form-field"><label for="spotifyUrl">Spotify URL <span class="optional">任意</span></label><input class="input" id="spotifyUrl" name="spotifyUrl" type="url" maxlength="1000" value="${h(p.spotifyUrl||'')}" placeholder="https://open.spotify.com/track/..."></div><div class="form-field"><label for="appleMusicUrl">Apple Music URL <span class="optional">任意</span></label><input class="input" id="appleMusicUrl" name="appleMusicUrl" type="url" maxlength="1000" value="${h(p.appleMusicUrl||'')}" placeholder="https://music.apple.com/..."></div><div class="form-field"><label for="videoUrl">その他のURL <span class="optional">任意</span></label><input class="input" id="videoUrl" name="videoUrl" type="url" maxlength="1000" value="${h(p.videoUrl||'')}" placeholder="https://..."></div><span class="hint">各サービスのURLを個別に登録できます。</span></div></fieldset>
          <div class="sticky-actions">${button('キャンセル','/users/'+p.id,'secondary')}<button class="button primary" type="submit">変更を保存</button></div>
        </form>
        <section class="detail-section account-settings" id="account-settings"><h2>アカウント</h2><div class="settings-account-row"><div><h3>ログアウト</h3><p class="muted">この端末でのログインを終了します。</p></div><button class="button secondary" type="button" id="logout">ログアウト</button></div><div class="settings-account-row"><div><h3>Band Linkから退会</h3><p class="muted">プロフィール、募集、会話、メッセージを削除します。同じメールアドレスで再登録できます。</p></div><button class="button danger" type="button" id="withdraw">退会する</button></div></section>
      </div>
    </div>
  </div>`, 'プロフィール編集');
  const form=main.querySelector('#profile-form');
  counter(form);
  // Say how many areas are chosen while choosing. The submit handler still rejects a fourth,
  // but being told off after pressing 保存 is a poor way to learn a limit.
  const areaStatus=form.querySelector('#prefectureIds-status');
  const updateAreas=()=>{
    const chosen=[...form.querySelectorAll('[name="prefectureIds"]:checked')];
    const names=chosen.map(x=>x.nextElementSibling.textContent).join('・');
    areaStatus.textContent=`${chosen.length} / 3つまで${names?'　'+names:''}`;
    areaStatus.classList.toggle('error',chosen.length>3);
  };
  form.querySelectorAll('[name="prefectureIds"]').forEach(el=>el.addEventListener('change',updateAreas));
  updateAreas();
  const imageInput=form.querySelector('#profileImage');
  const preview=form.querySelector('#profile-image-preview');
  const fileName=form.querySelector('#profile-image-name');
  const clearSelection=form.querySelector('#clear-profile-selection');
  let previewUrl=null;
  const releasePreview=()=>{if(previewUrl){URL.revokeObjectURL(previewUrl);previewUrl=null;}};
  const resetSelection=()=>{releasePreview();imageInput.value='';preview.innerHTML=avatar(p,true);fileName.textContent='JPEG・PNG・WebP / 5MBまで';clearSelection.hidden=true;};
  window.addEventListener('pagehide',releasePreview,{once:true});
  form.querySelector('#choose-profile-image').onclick=()=>imageInput.click();
  clearSelection.onclick=resetSelection;
  imageInput.addEventListener('change',async()=>{
    const image=imageInput.files?.[0];
    if(!image){resetSelection();return;}
    if(!image.size){resetSelection();fileName.textContent='このファイルは空です。別の画像を選んでください。';return;}
    if(image.size>5*1024*1024||!['image/jpeg','image/png','image/webp'].includes(image.type)){resetSelection();fileName.textContent='5MB以下のJPEG・PNG・WebP画像を選んでください。';return;}
    const cropped=await openImageCropper(image);
    if(!cropped){resetSelection();return;}
    const transfer=new DataTransfer();transfer.items.add(cropped);imageInput.files=transfer.files;
    releasePreview();previewUrl=URL.createObjectURL(cropped);
    preview.innerHTML=`<span class="avatar large"><img src="${h(previewUrl)}" alt="保存するプロフィール画像のプレビュー"></span>`;
    fileName.textContent=`${image.name} — 保存すると公開されます`;clearSelection.hidden=false;
  });
  bindForm(form,async fd=>{
    const body={username:fd.get('username').trim(),bio:fd.get('bio').trim(),age:fd.get('age')?Number(fd.get('age')):null,experienceYears:fd.get('experienceYears')?Number(fd.get('experienceYears')):null,gender:fd.get('gender')||null,videoUrl:fd.get('videoUrl').trim()||null,youtubeUrl:fd.get('youtubeUrl').trim()||null,tiktokUrl:fd.get('tiktokUrl').trim()||null,soundcloudUrl:fd.get('soundcloudUrl').trim()||null,spotifyUrl:fd.get('spotifyUrl').trim()||null,appleMusicUrl:fd.get('appleMusicUrl').trim()||null};
    for(const [key] of fields)body[key]=fd.getAll(key).map(Number);
    if(!body.age && body.age!==0)throw new Error('年齢を入力してください。');
    if(!body.experienceYears && body.experienceYears!==0)throw new Error('経験年数を入力してください。');
    if(!body.gender)throw new Error('性別を選択してください。');
    const requiredLabels={partIds:'担当パート',genreIds:'好きなジャンル',stanceIds:'活動スタンス',prefectureIds:'活動エリア'};
    for(const [key,label] of Object.entries(requiredLabels))if(!body[key].length)throw new Error(`${label}を1つ以上選択してください。`);
    if(body.prefectureIds.length>3)throw new Error('活動エリアは3つまで選択できます。');
    await api('/api/users/me',{method:'PUT',body});
    const image=fd.get('profileImage');
    if(image instanceof File&&image.size){const upload=new FormData();upload.append('file',image);await api('/api/users/me/image',{method:'POST',body:upload});}
    toast('プロフィールを更新しました。');location.assign('/users/'+p.id);
  });
  main.querySelector('#remove-profile-image')?.addEventListener('click',()=>confirmAction('プロフィール画像を削除しますか？','公開プロフィールから現在の画像を削除します。',async()=>{await api('/api/users/me/image',{method:'DELETE'});p.profileImageUrl=null;if(!imageInput.files?.length)resetSelection();main.querySelector('#remove-profile-image')?.remove();toast('プロフィール画像を削除しました。');}));
  main.querySelector('#logout').onclick=()=>confirmAction('ログアウトしますか？','次回はメールアドレスとパスワードでログインできます。',async()=>{await api('/api/auth/logout',{method:'POST'});location.assign('/login');});
  main.querySelector('#withdraw').onclick=()=>confirmAction('退会しますか？','プロフィール、募集、会話、メッセージを削除します。削除後は元に戻せません。同じメールアドレスで再登録できます。',async()=>{await api('/api/auth/withdraw',{method:'POST'});state.user=null;location.assign('/');});
}
async function feedbackPage(type){
  const feature=type==='FEATURE_REQUEST';
  const title=feature?'機能要望':'お問い合わせ';
  const description=feature?'新機能のアイデアやこうなったら使いやすい等を教えてください。':'困っていることや確認したいことを運営に知らせてください。';
  showPage(`<div class="page feedback-page"><a class="back-link" href="/posts">${icon('back')}募集一覧へ</a><div class="page-heading"><div><h1>${title}</h1><p>${description}</p></div></div><form id="feedback-form" class="feedback-form"><div class="form-field"><label for="feedback-message">${feature?'要望の内容':'お問い合わせ内容'}</label><textarea class="input" id="feedback-message" name="message" rows="9" maxlength="1000" required placeholder="自由にご記入ください。"></textarea><span class="hint">1000文字まで</span></div><div class="form-field"><label for="feedback-image">画像（任意）</label><input id="feedback-image" name="image" type="file" accept="image/jpeg,image/png,image/webp"><div class="feedback-attachment" data-feedback-attachment hidden></div><span class="hint">画面の状態が分かる画像を1枚添付できます（5MBまで）。</span></div><div id="feedback-status" aria-live="polite"></div><button class="button primary" type="submit">送信</button></form></div>`,title);
  const form=main.querySelector('#feedback-form');
  const imageInput=form.elements.image;
  const attachment=form.querySelector('[data-feedback-attachment]');
  let image=null, previewUrl=null;
  const clearPreview=()=>{if(previewUrl){URL.revokeObjectURL(previewUrl);previewUrl=null;}};
  const renderAttachment=()=>{
    clearPreview();
    attachment.hidden=!image;
    if(!image){attachment.innerHTML='';return;}
    previewUrl=URL.createObjectURL(image);
    attachment.innerHTML=`<img src="${h(previewUrl)}" alt="添付する画像のプレビュー"><div><strong>${h(image.name)}</strong><span class="muted">${(image.size/1024/1024).toFixed(1)} MB</span><button type="button" class="button quiet small" data-remove-feedback-image>取り消す</button></div>`;
    attachment.querySelector('[data-remove-feedback-image]').onclick=()=>{image=null;imageInput.value='';renderAttachment();};
  };
  imageInput.addEventListener('change',()=>{
    const file=imageInput.files?.[0];
    if(!file){image=null;renderAttachment();return;}
    const name=(file.name||'').toLowerCase();
    const inferred=file.type==='image/jpg'?'image/jpeg':file.type||(name.endsWith('.jpg')||name.endsWith('.jpeg')?'image/jpeg':name.endsWith('.png')?'image/png':name.endsWith('.webp')?'image/webp':'');
    if(!['image/jpeg','image/png','image/webp'].includes(inferred)||file.size>5*1024*1024){image=null;imageInput.value='';attachment.hidden=false;attachment.innerHTML=notice('JPEG・PNG・WebPの画像を、1枚5MB以内で選択してください。','error');return;}
    image=file.type===inferred?file:new File([file],file.name||'feedback-image',{type:inferred});
    renderAttachment();
  });
  window.addEventListener('pagehide',clearPreview,{once:true});
  bindForm(form,async fd=>{
    let imageUrl=null;
    if(image){const upload=new FormData();upload.append('file',image);imageUrl=await api('/api/feedback/images',{method:'POST',headers:{Accept:'text/plain'},body:upload});}
    await api(feature?'/api/feedback/feature-request':'/api/feedback/contact',{method:'POST',body:{message:fd.get('message'),imageUrl}});
    form.reset();image=null;renderAttachment();main.querySelector('#feedback-status').innerHTML=notice(feature?'機能要望を受け付けました。':'お問い合わせを受け付けました。','success');
  });
}

// Help is organized around the situations a reader actually arrives with, each ending at the
// screen that resolves it. Service inquiries and feature requests have separate forms above.
const helpItem = ([question, answer, link]) =>
  `<article class="help-item"><h3>${h(question)}</h3><p>${h(answer)}</p>${link ? `<a class="help-link" href="${h(link[1])}">${h(link[0])}${icon('arrow')}</a>` : ''}</article>`;
const helpSection = (id, title, items) =>
  `<section class="help-section" aria-labelledby="${id}"><h2 id="${id}">${h(title)}</h2><div class="help-list">${items.map(helpItem).join('')}</div></section>`;

function supportPage(){
  showPage(`<div class="page help-page">
    <div class="page-heading"><div><h1>ヘルプ</h1><p>つまずきやすいところと、その場で解決できる画面をまとめています。</p></div></div>
    ${helpSection('help-start', '使いはじめる', [
      ['登録直後、確認画面から他の画面へ進めない', 'メールアドレスの確認が終わるまでは、募集の閲覧・検索を含め確認画面以外の操作はできません。登録時のメール内リンクを開くと、通常の利用に戻ります。', ['メールアドレスを確認する', '/verify-email']],
      ['パスワードを忘れた', '登録したメールアドレスへ再設定用のトークンを送ります。届いたトークンと新しいパスワードを入力してください。', ['パスワードを再設定する', '/password-reset']],
    ])}
    ${helpSection('help-posts', '募集のきまり', [
      ['募集と加入は1件ずつ公開できる', '募集と加入希望は、それぞれ同時に1件ずつ公開できます。終了した投稿は自分の投稿一覧に残り、あとから再公開できます。', ['自分の投稿を見る', '/my/posts']],
      ['投稿・編集した直後は変更できない', '新規投稿には時間制限はありません。投稿を編集してから12時間は、その投稿の本文・条件・画像を変更できません。', null],
      ['募集は30日で自動的に終了する', '公開から30日経つと掲載が終わります。再公開すると、その時点から30日に更新されます。募集の終了と再公開はいつでも操作できます。', null],
    ])}
    ${helpSection('help-people', '相手とのやりとり', [
      ['やりとりしたくない相手がいる', 'ブロックすると、お互いの募集が相手の一覧に出なくなり、メッセージの送受信も双方できなくなります。過去の会話は残ります。ただし公開情報はログアウトすれば誰でも見られるため、相手に見られなくする機能ではありません。', ['ブロックを管理する', '/settings/blocks']],
      ['不適切な募集やメッセージを見つけた', '募集・メッセージ・プロフィールの各画面にある「通報」から運営へ知らせてください。通報された本文と理由だけが運営に渡り、前後のやりとりは渡りません。', null],
      ['相手が「利用停止中ユーザー」と表示される', 'その相手は運営によって一時的に利用停止になっています。これまでのやりとりは残りますが、新しくメッセージを送ることはできません。', null],
    ])}
    ${helpSection('help-account', 'アカウント', [
      ['退会すると何が残るか', 'プロフィール、募集、会話、メッセージは削除されます。退会後は同じメールアドレスで再登録できます。退会は取り消せません。', ['アカウントの設定を見る', '/settings']],
      ['アカウントが利用停止になった', '募集の掲載とメッセージの送信ができなくなり、公開していた募集とプロフィールは非公開になります。解除の手続きは運営が行います。', null],
    ])}
    <section class="help-contact" aria-labelledby="help-contact-title">
      <h2 id="help-contact-title">運営への連絡</h2>
      <p>募集・メッセージ・プロフィールの内容についての連絡は、各画面の「通報」から運営に届きます。</p>
      <p class="hint">サービスについての問い合わせや、改善の提案は専用フォームから送れます。画像も1枚添付できます。</p>
      <div class="row help-contact-actions"><a class="button secondary" href="/contact">お問い合わせ</a><a class="button secondary" href="/feature-request">機能要望</a></div>
    </section>
  </div>`, 'ヘルプ');
}

// Written from what the code actually stores and does, not a generic template - see each
// item's counterpart in RegisterRequest/ProfileUpdateRequest, PostService/MessageService,
// docs/logging.md, application.yaml (auth-rate-limit / google-analytics-id / line.*), and
// docs/decisions/0006・0007. Update this alongside any change to what those collect or keep.
function privacyPage(){
  showPage(`<div class="page help-page">
    <div class="page-heading"><div><h1>プライバシーポリシー</h1><p>Band Link（バンドリンク）が登録・利用にともなってお預かりする情報と、その取り扱いについて説明します。</p></div></div>
    ${helpSection('privacy-collect', '登録・プロフィールで入力いただく情報', [
      ['アカウント情報', 'メールアドレスとパスワードを登録時にお預かりします。パスワードはハッシュ化して保存しており、運営を含め誰も元のパスワードを読み取ることはできません。', null],
      ['プロフィール情報', '表示名、年齢、性別、活動エリア（都道府県、最大3つ）、担当パート、好きなジャンル、経験年数、活動スタンスを必須項目としてお預かりします。自己紹介（任意・500文字まで）、プロフィール画像（任意・1枚5MBまで）、YouTube・TikTok・SoundCloud・Spotify・Apple Musicの演奏動画・音源URL（いずれも任意）もあわせて登録できます。', null],
    ])}
    ${helpSection('privacy-use', '利用にともなって生じる情報', [
      ['募集投稿', '「募集」「加入希望」の投稿内容と、投稿にあわせて登録する画像（最大4枚・1枚5MBまで）をお預かりします。募集は一覧・詳細画面で公開されます。', null],
      ['メッセージ', '会話相手とのメッセージ本文と画像（送信1回につき1枚・5MBまで）をお預かりします。メッセージの画像は、その会話の参加者だけが閲覧できるようアクセスを制限しています。', null],
      ['通知・ブロック・通報', '新着メッセージなどのアプリ内通知、ブロックした相手の記録、通報の内容をお預かりします。メッセージを通報された場合に運営が確認できるのは、通報された本文・画像と通報理由だけで、前後のやりとりは含みません。', null],
    ])}
    ${helpSection('privacy-security', '安全のための情報処理', [
      ['ログインの試行回数', '不正なログインを防ぐため、同一IPアドレスからのログイン・確認・再設定の試行回数を一定時間ごとに制限しています。', null],
      ['アクセスログ', '障害調査のため、日時・処理内容・リクエストID・応答結果・所要時間を記録したアクセスログを保存しています。パスワード、認証トークン、Cookie、メッセージ本文、メールアドレス、画像の中身はログに記録しません。利用者を特定する記録が必要な場合も、元の値に戻せない形に変換したIDのみを記録します。', null],
    ])}
    ${helpSection('privacy-thirdparty', '外部サービスの利用', [
      ['Googleアナリティクス', '設定により、アクセス状況の把握を目的としたGoogleアナリティクス（GA4）を利用する場合があります。有効な場合、Googleがcookieを設置し、訪問状況を収集することがあります。', ['Googleのプライバシーポリシー', 'https://policies.google.com/privacy']],
      ['LINEログイン', 'LINEアカウントでログインした場合、LINEのユーザーIDと表示名を保存し、以後のログインに利用します。LINEのアクセストークンは保存しません。', null],
    ])}
    ${helpSection('privacy-retention', 'データの保存期間と削除', [
      ['退会したとき', '退会すると、プロフィール、募集とその画像、会話とメッセージ、通知、ブロックの記録、検索履歴、ログイン情報、LINEアカウントとの連携を削除します。削除後は、同じメールアドレスやLINEアカウントで新しく登録し直せます。この操作は取り消せません。', ['アカウントの設定を見る', '/settings']],
      ['利用停止になったとき', '運営による利用停止は退会とは異なり、アカウントと既存の会話はそのまま残ります。停止中は募集の掲載とメッセージの送信ができず、プロフィールと募集は非公開になります。解除は運営が行います。', null],
    ])}
    <section class="help-contact" aria-labelledby="privacy-contact-title">
      <h2 id="privacy-contact-title">お問い合わせ</h2>
      <p>このポリシーについてのご質問・ご意見は、お問い合わせフォームからご連絡ください。</p>
      <div class="row help-contact-actions"><a class="button secondary" href="/contact">お問い合わせ</a></div>
    </section>
    <p class="muted" style="margin-top:32px">最終更新日: 2026年9月15日</p>
  </div>`, 'プライバシーポリシー');
}
