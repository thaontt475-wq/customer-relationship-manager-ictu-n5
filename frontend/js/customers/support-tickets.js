"use strict";
// Legacy URL compatibility. New pages load js/pages/support-tickets.js directly.
(() => {
    const url=new URL("../pages/support-tickets.js",document.currentScript.src).href;
    if ([...document.scripts].some(script=>script.src===url)) return;
    const script=document.createElement("script");script.src=url;script.async=false;document.head.appendChild(script);
})();
