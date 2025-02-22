if (typeof window !== 'undefined') {
    const originalFetch = window.fetch;
  
    window.fetch = async (...args) => {
      const token = localStorage.getItem('tokenKey');
  
      // If the call is made with a Request object, clone and modify it.
      if (args[0] instanceof Request) {
        const request = args[0].clone();
        const modifiedHeaders = new Headers(request.headers);
        if (token) {
          modifiedHeaders.set('Authorization', `Bearer ${token}`);
        }
        const newRequest = new Request(request, {
          headers: modifiedHeaders
        });
        return originalFetch(newRequest);
      }
  
      // Otherwise, assume fetch is called with (resource, config)
      let [resource, config = {}] = args;
      config.headers = config.headers || {};
    
      // Ensure headers is a plain object
      if (token) {
        // If headers is a Headers instance, use set():
        if (config.headers instanceof Headers) {
          config.headers.set('Authorization', `Bearer ${token}`);
        } else {
          // Otherwise, merge into the plain object
          config.headers = { ...config.headers, 'Authorization': `Bearer ${token}` };
        }
      }
      return originalFetch(resource, config);
    };
  }