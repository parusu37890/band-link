async (page) => {
  const base='http://localhost:8080',password='BandLink-QA-ST-2026-PWBE!',stamp=String(Date.now());
  const browser=page.context().browser(), contexts=[];
  const make=async email=>{const c=await browser.newContext({viewport:{width:1280,height:900}});contexts.push(c);if(email){const r=await c.request.get(base+'/api/csrf'),t=await r.json(),login=await c.request.post(base+'/api/auth/login',{headers:{'Content-Type':'application/json',[t.headerName]:t.token},data:{email,password}});if(!login.ok())throw new Error(`login ${email} ${login.status()}`);}return await c.newPage();};
  const ready=async p=>{await p.waitForSelector('#main .page',{timeout:10000});await p.waitForTimeout(120)};
  const twice=async(p,formSelector,urlPart)=>{let count=0;const seen=[];const listener=req=>{if(req.method()==='POST'&&req.url().includes(urlPart)){count++;seen.push(req.url())}};p.on('request',listener);const state=await p.locator(formSelector).evaluate(form=>{form.dispatchEvent(new SubmitEvent('submit',{bubbles:true,cancelable:true}));form.dispatchEvent(new SubmitEvent('submit',{bubbles:true,cancelable:true}));const b=form.querySelector('[type=submit]');return{busy:form.dataset.busy==='true'||form.getAttribute('aria-busy')==='true',disabled:Boolean(b?.disabled),ariaBusy:b?.getAttribute('aria-busy')}});await p.waitForTimeout(1500);p.off('request',listener);return{count,state,seen};};
  const results={tool:'@playwright/mcp',stamp};

  const register=await make();await register.goto(base+'/register');await ready(register);
  await register.locator('#username').fill('QA_RELEASE_DUP_'+stamp.slice(-8));
  await register.locator('#email').fill(`qa-release-dup-${stamp}@example.test`);
  await register.locator('#password').fill(password);await register.locator('#age').fill('28');await register.locator('#experienceYears').fill('3');
  await register.locator('[name=gender][value="男性"]').check();
  await register.locator('#auth-form').evaluate(form=>{for(const name of ['prefectureIds','partIds','genreIds','stanceIds']){const input=form.querySelector(`[name=${name}]`);input.checked=true;input.dispatchEvent(new Event('change',{bubbles:true}));}});
  results.register=await twice(register,'#auth-form','/api/auth/register');

  const user=await make('qa-release-sender@example.test');await user.goto(base+'/posts/new');await ready(user);
  await user.locator('#post-form').evaluate(form=>{for(const name of ['prefectureIds','partIds','genreIds','stanceIds']){const input=form.querySelector(`[name=${name}]`);input.checked=true;input.dispatchEvent(new Event('change',{bubbles:true}));}});
  await user.locator('[data-editor-next]').click();await user.locator('#title').fill('PW-I-DUP-'+stamp.slice(-10));await user.locator('#content').fill('二重送信を防止する募集本文 '+stamp);await user.locator('[name=ageRanges][value=ANY]').check();await user.locator('[data-editor-next]').click();
  results.post=await twice(user,'#post-form','/api/posts');

  await user.goto(base+'/messages/930001');await ready(user);await user.locator('#message-content').fill('PW-I-DM-DUP-'+stamp);
  results.dm=await twice(user,'[data-message-form]','/api/messages?recipientId=');await user.evaluate(()=>dispatchEvent(new Event('pagehide')));

  await user.goto(base+'/users/910008');await ready(user);await user.locator('#report-user').click();await user.locator('#reason').fill('PW-I-REPORT-DUP-'+stamp);
  results.report=await twice(user,'#dialog form','/api/reports');

  await user.goto(base+'/contact');await ready(user);await user.locator('#feedback-message').fill('PW-I-FEEDBACK-DUP-'+stamp);await user.locator('#feedback-image').setInputFiles('C:/Users/parus/Desktop/band/target/pw-i-uploads/qa-valid.png');
  results.feedback=await twice(user,'#feedback-form','/api/feedback/contact');
  results.feedback.uploadRequests=results.feedback.seen.filter(x=>x.includes('/api/feedback/images')).length;

  results.pass=Object.entries(results).filter(([k])=>['register','post','dm','report','feedback'].includes(k)).every(([,v])=>v.count===1&&v.state.busy&&v.state.disabled);
  for(const c of contexts)await c.close();return results;
}
