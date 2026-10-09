#!/usr/bin/env python3
"""Exercise the local Compose auth flow through HTTP and real SMTP to Mailpit.
Creates one synthetic account. Prints only assertions, never credentials or tokens.
"""
import json,re,secrets,time,uuid
from urllib.request import Request,urlopen
from urllib.error import HTTPError,URLError
API='http://127.0.0.1:8081'
MAIL='http://127.0.0.1:8025'
def request(base,path,body=None,token=None):
    headers={'Content-Type':'application/json'}
    if token: headers['Authorization']='Bearer '+token
    req=Request(base+path,data=json.dumps(body).encode() if body is not None else None,headers=headers)
    try:
        with urlopen(req,timeout=5) as r:
            data=r.read(); return r.status,json.loads(data) if data and 'json' in r.headers.get('Content-Type','') else data.decode()
    except HTTPError as e:
        return e.code,None

def require(condition,label):
    if not condition: raise SystemExit('FAIL: '+label)
    print('PASS: '+label,flush=True)

deadline=time.monotonic()+90
while True:
    try:
        status,spec=request(API,'/v3/api-docs')
        if status==200 and isinstance(spec,dict) and spec.get('info',{}).get('title')=='HackTrain API': break
    except (URLError,TimeoutError,ConnectionError): pass
    if time.monotonic()>deadline: raise SystemExit('API did not become ready')
    time.sleep(1)
email='smoke-'+str(uuid.uuid4())+'@example.com'
password=secrets.token_urlsafe(24)
status,_=request(API,'/api/v1/auth/register',{'email':email,'password':password})
require(status==202,'registration accepted')
deadline=time.monotonic()+30
verification=None
while time.monotonic()<deadline:
    _,inbox=request(MAIL,'/api/v1/messages')
    for message in inbox.get('messages',[]):
        if any(recipient.get('Address')==email for recipient in message.get('To',[])):
            _,detail=request(MAIL,'/api/v1/message/'+message['ID'])
            match=re.search(r'#token=([A-Za-z0-9_-]{43})',detail.get('Text',''))
            if match: verification=match.group(1); break
    if verification: break
    time.sleep(1)
require(verification is not None,'encrypted outbox delivered verification over SMTP')
status,_=request(API,'/api/v1/auth/verify-email',{'token':verification})
require(status==204,'email verified')
status,tokens=request(API,'/api/v1/auth/login',{'email':email,'password':password})
require(status==200,'login returned tokens')
status,profile=request(API,'/api/v1/auth/me',token=tokens['accessToken'])
require(status==200 and profile['email']==email,'authenticated profile belongs to student')
status,next_tokens=request(API,'/api/v1/auth/refresh',{'token':tokens['refreshToken']})
require(status==200 and next_tokens['refreshToken']!=tokens['refreshToken'],'refresh rotated')
status,_=request(API,'/api/v1/auth/refresh',{'token':tokens['refreshToken']})
require(status==401,'replayed refresh rejected')
status,_=request(API,'/api/v1/auth/refresh',{'token':next_tokens['refreshToken']})
require(status==401,'replay revoked new family token')
status,spec=request(API,'/v3/api-docs')
require(status==200 and '/api/v1/auth/me' in spec['paths'],'OpenAPI served')
status,health=request('http://127.0.0.1:3000','/api/health')
require(status==200 and health.get('database')=='ok','Grafana health')

# Real organization and invitation delivery through the restricted runtime role.
status,org=request(API,'/api/v1/organizations',{'name':'Smoke academy '+str(uuid.uuid4())},tokens['accessToken'])
require(status==201,'organization created with restricted runtime role')
org_path='/api/v1/organizations/'+org['id']
status,group=request(API,org_path+'/groups',{'name':'Smoke group'},tokens['accessToken'])
require(status==201,'group persisted inside tenant')
student_email='smoke-student-'+str(uuid.uuid4())+'@example.com'
student_password=secrets.token_urlsafe(24)
status,_=request(API,'/api/v1/auth/register',{'email':student_email,'password':student_password})
require(status==202,'invited student registration accepted')
def mail_token(recipient,subject):
    deadline=time.monotonic()+30
    while time.monotonic()<deadline:
        _,inbox=request(MAIL,'/api/v1/messages')
        for message in inbox.get('messages',[]):
            if message.get('Subject')==subject and any(r.get('Address')==recipient for r in message.get('To',[])):
                _,detail=request(MAIL,'/api/v1/message/'+message['ID'])
                match=re.search(r'#token=([A-Za-z0-9_-]{43})',detail.get('Text',''))
                if match: return match.group(1)
        time.sleep(1)
    raise SystemExit('Expected test email did not arrive')
status,_=request(API,'/api/v1/auth/verify-email',{'token':mail_token(student_email,'HackTrain e-poçt təsdiqi')})
require(status==204,'invited student email verified')
status,student_tokens=request(API,'/api/v1/auth/login',{'email':student_email,'password':student_password})
require(status==200,'invited student authenticated')
status,_=request(API,org_path,token=student_tokens['accessToken'])
require(status==404,'nonmember cannot access tenant')
status,_=request(API,org_path+'/invitations',{'email':student_email,'role':'STUDENT'},tokens['accessToken'])
require(status==201,'organization invitation queued')
invitation_token=mail_token(student_email,'HackTrain təşkilat dəvəti')
status,member=request(API,'/api/v1/invitations/accept',{'token':invitation_token},student_tokens['accessToken'])
require(status==200 and member['organizationId']==org['id'],'email-bound invitation accepted')
# An empty JSON object is not needed for assignment POST; this route ignores body.
status,_=request(API,org_path+'/groups/'+group['id']+'/students/'+member['id'],{},tokens['accessToken'])
require(status==204,'student assigned to group')
status,visible=request(API,org_path+'/groups',token=student_tokens['accessToken'])
require(status==200 and visible['items'][0]['id']==group['id'],'student sees assigned group')
