window.jQuery.expr[':'].icontains = window.jQuery.expr.createPseudo(function (arg) {
  return function (elem) {
    return window.jQuery(elem).text().toUpperCase().indexOf(arg.toUpperCase()) >= 0
  }
})

window.SeleniumJQuery = {
  searches: [],
  log: false,
  minLength: null,
  maxLength: null,
  observer: null,
  debounce: null,
  observeNow: function () {
    const foundSearches = []
    for (let i = 0; i < SeleniumJQuery.searches.length; i++) {
      const search = SeleniumJQuery.searches[i]
      const foundItems = []
      let foundCallback = null
      for (let j = 0; j < search.length; j++) {
        const item = search[j]
        const found = jQuery(item.query)

        // Constraints
        if (item.atLeast != null && (found.length < item.atLeast)) {
          foundItems.push(null)
          if (SeleniumJQuery.log) console.log('Not enough items for', item.query, 'found', found.length, 'atLeast', item.atLeast)
          continue
        }
        if (item.atMost != null && (found.length > item.atMost)) {
          foundItems.push(null)
          if (SeleniumJQuery.log) console.log('Too many items for', item.query, "found", found.length)
          continue
        }
        foundCallback = item.callback
        foundItems.push(found.toArray())
      }
      if (foundCallback) {
        if (SeleniumJQuery.log) console.log('Found search group', search.map(s=>s.query).join(", "), foundItems.length)
        foundSearches.push({
          search,
          foundItems,
          foundCallback
        })
      } else {
        if (SeleniumJQuery.log) console.log('Did not find search group', search.map(s=>s.query).join(", "))
      }
    }
    for (let i = 0; i < foundSearches.length; i++) {
      const group = foundSearches[i]
      setTimeout(() => {group.foundCallback(group.foundItems)}, 1)
      SeleniumJQuery.searches.splice(SeleniumJQuery.searches.indexOf(group.search), 1)
    }
  },
  observe: function () {
    if (SeleniumJQuery.debounce) clearTimeout(SeleniumJQuery.debounce)
    if (SeleniumJQuery.searches.length === 0) return
    SeleniumJQuery.debounce = setTimeout(SeleniumJQuery.observeNow, 1)
  },

  search: function (log, queries, atLeasts, atMosts, callback) {
    SeleniumJQuery.log = log
    if (SeleniumJQuery.observer == null) {
      SeleniumJQuery.observer = new MutationObserver(SeleniumJQuery.observe)
      SeleniumJQuery.observer.observe(document.documentElement || document.body, {
        attributes: true,
        childList: true,
        subtree: true,
      })
    }
    SeleniumJQuery.searches.push(queries.map(function (query, index) {
      return {
        query: query,
        atLeast: atLeasts[index],
        atMost: atMosts[index],
        callback: callback
      }
    }))
    SeleniumJQuery.observe()
  }
}
SeleniumJQuery.searches = []
