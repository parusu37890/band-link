import { api, h, icon, avatar, state, main, showPage, notice, empty, button, toast,
  bindForm, report, confirmAction, time, poll } from './ui.js';

const positiveId = value => /^[1-9]\d*$/.test(String(value ?? '')) ? String(value) : null;
const personName = user => user?.status === 'WITHDRAWN' ? '退会済みユーザー' : (user?.username || 'ユーザー');
const heading = (title, description, action = '') => `<header class="page-heading"><div class="page-heading-copy"><h1>${h(title)}</h1><p class="muted">${h(description)}</p></div>${action}</header>`;
const errorText = error => error?.message || '読み込めませんでした。時間をおいて、もう一度お試しください。';
const retryMarkup = message => `${notice(message, 'error')}<button type="button" class="button secondary" data-retry>もう一度読み込む</button>`;

export async function communityPage(path) {
  path = path.replace(/\/+$/, '') || '/';
  if (!/^\/(messages(?:\/\d+)?|notifications|blocks|settings\/blocks|admin(?:\/reports)?)\/?$/.test(path)) return false;
  if (!state.user) {
    showPage(`<div class="page">${empty('ログインして、つながろう。', 'メッセージや通知はログイン後に確認できます。', button('ログイン', `/login?next=${encodeURIComponent(location.pathname + location.search)}`))}</div>`, 'ログインが必要です');
    return true;
  }
  if (path.startsWith('/messages')) await messagesPage(path);
  else if (path === '/notifications') await notificationsPage();
  else if (path.includes('blocks')) await blocksPage();
  else await adminPage();
  return true;
}

async function messagesPage(path) {
  let conversationId = positiveId(path.match(/^\/messages\/(\d+)\/?$/)?.[1]);
  let recipientId = positiveId(new URLSearchParams(location.search).get('to'));
  let conversations = [];
  let peer = null;
  let busy = false;
  let lastMessageState = '';
  let lastListState = '';
  let blocked = false;
  let disposed = false;
  let sending = false;
  let draftContent = '';
  let draftImage = null;
  let previewUrl = null;
  window.addEventListener('pagehide', () => { disposed = true; if (previewUrl) URL.revokeObjectURL(previewUrl); }, { once: true });

  showPage(`<div class="page messages-page">${heading('メッセージ', '気になる募集のこと、練習の日程。ここから直接話せます。')}
    <div class="chat-shell" data-chat-shell>
      <aside class="conversation-list" aria-label="会話一覧"><div class="inbox-heading"><h2>会話一覧</h2><a href="/posts">仲間を探す ${icon('arrow')}</a></div><div data-conversations aria-busy="true"><p class="panel muted" role="status">会話を読み込んでいます…</p></div></aside>
      <section class="chat-pane" aria-label="メッセージ"><div data-chat-header></div><p class="chat-status muted" data-chat-status role="status" aria-live="polite"></p><div class="chat-messages" data-messages aria-label="会話の内容"><p class="muted" role="status">読み込んでいます…</p></div><div data-composer></div></section>
    </div></div>`, 'メッセージ');

  const list = main.querySelector('[data-conversations]');
  const shell = main.querySelector('[data-chat-shell]');
  const header = main.querySelector('[data-chat-header]');
  const messages = main.querySelector('[data-messages]');
  const composer = main.querySelector('[data-composer]');
  const status = main.querySelector('[data-chat-status]');

  function renderList() {
    const fingerprint = JSON.stringify(conversations);
    if (lastListState === fingerprint) return;
    lastListState = fingerprint;
    list.setAttribute('aria-busy', 'false');
    list.innerHTML = conversations.length ? conversations.map(conversation => {
      const id = positiveId(conversation.id);
      if (!id) return '';
      const other = conversation.otherUser;
      return `<a class="conversation-item${id === conversationId ? ' active' : ''}" href="/messages/${id}"${id === conversationId ? ' aria-current="page"' : ''}>${avatar(other)}<span class="stack"><strong>${h(personName(other))}</strong><span class="muted">${h(time(conversation.lastMessageAt))}</span></span></a>`;
    }).join('') : `<div class="panel"><p class="muted">まだ会話がありません。</p>${button('募集から仲間を探す', '/posts', 'secondary')}</div>`;
  }

  function renderPeer() {
    if (!peer) {
      header.innerHTML = '';
      composer.innerHTML = '';
      messages.innerHTML = empty('気になる仲間に、声をかけよう。', '募集やプロフィールからメッセージを送れます。', button('募集を探す', '/posts', 'secondary'));
      return;
    }
    shell.classList.add('has-conversation');
    header.innerHTML = `<header class="chat-header"><a class="button secondary mobile-back" href="/messages" aria-label="会話一覧に戻る">${icon('arrow-left')}<span>会話一覧</span></a><div class="row">${avatar(peer)}<div><h2>${h(personName(peer))}</h2>${peer.status === 'WITHDRAWN' || peer.status === 'SUSPENDED' ? '<span class="muted">現在連絡できません</span>' : `<a href="/users/${positiveId(peer.id)}">プロフィールを見る</a>`}</div></div></header>`;
    const unavailable = peer.status === 'WITHDRAWN' || peer.status === 'SUSPENDED' || blocked;
    if (unavailable) {
      composer.innerHTML = `<div class="panel">${notice(blocked ? 'ブロック中のため、メッセージを送信できません。過去の会話は引き続き確認できます。' : 'このユーザーには現在メッセージを送信できません。過去の会話は引き続き確認できます。')}${blocked ? button('ブロックを管理する', '/blocks', 'secondary') : ''}</div>`;
      return;
    }
    if (!state.user.emailVerified) {
      composer.innerHTML = `<div class="panel">${notice('メッセージを送るにはメールアドレスの確認が必要です。')}${button('メール確認へ', '/verify-email', 'secondary')}</div>`;
      return;
    }
    // Keep the live form during polling, including focus, selection and attachment.
    if (composer.querySelector('[data-message-form]')) return;
    composer.innerHTML = `<form class="composer" data-message-form><label class="form-field composer-field" for="message-content">メッセージ<textarea class="input" id="message-content" name="content" rows="3" maxlength="2000" placeholder="自己紹介や、募集について聞きたいことを書いてください。" aria-describedby="message-help"></textarea></label><div class="composer-attachment" data-attachment hidden></div><div class="composer-toolbar"><div><button type="button" class="button secondary small" data-attach>画像を添付</button><input id="message-image" name="image" type="file" accept="image/jpeg,image/png,image/webp" aria-label="添付する画像" hidden><p class="muted composer-help" id="message-help">2,000文字まで · 画像は1枚 / 5MBまで</p></div><button class="button primary" type="submit">送信する ${icon('arrow')}</button></div><div data-form-error role="alert"></div></form>`;
    const form = composer.querySelector('form');
    const input = form.elements.content;
    const imageInput = form.elements.image;
    const attachment = form.querySelector('[data-attachment]');
    input.value = draftContent;
    input.addEventListener('input', () => { draftContent = input.value; });
    function renderAttachment() {
      if (previewUrl) URL.revokeObjectURL(previewUrl);
      previewUrl = draftImage ? URL.createObjectURL(draftImage) : null;
      attachment.hidden = !draftImage;
      attachment.innerHTML = draftImage ? `<img class="attachment-preview" src="${h(previewUrl)}" alt="送信する画像のプレビュー"><div class="attachment-details"><strong>${h(draftImage.name)}</strong><span class="muted">${(draftImage.size / 1024 / 1024).toFixed(1)} MB</span></div><button type="button" class="button quiet small" data-remove-image>取り消す</button>` : '';
      attachment.querySelector('[data-remove-image]')?.addEventListener('click', () => { draftImage = null; imageInput.value = ''; renderAttachment(); });
    }
    form.querySelector('[data-attach]').addEventListener('click', () => imageInput.click());
    imageInput.addEventListener('change', () => {
      const file = imageInput.files?.[0];
      if (!file) return;
      if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || file.size > 5 * 1024 * 1024) {
        imageInput.value = '';
        toast('JPEG・PNG・WebPの画像を、1枚5MB以内で選択してください。');
        return;
      }
      draftImage = file;
      renderAttachment();
    });
    renderAttachment();
    bindForm(form, async () => {
      if (sending) return;
      const content = input.value.trim();
      const image = draftImage;
      if (!content && !image) throw new Error('メッセージ本文か画像を選択してください。');
      sending = true;
      input.readOnly = true;
      form.querySelectorAll('button').forEach(element => { element.disabled = true; });
      try {
        let imageUrl = null;
        if (image) { const upload = new FormData(); upload.append('file', image); imageUrl = await api('/api/messages/images', { method: 'POST', body: upload }); }
        const result = await api(`/api/messages?recipientId=${positiveId(peer.id)}`, { method: 'POST', body: { content: content || null, imageUrl } });
        // Only clear the submitted draft after the server confirms receipt.
        input.value = ''; imageInput.value = ''; draftContent = ''; draftImage = null; renderAttachment();
        if (!conversationId) {
          conversationId = positiveId(result.conversationId);
          recipientId = null;
          history.replaceState(null, '', `/messages/${conversationId}`);
        }
        lastMessageState = '';
        status.textContent = '送信しました。';
        await refresh(true);
        input.focus();
      } finally {
        sending = false;
        input.readOnly = false;
        form.querySelectorAll('button').forEach(element => { element.disabled = false; });
      }
    });
  }

  function renderMessages(items, scrollToLatest) {
    const fingerprint = JSON.stringify(items);
    if (lastMessageState === fingerprint) return;
    const nearBottom = messages.scrollHeight - messages.scrollTop - messages.clientHeight < 100;
    const initial = !lastMessageState;
    lastMessageState = fingerprint;
    const oldScroll = messages.scrollTop;
    messages.innerHTML = items.length ? items.map(message => {
      const mine = String(message.senderId) === String(state.user.id);
      const id = positiveId(message.id);
      return `<article class="message${mine ? ' mine' : ''}"${id ? ` data-message-id="${id}"` : ''} aria-label="${mine ? '自分' : h(personName(peer))}のメッセージ"><p class="message-text">${h(message.content || '')}</p>${message.imageUrl && /^\/api\/messages\/images\/[A-Za-z0-9-]+\.(jpg|png|webp)$/.test(message.imageUrl) ? `<img class="message-image" src="${h(message.imageUrl)}" alt="メッセージ画像" loading="lazy">` : ''}<div class="message-meta"><time datetime="${h(message.createdAt)}">${h(time(message.createdAt))}</time>${mine && message.readAt ? '<span>既読</span>' : ''}${!mine && id ? `<button type="button" class="button text-button" data-report-message="${id}" aria-label="このメッセージを通報する">通報</button>` : ''}</div></article>`;
    }).join('') : empty('会話をはじめよう。', 'まずは自己紹介と、気になった募集について送ってみましょう。');
    messages.querySelectorAll('[data-report-message]').forEach(element => element.addEventListener('click', () => report('MESSAGE', element.dataset.reportMessage)));
    messages.scrollTop = initial || nearBottom || scrollToLatest ? messages.scrollHeight : oldScroll;
  }

  async function refresh(scrollToLatest = false) {
    if (busy || disposed || document.hidden) return;
    busy = true;
    try {
      conversations = await api('/api/messages/conversations');
      if (disposed) return;
      if (!Array.isArray(conversations)) throw new Error('会話を読み込めませんでした。');
      if (recipientId && !conversationId) {
        const existing = conversations.find(conversation => String(conversation.otherUser?.id) === recipientId);
        if (existing) {
          conversationId = positiveId(existing.id);
          history.replaceState(null, '', `/messages/${conversationId}`);
        }
      }
      const selected = conversationId ? conversations.find(conversation => String(conversation.id) === conversationId) : null;
      if (conversationId && !selected) throw new Error('この会話は見つからないか、閲覧できません。');
      if (selected && JSON.stringify(selected.otherUser) !== JSON.stringify(peer)) {
        peer = selected.otherUser;
        renderPeer();
      }
      renderList();
      if (conversationId) {
        const items = await api(`/api/messages/conversation/${conversationId}`);
        if (disposed) return;
        if (!Array.isArray(items)) throw new Error('メッセージを読み込めませんでした。');
        renderMessages(items, scrollToLatest);
        if (!document.hidden && items.some(item => String(item.senderId) !== String(state.user.id) && !item.readAt)) {
          await api(`/api/messages/conversation/${conversationId}/read`, { method: 'PATCH' });
        }
      } else if (!recipientId) renderPeer();
      status.textContent = '15秒ごとに自動更新しています。';
      status.classList.remove('error');
    } catch (error) {
      status.textContent = `${errorText(error)} 自動で再試行します。`;
      status.classList.add('error');
      if (!lastListState) {
        list.setAttribute('aria-busy', 'false');
        list.innerHTML = `<div class="panel">${retryMarkup('会話一覧を読み込めませんでした。')}</div>`;
        list.querySelector('[data-retry]').addEventListener('click', () => refresh());
      }
      if (!lastMessageState && conversationId) messages.innerHTML = empty('会話を表示できません。', errorText(error), button('会話一覧へ', '/messages', 'secondary'));
    } finally { busy = false; }
  }

  try {
    if (recipientId) {
      if (recipientId === String(state.user.id)) throw new Error('自分自身にメッセージは送信できません。');
      peer = await api(`/api/users/${recipientId}`);
      messages.innerHTML = empty('会話をはじめよう。', '自己紹介や好きな音楽について、最初のメッセージを送ってみましょう。');
    }
    // Do not silently treat a failed block lookup as an unblocked relationship.
    const blocks = await api('/api/blocks');
    await refresh();
    blocked = Array.isArray(blocks) && blocks.some(user => String(user.id) === String(peer?.id));
    renderPeer();
    poll(() => refresh());
  } catch (error) {
    messages.innerHTML = empty('メッセージを表示できません。', errorText(error), button('会話一覧へ', '/messages', 'secondary'));
    composer.innerHTML = '';
    status.textContent = '';
    list.setAttribute('aria-busy', 'false');
    list.innerHTML = `<div class="panel">${button('募集を探す', '/posts', 'secondary')}</div>`;
  }
}

async function notificationsPage() {
  showPage(`<div class="page notifications-page">${heading('通知', '届いたメッセージと、運営からのお知らせ。', '<button type="button" class="button secondary small" data-read-all disabled>すべて既読にする</button>')}<section class="notification-feed" aria-label="通知一覧"><div class="section-toolbar"><h2>お知らせ一覧</h2><span class="muted" data-unread-summary></span></div><div data-notifications aria-busy="true"><p role="status">通知を読み込んでいます…</p></div><p class="muted" data-notification-status role="status"></p></section></div>`, '通知');
  const container = main.querySelector('[data-notifications]');
  const status = main.querySelector('[data-notification-status]');
  const readAll = main.querySelector('[data-read-all]');
  const summary = main.querySelector('[data-unread-summary]');
  let last = '';
  let busy = false;
  let refreshAgain = false;
  let mutationPending = false;
  let revision = 0;
  async function refresh() {
    if (document.hidden) return;
    if (busy) { refreshAgain = true; return; }
    busy = true;
    const currentRevision = revision;
    try {
      const items = await api('/api/notifications');
      if (currentRevision !== revision) { refreshAgain = true; return; }
      if (!Array.isArray(items)) throw new Error('通知を読み込めませんでした。');
      const fingerprint = JSON.stringify(items);
      if (fingerprint !== last) {
        container.innerHTML = items.length ? items.map(item => `<article class="notification-item${!item.readAt ? ' is-unread' : ''}"><div class="notification-copy"><div class="notification-meta"><strong>${item.type === 'NEW_MESSAGE' ? '新しいメッセージ' : 'お知らせ'}</strong>${!item.readAt ? '<span class="badge">未読</span>' : '<span class="muted">既読</span>'}<time datetime="${h(item.createdAt)}">${h(time(item.createdAt))}</time></div><p>${h(item.content)}</p><div class="notification-actions">${item.type === 'NEW_MESSAGE' ? `<a href="${positiveId(item.relatedId) ? `/messages/${positiveId(item.relatedId)}` : '/messages'}">メッセージを開く ${icon('arrow')}</a>` : ''}${!item.readAt && positiveId(item.id) ? `<button type="button" class="button quiet small" data-read-notification="${positiveId(item.id)}">既読にする</button>` : ''}</div></div></article>`).join('') : empty('お知らせはまだありません。', 'メッセージが届くと、ここに通知されます。', button('募集を探す', '/posts', 'secondary'));
        last = fingerprint;
      }
      const unread = items.filter(item => !item.readAt).length;
      summary.textContent = unread ? `${unread}件の未読` : 'すべて確認済み';
      readAll.disabled = mutationPending || !unread;
      const dot = document.querySelector('[data-unread-dot]');
      if (dot) dot.hidden = !unread;
      status.textContent = '15秒ごとに自動更新しています。';
    } catch (error) {
      status.textContent = errorText(error);
      if (!last) {
        container.innerHTML = retryMarkup('通知を読み込めませんでした。');
        container.querySelector('[data-retry]').addEventListener('click', refresh);
      }
    } finally {
      busy = false;
      container.setAttribute('aria-busy', 'false');
      if (refreshAgain) { refreshAgain = false; await refresh(); }
    }
  }
  async function markRead(path, element, all = false) {
    if (mutationPending) return;
    mutationPending = true;
    element.disabled = true;
    readAll.disabled = true;
    try {
      await api(path, { method: 'PATCH' });
      revision++;
      if (all) toast('すべての通知を既読にしました。');
    } catch (error) { status.textContent = errorText(error); toast(errorText(error)); }
    finally {
      mutationPending = false;
      element.disabled = false;
      await refresh();
    }
  }
  // Delegate once: unchanged polling results must not add duplicate listeners.
  container.addEventListener('click', event => {
    const element = event.target.closest('[data-read-notification]');
    if (element && container.contains(element)) markRead(`/api/notifications/${element.dataset.readNotification}/read`, element);
  });
  readAll.addEventListener('click', () => markRead('/api/notifications/read-all', readAll, true));
  await refresh();
  poll(refresh);
}

async function blocksPage() {
  showPage(`<div class="page blocks-page">${heading('ブロック管理', '連絡を受け取りたくない相手を管理します。', button('設定に戻る', '/settings', 'quiet'))}<section class="block-list stack">${notice('ブロック中はお互いにメッセージを送れず、ログイン中の募集一覧にもお互いの投稿が表示されません。過去の会話は残ります。')}<div data-blocks aria-busy="true"><p role="status">読み込んでいます…</p></div></section></div>`, 'ブロック管理');
  const container = main.querySelector('[data-blocks]');
  async function load() {
    try {
      const users = await api('/api/blocks');
      if (!Array.isArray(users)) throw new Error('ブロック一覧を読み込めませんでした。');
      container.innerHTML = users.length ? users.map(user => `<article class="block-item">${avatar(user)}<div class="stack"><strong>${h(personName(user))}</strong><span class="muted">ブロック中</span></div><button class="button secondary small" type="button" data-unblock="${positiveId(user.id)}" data-name="${h(personName(user))}">ブロックを解除</button></article>`).join('') : empty('ブロック中のユーザーはいません。', '必要なときは、相手のプロフィールからブロックできます。', button('募集を探す', '/posts', 'secondary'));
      container.querySelectorAll('[data-unblock]').forEach(element => element.addEventListener('click', () => confirmAction('ブロックを解除しますか？', `${element.dataset.name}さんとのメッセージ送信が再び可能になります。相手からもブロックされている場合は送信できません。`, async () => {
        await api(`/api/blocks/${element.dataset.unblock}`, { method: 'DELETE' });
        toast('ブロックを解除しました。');
        await load();
      })));
    } catch (error) {
      container.innerHTML = retryMarkup(errorText(error));
      container.querySelector('[data-retry]').addEventListener('click', load);
    } finally { container.setAttribute('aria-busy', 'false'); }
  }
  await load();
}

async function adminPage() {
  if (state.user.role !== 'ADMIN') {
    showPage(`<div class="page">${empty('運営メンバー専用のページです。', 'このページを表示する権限がありません。', button('募集を探す', '/posts', 'secondary'))}</div>`, 'アクセスできません');
    return;
  }
  const statuses = { PENDING: '未対応', REVIEWED: '確認済み', DISMISSED: '対応不要', ACTIONED: '対応済み' };
  const types = { POST: '募集投稿', USER: 'ユーザー', MESSAGE: 'メッセージ' };
  showPage(`<div class="page admin-page">${heading('運営管理', '通報の確認と、ユーザーの利用状況を管理します。')}<div class="admin-layout"><section class="admin-reports stack"><div class="section-toolbar"><h2>通報一覧</h2><label class="form-field" for="report-status">対応状況<select class="input" id="report-status">${Object.entries(statuses).map(([value, label]) => `<option value="${value}">${label}</option>`).join('')}</select></label></div><div data-reports aria-busy="true"><p role="status">通報を読み込んでいます…</p></div></section><aside class="admin-tools stack"><h2>利用停止の解除</h2><p class="muted">確認済みのユーザーIDを指定して、利用停止を解除します。</p><form data-unsuspend-form class="stack"><label class="form-field" for="unsuspend-user">ユーザーID<input class="input" type="number" id="unsuspend-user" name="userId" min="1" step="1" required></label><button type="submit" class="button secondary">解除内容を確認</button><div data-form-error role="alert"></div></form></aside></div></div>`, '運営管理');
  const container = main.querySelector('[data-reports]');
  const select = main.querySelector('#report-status');
  let revision = 0;
  async function load() {
    const currentRevision = ++revision;
    container.setAttribute('aria-busy', 'true');
    container.innerHTML = '<p role="status">通報を読み込んでいます…</p>';
    try {
      const items = await api(`/api/admin/reports?status=${select.value}`);
      if (currentRevision !== revision) return;
      if (!Array.isArray(items)) throw new Error('通報を読み込めませんでした。');
      container.innerHTML = items.length ? items.map(item => {
        const id = positiveId(item.id);
        const target = positiveId(item.targetId);
        return `<article class="report-card panel stack"><div class="row"><h3>${h(types[item.targetType] || '対象')}への通報 <span class="muted">#${id}</span></h3><span class="badge">${h(statuses[item.status] || item.status)}</span></div><p class="muted">対象ID ${target} · ${h(time(item.createdAt))}</p><div><h4>通報理由</h4><p class="message-text">${h(item.reason)}</p></div>${item.targetType === 'MESSAGE' ? '<p class="muted">この一覧には通報理由が表示されます。</p>' : ''}<div class="row">${item.targetType === 'POST' ? `${button('募集を確認', `/posts/${target}`, 'secondary')}<button type="button" class="button danger" data-remove-post="${target}">募集を非公開にする</button>` : ''}${item.targetType === 'USER' ? `${button('プロフィールを確認', `/users/${target}`, 'secondary')}<button type="button" class="button danger" data-suspend="${target}">利用を停止する</button>` : ''}</div><form class="row" data-report-form="${id}"><label class="form-field" for="report-status-${id}">対応状況<select class="input" name="status" id="report-status-${id}">${Object.entries(statuses).map(([value, label]) => `<option value="${value}"${item.status === value ? ' selected' : ''}>${label}</option>`).join('')}</select></label><button type="submit" class="button secondary">状態を保存</button><div data-form-error role="alert"></div></form></article>`;
      }).join('') : empty('この状況の通報はありません。', '新しい通報や、別の対応状況を確認できます。');
      container.querySelectorAll('[data-report-form]').forEach(form => bindForm(form, async () => {
        await api(`/api/admin/reports/${form.dataset.reportForm}?status=${form.elements.status.value}`, { method: 'PATCH' });
        toast('対応状況を保存しました。');
        await load();
      }));
      container.querySelectorAll('[data-remove-post]').forEach(element => element.addEventListener('click', () => confirmAction('募集を非公開にしますか？', `募集ID ${element.dataset.removePost}を運営による非公開にします。内容を確認してから実行してください。`, async () => {
        await api(`/api/admin/posts/${element.dataset.removePost}`, { method: 'DELETE' });
        toast('募集を非公開にしました。必要に応じて対応状況を更新してください。');
        element.disabled = true;
        element.textContent = '非公開にしました';
      })));
      container.querySelectorAll('[data-suspend]').forEach(element => element.addEventListener('click', () => confirmAction('ユーザーの利用を停止しますか？', `ユーザーID ${element.dataset.suspend}の投稿・プロフィールを非公開にし、投稿やメッセージ送信を停止します。`, async () => {
        await api(`/api/admin/users/${element.dataset.suspend}/suspend`, { method: 'PATCH' });
        toast('利用を停止しました。必要に応じて対応状況を更新してください。');
        element.disabled = true;
        element.textContent = '利用停止にしました';
      })));
    } catch (error) {
      if (currentRevision !== revision) return;
      container.innerHTML = retryMarkup(errorText(error));
      container.querySelector('[data-retry]').addEventListener('click', load);
    } finally { if (currentRevision === revision) container.setAttribute('aria-busy', 'false'); }
  }
  select.addEventListener('change', load);
  const form = main.querySelector('[data-unsuspend-form]');
  bindForm(form, async () => {
    const id = positiveId(form.elements.userId.value);
    if (!id) throw new Error('有効なユーザーIDを入力してください。');
    confirmAction('利用停止を解除しますか？', `ユーザーID ${id}が再びサービスを利用できるようになります。`, async () => {
      await api(`/api/admin/users/${id}/unsuspend`, { method: 'PATCH' });
      toast(`ユーザーID ${id}の利用停止を解除しました。`);
      form.reset();
    });
  });
  await load();
}



