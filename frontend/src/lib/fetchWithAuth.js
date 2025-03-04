if (typeof window !== 'undefined') {
  const originalFetch = window.fetch;

  window.fetch = async (...args) => {
    // For Request object
    if (args[0] instanceof Request) {
      const request = args[0].clone();
      // Bypass if the URL matches
      if (request.url.startsWith('https://www.wikidata.org')) {
        return originalFetch(request);
      }
      const token = getCookie('tokenKey');
      const modifiedHeaders = new Headers(request.headers);
      if (token) {
        modifiedHeaders.set('Authorization', `Bearer ${token}`);
      }
      const newRequest = new Request(request, {
        headers: modifiedHeaders
      });
      return originalFetch(newRequest);
    }
    
    // For resource string
    let [resource, config = {}] = args;
    if (typeof resource === 'string' && resource.startsWith('https://www.wikidata.org')) {
      return originalFetch(resource, config);
    }
    
    const token = getCookie('tokenKey');
    config.headers = config.headers || {};
    // Ensure headers is a plain object
    if (token) {
      if (config.headers instanceof Headers) {
        config.headers.set('Authorization', `Bearer ${token}`);
      } else {
        config.headers = { ...config.headers, 'Authorization': `Bearer ${token}` };
      }
    }
    return originalFetch(resource, config);
  };
}

function getCookie(name) {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop().split(';').shift();
  return null;
}