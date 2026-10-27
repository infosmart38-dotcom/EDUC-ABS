/* جسر أندرويد: يُحقن في index.html عند البناء. لا يعمل خارج التطبيق. */
(function () {
  if (!window.AndroidBridge || window.__androidShim) return;
  window.__androidShim = true;
  var B = window.AndroidBridge;
  function toB64(blob) {
    return new Promise(function (res, rej) {
      var r = new FileReader();
      r.onload = function () { res(String(r.result).split(',')[1] || ''); };
      r.onerror = rej;
      r.readAsDataURL(blob);
    });
  }
  function saveUrl(url, name) {
    return fetch(url).then(function (r) { return r.blob(); }).then(function (blob) {
      return toB64(blob).then(function (b64) {
        B.saveFile(name || 'file', blob.type || 'application/octet-stream', b64);
      });
    });
  }
  var origClick = HTMLAnchorElement.prototype.click;
  HTMLAnchorElement.prototype.click = function () {
    if (this.hasAttribute('download') && /^blob:|^data:/.test(this.href)) {
      saveUrl(this.href, this.getAttribute('download'));
      return;
    }
    return origClick.apply(this, arguments);
  };
  document.addEventListener('click', function (e) {
    var a = e.target && e.target.closest && e.target.closest('a[download]');
    if (a && /^blob:|^data:/.test(a.href)) { e.preventDefault(); saveUrl(a.href, a.getAttribute('download')); }
  }, true);
  window.print = function () { B.print(); };
})();
