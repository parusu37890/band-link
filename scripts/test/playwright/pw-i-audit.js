async (page) => {
  const base = 'http://localhost:8080';
  const password = 'BandLink-QA-ST-2026-PWBE!';
  const browser = page.context().browser();
  if (!browser) throw new Error('Playwright browser is unavailable');

  const login = async (context, email) => {
    const csrfResponse = await context.request.get(base + '/api/csrf');
    const csrf = await csrfResponse.json();
    const response = await context.request.post(base + '/api/auth/login', {
      headers: { 'Content-Type': 'application/json', [csrf.headerName]: csrf.token },
      data: { email, password }
    });
    if (response.status() !== 200) throw new Error(`login failed for ${email}: ${response.status()}`);
    return response.json();
  };

  const contexts = {
    anonymous: await browser.newContext({ viewport: { width: 1440, height: 900 } }),
    user: await browser.newContext({ viewport: { width: 1440, height: 900 } }),
    recipient: await browser.newContext({ viewport: { width: 1440, height: 900 } }),
    admin: await browser.newContext({ viewport: { width: 1440, height: 900 } })
  };
  const pages = {};
  for (const [name, context] of Object.entries(contexts)) pages[name] = await context.newPage();
  await login(contexts.user, 'qa-release-sender@example.test');
  await login(contexts.recipient, 'qa-release-receiver@example.test');
  await login(contexts.admin, 'qa-release-admin@example.test');

  const waitForPage = async p => {
    await p.waitForSelector('#main .page, #main .notice, #main .empty-state', { timeout: 10000 });
    await p.waitForTimeout(250);
  };
  const json = async response => response.ok() ? response.json() : ({ status: response.status() });

  // NFT-007: different cookie jars, identities, searches, notifications, and drafts.
  const meA = await json(await contexts.user.request.get(base + '/api/auth/me'));
  const meB = await json(await contexts.recipient.request.get(base + '/api/auth/me'));
  const cookiesA = await contexts.user.cookies(base);
  const cookiesB = await contexts.recipient.cookies(base);
  const keyword = 'PW-I-U07-ONLY-' + Date.now();
  await contexts.user.request.get(base + '/api/posts/page?keyword=' + encodeURIComponent(keyword));
  const historyA = await json(await contexts.user.request.get(base + '/api/search-history'));
  const historyB = await json(await contexts.recipient.request.get(base + '/api/search-history'));
  const notificationsA = await json(await contexts.user.request.get(base + '/api/notifications'));
  const notificationsB = await json(await contexts.recipient.request.get(base + '/api/notifications'));
  await pages.user.goto(base + '/messages/930001'); await waitForPage(pages.user);
  await pages.recipient.goto(base + '/messages/930001'); await waitForPage(pages.recipient);
  const draftA = 'U07だけの下書き-' + Date.now();
  const draftB = 'U08だけの下書き-' + Date.now();
  await pages.user.locator('#message-content').fill(draftA);
  await pages.recipient.locator('#message-content').fill(draftB);
  const drafts = {
    user: await pages.user.locator('#message-content').inputValue(),
    recipient: await pages.recipient.locator('#message-content').inputValue()
  };
  const sessionIsolation = {
    identitiesDistinct: meA.id === 910007 && meB.id === 910008,
    cookiesDistinct: cookiesA.find(c => c.name === 'JSESSIONID')?.value !== cookiesB.find(c => c.name === 'JSESSIONID')?.value,
    searchIsolated: Array.isArray(historyA) && historyA.some(item => JSON.stringify(item).toLowerCase().includes(keyword.toLowerCase())) && !JSON.stringify(historyB).toLowerCase().includes(keyword.toLowerCase()),
    notificationOwnersIsolated: Array.isArray(notificationsA) && Array.isArray(notificationsB) && notificationsA.every(item => !item.userId || item.userId === 910007) && notificationsB.every(item => !item.userId || item.userId === 910008),
    draftsIsolated: drafts.user === draftA && drafts.recipient === draftB && drafts.user !== drafts.recipient,
    ids: [meA.id, meB.id],
    notificationCounts: [Array.isArray(notificationsA) ? notificationsA.length : -1, Array.isArray(notificationsB) ? notificationsB.length : -1]
  };

  const routes = [
    ['anonymous', '/'], ['anonymous', '/posts'], ['anonymous', '/posts/920001'], ['anonymous', '/users/910005'],
    ['anonymous', '/login'], ['anonymous', '/register'], ['anonymous', '/password-reset'], ['anonymous', '/password-reset/confirm'],
    ['anonymous', '/verify-email'], ['anonymous', '/support'],
    ['user', '/my/posts'], ['user', '/posts/new'], ['user', '/settings'], ['user', '/settings/profile'],
    ['user', '/settings/blocks'], ['user', '/messages'], ['user', '/messages/930001'], ['user', '/notifications'],
    ['user', '/blocks'], ['user', '/contact'], ['user', '/feature-request'], ['user', '/users/910007'],
    ['user', '/users/910008'], ['admin', '/admin'], ['admin', '/admin/reports']
  ];
  const widths = [375, 390, 768, 1440];
  const geometry = [];
  const keyboard = [];
  const accessibility = [];
  const contrast = [];
  const consoleErrors = [];
  const navigationFailures = [];

  const auditDom = async p => p.evaluate(() => {
    const visible = element => {
      const style = getComputedStyle(element); const rect = element.getBoundingClientRect();
      return style.display !== 'none' && style.visibility !== 'hidden' && Number(style.opacity) !== 0 && rect.width > 0 && rect.height > 0;
    };
    const focusables = [...document.querySelectorAll('a[href],button,input,select,textarea,[tabindex]')]
      .filter(el => visible(el) && !el.disabled && el.getAttribute('tabindex') !== '-1');
    const smallTargets = focusables.map(el => {
      const target = ['checkbox','radio'].includes(el.type) && el.closest('label') ? el.closest('label') : el;
      const r = target.getBoundingClientRect();
      return { tag: el.tagName, text: (el.getAttribute('aria-label') || el.textContent || el.getAttribute('name') || '').trim().slice(0, 60), width: Math.round(r.width), height: Math.round(r.height) };
    }).filter(x => x.width < 44 || x.height < 44);
    const clipped = focusables.map(el => {
      const r = el.getBoundingClientRect();
      return { tag: el.tagName, text: (el.getAttribute('aria-label') || el.textContent || '').trim().slice(0, 60), left: Math.round(r.left), right: Math.round(r.right) };
    }).filter(x => x.left < -1 || x.right > innerWidth + 1);
    const backgrounds = [...document.querySelectorAll('body,main,.page,.panel,.chat-shell,.conversation-list,.chat-pane,.site-footer')]
      .filter(visible).map(el => ({ selector: el.tagName.toLowerCase() + (el.className ? '.' + String(el.className).trim().split(/\s+/).join('.') : ''), color: getComputedStyle(el).backgroundColor }));
    return {
      title: document.title,
      horizontalOverflow: document.documentElement.scrollWidth - document.documentElement.clientWidth,
      smallTargets: smallTargets.slice(0, 30),
      smallTargetCount: smallTargets.length,
      clipped,
      backgrounds,
      focusableCount: focusables.length
    };
  });

  const a11yDom = async p => p.evaluate(() => {
    const visible = el => { const s=getComputedStyle(el),r=el.getBoundingClientRect(); return s.display!=='none'&&s.visibility!=='hidden'&&r.width>0&&r.height>0; };
    const controls=[...document.querySelectorAll('button,input,select,textarea,a[href]')].filter(visible);
    const nameOf = el => (el.getAttribute('aria-label') || (el.labels ? [...el.labels].map(x=>x.textContent).join(' ') : '') || el.textContent || el.querySelector?.('img[alt]')?.alt || el.getAttribute('title') || '').trim();
    const unnamed=controls.filter(el=>!nameOf(el)).map(el=>el.outerHTML.slice(0,180));
    const images=[...document.images].filter(visible).filter(img=>!img.hasAttribute('alt')).map(img=>img.outerHTML.slice(0,180));
    const headings=[...document.querySelectorAll('h1,h2,h3,h4,h5,h6')].filter(visible).map(h=>({level:Number(h.tagName[1]),text:h.textContent.trim().slice(0,80)}));
    const jumps=[]; for(let i=1;i<headings.length;i++) if(headings[i].level>headings[i-1].level+1) jumps.push([headings[i-1],headings[i]]);
    return { unnamed, imagesWithoutAlt:images, headings, headingJumps:jumps, h1Count:headings.filter(h=>h.level===1).length, alerts:document.querySelectorAll('[role=alert]').length, liveRegions:document.querySelectorAll('[aria-live]').length };
  });

  const contrastDom = async p => p.evaluate(() => {
    const rgb = value => { const m=value.match(/rgba?\((\d+)[, ]+(\d+)[, ]+(\d+)/); return m ? [+m[1],+m[2],+m[3]] : null; };
    const lum = c => { const x=c.map(v=>v/255).map(v=>v<=.04045?v/12.92:Math.pow((v+.055)/1.055,2.4)); return .2126*x[0]+.7152*x[1]+.0722*x[2]; };
    const ratio = (a,b) => { const x=lum(a),y=lum(b); return (Math.max(x,y)+.05)/(Math.min(x,y)+.05); };
    const bg = el => { let n=el; while(n){const s=getComputedStyle(n).backgroundColor;if(s && !s.endsWith(', 0)') && s!=='transparent'){const c=rgb(s);if(c)return c;}n=n.parentElement;}return [255,255,255]; };
    const visible = el => {const s=getComputedStyle(el),r=el.getBoundingClientRect();return s.display!=='none'&&s.visibility!=='hidden'&&r.width>0&&r.height>0;};
    const candidates=[...document.querySelectorAll('p,span,label,a,button,input,textarea,select,h1,h2,h3,h4,li,strong')].filter(visible).filter(el=>(el.innerText||el.value||el.placeholder||'').trim());
    const failures=[];
    for(const el of candidates){const s=getComputedStyle(el),fg=rgb(s.color);if(!fg)continue;const size=parseFloat(s.fontSize),weight=parseInt(s.fontWeight)||400,large=size>=24||(size>=18.66&&weight>=700),need=large?3:4.5,r=ratio(fg,bg(el));if(r+0.01<need)failures.push({tag:el.tagName,text:(el.innerText||el.value||el.placeholder||'').trim().slice(0,70),ratio:+r.toFixed(2),need,color:s.color,background:'rgb('+bg(el).join(',')+')'});}
    return { checked:candidates.length, failureCount:failures.length, failures:failures.slice(0,40) };
  });

  for (const width of widths) {
    for (const [actor, route] of routes) {
      const p = pages[actor];
      p.setDefaultNavigationTimeout(12000);
      await p.setViewportSize({ width, height: width < 500 ? 844 : 900 });
      const errors=[]; const listener=msg=>{if(msg.type()==='error')errors.push(msg.text())}; p.on('console',listener);
      try { await p.goto(base + route, { waitUntil: 'domcontentloaded' }); await waitForPage(p); }
      catch (error) { navigationFailures.push({width,actor,route,error:String(error)}); p.off('console',listener); continue; }
      const dom=await auditDom(p);
      geometry.push({ width, actor, route, ...dom });
      if(errors.length)consoleErrors.push({width,actor,route,errors});
      p.off('console',listener);
      if(width===1440){
        accessibility.push({actor,route,...await a11yDom(p)});
        contrast.push({actor,route,...await contrastDom(p)});
        const expected=Math.min(dom.focusableCount+3,90),sequence=[];
        await p.locator('body').press('Home').catch(()=>{});
        for(let i=0;i<expected;i++){
          await p.keyboard.press('Tab');
          const state=await p.evaluate(()=>{const e=document.activeElement;return {tag:e?.tagName||'',id:e?.id||'',text:(e?.getAttribute?.('aria-label')||e?.textContent||e?.getAttribute?.('name')||'').trim().slice(0,60),visible:!!e?.matches?.(':focus-visible')};});
          sequence.push(state);
        }
        keyboard.push({actor,route,steps:sequence.length,missingVisible:sequence.filter(x=>x.tag!=='BODY'&&!x.visible).length,unique:new Set(sequence.filter(x=>x.tag!=='BODY').map(x=>x.tag+'#'+x.id+':'+x.text)).size,focusableCount:dom.focusableCount,sequence:sequence.slice(0,50)});
      }
    }
  }

  const screenshots = [
    ['anonymous','/posts',375,'pw-i-posts-375.png'], ['user','/settings/profile',390,'pw-i-profile-390.png'],
    ['user','/messages/930001',768,'pw-i-messages-768.png'], ['user','/my/posts',1440,'pw-i-my-posts-1440.png'],
    ['admin','/admin',1440,'pw-i-admin-1440.png']
  ];
  for(const [actor,route,width,name] of screenshots){const p=pages[actor];await p.setViewportSize({width,height:900});await p.goto(base+route);await waitForPage(p);await p.screenshot({path:`C:/Users/parus/Desktop/band/docs/test-results/playwright-harness/${name}`,fullPage:true});}

  const result = {
    generatedAt: new Date().toISOString(), tool: '@playwright/mcp',
    sessionIsolation,
    responsive: { combinations: geometry.length, navigationFailures, overflowFailures: geometry.filter(x=>x.horizontalOverflow>1), clippedFailures: geometry.filter(x=>x.clipped.length), smallTargetCombinations: geometry.filter(x=>x.smallTargetCount>0).length, smallTargetExamples: geometry.filter(x=>x.smallTargetCount>0).slice(0,20).map(x=>({width:x.width,route:x.route,count:x.smallTargetCount,examples:x.smallTargets.slice(0,5)})), backgroundColors:[...new Set(geometry.flatMap(x=>x.backgrounds.map(b=>b.color)))], consoleErrors },
    keyboard: { routes:keyboard.length, failures:keyboard.filter(x=>x.missingVisible>0||x.unique<Math.min(x.focusableCount,2)), details:keyboard },
    accessibility: { routes:accessibility.length, unnamed:accessibility.filter(x=>x.unnamed.length), missingAlt:accessibility.filter(x=>x.imagesWithoutAlt.length), headingFailures:accessibility.filter(x=>x.h1Count!==1||x.headingJumps.length), details:accessibility },
    contrast: { routes:contrast.length, checked:contrast.reduce((n,x)=>n+x.checked,0), failingRoutes:contrast.filter(x=>x.failureCount), details:contrast }
  };
  for(const context of Object.values(contexts)) await context.close();
  return result;
}
