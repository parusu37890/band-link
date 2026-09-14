import csv
from pathlib import Path
from itertools import product

features = [
 ('AUTH','R-AUTH-01..09','Authentication',['normal','empty','min','max','over','invalid'],['anonymous','unverified','user','suspended','withdrawn','admin'],['new','duplicate','valid-token','expired-token','used-token'],['view','submit','retry','repeat']),
 ('PROF','R-PROF-01..06','Profile',['complete','empty','min','max','unknown-master','invalid-url'],['anonymous','user','suspended','withdrawn','admin'],['incomplete','complete','no-image','image','old-image'],['view','save','replace','delete','retry']),
 ('POST','R-POST-01..08','Recruitment-post',['recruit','join','empty-body','body-boundary','missing-choice','too-many-choice','zero-images','five-images','six-images'],['anonymous','unverified','user','suspended','withdrawn','admin','other-owner'],['published','closed','expired','edit-locked','owner','non-owner'],['list','detail','create','edit','close','republish','delete','double-submit']),
 ('SEARCH','R-SRCH-01..05','Search',['empty-keyword','title-hit','body-hit','area-hit','no-hit','max-length','huge-cursor','invalid-cursor'],['anonymous','user','suspended','withdrawn','admin'],['zero','one','many','tie','history','no-history'],['initial','search','filter','sort','load-more','retry','clear']),
 ('DM','R-MSG-01..07','Direct-message',['text','image','text-image','empty','text-boundary','fake-image','oversize-image'],['anonymous','unverified','sender','recipient','third-party','suspended','withdrawn','admin'],['new-conversation','existing','unread','read','mutual-block','one-way-block','reported'],['list','start','send','retry','read','expand','block','unblock','report']),
 ('IMG','R-IMG-01..04','Image-storage',['jpeg','png','webp','empty','exact-5mb','over-5mb','fake-mime','double-extension','truncated','path-traversal'],['anonymous','user','participant','third-party','admin'],['public','dm','feedback','missing'],['upload','read','delete','reload','direct-url']),
 ('REPORT','R-RPT-01..02','Report-admin',['post','user','dm','empty-reason','reason-boundary','unknown-id'],['anonymous','user','target','third-party','admin'],['open','confirmed','ignored','resolved','snapshot'],['submit','list','change-status','retry','id-tamper']),
 ('FEEDBACK','R-FBK-01..04','Feedback',['contact','feature','empty-body','body-boundary','jpeg','png','webp','no-image','oversize','invalid-url'],['anonymous','unverified','user','suspended','withdrawn','admin'],['new','existing','with-image','without-image'],['view','submit','retry','admin-view','image-view']),
 ('SEC','R-SEC-01..16','Security',['valid-csrf','missing-csrf','other-session','expired','idor','xss','huge-number','unknown-enum','log-injection'],['anonymous','unverified','user','suspended','withdrawn','admin','third-party'],['public','private','owner','other','deleted'],['get','post','put','delete','retry']),
 ('NFR','R-NFR-01..11','Non-functional',['desktop','mobile-375','keyboard','screen-reader','large-data','latency','offline','double-click','concurrent'],['anonymous','user','admin'],['empty','normal','large','boundary'],['view','operate','retry','recover'])]

fields = ['TestID','RequirementID','Layer','Priority','Feature','UserState','InputState','DataState','Operation','NetworkState','Purpose','Preconditions','TestData','Steps','Expected','DBCheck','APICheck','UICheck','Evidence','Cleanup','Triage','Status']
rows=[]; n=1
for code, req, name, inputs, users, data, ops in features:
    priority = 'P0' if code in {'AUTH','DM','IMG','SEC','REPORT'} else ('P1' if code in {'POST','PROF','FEEDBACK','SEARCH'} else 'P2')
    for u,i,d,o in product(users,inputs,data,ops):
        rows.append({'TestID':f'FULL-{n:05d}','RequirementID':req,'Layer':'unit+integration+system','Priority':priority,'Feature':name,'UserState':u,'InputState':i,'DataState':d,'Operation':o,'NetworkState':'normal|latency|disconnect|retry','Purpose':f'Verify {name} combination','Preconditions':'fixed users, isolated test DB, seeded data','TestData':f'{u};{i};{d};{o}','Steps':'seed -> operate by UI/API -> retry/reload -> compare DB/API/UI','Expected':'specified success or safe error; no leak, duplicate, or invalid transition','DBCheck':'rows, FK, UNIQUE, status, image URL, audit values','APICheck':'HTTP status, contract, authorization, error code, retry','UICheck':'display, validation, recovery, responsive, keyboard','Evidence':'JUnit XML, MockMvc log, Playwright trace/snapshot, SQL','Cleanup':'restore fixed seed and delete files','Triage':'classify UI/HTTP first, then server log, DB diff, minimal reproduction','Status':'NOT_RUN'}); n+=1
out=Path(__file__).resolve().parents[2]/'docs'/'test-plan'/'full-case-inventory.csv'
with out.open('w',newline='',encoding='utf-8-sig') as f: csv.DictWriter(f,fieldnames=fields).writeheader(); csv.DictWriter(f,fieldnames=fields).writerows(rows)
print(f'Generated {len(rows)} cases at {out}')
