---
---
const APP_BASE="/NephroticSyndrome/";
self.addEventListener("push",event=>{
 let data={title:"Message from your doctor",body:"You have a new notification in NephroTrack.",url:APP_BASE+"dashboard/"};
 try{if(event.data)data={...data,...event.data.json()}}catch{}
 event.waitUntil(self.registration.showNotification(data.title,{body:data.body,tag:data.tag||"nephrotrack-doctor-message",data:{url:data.url||APP_BASE+"dashboard/"},renotify:false}));
});
self.addEventListener("notificationclick",event=>{
 event.notification.close();const target=new URL(event.notification.data?.url||APP_BASE+"dashboard/",self.location.origin).href;
 event.waitUntil((async()=>{const clients=await self.clients.matchAll({type:"window",includeUncontrolled:true});for(const client of clients){if(client.url.startsWith(self.location.origin+APP_BASE)){await client.focus();client.navigate(target);return}}await self.clients.openWindow(target)})());
});
