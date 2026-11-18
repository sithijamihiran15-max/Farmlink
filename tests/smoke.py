#!/usr/bin/env python3
"""End-to-end API smoke tests. Start ./run.sh then: python3 tests/smoke.py
Uses only the Python standard library; creates separate test users and harvests.
"""
import http.cookiejar
import json
import os
import random
import urllib.error
import urllib.request

BASE = os.getenv('FARMLINK_TEST_URL', 'http://127.0.0.1:8080')

def client():
    cookies = http.cookiejar.CookieJar()
    return urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cookies))

def api(opener, path, method='GET', data=None, status=200):
    body = json.dumps(data).encode() if data is not None else None
    headers = {'Content-Type': 'application/json'} if body else {}
    req = urllib.request.Request(BASE + '/api' + path, data=body, method=method, headers=headers)
    try:
        with opener.open(req, timeout=10) as response:
            assert response.status == status, f'{path}: HTTP {response.status} != {status}'
            return json.load(response)
    except urllib.error.HTTPError as e:
        result = json.loads(e.read().decode())
        if e.code != status:
            raise AssertionError(f'{method} {path} returned {e.code}, expected {status}: {result}') from e
        return result

def main():
    visitor, farmer, buyer, stranger = [client() for _ in range(4)]
    assert api(visitor, '/health')['status'] == 'ok'
    assert len(api(visitor, '/harvests')['harvests']) >= 9
    assert api(visitor, '/profile', status=401)['error']
    # Editable HTML/CSS/JS are served directly by the Java application.
    for asset in ('/index.html','/explore.html','/css/style.css','/css/responsive-fixes.css','/js/app.js'):
        assert urllib.request.urlopen(BASE+asset,timeout=10).status == 200, asset
    unique = str(random.randrange(100_000_000, 999_999_999))
    def person(role, name, nic):
        return dict(role=role, firstName=name, lastName='Tester', nic=nic, email='', phone='0771234567',
                    district='Kandy', province='Central', address='1 Farm Lane', password='ExamplePass123!', confirmPassword='ExamplePass123!')
    f = api(farmer, '/auth/register', 'POST', person('FARMER', 'Florence', 'F'+unique), 201)['user']
    b = api(buyer, '/auth/register', 'POST', person('BUYER', 'Benjamin', 'B'+unique), 201)['user']
    other = api(stranger, '/auth/register', 'POST', person('BUYER', 'Caroline', 'C'+unique), 201)['user']
    assert f['role'] == 'FARMER' and b['role'] == 'BUYER' and other['id'] != b['id']
    assert api(visitor, '/auth/login', 'POST', {'nic':'B'+unique, 'password':'bad'},401)['error']
    assert api(farmer, '/auth/me')['user']['id'] == f['id']
    assert api(buyer, '/auth/me')['user']['id'] == b['id']
    assert api(buyer, '/harvests', 'POST', {},403)['error']
    product = dict(title='Quality Pumpkins '+unique, category='Vegetables', district='Kandy',price=175,
                   quantity=140, unit='kg',status='active',description='Beautiful pumpkins raised in Kandy for local buyers.',image='')
    product['image'] = ('data:image/png;base64,'
                        'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jB4kAAAAASUVORK5CYII=')
    created = api(farmer, '/harvests', 'POST', product,201)['harvest']
    hid = created['id'];assert created['farmerId'] == f['id'] and created['image'].startswith('data:image/png;base64,')
    assert api(visitor, f'/harvests/{hid}')['harvest']['title'] == product['title']
    assert any(h['id'] == hid for h in api(visitor, '/harvests?search=Pumpkins&district=Kandy')['harvests'])
    assert api(buyer, f'/harvests/{hid}', 'PUT', product,403)['error']
    product['price'] = 200
    assert api(farmer, f'/harvests/{hid}', 'PUT', product)['harvest']['price'] == 200
    assert api(buyer, f'/saved/{hid}', 'POST', {})['ok']
    assert api(buyer, f'/saved/{hid}', 'POST', {})['ok'] # idempotent favourite
    assert any(h['id']==hid for h in api(buyer, '/saved')['harvests'])
    assert api(farmer, '/saved', status=403)['error']
    q=api(buyer,'/requests','POST',{'harvestId':hid,'quantity':20,'message':'Can I collect next Thursday?'},201)['request']
    rid=q['id']; assert q['status']=='pending'
    assert api(client(),f'/requests/{rid}',status=401)['error']
    assert api(stranger,f'/requests/{rid}',status=403)['error']
    assert len(api(buyer,f'/requests/{rid}/messages')['messages']) == 1
    assert api(farmer,f'/requests/{rid}','PATCH',{'status':'accepted'})['request']['status']=='accepted'
    assert api(visitor, f'/harvests/{hid}')['harvest']['quantity'] == 120
    assert api(buyer,f'/requests/{rid}/messages','POST',{'text':'Thanks!'},201)['message']['text']=='Thanks!'
    assert len(api(farmer,f'/requests/{rid}/messages')['messages']) == 2
    assert api(buyer,f'/requests/{rid}','PATCH',{'status':'cancelled'})['request']['status']=='cancelled'
    assert api(visitor, f'/harvests/{hid}')['harvest']['quantity'] == 140
    r2=api(buyer,'/requests','POST',{'harvestId':hid,'quantity':10,'message':'Second order'},201)['request']
    assert api(farmer,f'/requests/{r2["id"]}','PATCH',{'status':'accepted'})['request']['status']=='accepted'
    assert api(farmer,f'/requests/{r2["id"]}','PATCH',{'status':'completed'})['request']['status']=='completed'
    assert api(buyer,f'/requests/{r2["id"]}','PATCH',{'status':'cancelled'},409)['error']
    assert api(farmer,'/dashboard')['completed'] == 1
    updated = dict(firstName='Benjamin',lastName='Tester',phone='0770001111',email='',address='1 Farm Lane',district='Kandy',province='Central')
    assert api(buyer,'/profile','PUT',updated)['user']['phone'] == '0770001111'
    assert api(farmer,f'/harvests/{hid}','DELETE')['ok']
    assert api(visitor,f'/harvests/{hid}',status=404)['error']
    assert api(farmer,'/harvests?farmerId='+str(f['id']))['harvests'][0]['status'] == 'inactive'
    assert api(buyer,f'/saved/{hid}','DELETE')['ok']
    assert api(buyer,'/auth/logout','POST',{})['ok']
    assert api(buyer,'/profile',status=401)['error']
    print('PASS: registration, authentication, roles, CRUD, search, favourites, requests, inventory, messaging, dashboard, profile, privacy, logout')

if __name__ == '__main__': main()
