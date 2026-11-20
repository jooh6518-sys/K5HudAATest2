(function(){
 'use strict';
 try{
 var copy=document.documentElement.cloneNode(true);
 copy.querySelectorAll('script,style,noscript,svg,canvas').forEach(function(e){e.remove()});
 copy.querySelectorAll('input,textarea').forEach(function(e){e.removeAttribute('value');e.value='';e.textContent=''});
 copy.querySelectorAll('*').forEach(function(e){Array.prototype.slice.call(e.attributes).forEach(function(a){
  if(/^on/i.test(a.name)||/^(href|src|srcset|poster|action|integrity)$/i.test(a.name)||/^data-/i.test(a.name)||/(token|secret|password|authorization|session|cookie|key)/i.test(a.name))e.removeAttribute(a.name);
 })});
 var html='<!DOCTYPE html>\n'+copy.outerHTML,maxSize=350000,clipped=html.length>maxSize;
 if(clipped)html=html.slice(0,maxSize)+'\n<!-- truncated after 350000 characters -->';
 return JSON.stringify({url:location.origin+location.pathname,html:html,truncated:clipped});
 }catch(e){return JSON.stringify({error:String(e)})}
})();