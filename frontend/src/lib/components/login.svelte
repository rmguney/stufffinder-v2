<script>
  import { Button } from "$lib/components/ui/button";
  import { Input } from "$lib/components/ui/input";
  import { Label } from "$lib/components/ui/label/index.js";
  import * as Sheet from "$lib/components/ui/sheet";
  import * as Tabs from "$lib/components/ui/tabs/index.js";
  import * as Card from "$lib/components/ui/card/index.js";
  import { activeUser } from '../../userStore';
  import { onMount } from 'svelte';
  import { fly } from 'svelte/transition';
  import { PUBLIC_API_URL } from "$env/static/public";

  // Make loginBar accessible to parent components
  export let loginBar = false;
  let activeTab = "login";
  let tabIndicator;
  let tabsContainer;
  
  // Registration and login states
  let registerUsername = "";
  let registerPassword = "";
  let registerErrors = {};
  let registerSuccess = "";
  let registerLoading = false;

  let loginUsername = "";
  let loginPassword = "";
  let loginErrors = {};
  let loginLoading = false;

  // Export toggle function for parent components to use
  export function toggleLoginBar() {
    loginBar = !loginBar;
  }

  // Update the indicator position when the tab changes
  function updateIndicator(value) {
    activeTab = value;
    
    if (tabsContainer && tabIndicator) {
      const activeTabElement = tabsContainer.querySelector(`[data-value="${value}"]`);
      if (activeTabElement) {
        // Position and width calculations
        const tabLeft = activeTabElement.offsetLeft;
        const tabWidth = activeTabElement.offsetWidth;
        
        // Update indicator position and width
        tabIndicator.style.transform = `translateX(${tabLeft}px)`;
        tabIndicator.style.width = `${tabWidth}px`;
      }
    }
  }

  function setCookie(name, value) {
    // Set cookie with 1 hour expiration time
    const expirationDate = new Date();
    expirationDate.setTime(expirationDate.getTime() + (60 * 60 * 1000)); // 1 hour in milliseconds
    document.cookie = `${name}=${value}; path=/; expires=${expirationDate.toUTCString()}; SameSite=Strict`;
  }

  function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
  }

  function deleteCookie(name) {
    document.cookie = `${name}=; path=/; expires=Thu, 01 Jan 1970 00:00:01 GMT`;
  }

  function isTokenExpired(token) {
    if (!token) return true;
    
    try {
      const payload = token.split('.')[1];
      if (!payload) return true;
      
      const decodedPayload = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
      
      if (!decodedPayload.exp) return true;
      
      const expirationTime = decodedPayload.exp * 1000; 
      return Date.now() >= expirationTime;
    } catch (error) {
      console.error('Error parsing token:', error);
      return true; 
    }
  }

  onMount(() => {
    updateIndicator(activeTab);
  });


  let handleRegister = async () => {
    registerErrors = {};
    registerSuccess = "";
    registerLoading = true;
    
    try {
      // console.log('Starting registration with username:', registerUsername);

      // Validate required fields
      if (!registerUsername) {
        registerErrors.username = "Username is required";
      }
      
      if (!registerPassword) {
        registerErrors.password = "Password is required";
      }
      
      if (Object.keys(registerErrors).length > 0) {
        return;
      }

      const payload = {
        username: registerUsername,
        email: `${registerUsername}@example.com`,
        password: registerPassword
      };

      const response = await fetch(`${PUBLIC_API_URL}/api/auth/register`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      let data;
      try {
        const rawText = await response.text();
        data = rawText ? JSON.parse(rawText) : {};
      } catch (parseError) {
        console.error('Error parsing response:', parseError);
        throw new Error('Invalid server response');
      }

      if (!response.ok) {
        if (response.status === 409) {
          registerErrors.username = "This username is already taken";
          throw new Error("This username is already taken");
        }
        throw new Error(data.message || `Registration failed with status ${response.status}`);
      }

      // Show success message
      registerSuccess = "Registration successful! Logging you in...";
      
      // Success - try to log in automatically
      await handleLogin(registerUsername, registerPassword);
      loginBar = false;
      registerUsername = "";
      registerPassword = "";
      
    } catch (error) {
      console.error("Error registering user:", error);
      if (!registerErrors.username && !registerErrors.password) {
        registerErrors.message = error.message || "Registration failed. Please try again later.";
      }
    } finally {
      registerLoading = false;
    }
  };

  let handleLogin = async (username = loginUsername, password = loginPassword) => {
    loginErrors = {};
    loginLoading = true;
    
    try {
      // Validate required fields
      if (!username) {
        loginErrors.username = "Username is required";
      }
      
      if (!password) {
        loginErrors.password = "Password is required";
      }
      
      if (Object.keys(loginErrors).length > 0) {
        return;
      }

      const payload = {
        email: `${username}@example.com`,
        password: password
      };

      const response = await fetch(`${PUBLIC_API_URL}/api/auth/login`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      const data = await response.json();

      if (!response.ok) {
        if (response.status === 401) {
          throw new Error("Invalid username or password");
        }
        throw new Error(data.message || 'Login failed');
      }

      // Save auth token to localStorage
      if (data.token) {
        setCookie('tokenKey', data.token);
      }

      // Store user data
      activeUser.set(username);
      loginBar = false;
      loginUsername = "";
      loginPassword = "";
      
    } catch (error) {
      console.error("Error logging in:", error);
      loginErrors.message = error.message || "Login failed";
    } finally {
      loginLoading = false;
    }
  };
</script>

<!-- Regular and mobile login buttons are now handled in the header component -->

<Sheet.Root bind:open={loginBar}>
  <Sheet.Overlay class="backdrop-blur-sm" />
  <Sheet.Content side="right" class="w-[92%] max-w-[360px] sm:w-[380px] rounded-l-xl border-l border-neutral-100/30 dark:border-neutral-900/20 shadow-xl">
    <div class="pt-6 px-4">
      <Tabs.Root value={activeTab} class="w-full" onValueChange={updateIndicator}>
        <div class="relative mb-6">
          <Tabs.List bind:this={tabsContainer} class="grid w-full grid-cols-2 bg-neutral-50/30 dark:bg-neutral-800/30 rounded-md p-1">
            <Tabs.Trigger 
              value="login" 
              data-value="login"
              class="data-[state=active]:text-neutral-800 dark:data-[state=active]:text-white data-[state=active]:font-medium py-2 relative z-10 rounded-md transition-colors"
            >
              Login
            </Tabs.Trigger>
            <Tabs.Trigger 
              value="register" 
              data-value="register"
              class="data-[state=active]:text-neutral-800 dark:data-[state=active]:text-white data-[state=active]:font-medium py-2 relative z-10 rounded-md transition-colors"
            >
              Register
            </Tabs.Trigger>
          </Tabs.List>
          <!-- Animated tab indicator -->
          <div 
            bind:this={tabIndicator} 
            class="absolute top-1 bottom-1 bg-white dark:bg-neutral-700 rounded-md shadow-sm transition-all duration-300 ease-in-out z-0"
          ></div>
        </div>
        <Tabs.Content value="login" class="transition-all animate-in fade-in duration-300">
          <Card.Root class="border-none shadow-none">
            <Card.Header class="p-0 pb-4">
              <Card.Title class="text-lg font-bold text-neutral-800 dark:text-neutral-500">Sign In</Card.Title>
              <Card.Description class="text-neutral-500 dark:text-neutral-400 mt-1">
                Enter your credentials to access your account
              </Card.Description>
            </Card.Header>
            <Card.Content class="p-0 space-y-4">
              <div class="space-y-1.5">
                <Label for="login-username" class="text-sm font-medium block">Username</Label>
                <div class="relative">
                  <div class="absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8" r="5"/><path d="M20 21a8 8 0 0 0-16 0"/></svg>
                  </div>
                  <Input 
                    id="login-username" 
                    type="text" 
                    bind:value={loginUsername} 
                    class={loginErrors.username 
                      ? "pl-9 focus-visible:ring-neutral-500 border-red-300 bg-red-50/30 rounded-full" 
                      : "pl-9 focus-visible:ring-neutral-500 border-neutral-200 dark:border-neutral-700 rounded-full hover:border-neutral-300 transition-colors"} 
                    placeholder="Enter username"
                  />
                </div>
                {#if loginErrors.username}
                  <p class="text-xs font-medium text-neutral-500 flex items-center gap-1.5" transition:fly={{ y: -10, duration: 150 }}>
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" class="w-3.5 h-3.5">
                      <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z" clip-rule="evenodd" />
                    </svg>
                    {loginErrors.username}
                  </p>
                {/if}
              </div>
              <div class="space-y-1.5">
                <Label for="login-password" class="text-sm font-medium block">Password</Label>
                <div class="relative">
                  <div class="absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="18" height="11" x="3" y="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                  </div>
                  <Input 
                    id="login-password" 
                    type="password" 
                    bind:value={loginPassword} 
                    class={loginErrors.password 
                      ? "pl-9 focus-visible:ring-neutral-500 border-red-300 rounded-full bg-red-50/30" 
                      : "pl-9 focus-visible:ring-neutral-500 border-neutral-200 rounded-full dark:border-neutral-700 hover:border-neutral-300 transition-colors"} 
                    placeholder="Enter password"
                  />
                </div>
                {#if loginErrors.password}
                  <p class="text-xs font-medium text-neutral-500 flex items-center gap-1.5" transition:fly={{ y: -10, duration: 150 }}>
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" class="w-3.5 h-3.5">
                      <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z" clip-rule="evenodd" />
                    </svg>
                    {loginErrors.password}
                  </p>
                {/if}
              </div>
              {#if loginErrors.message}
                <div class="flex items-center gap-1.5 p-3 bg-red-50/50 dark:bg-red-900/10 border border-red-100 dark:border-red-900/20 rounded-md" transition:fly={{ y: -10, duration: 200 }}>
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" class="w-4 h-4 text-neutral-500 flex-shrink-0">
                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z" clip-rule="evenodd" />
                  </svg>
                  <p class="text-xs font-medium text-neutral-600">{loginErrors.message}</p>
                </div>
              {/if}
            </Card.Content>
            <Card.Footer class="p-0 pt-4">
              <Button 
                variant="outline"
                class="text-xs py-1 px-3 hover:bg-neutral-100 dark:hover:bg-neutral-800 w-full rounded-full" 
                on:click={() => handleLogin()}
                disabled={loginLoading}
              >
                {#if loginLoading}
                  <span class="inline-block h-4 w-4 border-2 border-white/30 border-t-white rounded-full animate-spin mr-2"></span>
                  Logging in...
                {:else}
                  Sign In
                {/if}
              </Button>
            </Card.Footer>
          </Card.Root>
        </Tabs.Content>
        <Tabs.Content value="register" class="transition-all animate-in fade-in duration-300">
          <Card.Root class="border-none shadow-none">
            <Card.Header class="p-0 pb-4">
              <Card.Title class="text-lg font-bold text-neutral-800 dark:text-neutral-500">Create Account</Card.Title>
              <Card.Description class="text-neutral-500 dark:text-neutral-400 mt-1">
                Join our community in just a few steps
              </Card.Description>
            </Card.Header>
            <Card.Content class="p-0 space-y-4">
              <div class="space-y-1.5">
                <Label for="register-username" class="text-sm font-medium block">Username</Label>
                <div class="relative">
                  <div class="absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8" r="5"/><path d="M20 21a8 8 0 0 0-16 0"/></svg>
                  </div>
                  <Input 
                    id="register-username" 
                    type="text" 
                    bind:value={registerUsername} 
                    class={registerErrors.username 
                      ? "pl-9 focus-visible:ring-neutral-500 border-red-300 rounded-full bg-red-50/30" 
                      : "pl-9 focus-visible:ring-neutral-500 border-neutral-200 dark:border-neutral-700 rounded-full hover:border-neutral-300 transition-colors"} 
                    placeholder="Choose a username"
                  />
                </div>
                {#if registerErrors.username}
                  <p class="text-xs font-medium text-neutral-500 flex items-center gap-1.5" transition:fly={{ y: -10, duration: 150 }}>
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" class="w-3.5 h-3.5">
                      <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z" clip-rule="evenodd" />
                    </svg>
                    {registerErrors.username}
                  </p>
                {/if}
              </div>
              <div class="space-y-1.5">
                <Label for="register-password" class="text-sm font-medium block">Password</Label>
                <div class="relative">
                  <div class="absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="18" height="11" x="3" y="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                  </div>
                  <Input 
                    id="register-password" 
                    type="password" 
                    bind:value={registerPassword} 
                    class={registerErrors.password 
                      ? "pl-9 focus-visible:ring-neutral-500 border-red-300 rounded-full bg-red-50/30" 
                      : "pl-9 focus-visible:ring-neutral-500 border-neutral-200 rounded-full dark:border-neutral-700 hover:border-neutral-300 transition-colors"} 
                    placeholder="Create a password"
                  />
                </div>
                {#if registerErrors.password}
                  <p class="text-xs font-medium text-neutral-500 flex items-center gap-1.5" transition:fly={{ y: -10, duration: 150 }}>
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" class="w-3.5 h-3.5">
                      <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z" clip-rule="evenodd" />
                    </svg>
                    {registerErrors.password}
                  </p>
                {/if}
              </div>
              {#if registerSuccess}
                <div class="flex items-center gap-1.5 p-3 bg-emerald-50/50 dark:bg-emerald-900/10 border border-emerald-100 dark:border-emerald-900/20 rounded-md" transition:fly={{ y: -10, duration: 200 }}>
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" class="w-4 h-4 text-emerald-500 flex-shrink-0">
                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.857-9.809a.75.75 0 00-1.214-.882l-3.483 4.79-1.88-1.88a.75.75 0 10-1.06 1.061l2.5 2.5a.75.75 0 001.137-.089l4-5.5z" clip-rule="evenodd" />
                  </svg>
                  <p class="text-xs font-medium text-emerald-600">{registerSuccess}</p>
                </div>
              {/if}
              {#if registerErrors.message}
                <div class="flex items-center gap-1.5 p-3 bg-red-50/50 dark:bg-red-900/10 border border-red-100 dark:border-red-900/20 rounded-md" transition:fly={{ y: -10, duration: 200 }}>
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" class="w-4 h-4 text-neutral-500 flex-shrink-0">
                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z" clip-rule="evenodd" />
                  </svg>
                  <p class="text-xs font-medium text-neutral-600">{registerErrors.message}</p>
                </div>
              {/if}
            </Card.Content>
            <Card.Footer class="p-0 pt-4">
              <Button 
                variant="outline"
                class="text-xs py-1 px-3 hover:bg-neutral-100 dark:hover:bg-neutral-800 rounded-full w-full" 
                on:click={handleRegister}
                disabled={registerLoading}
              >
                {#if registerLoading}
                  <span class="inline-block h-4 w-4 border-2 border-white/30 border-t-white rounded-full animate-spin mr-2"></span>
                  Creating account...
                {:else}
                  Create Account
                {/if}
              </Button>
            </Card.Footer>
          </Card.Root>
        </Tabs.Content>
      </Tabs.Root>
    </div>
  </Sheet.Content>
</Sheet.Root>
