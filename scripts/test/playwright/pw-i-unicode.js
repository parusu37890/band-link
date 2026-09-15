async (page) => {
  const base='http://localhost:8080',password='BandLink-QA-ST-2026-PWBE!',stamp=String(Date.now());
  const browser=page.context().browser(),contexts=[];
  const makeText=(limit,prefix,withBreaks=true)=>{let s=prefix,tokens=withBreaks?['日本語','🎸','e\u0301','\n']:['名','🎸','e\u0301'];let i=0;while(s.length+tokens[i%tokens.length].length<=limit){s+=tokens[i%tokens.length];i++;}while(s.length<limit)s+='音';return s;};
  const login=async email=>{const c=await browser.newContext({viewport:{width:1280,height:900}});contexts.push(c);const x=await c.request.get(base+'/api/csrf'),t=await x.json(),r=await c.request.post(base+'/api/auth/login',{headers:{'Content-Type':'application/json',[t.headerName]:t.token},data:{email,password}});if(!r.ok())throw new Error(`login ${email}: ${r.status()}`);return{c,p:await c.newPage()};};
  const ready=async p=>{await p.waitForSelector('#main .page',{timeout:10000});await p.waitForTimeout(120)};
  const json=async r=>{if(!r.ok())throw new Error(`${r.url()} ${r.status()}`);return r.json()};
  const results={tool:'@playwright/mcp',stamp};

  const profile=await login('qa-release-complete@example.test'), username=makeText(80,'QA'+stamp.slice(-4),false),bio=makeText(500,'BIO'+stamp+'\n');
  await profile.p.goto(base+'/settings/profile');await ready(profile.p);await profile.p.locator('#username').fill(username);await profile.p.locator('#bio').fill(bio);
  await profile.p.locator('#profile-form [type=submit]').click();await profile.p.waitForFunction(()=>location.pathname==='/users/910004',{timeout:10000});
  const profileSaved=await json(await profile.c.request.get(base+'/api/users/910004'));
  results.profile={usernameLength:username.length,bioLength:bio.length,usernameExact:profileSaved.username===username,bioExact:profileSaved.bio===bio,overflow:await profile.p.evaluate(()=>document.documentElement.scrollWidth-document.documentElement.clientWidth)};

  const post=await login('qa-release-edit-ready@example.test'),title=makeText(30,'T'+stamp.slice(-4),false),content=makeText(500,'POST'+stamp+'\n'),area=makeText(100,'AREA'+stamp,false);
  await post.p.goto(base+'/posts/920003/edit');await ready(post.p);
  // The current requirements removed station/sub-area input. Keep this case focused on
  // the remaining title/body Unicode round-trip instead of targeting the retired #areaSub.
  await post.p.locator('[data-editor-next]').click();await post.p.locator('#title').fill(title);await post.p.locator('#content').fill(content);await post.p.locator('[data-editor-next]').click();await post.p.locator('[data-editor-submit]').click();await post.p.waitForFunction(()=>location.pathname==='/posts/920003',{timeout:10000});
  const postSaved=await json(await post.c.request.get(base+'/api/posts/920003'));
  results.post={titleLength:title.length,contentLength:content.length,titleExact:postSaved.title===title,contentExact:postSaved.content===content,overflow:await post.p.evaluate(()=>document.documentElement.scrollWidth-document.documentElement.clientWidth)};

  const dm=await login('qa-release-sender@example.test'),dmText=makeText(500,'DM'+stamp+'\n');
  await dm.p.goto(base+'/messages/930001');await ready(dm.p);await dm.p.locator('#message-content').fill(dmText);await dm.p.locator('[data-message-form] [type=submit]').click();await dm.p.waitForFunction(()=>document.querySelector('[data-chat-status]')?.textContent.includes('送信しました'),null,{timeout:10000});
  const messages=await json(await dm.c.request.get(base+'/api/messages/conversation/930001')),dmSaved=messages.find(x=>x.content===dmText);
  await dm.p.waitForFunction(marker=>[...document.querySelectorAll('.message-text')].some(x=>x.textContent.includes(marker)),'DM'+stamp,{timeout:10000});
  results.dm={length:dmText.length,exact:Boolean(dmSaved),rendered:await dm.p.locator('.message-text').filter({hasText:'DM'+stamp}).count()===1,overflow:await dm.p.evaluate(()=>document.documentElement.scrollWidth-document.documentElement.clientWidth)};await dm.p.evaluate(()=>dispatchEvent(new Event('pagehide')));

  const reason=makeText(1000,'REPORT'+stamp+'\n');await profile.p.goto(base+'/users/910008');await ready(profile.p);await profile.p.locator('#report-user').click();await profile.p.locator('#reason').fill(reason);await profile.p.locator('#dialog [type=submit]').click();await profile.p.waitForFunction(()=>!document.querySelector('#dialog')?.open,null,{timeout:10000});
  results.report={length:reason.length,dialogClosed:true};

  // Contact and feature-request copy is capped at 1000 characters in the current
  // requirements; exercise the accepted boundary rather than the retired 3000 limit.
  const contact=makeText(1000,'CONTACT'+stamp+'\n'),feature=makeText(1000,'FEATURE'+stamp+'\n');
  await profile.p.goto(base+'/contact');await ready(profile.p);await profile.p.locator('#feedback-message').fill(contact);await profile.p.locator('#feedback-form [type=submit]').click();await profile.p.waitForFunction(()=>document.querySelector('#feedback-status')?.textContent.includes('受け付けました'),null,{timeout:10000});
  await profile.p.goto(base+'/feature-request');await ready(profile.p);await profile.p.locator('#feedback-message').fill(feature);await profile.p.locator('#feedback-form [type=submit]').click();await profile.p.waitForFunction(()=>document.querySelector('#feedback-status')?.textContent.includes('受け付けました'),null,{timeout:10000});
  const admin=await login('qa-release-admin@example.test'),reports=await json(await admin.c.request.get(base+'/api/admin/reports?status=PENDING')),feedback=await json(await admin.c.request.get(base+'/api/admin/feedback'));
  results.report.exact=reports.some(x=>x.reason===reason||x.reasonText===reason);
  results.feedback={contactLength:contact.length,featureLength:feature.length,contactExact:feedback.some(x=>x.message===contact),featureExact:feedback.some(x=>x.message===feature)};
  results.pass=[results.profile.usernameExact,results.profile.bioExact,results.post.titleExact,results.post.contentExact,results.dm.exact,results.dm.rendered,results.report.exact,results.feedback.contactExact,results.feedback.featureExact].every(Boolean)&&[results.profile.overflow,results.post.overflow,results.dm.overflow].every(x=>x<=1);
  for(const c of contexts)await c.close();return results;
}
