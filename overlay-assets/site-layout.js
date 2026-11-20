(function () {
 'use strict';
 var root=document.documentElement, tag='mywebviewer-layout-css-v1';
 var old=document.getElementById(tag); if(old)old.remove();
 var css=__MYWEB_CSS_LITERAL__;
 var known=!!document.querySelector('.header.header-top .logo-wrapper')&&!!document.querySelector('.mobile-main, #index, .details');
 if(!known || !css){root.classList.remove('mwv-layout-active','mwv-page-play');return}
 var style=document.createElement('style');style.id=tag;style.textContent=css;
 (document.head||root).appendChild(style);root.classList.add('mwv-layout-active');
 var playing=/\/vod\/play\//.test(location.pathname);
 root.classList.toggle('mwv-page-play',playing);
 function rearrange(){
  var ad=document.getElementById('advertiseMain');
  var card=ad&&ad.closest('.card');
  if(card&&!card.querySelector('.movie-list-item, .vod-search-list, .play-select'))card.classList.add('mwv-hide-ad-card');
  if(!playing)return;
  var eps=document.querySelector('.play-select');
  var player=document.querySelector('#MacPlayer, #player, #playbox, .player-box, .player-container, .play-player');
  if(eps&&player&&eps.parentElement===player.parentElement&&player.nextElementSibling!==eps)player.insertAdjacentElement('afterend',eps);
 }
 rearrange();setTimeout(rearrange,700);setTimeout(rearrange,2200);
})();