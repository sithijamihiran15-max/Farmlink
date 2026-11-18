/* FarmLink progressive enhancement: all editable markup remains in its original HTML files.
   No framework, bundler or build step required. Backend: Java /api/*. */
(() => {
  'use strict';
  const PAGE = location.pathname.split('/').pop() || 'index.html';
  const params = new URLSearchParams(location.search);
  const $ = (sel, root=document) => root.querySelector(sel);
  const $$ = (sel, root=document) => [...root.querySelectorAll(sel)];
  const fallbackImage = 'assets/harvest-illustration.svg';
  let currentUser = null;
  let savedIds = new Set();

  const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const photo = url => /^data:image\/(jpeg|png|webp);base64,[A-Za-z0-9+/=]+$/.test(url || '') ? url : fallbackImage;
  const money = n => 'Rs. ' + Number(n || 0).toLocaleString('en-LK', {maximumFractionDigits:2});
  const amount = n => Number(n || 0).toLocaleString('en-LK', {maximumFractionDigits:2});
  const dateText = str => {if (!str) return ''; let v = new Date(String(str).replace(' ','T'));return Number.isNaN(v.getTime()) ? String(str).slice(0,10) : v.toLocaleDateString('en-LK',{year:'numeric',month:'short',day:'numeric'});};
  const urlId = () => {let s=params.get('id'); return s !== null && /^\d+$/.test(s) ? Number(s) : 0;};
  const dashboardUrl = user => user.role === 'ADMIN' ? 'admin-dashboard.html' : (user.role === 'FARMER' ? 'farmer-dashboard.html' : 'buyer-dashboard.html');
  const showEmpty = text => `<div class="empty-state"><span class="empty-icon">🌱</span><h3>Nothing here yet</h3><p>${esc(text)}</p></div>`;
  function toast(message,error=false) {
    $('.toast')?.remove(); const node=document.createElement('div'); node.className='toast'+(error?' error':'');node.setAttribute('role','status');node.textContent=message;
    document.body.append(node);setTimeout(() => node.remove(),4200);
  }
  async function api(path,options={}) {
    const req={credentials:'same-origin',...options,headers:{...(options.body?{'Content-Type':'application/json'}:{}),...(options.headers||{})}};
    if (req.body && typeof req.body !== 'string') req.body=JSON.stringify(req.body);
    let response;
    try{response=await fetch('/api'+path,req);}catch{throw Error('Cannot reach the Java server. Start it with run.sh or run.bat.');}
    const data=await response.json().catch(()=>({}));
    if(!response.ok) throw Error(data.error || `HTTP error ${response.status}`);
    return data;
  }
  const protect = role => {
    if(!currentUser) { location.replace('login.html?next='+encodeURIComponent(PAGE+location.search));return false; }
    if (role && currentUser.role!==role) {location.replace(dashboardUrl(currentUser));return false;}
    return true;
  };
  const readForm = form => Object.fromEntries(new FormData(form).entries());
  function busy(button,active){if(!button)return;button.disabled=active;button.classList.toggle('is-busy',active);}
  function wireForm(form,handler){
    if(!form)return;
    form.addEventListener('submit',async e=>{e.preventDefault();if(!form.reportValidity())return;
      const submit=$('[type=submit]',form);busy(submit,true);
      try{await handler(form);}catch(err){toast(err.message,true);}finally{busy(submit,false);}
    });
  }
  function identity(){
    if(!currentUser)return;
    const name=esc(currentUser.firstName), initials=esc((currentUser.firstName[0]||'')+(currentUser.lastName[0]||''));
    const authNav=$('.auth-nav');
    if(authNav)authNav.innerHTML=`<a class="account-chip" href="${dashboardUrl(currentUser)}"><span class="avatar">${initials}</span><b>${name}</b></a><button class="btn btn-outline btn-small" data-action="logout" type="button">Log out</button>`;
    $$('.site-header a').forEach(a=>{
      if(a.textContent.trim()==='Dashboard')a.href=dashboardUrl(currentUser);
      if(a.textContent.trim()==='My profile')a.href=currentUser.role==='ADMIN'?'admin-dashboard.html':(currentUser.role==='FARMER'?'farmer-profile.html':'buyer-profile.html');
      if(a.textContent.trim()==='Switch account'){a.href='#';a.dataset.action='logout';a.textContent='Log out';}
    });
    // Keep the inherited mobile navigation useful for signed-in accounts too.
    const mobileNav=$('.static-mobile-menu nav');
    if(mobileNav){
      $$('a',mobileNav).forEach(link=>{
        if(link.textContent.trim()==='Log in'){link.textContent='Log out';link.href='#';link.dataset.action='logout';}
      });
      if(!$$('a',mobileNav).some(link=>link.textContent.trim()==='Dashboard')){
        const link=document.createElement('a');link.href=dashboardUrl(currentUser);link.textContent='Dashboard';mobileNav.append(link);
      }
    }
    // Some inherited dashboard headers contain an account chip outside .auth-nav.
    const chip=$('.site-header .account-chip');
    if(chip){chip.href=dashboardUrl(currentUser);const av=$('.avatar',chip),b=$('b',chip);if(av)av.textContent=initials;if(b)b.textContent=currentUser.firstName;}
    const greeting=$('.dash-greeting');
    if(greeting){const av=$('.avatar',greeting),b=$('b',greeting);if(av)av.textContent=initials;if(b)b.textContent=`Hi, ${currentUser.firstName}!`;}
    if(PAGE==='conversation.html' && currentUser.role==='FARMER'){
      const sidebar=$('.dash-sidebar');if(sidebar)sidebar.innerHTML=`<div class="dash-greeting"><span class="avatar">${initials}</span><div><b>Hi, ${name}!</b><small>Farmer account</small></div></div>
       <a class="dash-link" href="farmer-dashboard.html">⌂ Overview</a><a class="dash-link" href="farmer-listings.html">♧ My harvests</a><a class="dash-link active" href="farmer-requests.html">✉ Buyer requests</a><a class="dash-link" href="post-harvest.html">＋ Post harvest</a><a class="dash-link" href="farmer-profile.html">♙ My profile</a>`;
    }
  }
  function card(h){
    const saved=savedIds.has(h.id);
    return `<article class="listing-card" data-id="${h.id}">
      <a class="card-photo" href="harvest-details.html?id=${h.id}"><img src="${photo(h.image)}" alt="${esc(h.title)}" loading="lazy"/><span class="card-badge">${esc(h.category)}</span></a>
      <button type="button" class="save-btn ${saved?'is-saved':''}" data-action="save" data-id="${h.id}" aria-label="${saved?'Remove saved harvest':'Save harvest'}" aria-pressed="${saved}">${saved?'♥':'♡'}</button>
      <div class="card-info"><div class="category-label">${esc(h.category)}</div><h3><a title="${esc(h.title)}" href="harvest-details.html?id=${h.id}">${esc(h.title)}</a></h3>
      <div class="card-location">⌖ ${esc(h.district)} · ${esc(h.farmerName)}</div><div class="card-bottom"><div><div class="card-price">${money(h.price)} <small>/ ${esc(h.unit)}</small></div><div class="card-qty">${amount(h.quantity)} ${esc(h.unit)} available</div></div>
      <a class="card-arrow" aria-label="View ${esc(h.title)}" href="harvest-details.html?id=${h.id}">→</a></div></div></article>`;
  }
  function renderCards(target,items,none='No harvests match your search.'){
    if(target)target.innerHTML=items.length?items.map(card).join(''):showEmpty(none);
  }
  async function loadSavedIds(){if(currentUser?.role==='BUYER'){
    try{savedIds=new Set((await api('/saved')).harvests.map(h=>h.id));}catch{savedIds=new Set();}
  }}
  async function home(){const list=(await api('/harvests')).harvests;renderCards($('.card-grid'),list.slice(0,4));}
  async function explore(){
    const refresh=async()=>{
      const filters=readForm($('#filters-form'));filters.search=$('#search-input')?.value.trim()||'';
      $$('.category-pill').forEach(p=>p.classList.toggle('selected',p.textContent.trim()===(filters.category||'All produce')));
      const qs=new URLSearchParams();Object.entries(filters).forEach(([k,v])=>{if(String(v).trim())qs.set(k,v);});
      const harvests=(await api('/harvests?'+qs.toString())).harvests;
      const sort=$('#sort-listings')?.value;
      if(sort==='price-low')harvests.sort((a,b)=>a.price-b.price);
      else if(sort==='price-high')harvests.sort((a,b)=>b.price-a.price);
      else if(sort==='quantity')harvests.sort((a,b)=>b.quantity-a.quantity);
      else harvests.sort((a,b)=>b.id-a.id);
      const title=$('.results-caption');if(title)title.textContent=`Showing ${harvests.length} available harvest${harvests.length===1?'':'s'}`;
      renderCards($('.market-cards'),harvests);
    };
    wireForm($('#filters-form'),refresh);wireForm($('#search-form'),refresh);
    $$('#filters-form button').forEach(b=>{
      if(/clear/i.test(b.textContent))b.addEventListener('click',()=>{$('#filters-form').reset();$('#search-input').value='';$$('.category-pill').forEach(p=>p.classList.toggle('selected',p.textContent.trim()==='All produce'));refresh().catch(e=>toast(e.message,true));});
    });
    $$('.category-pill').forEach(b=>b.addEventListener('click',()=>{
      $$('.category-pill').forEach(p=>p.classList.remove('selected'));b.classList.add('selected');
      const name=b.textContent.trim();$('#filter-category').value=name==='All produce'?'':name;refresh().catch(e=>toast(e.message,true));
    }));
    $('#sort-listings')?.addEventListener('change',()=>refresh().catch(e=>toast(e.message,true)));
    await refresh();
  }
  function populateDetail(h){
    const image=$('.detail-image img');if(image){image.src=photo(h.image);image.alt=h.title;}
    const e=$('.detail-info');if(!e)return;
    const set=(s,value)=>{const node=$(s,e);if(node)node.textContent=value;};
    set('.eyebrow',h.category+' · Fresh harvest');set('.detail-title',h.title);
    const location=$('.detail-location',e);if(location)location.textContent='⌖ Grown in '+h.district+', Sri Lanka';
    set('.detail-pricing strong',money(h.price));set('.detail-pricing small','/ '+h.unit);
    set('.detail-availability','✓ '+amount(h.quantity)+' '+h.unit+' available');
    const description=$('.detail-info > p',document);if(description && description.previousElementSibling?.tagName==='H3')description.textContent=h.description;
    const fields=$$('.detail-meta b',e);[h.category,amount(h.quantity)+' '+h.unit,h.district,dateText(h.createdAt)].forEach((v,i)=>{if(fields[i])fields[i].textContent=v;});
    const farmer=$('.farmer-strip');if(farmer){const b=$('b',farmer),small=$('small',farmer),av=$('.avatar',farmer);
      if(b)b.textContent=h.farmerName;if(small)small.textContent='Farmer · '+h.district;
      if(av)av.textContent=h.farmerName.split(' ').map(w=>w[0]).slice(0,2).join('');}
    const request=$('.detail-actions .btn-primary');if(request){request.href='request-harvest.html?id='+h.id;}
    const save=$('.detail-actions .btn-outline');if(save){save.href='#';save.dataset.action='save';save.dataset.id=h.id;save.textContent=savedIds.has(h.id)?'♥ Saved harvest':'♡ Save harvest';}
    document.title=`${h.title} — FarmLink`;
    const crumb=$('.breadcrumb');if(crumb){const last=$('span:last-child',crumb);if(last)last.textContent=h.title;}
    if(h.status!=='active'||h.quantity<=0){if(request){request.removeAttribute('href');request.classList.add('disabled');request.setAttribute('aria-disabled','true');request.textContent='Currently unavailable';}}
  }
  async function detail(requestMode=false){
    // First listing is the default when opening the old bookmark without an ID.
    let id=urlId();if(!params.has('id')){const items=(await api('/harvests')).harvests;id=items[0]?.id??0;}
    const h=(await api('/harvests/'+id)).harvest;populateDetail(h);
    if(!requestMode)return;
    if(!protect('BUYER'))return;
    const form=$('#inquiry-form');const qty=$('#request-quantity');const total=$('#request-total');
    if(!form||!qty)return;
    qty.max=h.quantity;qty.step='any';
    const qtyLabel=$('#inquiry-form .form-label');if(qtyLabel)qtyLabel.firstChild.textContent='Quantity needed ('+h.unit+') ';
    const summary=$('.modal-panel');
    if(summary){
      const productName=$('.mini-product b',summary)||$('h4',summary)||$('.request-product b',summary);
      if(productName)productName.textContent=h.title;
      const productImage=$('img',summary);if(productImage){productImage.src=photo(h.image);productImage.alt=h.title;}
      const productMeta=$('.mini-product small',summary)||$('.request-product small',summary);
      if(productMeta)productMeta.textContent=money(h.price)+' per '+h.unit+' · '+h.district;
    }
    const calc=()=>{if(total)total.textContent=money((Number(qty.value)||0)*h.price);};qty.addEventListener('input',calc);calc();
    const cancel=$('.form-actions .btn-outline',form);if(cancel){cancel.addEventListener('click',()=>{location.href='harvest-details.html?id='+h.id;});}
    wireForm(form,async f=>{
      const d=readForm(f);const output=await api('/requests',{method:'POST',body:{harvestId:h.id,quantity:Number(d.quantity),message:d.message}});
      toast('Request sent! You can now message the farmer.');location.href='conversation.html?id='+output.request.id;
    });
    const dismiss=$('.modal-close');dismiss?.addEventListener('click',()=>{location.href='harvest-details.html?id='+h.id;});
  }
  async function authPage(role){
    const form=$('#signup-form');
    wireForm(form,async f=>{
      const d=readForm(f);d.role=role;
      if(d.password!==d.confirmPassword)throw Error('Passwords do not match');
      await api('/auth/register',{method:'POST',body:d});location.href='registration-success.html';
    });
  }
  async function login(){
    wireForm($('#login-form'),async f=>{
      const data=await api('/auth/login',{method:'POST',body:readForm(f)});
      const next=params.get('next');
      const allowed=next && /^(?:[a-z-]+\.html)(?:\?[^#]*)?$/.test(next) && !next.startsWith('login.html');
      location.href=allowed?next:dashboardUrl(data.user);
    });
  }
  function mini(h){return `<div class="mini-product"><img src="${photo(h.image)}" alt="${esc(h.title)}" loading="lazy"/><div><b>${esc(h.title)}</b><small>${money(h.price)}/${esc(h.unit)} · ${esc(h.district)}</small><a class="btn btn-outline btn-small" href="harvest-details.html?id=${h.id}">View harvest</a></div></div>`;}
  function requestCard(r){
    const isFarmer=currentUser?.role==='FARMER';
    const status=esc(r.status), name=isFarmer?r.buyerName:r.farmerName;
    let actions='';
    if(isFarmer && r.status==='pending')actions=`<button type="button" class="btn btn-primary btn-small" data-action="status" data-id="${r.id}" data-status="accepted">Accept</button><button type="button" class="btn btn-danger btn-small" data-action="status" data-id="${r.id}" data-status="declined">Decline</button>`;
    if(isFarmer && r.status==='accepted')actions=`<button type="button" class="btn btn-primary btn-small" data-action="status" data-id="${r.id}" data-status="completed">Mark completed</button>`;
    if(!isFarmer && ['pending','accepted'].includes(r.status))actions=`<button type="button" class="btn btn-danger btn-small" data-action="status" data-id="${r.id}" data-status="cancelled">Cancel request</button>`;
    return `<div class="inquiry-card"><img src="${photo(r.image)}" alt="${esc(r.title)}" loading="lazy"/><div><h4>${esc(r.title)}</h4><p>${isFarmer?'Buyer':'Farmer'}: <strong>${esc(name)}</strong></p>
      <p>${amount(r.quantity)} ${esc(r.unit)} · ${money(r.price*r.quantity)} estimated · ${esc(dateText(r.createdAt))}</p><span class="status-pill ${status}">${status}</span></div>
      <div class="inquiry-actions"><a class="btn btn-outline btn-small" href="conversation.html?id=${r.id}">✉ Message</a>${actions}</div></div>`;
  }
  function renderReqCards(container,requests,emptyText='No requests yet.'){if(container)container.innerHTML=requests.length?requests.map(requestCard).join(''):showEmpty(emptyText);}
  async function inbox(){
    if(!protect(PAGE.startsWith('farmer-')?'FARMER':'BUYER'))return;
    const req=(await api('/requests')).requests;
    const container=$('.dash-card');if(!container)return;
    const label=$('.dash-card-head .muted',container);if(label)label.textContent=req.length+' total';
    $$('.inquiry-card',container).forEach(e=>e.remove());
    const content=document.createElement('div');content.className='inquiry-list';content.innerHTML=req.length?req.map(requestCard).join(''):showEmpty('When you send or receive a request, it will appear here.');container.append(content);
  }
  async function dashboard(){
    if(!protect(PAGE.startsWith('farmer-')?'FARMER':'BUYER'))return;
    const d=await api('/dashboard');const values=$$('.stats-grid .stat-value');
    [d.mainCount,d.totalRequests,d.pending,d.completed].forEach((n,i)=>{if(values[i])values[i].textContent=n;});
    const head=$('.dash-head h1');if(head)head.textContent='Welcome back, '+currentUser.firstName+' 👋';
    const candidates=d.harvests.filter(h=>h.status==='active').slice(0,4);
    const list=$('.dashboard-listings');if(list)list.innerHTML=candidates.length?candidates.map(mini).join(''):showEmpty('Browse the market or post your first harvest.');
    const inquiries=$$('.dash-card').find(el=>!!$('.inquiry-card',el));
    if(inquiries){$$('.inquiry-card',inquiries).forEach(e=>e.remove());const slot=document.createElement('div');slot.className='inquiry-list';slot.innerHTML=d.requests.length?d.requests.slice(0,3).map(requestCard).join(''):showEmpty('No requests yet.');inquiries.append(slot);}
  }
  async function adminDashboard(){
    if(!protect('ADMIN'))return;
    const [summary, accounts, listings]=await Promise.all([
      api('/admin/overview'),api('/admin/users'),api('/admin/harvests')
    ]);
    const o=summary.overview;
    $('#admin-stats').innerHTML=[
      ['Farmers',o.farmers],['Buyers',o.buyers],['Administrators',o.admins],
      ['Active harvests',o.activeHarvests],['All harvests',o.harvests],['Buyer requests',o.requests]
    ].map(([name,value])=>`<div class="admin-stat"><span>${esc(name)}</span><strong>${esc(value)}</strong></div>`).join('');
    const userRows=accounts.users.map(u=>`<tr><td>${esc(u.id)}</td><td>${esc(u.name)}</td><td>${esc(u.nic)}</td><td>${esc(u.role)}</td><td>${esc(u.district)}</td><td>${u.role==='ADMIN'?'Protected':`<button type="button" class="btn btn-outline btn-small admin-delete-btn" data-action="admin-delete-user" data-id="${esc(u.id)}" aria-label="Delete ${esc(u.name)} account">Delete</button>`}</td></tr>`).join('');
    $('#admin-users').innerHTML=userRows || '<tr><td colspan="6">No users found.</td></tr>';
    const harvestRows=listings.harvests.map(h=>`<tr><td>${esc(h.id)}</td><td>${esc(h.title)}</td><td>${esc(h.farmerName)}</td><td>${esc(h.status)}</td><td>${h.status==='active'?`<button type="button" class="btn btn-outline btn-small" data-action="admin-unlist" data-id="${esc(h.id)}">Unlist</button>`:'—'}</td></tr>`).join('');
    $('#admin-harvests').innerHTML=harvestRows || '<tr><td colspan="5">No harvests found.</td></tr>';
  }
  async function listings(){
    if(!protect('FARMER'))return;
    const list=(await api('/harvests?farmerId='+currentUser.id)).harvests;
    const body=$('.dash-table tbody');if(!body)return;
    body.innerHTML=list.length?list.map(h=>`<tr><td><div class="table-product"><img src="${photo(h.image)}" alt=""/><b>${esc(h.title)}</b></div></td>
      <td>${money(h.price)}/${esc(h.unit)}</td><td>${amount(h.quantity)} ${esc(h.unit)}</td><td><span class="status-pill ${esc(h.status)}">${esc(h.status)}</span></td>
      <td><div class="table-actions"><a class="btn btn-small btn-outline" href="edit-harvest.html?id=${h.id}">Edit</a>
      ${h.status==='active'?`<button class="btn btn-small btn-danger" type="button" data-action="unlist" data-id="${h.id}">Unlist</button>`:''}</div></td></tr>`).join(''):'<tr><td colspan="5">No harvests yet. <a href="post-harvest.html">Post your first harvest</a>.</td></tr>';
  }
  const fields=['title','category','district','price','quantity','unit','status','description'];
  // Decode local photos as data: URLs, which our strict Content-Security-Policy permits.
  // The original blob: preview URL was blocked by the Java server's img-src policy.
  // Photos are compressed before sending JSON, so the existing database needs no migration.
  const fileToImage=async file=>{
    if(!file)return null;
    const sizeLimit=10*1024*1024;
    if(file.size>sizeLimit)throw Error('Photo is over 10 MB. Please choose a smaller JPEG, PNG or WebP photo.');
    if(!['image/png','image/jpeg','image/webp'].includes(file.type))
      throw Error('Please select a JPEG, PNG or WebP photo. For iPhone HEIC/HEIF photos, export as JPEG first.');
    const source=await new Promise((resolve,reject)=>{
      const reader=new FileReader();
      reader.onerror=()=>reject(Error('Could not open the selected photo. Try saving it locally as JPEG or PNG.'));
      reader.onload=()=>resolve(String(reader.result||''));
      reader.readAsDataURL(file);
    });
    const image=new Image();
    await new Promise((resolve,reject)=>{
      image.onload=resolve;
      image.onerror=()=>reject(Error('This photo cannot be decoded. Try exporting it as JPEG or PNG.'));
      image.src=source;
    });
    if(!image.naturalWidth||!image.naturalHeight)throw Error('The selected photo has invalid dimensions.');
    const canvas=document.createElement('canvas');
    const context=canvas.getContext('2d');
    if(!context)throw Error('Your browser cannot process photos. Try Chrome, Firefox or Edge.');
    // The backend accepts at most 300,000 characters of base64 image data.
    // Resize as needed rather than failing for normal camera photos.
    for(const maxEdge of [1200,1000,850,700,550,420]){
      const scale=Math.min(1,maxEdge/Math.max(image.naturalWidth,image.naturalHeight));
      canvas.width=Math.max(1,Math.round(image.naturalWidth*scale));
      canvas.height=Math.max(1,Math.round(image.naturalHeight*scale));
      context.drawImage(image,0,0,canvas.width,canvas.height);
      for(const quality of [0.82,0.68,0.54,0.4]){
        const encoded=canvas.toDataURL('image/jpeg',quality);
        if(encoded.startsWith('data:image/jpeg;base64,')&&encoded.length<290000)return encoded;
      }
    }
    throw Error('The photo could not be compressed. Please try a different JPEG or PNG photo.');
  };
  async function editor(isEdit){
    if(!protect('FARMER'))return;
    const form=$('#listing-form');let existing=null;
    if(isEdit){existing=(await api('/harvests/'+urlId())).harvest;
      if(existing.farmerId!==currentUser.id)throw Error('This listing belongs to another farmer');
      for(const k of fields){const control=form.elements.namedItem(k);if(control)control.value=existing[k];}
      if(existing.image){$('#photo-preview').innerHTML=`<img class="edit-preview" src="${photo(existing.image)}" alt="Current harvest photo"/>`;}
    }
    const file=$('#listing-photo');file?.addEventListener('change',async()=>{
      try {const img=await fileToImage(file.files[0]);const slot=$('#photo-preview');if(img && slot)slot.innerHTML=`<img class="edit-preview" src="${img}" alt="New harvest photo"/>`;}
      catch(err){toast(err.message,true);file.value='';}
    });
    wireForm(form,async f=>{
      const d=readForm(f);d.image=existing?.image||'';
      if(file?.files[0])d.image=await fileToImage(file.files[0]);
      delete d.photo;
      await api(isEdit?'/harvests/'+existing.id:'/harvests',{method:isEdit?'PUT':'POST',body:d});
      toast(isEdit?'Harvest updated':'Harvest published');location.href='farmer-listings.html';
    });
  }
  async function profile(){
    if(!protect(PAGE.startsWith('farmer-')?'FARMER':'BUYER'))return;
    const form=$('#profile-form');
    for(const [key,val] of Object.entries(currentUser)){const el=form?.elements.namedItem(key);if(el)el.value=val||'';}
    wireForm(form,async f=>{const result=await api('/profile',{method:'PUT',body:readForm(f)});currentUser=result.user;identity();toast('Profile saved successfully');});
  }
  async function saved(){if(!protect('BUYER'))return;const data=await api('/saved');renderCards($('.card-grid'),data.harvests,'Save a harvest in the marketplace and it will appear here.');}
  async function conversation(){
    if(!protect())return;
    let id=urlId();if(!params.has('id')){const reqs=(await api('/requests')).requests;id=reqs[0]?.id;if(id===undefined){location.replace(dashboardUrl(currentUser));return;}}
    const r=(await api('/requests/'+id)).request;
    const main=$('.dash-main');if(!main)return;
    const other=currentUser.role==='FARMER'?r.buyerName:r.farmerName;
    main.innerHTML=`<div class="dash-head"><div><span class="eyebrow">Private harvest conversation</span><h1>Chat with ${esc(other)}</h1>
      <p>${esc(r.title)} · ${amount(r.quantity)} ${esc(r.unit)} · ${money(r.quantity*r.price)} estimated</p></div>
      <a class="btn btn-outline" href="${currentUser.role==='FARMER'?'farmer-requests.html':'buyer-requests.html'}">← Back to requests</a></div>
      <div class="dash-card"><div class="dash-card-head"><h3>Messages</h3><span class="status-pill ${esc(r.status)}">${esc(r.status)}</span></div>
      <div class="message-thread" id="conversation-messages" aria-live="polite"></div>
      <form id="message-form"><label class="form-label" for="message-text">Your message</label><textarea id="message-text" class="field" rows="3" name="text" required maxlength="2000" placeholder="Ask about collection, price, or delivery..."></textarea>
      <div class="form-actions"><button class="btn btn-primary" type="submit">Send message</button><button class="btn btn-outline" type="button" data-action="refresh-messages">Refresh messages</button></div></form></div>`;
    const render=async()=>{const messages=(await api('/requests/'+id+'/messages')).messages;
      const thread=$('#conversation-messages');thread.innerHTML=messages.length?messages.map(m=>`<div class="bubble ${m.senderId===currentUser.id?'mine':''}"><b>${esc(m.senderName)}</b><div>${esc(m.text)}</div><small>${esc(dateText(m.createdAt))}</small></div>`).join(''):showEmpty('No messages yet. Say hello!');thread.scrollTop=thread.scrollHeight;};
    const form=$('#message-form');if(['declined','cancelled'].includes(r.status)){$('#message-text').disabled=true;$('[type=submit]',form).disabled=true;toast('This request is closed');}
    wireForm(form,async f=>{await api('/requests/'+id+'/messages',{method:'POST',body:readForm(f)});f.reset();await render();});
    $('[data-action=refresh-messages]')?.addEventListener('click',()=>render().catch(e=>toast(e.message,true)));
    await render();
  }
  function registrationSuccess(){if(!protect())return;
    const go=$('a[href="farmer-dashboard.html"]');if(go)go.href=dashboardUrl(currentUser);
    const title=$('h1');if(title)title.textContent='Welcome to FarmLink, '+currentUser.firstName+'!';
  }
  async function actionClick(event){
    const target=event.target.closest('[data-action]');if(!target)return;
    const action=target.dataset.action;event.preventDefault();if(target.disabled)return;
    try{
      if(action==='logout') {await api('/auth/logout',{method:'POST',body:{}});location.href='index.html';return;}
      if(action==='save') {
        if(!currentUser){location.href='login.html?next='+encodeURIComponent(PAGE+location.search);return;}
        if(currentUser.role!=='BUYER')throw Error('Log in with a buyer account to save harvests');
        const id=Number(target.dataset.id);const isSaved=savedIds.has(id);
        await api('/saved/'+id,{method:isSaved?'DELETE':'POST',body:isSaved?undefined:{}});
        if(isSaved)savedIds.delete(id);else savedIds.add(id);
        if(PAGE==='buyer-saved.html')await saved();
        else {$$(`[data-action="save"][data-id="${id}"]`).forEach(n=>{n.classList.toggle('is-saved',!isSaved);n.textContent=n.classList.contains('save-btn')?(!isSaved?'♥':'♡'):(!isSaved?'♥ Saved harvest':'♡ Save harvest');n.setAttribute('aria-pressed',String(!isSaved));});}
        toast(isSaved?'Removed from favourites':'Harvest saved');return;
      }
      if(action==='status'){
        const val=target.dataset.status;if(!confirm(`Change this request to "${val}"?`))return;
        await api('/requests/'+target.dataset.id,{method:'PATCH',body:{status:val}});
        toast('Request updated');if(PAGE.includes('dashboard'))await dashboard();else await inbox();return;
      }
      if(action==='admin-delete-user') {
        if(currentUser?.role!=='ADMIN')throw Error('Administrator access required');
        const id=Number(target.dataset.id);
        const details=(await api('/admin/users/'+id+'/delete-impact')).impact;
        const warning=`Permanently delete ${details.name} (${details.role}, NIC: ${details.nic})?\n\nThis will delete:\n`+
          `• ${details.harvests} harvest listing(s)\n• ${details.requests} request(s)\n`+
          `• ${details.messages} message(s)\n• ${details.saved} saved item(s)\n`+
          `• ${details.sessions} login session(s)\n\n`+
          'Some of these records may belong to other users. This cannot be undone.\n\nHave you backed up your database?';
        if(!confirm(warning))return;
        busy(target,true);
        try {
          await api('/admin/users/'+id,{method:'DELETE'});
          toast('Account permanently deleted');await adminDashboard();
        }finally {busy(target,false);}
        return;
      }
      if(action==='admin-unlist'){
        if(currentUser?.role!=='ADMIN')throw Error('Administrator access required');
        if(!confirm('Remove this harvest from the public marketplace?'))return;
        await api('/admin/harvests/'+target.dataset.id+'/unlist',{method:'POST',body:{}});
        toast('Harvest unlisted');await adminDashboard();return;
      }
      if(action==='unlist'){
        if(!confirm('Unlist this harvest? It will disappear from the marketplace, but you can edit it later.'))return;
        await api('/harvests/'+target.dataset.id,{method:'DELETE'});toast('Harvest unlisted');await listings();return;
      }
    }catch(err){toast(err.message,true);}
  }
  async function boot(){
    document.addEventListener('click',actionClick);
    try{currentUser=(await api('/auth/me')).user;}catch(err){toast(err.message,true);return;}
    $$('.ui-preview-hint').forEach(el=>el.remove());identity();
    await loadSavedIds();
    const routes={
      'index.html':home,'explore.html':explore,'harvest-details.html':()=>detail(false),'request-harvest.html':()=>detail(true),
      'register-farmer.html':()=>authPage('FARMER'),'register-buyer.html':()=>authPage('BUYER'),'login.html':login,
      'farmer-dashboard.html':dashboard,'buyer-dashboard.html':dashboard,'admin-dashboard.html':adminDashboard,
      'farmer-listings.html':listings,'post-harvest.html':()=>editor(false),'edit-harvest.html':()=>editor(true),
      'farmer-requests.html':inbox,'buyer-requests.html':inbox,'conversation.html':conversation,
      'farmer-profile.html':profile,'buyer-profile.html':profile,'buyer-saved.html':saved,'registration-success.html':registrationSuccess
    };
    try{await routes[PAGE]?.();}catch(err){toast(err.message,true);const main=$('.dash-main')||$('#app');
      if(main)main.insertAdjacentHTML('afterbegin',`<p class="ui-error" role="alert">${esc(err.message)}. <a href="explore.html">Return to the marketplace</a>.</p>`);}
  }
  document.addEventListener('DOMContentLoaded',boot);
})();
