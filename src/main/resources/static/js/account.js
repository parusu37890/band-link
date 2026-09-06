import {api,h,icon,avatar,state,main,showPage,notice,empty,button,toast,bindForm,confirmAction,report,choices,counter,requireUser,verificationNotice} from './ui.js';

const fields=[['prefectureIds','活動エリア','prefectures'],['partIds','パート','parts'],['genreIds','ジャンル','genres'],['stanceIds','活動スタンス','stances']];
const videoHref = value => { try { const u=new URL(value); return ['http:','https:'].includes(u.protocol)?u.href:''; } catch { return ''; } };
const youtubeId = value => { try {const u=new URL(value); if(!['youtube.com','www.youtube.com','m.youtube.com','youtu.be'].includes(u.hostname))return ''; const id=u.hostname==='youtu.be'?u.pathname.slice(1):u.searchParams.get('v')||u.pathname.split('/').pop();return /^[a-zA-Z0-9_-]{11}$/.test(id||'')?id:'';}catch{return '';} };
const names = items => (items||[]).map(x=>x.name).join('・');
// Showing every field with 未設定 filled the screen with absences instead of the person.
// Only filled rows render; an empty profile says so once.
const factRows = p => [
  ['担当パート', names(p.parts)],
  ['好きなジャンル', names(p.genres)],
  ['活動エリア', names(p.prefectures)],
  ['活動スタンス', names(p.stances)],
  ['経験年数', p.experienceYears == null ? '' : p.experienceYears + '年'],
  ['年代', p.ageRange || ''],
  ['最近の活動', p.activity || '']
].filter(([, value]) => value);
const facts = p => {
  const rows = factRows(p);
  if (!rows.length) return `<section class="detail-section profile-facts"><h2>音楽と活動</h2><p class="muted">まだ登録されていません。</p></section>`;
  return `<section class="detail-section profile-facts"><h2>音楽と活動</h2><dl class="facts">${rows.map(([label, value]) => `<dt>${h(label)}</dt><dd>${h(value)}</dd>`).join('')}</dl></section>`;
};
export async function accountPage(path){
  if(path==='/login'||path==='/register'||path==='/verify-email'||path==='/password-reset'||path==='/password-reset/confirm'){await authPage(path);return true;}
  if(path==='/settings'||path==='/settings/profile'){if(requireUser()) await profileEdit();return true;}
  if(path==='/settings/blocks') return false;
  if(path==='/support'){await supportPage();return true;}
  if(/^\/users\/\d+$/.test(path)){await profilePage(path.split('/')[2]);return true;}
  return false;
}
async function authPage(path){
  const requestedNext=new URLSearchParams(location.search).get('next');
  let next='/posts';
  try { const destination=new URL(requestedNext||'/posts',location.origin);if(destination.origin===location.origin)next=destination.pathname+destination.search+destination.hash; } catch { /* Keep the local default. */ }
  const config={
    '/login':['ログイン','登録したメールアドレスでログインしてください。','気になる募集が見つかったら、プロフィールからメッセージを。'],
    '/register':['アカウントを作成','表示名とメールアドレスを登録してください。','担当パートも、好きな音楽も。プロフィールが、最初の自己紹介になります。'],
    '/verify-email':['メールアドレスを確認','登録時に届いた確認トークンを入力してください。','メールアドレスの確認後、募集の投稿とメッセージの送信ができます。'],
    '/password-reset':['パスワードを再設定','登録メールアドレスに再設定用の案内を送ります。','パスワードを忘れた場合は、こちらから再設定できます。'],
    '/password-reset/confirm':['新しいパスワードを設定','届いたトークンと新しいパスワードを入力してください。','再設定後は、新しいパスワードでログインしてください。']
  }[path];
  const register=path==='/register', verify=path==='/verify-email', reset=path==='/password-reset', confirm=path==='/password-reset/confirm';
  let form='';
  if(path==='/login') form=`<form id="auth-form"><div class="form-field"><label for="email">メールアドレス</label><input class="input" id="email" name="email" type="email" autocomplete="email" required></div><div class="form-field"><label for="password">パスワード</label><input class="input" id="password" name="password" type="password" autocomplete="current-password" minlength="8" required></div><button class="button primary full" type="submit">ログイン</button></form>`;
  if(register) form=`<form id="auth-form"><div class="form-field"><label for="username">表示名</label><input class="input" id="username" name="username" maxlength="80" autocomplete="nickname" required placeholder="活動名やニックネーム"></div><div class="form-field"><label for="email">メールアドレス</label><input class="input" id="email" name="email" type="email" maxlength="320" autocomplete="email" required></div><div class="form-field"><label for="password">パスワード</label><input class="input" id="password" name="password" type="password" minlength="8" maxlength="128" autocomplete="new-password" required><span class="hint">8文字以上で設定してください。</span></div><button class="button primary full" type="submit">アカウントを作成</button></form>`;
  if(verify) form=`<form id="auth-form"><div class="form-field"><label for="token">確認トークン</label><input class="input" id="token" name="token" maxlength="100" required autocomplete="one-time-code" placeholder="メールに記載されたトークン"></div><button class="button primary full" type="submit">メールアドレスを確認</button></form>`;
  if(reset) form=`<form id="auth-form"><div class="form-field"><label for="email">メールアドレス</label><input class="input" id="email" name="email" type="email" autocomplete="email" required></div><button class="button primary full" type="submit">再設定メールを送る</button></form>`;
  if(confirm) form=`<form id="auth-form"><div class="form-field"><label for="token">再設定トークン</label><input class="input" id="token" name="token" maxlength="100" required></div><div class="form-field"><label for="newPassword">新しいパスワード</label><input class="input" id="newPassword" name="newPassword" type="password" minlength="8" maxlength="128" autocomplete="new-password" required></div><button class="button primary full" type="submit">パスワードを更新</button></form>`;
  showPage(`<div class="page auth-page auth-layout"><aside class="auth-aside"><a class="back-link" href="/posts">${icon('back')}募集を探す</a><p class="eyebrow">バンドメンバー募集・参加希望</p><h2>一緒に演奏する<br>相手を見つける。</h2><p>${h(config[2])}</p><div class="auth-aside-note"><span>Band Link</span><p>活動エリア、パート、好きな音楽。<br>自分に合う条件で、仲間を探せます。</p></div></aside><section class="auth-panel"><h1>${h(config[0])}</h1><p class="muted">${h(config[1])}</p><div id="auth-message" aria-live="polite"></div>${form}<div class="auth-footer">${path==='/login'?`アカウントをお持ちでない方は <a href="/register">新規登録</a><br><a href="/password-reset">パスワードを忘れた方</a>`:register?`すでに登録済みの方は <a href="/login">ログイン</a>`:`<a href="/login">ログインへ戻る</a>`}</div></section></div>`,config[0]);
  bindForm(main.querySelector('#auth-form'),async fd=>{
    let response;
    if(path==='/login') response=await api('/api/auth/login',{method:'POST',body:{email:fd.get('email'),password:fd.get('password')}});
    else if(register) response=await api('/api/auth/register',{method:'POST',body:{username:fd.get('username'),email:fd.get('email'),password:fd.get('password')}});
    else if(verify){await api('/api/auth/verify-email',{method:'POST',body:{token:fd.get('token')}});main.querySelector('#auth-message').innerHTML=notice('メールアドレスを確認しました。ログインすると募集を投稿できます。','success');return;}
    else if(reset){await api('/api/auth/password-reset/request',{method:'POST',body:{email:fd.get('email')}});main.querySelector('#auth-message').innerHTML=notice('再設定の案内を送信しました。メールをご確認ください。','success');return;}
    else {await api('/api/auth/password-reset/confirm',{method:'POST',body:{token:fd.get('token'),newPassword:fd.get('newPassword')}});main.querySelector('#auth-message').innerHTML=notice('パスワードを更新しました。ログインしてください。','success');return;}
    if(response) {state.user=response;toast(register?'アカウントを作成しました。':'ログインしました。');location.assign(next);}
  });
}
async function profilePage(id){
  try {
    const p=await api('/api/users/'+Number(id));
    const own=state.user&&String(state.user.id)===String(p.id);
    const video=videoHref(p.videoUrl), yt=youtubeId(p.videoUrl);
    const contact=own?button('プロフィールを編集','/settings/profile','secondary'):button('メッセージを送る',state.user?'/messages?to='+p.id:'/login?next='+encodeURIComponent('/messages?to='+p.id));
    showPage(`<div class="page profile-page">
      <a class="back-link" href="/posts">${icon('back')}募集一覧へ</a>
      <div class="profile-layout">
        <aside class="profile-identity">
          ${avatar(p,true)}
          <p class="eyebrow">公開プロフィール</p>
          <h1>${h(p.username)}</h1>
          <p class="profile-part">${h(names(p.parts)||'パート未設定')}</p>
          <p class="muted">${h(names(p.prefectures)||'活動エリア未設定')}</p>
          <div class="profile-contact">${contact}${!own?'<p class="hint">募集が出ていなくても、連絡できます。</p>':''}</div>
          ${!own&&state.user?'<div class="profile-guard"><button class="button quiet small" id="block-user">この人をブロック</button><button class="button quiet small" id="report-user">プロフィールを通報</button></div>':''}
        </aside>
        <article class="profile-story">
          <section class="profile-intro"><h2>自己紹介</h2><p class="body-text">${h(p.bio||'自己紹介はまだ登録されていません。')}</p></section>
          ${facts(p)}
          ${video?`<section class="detail-section profile-video"><h2>演奏動画</h2>${yt?`<iframe class="video" src="https://www.youtube-nocookie.com/embed/${h(yt)}" title="${h(p.username)}の演奏動画" loading="lazy" allowfullscreen></iframe>`:`<a class="row" href="${h(video)}" target="_blank" rel="noopener noreferrer">${icon('external')}演奏動画を開く</a>`}</section>`:''}
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
  // they replaced hid the choice behind Ctrl-click — the same control the search rail dropped.
  // Reuses the rail's checkbox list so a selection is visible in both places.
  const section=(key,label,source)=>{
    const selected=(p[source]||[]).map(x=>x.id);
    const field=body=>`<div class="form-field"><span class="form-label">${label} <span class="optional">任意</span></span>${body}</div>`;
    if(source!=='prefectures')return field(choices(key,m[source],selected));
    return field(`<div class="filter-options prefecture-options area-options" role="group" aria-label="${label}" aria-describedby="${key}-status">${m[source].map(x=>`<label class="filter-option"><input type="checkbox" name="${key}" value="${x.id}" ${selected.includes(x.id)?'checked':''}><span>${h(x.name)}</span></label>`).join('')}</div><p class="hint" id="${key}-status" role="status"></p>`);
  };
  showPage(`<div class="page settings-page">
    <a class="back-link" href="/users/${p.id}">${icon('back')}公開プロフィールへ</a>
    <div class="page-heading"><div><h1>プロフィール・設定</h1><p>一緒に演奏する相手へ、あなたの音楽や活動のことを伝えましょう。</p></div></div>
    <div class="settings-layout">
      <nav class="settings-nav" aria-label="設定メニュー"><a href="#profile-form" aria-current="page">プロフィール</a><a href="/my/posts">自分の募集</a><a href="/settings/blocks">ブロック管理</a><a href="#account-settings">アカウント</a><a href="/support">ヘルプ・お問い合わせ</a></nav>
      <div class="settings-content">${verificationNotice()}
        <form id="profile-form">
          <fieldset class="form-section"><legend>プロフィール画像</legend><div class="profile-image-editor"><div id="profile-image-preview">${avatar(p,true)}</div><div class="stack"><input id="profileImage" name="profileImage" type="file" accept="image/jpeg,image/png,image/webp" hidden><div class="row"><button type="button" class="button secondary" id="choose-profile-image">画像を選ぶ</button><button type="button" class="button quiet small" id="clear-profile-selection" hidden>選択を取り消す</button></div><p class="hint" id="profile-image-name" aria-live="polite">JPEG・PNG・WebP / 5MBまで</p>${p.profileImageUrl?'<button type="button" class="button quiet small" id="remove-profile-image">現在の画像を削除</button>':''}</div></div></fieldset>
          <fieldset class="form-section"><legend>自己紹介</legend><div class="stack"><div class="form-field"><label for="username">表示名 <span class="required">必須</span></label><input class="input" id="username" name="username" maxlength="80" autocomplete="nickname" required value="${h(p.username)}"></div><div class="form-field"><label for="bio">自己紹介 <span class="optional">任意</span></label><textarea class="input" id="bio" name="bio" maxlength="1000" rows="7" placeholder="好きなアーティスト、これまでの活動、これからやりたい音楽など。">${h(p.bio)}</textarea><span class="hint" data-count="bio"></span></div></div></fieldset>
          <fieldset class="form-section"><legend>音楽と活動エリア</legend><div class="stack">${fields.map(x=>section(...x)).join('')}</div></fieldset>
          <fieldset class="form-section"><legend>基本情報</legend><div class="stack"><div class="form-grid"><div class="form-field"><label for="age">年齢 <span class="optional">任意</span></label><input class="input" id="age" name="age" type="number" min="0" max="120" value="${p.age??''}"><span class="hint">公開されるのは「20代」などの年代だけです。</span></div><div class="form-field"><label for="experienceYears">経験年数 <span class="optional">任意</span></label><input class="input" id="experienceYears" name="experienceYears" type="number" min="0" max="100" value="${p.experienceYears??''}"></div></div><div class="form-field"><label for="gender">性別 <span class="optional">任意</span></label><input class="input" id="gender" name="gender" maxlength="40" value="${h(p.gender)}"></div></div></fieldset>
          <fieldset class="form-section"><legend>演奏動画</legend><div class="form-field"><label for="videoUrl">動画のURL <span class="optional">任意</span></label><input class="input" id="videoUrl" name="videoUrl" type="url" maxlength="1000" value="${h(p.videoUrl)}" placeholder="https://youtu.be/..."><span class="hint">YouTubeはプロフィール内で再生できます。その他の動画はリンクで表示します。</span></div></fieldset>
          <div class="sticky-actions">${button('キャンセル','/users/'+p.id,'secondary')}<button class="button primary" type="submit">変更を保存</button></div>
        </form>
        <section class="detail-section account-settings" id="account-settings"><h2>アカウント</h2><div class="settings-account-row"><div><h3>ログアウト</h3><p class="muted">この端末でのログインを終了します。</p></div><button class="button secondary" type="button" id="logout">ログアウト</button></div><div class="settings-account-row"><div><h3>Band Linkから退会</h3><p class="muted">プロフィールと募集は公開を終了します。送信済みメッセージは相手側に残ります。</p></div><button class="button danger" type="button" id="withdraw">退会する</button></div></section>
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
  imageInput.addEventListener('change',()=>{
    const image=imageInput.files?.[0];
    if(!image){resetSelection();return;}
    if(image.size>5*1024*1024||!['image/jpeg','image/png','image/webp'].includes(image.type)){resetSelection();fileName.textContent='5MB以下のJPEG・PNG・WebP画像を選んでください。';return;}
    releasePreview();previewUrl=URL.createObjectURL(image);
    preview.innerHTML=`<span class="avatar large"><img src="${h(previewUrl)}" alt="保存するプロフィール画像のプレビュー"></span>`;
    fileName.textContent=`${image.name} — 保存すると公開されます`;clearSelection.hidden=false;
  });
  bindForm(form,async fd=>{
    const body={username:fd.get('username').trim(),bio:fd.get('bio').trim(),age:fd.get('age')?Number(fd.get('age')):null,experienceYears:fd.get('experienceYears')?Number(fd.get('experienceYears')):null,gender:fd.get('gender').trim(),videoUrl:fd.get('videoUrl').trim()||null};
    for(const [key] of fields)body[key]=fd.getAll(key).map(Number);
    if(body.prefectureIds.length>3)throw new Error('活動エリアは3つまで選択できます。');
    await api('/api/users/me',{method:'PUT',body});
    const image=fd.get('profileImage');
    if(image instanceof File&&image.size){const upload=new FormData();upload.append('file',image);await api('/api/users/me/image',{method:'POST',body:upload});}
    toast('プロフィールを更新しました。');location.assign('/users/'+p.id);
  });
  main.querySelector('#remove-profile-image')?.addEventListener('click',()=>confirmAction('プロフィール画像を削除しますか？','公開プロフィールから現在の画像を削除します。',async()=>{await api('/api/users/me/image',{method:'DELETE'});p.profileImageUrl=null;if(!imageInput.files?.length)resetSelection();main.querySelector('#remove-profile-image')?.remove();toast('プロフィール画像を削除しました。');}));
  main.querySelector('#logout').onclick=()=>confirmAction('ログアウトしますか？','次回はメールアドレスとパスワードでログインできます。',async()=>{await api('/api/auth/logout',{method:'POST'});location.assign('/login');});
  main.querySelector('#withdraw').onclick=()=>confirmAction('退会しますか？','プロフィールと募集は公開を終了し、アカウントを復元できません。送信済みメッセージは相手側に残ります。',async()=>{await api('/api/auth/withdraw',{method:'POST'});state.user=null;location.assign('/');});
}
// Two symmetric cards said little and promised a contact address the page did not have —
// the suspension screen sends people here for exactly that (requirements 5章・53行). Rewritten
// as the situations a reader actually arrives with, each ending at the screen that resolves it.
const helpItem = ([question, answer, link]) =>
  `<article class="help-item"><h3>${h(question)}</h3><p>${h(answer)}</p>${link ? `<a class="help-link" href="${h(link[1])}">${h(link[0])}${icon('arrow')}</a>` : ''}</article>`;
const helpSection = (id, title, items) =>
  `<section class="help-section" aria-labelledby="${id}"><h2 id="${id}">${h(title)}</h2><div class="help-list">${items.map(helpItem).join('')}</div></section>`;

function supportPage(){
  showPage(`<div class="page help-page">
    <div class="page-heading"><div><h1>ヘルプ</h1><p>つまずきやすいところと、その場で解決できる画面をまとめています。</p></div></div>
    ${helpSection('help-start', '使いはじめる', [
      ['募集を投稿できない、メッセージを送れない', 'メールアドレスの確認が終わっていない可能性があります。登録時に届いたトークンを確認画面へ入力すると、投稿と送信ができるようになります。閲覧と検索は確認前でもできます。', ['メールアドレスを確認する', '/verify-email']],
      ['パスワードを忘れた', '登録したメールアドレスへ再設定用のトークンを送ります。届いたトークンと新しいパスワードを入力してください。', ['パスワードを再設定する', '/password-reset']],
      ['プロフィールに何を書けばよいか', '担当パート・活動エリア・好きなジャンルが埋まっていると、相手が連絡するか判断できます。空の項目は相手の画面に表示されないので、書ける範囲で構いません。', ['プロフィールを編集する', '/settings/profile']],
    ])}
    ${helpSection('help-posts', '募集のきまり', [
      ['公開できる募集は同時に1件', '2件目を出すには、いま公開中の募集を終了してください。終了した募集は自分の募集一覧に残り、あとから再公開できます。', ['自分の募集を見る', '/my/posts']],
      ['投稿・編集した直後は変更できない', '投稿または編集してから12時間は、新しい募集の作成も編集もできません。画像の追加・削除・並べ替えも編集に含まれます。本文・条件・画像はまとめて保存してください。', null],
      ['募集は30日で自動的に終了する', '公開から30日経つと掲載が終わります。再公開すると、その時点から30日に更新されます。募集の終了と再公開はいつでも操作できます。', null],
    ])}
    ${helpSection('help-people', '相手とのやりとり', [
      ['やりとりしたくない相手がいる', 'ブロックすると、お互いの募集が相手の一覧に出なくなり、メッセージの送受信も双方できなくなります。過去の会話は残ります。ただし公開情報はログアウトすれば誰でも見られるため、相手に見られなくする機能ではありません。', ['ブロックを管理する', '/settings/blocks']],
      ['不適切な募集やメッセージを見つけた', '募集・メッセージ・プロフィールの各画面にある「通報」から運営へ知らせてください。通報された本文と理由だけが運営に渡り、前後のやりとりは渡りません。', null],
      ['相手が「退会済みユーザー」と表示される', 'その相手は退会しています。これまでのやりとりは残りますが、新しくメッセージを送ることはできません。', null],
    ])}
    ${helpSection('help-account', 'アカウント', [
      ['退会すると何が残るか', 'プロフィールと募集の公開が終わります。相手の画面に残っている送信済みのメッセージはそのままで、送信者名が「退会済みユーザー」に変わります。退会は取り消せません。', ['アカウントの設定を見る', '/settings']],
      ['アカウントが利用停止になった', '募集の掲載とメッセージの送信ができなくなり、公開していた募集とプロフィールは非公開になります。解除の手続きは運営が行います。', null],
    ])}
    <section class="help-contact" aria-labelledby="help-contact-title">
      <h2 id="help-contact-title">運営への連絡</h2>
      <p>募集・メッセージ・プロフィールの内容についての連絡は、各画面の「通報」から運営に届きます。</p>
      <p class="hint">利用停止など、通報では扱えない件の問い合わせ先はまだ公開していません。決まりしだいこのページに掲載します。</p>
    </section>
  </div>`, 'ヘルプ');
}

