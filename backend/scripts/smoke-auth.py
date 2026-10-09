#!/usr/bin/env python3
"""Exercise local Compose auth and organization flows without outbound email."""
import json,secrets,time,uuid
from urllib.request import Request,urlopen
from urllib.error import HTTPError,URLError

API='http://127.0.0.1:8081'

def request(path,body=None,token=None):
    headers={'Content-Type':'application/json'}
    if token: headers['Authorization']='Bearer '+token
    req=Request(API+path,data=json.dumps(body).encode() if body is not None else None,headers=headers)
    try:
        with urlopen(req,timeout=5) as r:
            data=r.read()
            return r.status,json.loads(data) if data and 'json' in r.headers.get('Content-Type','') else data.decode()
    except HTTPError as e:
        return e.code,None

def require(condition,label):
    if not condition: raise SystemExit('FAIL: '+label)
    print('PASS: '+label,flush=True)

deadline=time.monotonic()+90
while True:
    try:
        status,spec=request('/v3/api-docs')
        if status==200 and isinstance(spec,dict) and spec.get('info',{}).get('title')=='HackTrain API': break
    except (URLError,TimeoutError,ConnectionError):
        pass
    if time.monotonic()>deadline: raise SystemExit('API did not become ready')
    time.sleep(1)

email='smoke-'+str(uuid.uuid4())+'@example.com'
password=secrets.token_urlsafe(24)
status,_=request('/api/v1/auth/register',{'email':email,'password':password})
require(status==202,'registration accepted without email delivery')
status,tokens=request('/api/v1/auth/login',{'email':email,'password':password})
require(status==200,'login returned tokens')
status,profile=request('/api/v1/auth/me',token=tokens['accessToken'])
require(status==200 and profile['email']==email,'authenticated profile uses email account identifier')
status,next_tokens=request('/api/v1/auth/refresh',{'token':tokens['refreshToken']})
require(status==200 and next_tokens['refreshToken']!=tokens['refreshToken'],'refresh rotated')
status,_=request('/api/v1/auth/refresh',{'token':tokens['refreshToken']})
require(status==401,'replayed refresh rejected')
status,_=request('/api/v1/auth/refresh',{'token':next_tokens['refreshToken']})
require(status==401,'replay revoked new family token')
status,spec=request('/v3/api-docs')
require(status==200 and '/api/v1/auth/me' in spec['paths'],'OpenAPI served')
status=0
health=None
try:
    with urlopen('http://127.0.0.1:3000/api/health',timeout=5) as response:
        status=response.status
        health=json.loads(response.read())
except (HTTPError,URLError,TimeoutError):
    status=0
require(status==200 and health.get('database')=='ok','Grafana health')

# Invitations return a one-time token for the organization admin to share manually.
status,org=request('/api/v1/organizations',{'name':'Smoke academy '+str(uuid.uuid4())},tokens['accessToken'])
require(status==201,'organization created with restricted runtime role')
org_path='/api/v1/organizations/'+org['id']
status,group=request(org_path+'/groups',{'name':'Smoke group'},tokens['accessToken'])
require(status==201,'group persisted inside tenant')
student_email='smoke-student-'+str(uuid.uuid4())+'@example.com'
student_password=secrets.token_urlsafe(24)
status,_=request('/api/v1/auth/register',{'email':student_email,'password':student_password})
require(status==202,'invited student registration accepted')
status,student_tokens=request('/api/v1/auth/login',{'email':student_email,'password':student_password})
require(status==200,'invited student authenticated')
status,_=request(org_path,token=student_tokens['accessToken'])
require(status==404,'nonmember cannot access tenant')
status,invitation=request(org_path+'/invitations',{'email':student_email,'role':'STUDENT'},tokens['accessToken'])
require(status==201 and len(invitation['token'])==43,'organization invitation returns one-time sharing token')
status,member=request('/api/v1/invitations/accept',{'token':invitation['token']},student_tokens['accessToken'])
require(status==200 and member['organizationId']==org['id'],'email-bound invitation accepted')
status,_=request(org_path+'/groups/'+group['id']+'/students/'+member['id'],{},tokens['accessToken'])
require(status==204,'student assigned to group')
status,visible=request(org_path+'/groups',token=student_tokens['accessToken'])
require(status==200 and visible['items'][0]['id']==group['id'],'student sees assigned group')
