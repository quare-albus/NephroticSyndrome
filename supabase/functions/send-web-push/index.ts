import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2";
import webpush from "npm:web-push@3.6.7";

const cors={ "Access-Control-Allow-Origin":"https://quare-albus.github.io", "Access-Control-Allow-Headers":"authorization, apikey, content-type, x-client-info", "Access-Control-Allow-Methods":"POST, OPTIONS", "Content-Type":"application/json" };
const json=(body:unknown,status=200)=>new Response(JSON.stringify(body),{status,headers:cors});
const url=Deno.env.get("SUPABASE_URL")!;
const serviceKey=Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const admin=createClient(url,serviceKey,{auth:{persistSession:false,autoRefreshToken:false}});
const vapidPublic=Deno.env.get("VAPID_PUBLIC_KEY")!;
const vapidPrivate=Deno.env.get("VAPID_PRIVATE_KEY")!;
webpush.setVapidDetails("https://quare-albus.github.io/NephroticSyndrome/",vapidPublic,vapidPrivate);

Deno.serve(async(req)=>{
 if(req.method==="OPTIONS")return new Response("ok",{headers:cors});
 if(req.method!=="POST")return json({error:"Method not allowed"},405);
 try{
  const token=(req.headers.get("Authorization")||"").replace(/^Bearer\s+/i,"");
  if(!token)return json({error:"Missing user session"},401);
  const{data:{user},error:authError}=await admin.auth.getUser(token);
  if(authError||!user)return json({error:"Invalid user session"},401);
  const body=await req.json();const notificationId=Number(body.notification_id);
  if(!Number.isSafeInteger(notificationId)||notificationId<=0)return json({error:"Invalid notification_id"},400);
  const{data:notification,error:nerr}=await admin.from("notifications").select("id,doctor_id,patient_id,title,message,type,appointment_at").eq("id",notificationId).single();
  if(nerr||!notification)return json({error:"Notification not found"},404);
  if(notification.doctor_id!==user.id)return json({error:"Only the sending doctor may trigger delivery"},403);
  const{data:subs,error:serr}=await admin.from("push_subscriptions").select("id,endpoint,subscription").eq("patient_id",notification.patient_id);
  if(serr)return json({error:"Could not load subscriptions"},500);
  if(!subs?.length)return json({sent:0,expired:0,reason:"Patient has no push subscription"});
  const payload=JSON.stringify({title:notification.title||"Message from your doctor",body:notification.message,url:"/NephroticSyndrome/dashboard/",tag:"nephrotrack-notification-"+notification.id});
  let sent=0,expired=0;
  for(const sub of subs){
   try{await webpush.sendNotification(sub.subscription,payload);sent++}
   catch(e){const status=Number(e?.statusCode||0);if(status===404||status===410){expired++;await admin.from("push_subscriptions").delete().eq("id",sub.id)}else console.error("Push delivery failed",status,String(e))}
  }
  return json({sent,expired});
 }catch(e){console.error("send-web-push error",e);return json({error:"Push delivery failed"},500)}
});
