import {api,h,icon,state,main,showPage,notice,button,toast,confirmAction,navigate,frequencies,ages,frequencyLabel,counter,verificationNotice} from './ui.js';

const groups = [
  ['prefectureIds','活動エリア','prefectures',3],
  ['partIds','パート','parts',5],
  ['genreIds','ジャンル','genres',3],
  ['stanceIds','活動スタンス','stances',1]
];
const labels = ['条件を選ぶ','募集を書く','確認して公開'];
const inputChoices = (name,items,selected=[],radio=false) => `<div class="editor-choices">${items.map(item=>{
  const [value,label]=Array.isArray(item)?item:[item.id,item.name];
  return `<label class="editor-choice"><input type="${radio?'radio':'checkbox'}" name="${h(name)}" value="${h(value)}" ${selected.map(String).includes(String(value))?'checked':''}><span>${h(label)}</span></label>`;
}).join('')}</div>`;

export async function postEditor(id) {
  const [m,p,images]=await Promise.all([
    state.masters?Promise.resolve(state.masters):api('/api/masters'),
    id?api('/api/posts/'+id):Promise.resolve(null),
    id?api('/api/posts/'+id+'/images'):Promise.resolve([])
  ]);
  state.masters=m;
  if(p&&String(p.userId)!==String(state.user.id))throw new Error('この募集を編集できるのは投稿者本人だけです。');
  const fieldGroup=([key,label,source,max])=>{
    const options=inputChoices(key,m[source],p?.[source]?.map(x=>x.id)||[],max===1);
    return `<fieldset class="editor-fieldset" data-selection="${key}" data-max="${max}"><legend>${label}<span class="required">必須</span></legend><p class="editor-selection-status" id="${key}-status" aria-live="polite"></p>${source==='prefectures'?`<details class="editor-area-options"><summary>都道府県を選ぶ・変更する</summary>${options}</details>`:options}</fieldset>`;
  };
  showPage(`<div class="page post-editor-page guided-editor"><a class="back-link" href="/my/posts">${icon('back')}自分の募集へ</a>
    <div class="page-heading"><div><h1>${id?'募集を編集する':'募集を掲載する'}</h1><p>条件を選び、内容を確認してから${id?'変更を保存':'公開'}します。</p></div></div>
    <nav class="editor-progress" aria-label="募集の入力手順"><ol>${labels.map((label,index)=>`<li><button type="button" data-editor-go="${index}" ${index===0?'aria-current="step"':''}><span class="editor-step-number" aria-hidden="true">${index+1}</span><span>${label}</span></button></li>`).join('')}</ol></nav>
    ${verificationNotice()}<form id="post-form" novalidate>
      <div class="form-error" tabindex="-1" role="alert" id="editor-error"></div>
      <section class="editor-step" data-editor-step="0" aria-labelledby="editor-heading-0">
        <div class="editor-step-heading"><h2 id="editor-heading-0" tabindex="-1">誰と、どこで活動しますか？</h2><p>検索に使われる条件です。合うものを選んでください。</p></div>
        <div class="editor-form-content">
          ${!id?`<fieldset class="editor-fieldset"><legend>募集の種類<span class="required">必須</span></legend>${inputChoices('type',[['MEMBER_WANTED','メンバーを募集したい'],['WANTS_TO_JOIN','バンドに参加したい']],['MEMBER_WANTED'],true)}</fieldset>`:`<p class="hint">募集の種類：${p.type==='WANTS_TO_JOIN'?'参加希望':'メンバー募集'}</p>`}
          ${fieldGroup(groups[0])}
          <div class="form-field"><label for="areaSub">市区町村・駅など<span class="optional">任意</span></label><input id="areaSub" name="areaSub" maxlength="100" value="${h(p?.areaSub)}" placeholder="例：下北沢、新宿周辺のスタジオ"><p class="hint">通える場所が伝わると、活動の相談がしやすくなります。</p></div>
          ${fieldGroup(groups[1])}<p class="hint" data-part-hint>メンバー募集では募集するパート、参加希望では自分が担当したいパートを選びます。</p>
          ${fieldGroup(groups[2])}${fieldGroup(groups[3])}
        </div>
      </section>
      <section class="editor-step" data-editor-step="1" aria-labelledby="editor-heading-1" hidden>
        <div class="editor-step-heading"><h2 id="editor-heading-1" tabindex="-1">やりたい音楽を伝えましょう</h2><p>好きな音楽や、どんな活動にしたいかを自分の言葉で。</p></div>
        <div class="editor-form-content">
          <div class="form-field"><label for="title">募集タイトル<span class="required">必須</span></label><input id="title" name="title" required maxlength="100" value="${h(p?.title)}" placeholder="例：下北沢で月2回、インディーロックのドラム募集"><span class="hint" data-count="title"></span></div>
          <div class="form-field"><label for="content">募集の本文<span class="required">必須</span></label><p class="hint" id="content-help">好きなアーティスト、現在のメンバー、練習の曜日、ライブの予定など。決まっていないことは、相談したいと書いても大丈夫です。</p><textarea id="content" name="content" required maxlength="2000" rows="9" aria-describedby="content-help" placeholder="どんな音楽を、どんな仲間とやってみたいですか？">${h(p?.content)}</textarea><span class="hint" data-count="content"></span></div>
          <div class="editor-extra-fields"><h3>活動のペースと希望</h3><div class="form-field"><label for="frequency">活動頻度</label><select id="frequency" name="activityFrequency">${frequencies.map(([value,label])=>`<option value="${value}" ${(p?.activityFrequency||'NEGOTIABLE')===value?'selected':''}>${h(label)}</option>`).join('')}</select></div><fieldset class="editor-fieldset"><legend>希望年齢層</legend>${inputChoices('ageRanges',ages,p?.ageRanges?.length?p.ageRanges:['ANY'])}</fieldset></div>
          <div class="editor-extra-fields"><h3>募集画像<span class="optional">任意</span></h3><p class="hint">演奏やバンドの雰囲気を伝える画像があれば追加できます。保存済みと合わせて5枚まで、1枚5MBまで。</p><label class="sr-only" for="images">募集画像を選択</label><input id="images" name="images" type="file" accept="image/jpeg,image/png,image/webp" multiple><p class="hint">jpg / png / webp。新しく選んだ画像は、最後の保存時に公開されます。</p><div id="image-selection" class="image-selection" aria-live="polite"></div><button type="button" class="button quiet small" data-clear-images hidden>選択した画像を取り消す</button><div id="saved-images" class="saved-images" aria-live="polite"></div></div>
        </div>
      </section>
      <section class="editor-step" data-editor-step="2" aria-labelledby="editor-heading-2" hidden>
        <div class="editor-step-heading"><h2 id="editor-heading-2" tabindex="-1">この内容で${id?'保存':'公開'}しますか？</h2><p>タイトル・本文・条件・画像を確認してください。各項目に戻って直せます。</p></div>
        <div id="editor-preview"></div>
        <p class="editor-publish-note">${id?'変更を保存した後':'公開した後'}12時間は、新規投稿・編集ができません。${id?'編集では掲載順位・掲載期限は変わりません。':'掲載期間は30日です。'}募集の終了はいつでもできます。</p>
      </section>
      <div class="editor-actions"><div><a class="button quiet" href="/my/posts">キャンセル</a><button type="button" class="button secondary" data-editor-back hidden>前に戻る</button></div><button type="button" class="button primary" data-editor-next>募集を書く ${icon('arrow')}</button><button type="submit" class="button primary" data-editor-submit hidden ${!state.user.emailVerified?'disabled':''}>${id?'変更を保存する':'募集を公開する'}</button></div>
    </form></div>`,id?'募集の編集':'募集の作成');

  const form=main.querySelector('#post-form');
  const errorBox=form.querySelector('#editor-error');
  const next=form.querySelector('[data-editor-next]');
  const back=form.querySelector('[data-editor-back]');
  const submit=form.querySelector('[data-editor-submit]');
  let step=0, saving=false, imageBusy=false, persistedId=null;
  let saved=Array.isArray(images)?[...images]:[];
  let imagePreviews=[];
  counter(form);
  const values=name=>[...form.querySelectorAll(`[name="${name}"]:checked`)].map(el=>el.value);
  const chosenNames=(name,source)=>values(name).map(value=>m[source].find(x=>String(x.id)===value)?.name).filter(Boolean).join('・');
  const clearError=()=>{errorBox.innerHTML='';};
  const error=(message,field)=>{
    errorBox.innerHTML=notice(message,'error');
    if(field){field.closest('details')?.setAttribute('open','');field.focus();field.scrollIntoView({block:'center'});}
    else errorBox.focus();
    return false;
  };
  function updateSelection() {
    groups.forEach(([name,,source,max])=>{
      const count=values(name).length;
      form.querySelector('#'+name+'-status').textContent=`${count} / ${max}${max===1?'つ選択':'つまで'}${count?'　'+chosenNames(name,source):''}`;
      form.querySelectorAll(`[name="${name}"]`).forEach(el=>el.setAttribute('aria-describedby',name+'-status'));
    });
  }
  function validate(index) {
    clearError();
    if(index===0){
      for(const [name,label,,max] of groups){
        const count=values(name).length;
        if(!count||count>max)return error(`${label}を1〜${max}つ選択してください。`,form.querySelector(`[name="${name}"]`));
      }
      if(!form.elements.areaSub.checkValidity())return error('市区町村・駅などは100文字以内で入力してください。',form.elements.areaSub);
    }
    if(index===1){
      for(const [name,label] of [['title','募集タイトル'],['content','募集の本文']]){
        const field=form.elements[name];
        if(!field.value.trim())return error(label+'を入力してください。',field);
        if(!field.checkValidity())return error(`${label}は${field.maxLength}文字以内で入力してください。`,field);
      }
      if(!values('ageRanges').length)return error('希望年齢層を選択してください。指定しない場合は「年齢不問」を選べます。',form.querySelector('[name="ageRanges"]'));
      const files=[...form.elements.images.files];
      if(saved.length+files.length>5)return error('画像は保存済みと合わせて5枚までです。選択を取り消すか、枚数を減らしてください。',form.elements.images);
      if(files.some(file=>file.size>5*1024*1024||!['image/jpeg','image/png','image/webp'].includes(file.type)))return error('画像はjpg / png / webp、1枚5MBまでです。選び直してください。',form.elements.images);
    }
    return true;
  }
  function preview() {
    const line=(label,text)=>text?`<div><dt>${h(label)}</dt><dd>${h(text)}</dd></div>`:'';
    form.querySelector('#editor-preview').innerHTML=`<article class="editor-preview-article"><div class="row spread"><span class="post-type">${(p?.type||values('type')[0])==='WANTS_TO_JOIN'?'参加希望':'メンバー募集'}</span><button type="button" class="button quiet small" data-editor-go="1">本文・画像を修正</button></div><h3>${h(form.elements.title.value.trim())}</h3><div class="body-text">${h(form.elements.content.value.trim())}</div>${saved.length+imagePreviews.length?`<div class="editor-preview-images">${saved.map((image,i)=>`<img src="${h(image.imageUrl)}" alt="保存済みの募集画像 ${i+1}枚目">`).join('')}${imagePreviews.map((url,i)=>`<img src="${h(url)}" alt="新しく追加する募集画像 ${i+1}枚目">`).join('')}</div>`:''}</article><section class="editor-preview-conditions"><div class="row spread"><h3>活動条件</h3><button type="button" class="button quiet small" data-editor-go="0">条件を修正</button></div><dl>${groups.map(([name,label,source])=>line(label,chosenNames(name,source))).join('')}${line('市区町村・駅など',form.elements.areaSub.value.trim())}${line('活動頻度',frequencyLabel(form.elements.activityFrequency.value))}${line('希望年齢層',values('ageRanges').map(v=>ages.find(x=>x[0]===v)?.[1]).filter(Boolean).join('・'))}</dl></section>`;
  }
  function setStep(index,focus=true) {
    step=index;
    form.querySelectorAll('[data-editor-step]').forEach(el=>el.hidden=Number(el.dataset.editorStep)!==step);
    main.querySelectorAll('.editor-progress [data-editor-go]').forEach(el=>{
      if(Number(el.dataset.editorGo)===step)el.setAttribute('aria-current','step');else el.removeAttribute('aria-current');
    });
    back.hidden=step===0;next.hidden=step===2;submit.hidden=step!==2;
    next.innerHTML=step===0?`募集を書く ${icon('arrow')}`:`内容を確認する ${icon('arrow')}`;
    if(step===2)preview();
    if(focus)form.querySelector('#editor-heading-'+step).focus();
  }
  function go(index) {
    if(saving||imageBusy||persistedId)return;
    clearError();
    if(index>step){for(let i=0;i<index;i++){setStep(i,false);if(!validate(i))return;}}
    setStep(index);
  }
  main.querySelector('.guided-editor').addEventListener('click',event=>{
    const target=event.target.closest('[data-editor-go]');
    if(target)go(Number(target.dataset.editorGo));
  });
  back.onclick=()=>go(step-1);next.onclick=()=>go(step+1);
  form.addEventListener('keydown',event=>{
    // Enter in a short field advances only after validation; publishing requires the final button.
    if(event.key==='Enter'&&!event.isComposing&&event.target.matches('input:not([type=file]):not([type=checkbox]):not([type=radio])')){
      event.preventDefault();if(step<2)go(step+1);
    }
  });
  form.addEventListener('change',event=>{
    const field=event.target;
    if(field.name==='ageRanges'&&field.checked){
      form.querySelectorAll('[name="ageRanges"]').forEach(other=>{if(other!==field&&(field.value==='ANY'||other.value==='ANY'))other.checked=false;});
    }
    const group=groups.find(x=>x[0]===field.name);
    if(group&&field.checked&&values(field.name).length>group[3]){
      field.checked=false;error(`${group[1]}は${group[3]}つまでです。変更する場合は、選択済みの項目を外してください。`,field);
    }else if(field.name!=='images')clearError();
    updateSelection();
  });
  const savedBox=form.querySelector('#saved-images');
  function renderSaved(focusId,direction) {
    savedBox.hidden=!saved.length;
    savedBox.innerHTML=saved.length?`<p class="form-label">保存済みの画像 <span class="optional">${saved.length}枚</span></p><p class="hint">ここでの削除・並べ替えはすぐに保存され、12時間の編集制限がかかります。編集中の本文は保存されません。左端が先頭の画像です。</p><ol class="saved-image-list">${saved.map((image,index)=>`<li><img src="${h(image.imageUrl)}" alt="保存済みの募集画像 ${index+1}枚目"><div class="saved-image-actions"><button type="button" class="button quiet small" data-move="${image.id}" data-dir="-1" ${index===0||imageBusy?'disabled':''} aria-label="${index+1}枚目を前へ">前へ</button><button type="button" class="button quiet small" data-move="${image.id}" data-dir="1" ${index===saved.length-1||imageBusy?'disabled':''} aria-label="${index+1}枚目を後ろへ">後ろへ</button><button type="button" class="button text-button" data-delete-image="${image.id}" ${imageBusy?'disabled':''} aria-label="${index+1}枚目を削除">削除</button></div></li>`).join('')}</ol>`:'';
    if(focusId){const target=savedBox.querySelector(`[data-move="${focusId}"][data-dir="${direction}"]:not(:disabled)`)||savedBox.querySelector(`[data-delete-image="${focusId}"]`);target?.focus();}
  }
  renderSaved();
  savedBox.addEventListener('click',async event=>{
    if(imageBusy||saving)return;
    const move=event.target.closest('[data-move]'),remove=event.target.closest('[data-delete-image]');
    if(move){
      const from=saved.findIndex(x=>String(x.id)===move.dataset.move),to=from+Number(move.dataset.dir);
      if(from<0||to<0||to>=saved.length)return;
      const ordered=[...saved];[ordered[from],ordered[to]]=[ordered[to],ordered[from]];
      imageBusy=true;renderSaved();
      try{await api(`/api/posts/${id}/images`,{method:'PATCH',body:ordered.map(x=>x.id)});saved=ordered;toast('画像の並び順を保存しました。');}
      catch(e){error(e.message);}
      finally{imageBusy=false;renderSaved(move.dataset.move,move.dataset.dir);}
    }
    if(remove)confirmAction('この画像を削除しますか？','削除はすぐに保存され、12時間の編集制限がかかります。編集中の本文は保存されません。画像の削除は元に戻せません。',async()=>{
      if(imageBusy||saving)throw new Error('保存処理が終わってから、もう一度お試しください。');
      imageBusy=true;
      try{await api(`/api/posts/${id}/images/${remove.dataset.deleteImage}`,{method:'DELETE'});saved=saved.filter(x=>String(x.id)!==remove.dataset.deleteImage);toast('画像を削除しました。');}
      finally{imageBusy=false;renderSaved();}
    });
  });
  const clearPreviews=()=>{imagePreviews.forEach(URL.revokeObjectURL);imagePreviews=[];};
  function renderSelectedImages() {
    clearPreviews();
    const files=[...form.elements.images.files];
    form.querySelector('#image-selection').innerHTML=files.slice(0,5).map(file=>{
      const url=URL.createObjectURL(file);imagePreviews.push(url);
      return `<figure><img src="${h(url)}" alt="新しく選んだ募集画像"><figcaption>${h(file.name)}</figcaption></figure>`;
    }).join('');
    form.querySelector('[data-clear-images]').hidden=!files.length;
    if(files.length+saved.length>5)error('画像は保存済みと合わせて5枚までです。選び直してください。');
  }
  form.elements.images.addEventListener('change',renderSelectedImages);
  form.querySelector('[data-clear-images]').onclick=()=>{form.elements.images.value='';renderSelectedImages();clearError();form.elements.images.focus();};
  window.addEventListener('pagehide',clearPreviews,{once:true});
  form.addEventListener('submit',async event=>{
    event.preventDefault();
    if(saving||persistedId||imageBusy)return;
    if(step!==2){go(step+1);return;}
    if(!state.user.emailVerified){error('公開・編集にはメールアドレスの確認が必要です。');return;}
    for(let i=0;i<2;i++){setStep(i,false);if(!validate(i))return;}
    setStep(2,false);
    const fd=new FormData(form);
    const body={title:fd.get('title').trim(),content:fd.get('content').trim(),areaSub:fd.get('areaSub').trim(),activityFrequency:fd.get('activityFrequency'),ageRanges:fd.getAll('ageRanges')};
    groups.forEach(([name])=>body[name]=fd.getAll(name).map(Number));
    if(!id)body.type=fd.get('type');
    const files=[...form.elements.images.files];
    saving=true;submit.disabled=true;submit.setAttribute('aria-busy','true');form.setAttribute('aria-busy','true');
    main.querySelectorAll('.editor-progress button, [data-editor-go], [data-editor-back]').forEach(el=>el.disabled=true);
    try{
      const result=await api('/api/posts'+(id?'/'+id:''),{method:id?'PUT':'POST',body});
      persistedId=result.id;
      if(files.length){const payload=new FormData();files.forEach(file=>payload.append('files',file));await api(`/api/posts/${persistedId}/images/batch`,{method:'POST',body:payload});}
      navigate('/posts/'+persistedId);
    }catch(e){
      if(persistedId){errorBox.innerHTML=notice('本文と条件は保存されましたが、追加画像の保存に失敗しました。'+e.message,'error')+button('保存された募集を確認する','/posts/'+persistedId,'secondary');errorBox.focus();}
      else error(e.message);
    }finally{
      saving=false;submit.disabled=Boolean(persistedId)||!state.user.emailVerified;submit.removeAttribute('aria-busy');form.removeAttribute('aria-busy');
      main.querySelectorAll('.editor-progress button, [data-editor-go], [data-editor-back]').forEach(el=>el.disabled=Boolean(persistedId));
    }
  });
  updateSelection();setStep(0,false);
}
