/* Renders an internal markdown doc into the styled site shell.
   The .md files are the canonical source (internal); visitors only see these pages. */
(function () {
  var article = document.querySelector('.md');
  var side = document.getElementById('sidenav');
  var file = article.getAttribute('data-doc');

  // Map internal markdown docs -> their public HTML pages, so cross-links stay on-site.
  var DOC_PAGES = {
    'API_ENDPOINTS.md': 'api.html',
    'DATABASE_QUERIES.md': 'database.html',
    'DEVELOPER_GUIDE.md': 'developer-guide.html',
    'DEMO_GUIDE.md': 'demo-guide.html'
  };
  var REPO_BLOB = 'https://github.com/ofekgki/IAPManagement/blob/main/';

  function githubSlug(text) {
    return text.trim().toLowerCase()
      .replace(/[^\w\- ]+/g, '')
      .replace(/\s+/g, '-')
      .replace(/-+/g, '-');
  }

  function render(md) {
    marked.setOptions({ gfm: true, breaks: false, headerIds: false, mangle: false });
    article.innerHTML = (marked.parse ? marked.parse(md) : marked(md));

    // 1) Assign GitHub-compatible ids to headings + hover anchor links.
    var used = {};
    article.querySelectorAll('h1,h2,h3,h4').forEach(function (h) {
      var base = githubSlug(h.textContent), slug = base, i = 0;
      while (used[slug]) { i++; slug = base + '-' + i; }
      used[slug] = true;
      h.id = slug;
      if (h.tagName !== 'H1') {
        var a = document.createElement('a');
        a.className = 'anchor'; a.href = '#' + slug; a.textContent = '#';
        a.setAttribute('aria-label', 'Link to this section');
        h.appendChild(a);
      }
    });

    // 2) Build the sidebar TOC from h2/h3.
    var toc = document.createElement('div');
    var grp = document.createElement('div');
    grp.className = 'group'; grp.textContent = 'On this page';
    toc.appendChild(grp);
    article.querySelectorAll('h2,h3').forEach(function (h) {
      var link = document.createElement('a');
      link.href = '#' + h.id;
      link.textContent = h.textContent.replace(/#$/, '');
      if (h.tagName === 'H3') link.className = 'h3';
      toc.appendChild(link);
    });
    side.innerHTML = '';
    side.appendChild(toc);

    // 3) Rewrite links: internal .md -> .html page; repo-relative (../) -> GitHub blob.
    article.querySelectorAll('a[href]').forEach(function (a) {
      var href = a.getAttribute('href');
      if (!href) return;
      var m = href.match(/^(?:\.\/)?([A-Za-z_]+\.md)(#.*)?$/);
      if (m && DOC_PAGES[m[1]]) { a.setAttribute('href', DOC_PAGES[m[1]] + (m[2] || '')); return; }
      if (href.indexOf('../') === 0) {
        a.setAttribute('href', REPO_BLOB + href.replace(/^(\.\.\/)+/, ''));
        a.setAttribute('target', '_blank'); a.setAttribute('rel', 'noopener');
      }
    });

    // 4) Copy buttons on code blocks.
    article.querySelectorAll('pre').forEach(function (pre) {
      var b = document.createElement('button');
      b.className = 'copy'; b.textContent = 'Copy';
      b.onclick = function () {
        navigator.clipboard.writeText(pre.innerText.replace(/\nCopy$/, ''));
        b.textContent = 'Copied'; setTimeout(function () { b.textContent = 'Copy'; }, 1200);
      };
      pre.appendChild(b);
    });

    // 5) Scrollspy: highlight the active TOC entry.
    var links = [].slice.call(side.querySelectorAll('a'));
    var map = {};
    links.forEach(function (a) { map[a.getAttribute('href').slice(1)] = a; });
    var obs = new IntersectionObserver(function (entries) {
      entries.forEach(function (e) {
        if (e.isIntersecting) {
          links.forEach(function (a) { a.classList.remove('active'); });
          if (map[e.target.id]) map[e.target.id].classList.add('active');
        }
      });
    }, { rootMargin: '-40% 0px -55% 0px' });
    article.querySelectorAll('h2,h3').forEach(function (h) { obs.observe(h); });

    // 6) Honor a deep-link hash now that content exists.
    if (location.hash) {
      var t = document.getElementById(decodeURIComponent(location.hash.slice(1)));
      if (t) t.scrollIntoView();
    }
  }

  function fail(msg) {
    article.innerHTML = '<p class="status err">' + msg +
      ' <a href="' + file + '">Open the raw document</a>.</p>';
  }

  function boot() {
    if (typeof marked === 'undefined') { fail('Renderer failed to load.'); return; }
    fetch(file, { cache: 'no-cache' })
      .then(function (r) { if (!r.ok) throw new Error(r.status); return r.text(); })
      .then(render)
      .catch(function () { fail('Could not load this document.'); });
  }

  // Load marked from CDN, then render.
  var s = document.createElement('script');
  s.src = 'https://cdn.jsdelivr.net/npm/marked/marked.min.js';
  s.onload = boot;
  s.onerror = function () { fail('Renderer failed to load (offline?).'); };
  document.head.appendChild(s);
})();
