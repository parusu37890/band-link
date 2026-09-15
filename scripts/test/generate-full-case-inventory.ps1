$ErrorActionPreference='Stop'
$out=Join-Path $PSScriptRoot '..\..\docs\test-plan\full-case-inventory.csv'
$features=@(
 @{Code='AUTH';Req='R-AUTH-01..09';Name='Authentication';Inputs=@('normal','empty','min','max','over','invalid');Users=@('anonymous','unverified','user','suspended','withdrawn','admin');Data=@('new','duplicate','valid-token','expired-token','used-token');Ops=@('view','submit','retry','repeat')},
 @{Code='PROF';Req='R-PROF-01..06';Name='Profile';Inputs=@('complete','empty','min','max','unknown-master','invalid-url');Users=@('anonymous','user','suspended','withdrawn','admin');Data=@('incomplete','complete','no-image','image','old-image');Ops=@('view','save','replace','delete','retry')},
 @{Code='POST';Req='R-POST-01..08';Name='Recruitment-post';Inputs=@('recruit','join','empty-body','body-boundary','missing-choice','too-many-choice','zero-images','five-images','six-images');Users=@('anonymous','unverified','user','suspended','withdrawn','admin','other-owner');Data=@('posted-0-to-4-weeks','closed','expired','edit-locked','same-type-open','other-type-open');Ops=@('list','detail','create','edit','close','republish','delete','double-submit')},
 @{Code='SEARCH';Req='R-SRCH-01..05';Name='Search';Inputs=@('no-filter','single-filter','multi-value-or','cross-field-and','all-filters','no-hit','huge-cursor','invalid-cursor');Users=@('anonymous','user','suspended','withdrawn','admin');Data=@('zero','one','many','tie','history','no-history');Ops=@('initial','apply','change','sort','load-more','retry','clear')},
 @{Code='DM';Req='R-MSG-01..07';Name='Direct-message';Inputs=@('text','image','text-image','empty','text-boundary','fake-image','oversize-image');Users=@('anonymous','unverified','sender','recipient','third-party','suspended','withdrawn','admin');Data=@('new-conversation','existing','unread','read','mutual-block','one-way-block','reported');Ops=@('list','start','send','retry','read','expand','block','unblock','report')},
 @{Code='IMG';Req='R-IMG-01..04';Name='Image-storage';Inputs=@('jpeg','png','webp','empty','exact-5mb','over-5mb','fake-mime','double-extension','truncated','path-traversal');Users=@('anonymous','user','participant','third-party','admin');Data=@('public','dm','feedback','missing');Ops=@('upload','read','delete','reload','direct-url')},
 @{Code='REPORT';Req='R-RPT-01..02';Name='Report-admin';Inputs=@('post','user','dm','empty-reason','reason-boundary','unknown-id');Users=@('anonymous','user','target','third-party','admin');Data=@('open','confirmed','ignored','resolved','snapshot');Ops=@('submit','list','change-status','retry','id-tamper')},
 @{Code='FEEDBACK';Req='R-FBK-01..04';Name='Feedback';Inputs=@('contact','feature','empty-body','body-boundary','jpeg','png','webp','no-image','oversize','invalid-url');Users=@('anonymous','unverified','user','suspended','withdrawn','admin');Data=@('new','existing','with-image','without-image');Ops=@('view','submit','retry','admin-view','image-view')},
 @{Code='SEC';Req='R-SEC-01..16';Name='Security';Inputs=@('valid-csrf','missing-csrf','other-session','expired','idor','xss','huge-number','unknown-enum','log-injection');Users=@('anonymous','unverified','user','suspended','withdrawn','admin','third-party');Data=@('public','private','owner','other','deleted');Ops=@('get','post','put','delete','retry')},
 @{Code='NFR';Req='R-NFR-01..11';Name='Non-functional';Inputs=@('desktop','mobile-375','keyboard','screen-reader','large-data','latency','offline','double-click','concurrent');Users=@('anonymous','user','admin');Data=@('empty','normal','large','boundary');Ops=@('view','operate','retry','recover')}
)
$rows=@();$n=1
foreach($f in $features){
 foreach($u in $f.Users){
  foreach($i in $f.Inputs){
   foreach($d in $f.Data){
    foreach($o in $f.Ops){
     if($f.Code -in @('AUTH','DM','IMG','SEC','REPORT')){$p='P0'}elseif($f.Code -in @('POST','PROF','FEEDBACK','SEARCH')){$p='P1'}else{$p='P2'}
     $rows += [pscustomobject]@{TestID=('FULL-{0:D5}' -f $n);RequirementID=$f.Req;Layer='unit+integration+system';Priority=$p;Feature=$f.Name;UserState=$u;InputState=$i;DataState=$d;Operation=$o;NetworkState='normal|latency|disconnect|retry';Purpose="Verify $($f.Name) combination";Preconditions='fixed users, isolated test DB, seeded data';TestData="$u;$i;$d;$o";Steps='seed -> operate by UI/API -> retry/reload -> compare DB/API/UI';Expected='specified success or safe error; no leak, duplicate, or invalid transition';DBCheck='rows, FK, UNIQUE, status, image URL, audit values';APICheck='HTTP status, contract, authorization, error code, retry';UICheck='display, validation, recovery, responsive, keyboard';Evidence='JUnit XML, MockMvc log, Playwright trace/snapshot, SQL';Cleanup='restore fixed seed and delete files';Triage='classify UI/HTTP first, then server log, DB diff, minimal reproduction';Status='NOT_RUN'}
     $n++
    }
   }
  }
 }
}
$rows | Export-Csv -LiteralPath $out -NoTypeInformation -Encoding UTF8
Write-Output "Generated $($rows.Count) cases at $out"
