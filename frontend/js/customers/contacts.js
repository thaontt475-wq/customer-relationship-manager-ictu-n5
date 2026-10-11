"use strict";
// Legacy URL compatibility. New pages load js/pages/contacts.js directly.
(() => {
    const url=new URL("../pages/contacts.js",document.currentScript.src).href;
    if ([...document.scripts].some(script=>script.src===url)) return;
    const script=document.createElement("script");script.src=url;script.async=false;document.head.appendChild(script);
})();
