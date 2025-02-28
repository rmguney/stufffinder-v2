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

  let loginBar = false;
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
    
    const token = localStorage.getItem('tokenKey');
    if (token && isTokenExpired(token)) {
      console.log('Token expired, removing from storage');
      localStorage.removeItem('tokenKey');
      activeUser.set(null);
    }
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
      await handleLogin(`${registerUsername}@example.com`, registerPassword);
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
        localStorage.setItem('tokenKey', data.token);
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

<Button 
    variant="ghost"
    class="text-sm font-medium hover:bg-rose-900 hover:text-white" 
    on:click={() => (loginBar = !loginBar)}
>
    Sign In
</Button>

<Sheet.Root bind:open={loginBar}>
  <Sheet.Overlay />
  <Sheet.Content side="right" class="lg:w-96 w-full">
    <Sheet.Header>
      <Sheet.Close />
    </Sheet.Header>
    <div class="pt-8">
      <Tabs.Root value={activeTab} class="w-full p-4" onValueChange={updateIndicator}>
        <div class="relative">
          <Tabs.List bind:this={tabsContainer} class="grid w-full grid-cols-2">
            <Tabs.Trigger 
              value="login" 
              data-value="login"
              class="data-[state=active]:text-rose-900 data-[state=active]:bg-transparent relative z-10"
            >
              Login
            </Tabs.Trigger>
            <Tabs.Trigger 
              value="register" 
              data-value="register"
              class="data-[state=active]:text-rose-900 data-[state=active]:bg-transparent relative z-10"
            >
              Register
            </Tabs.Trigger>
          </Tabs.List>
          <!-- Animated tab indicator -->
          <div 
            bind:this={tabIndicator} 
            class="absolute bottom-0 h-0.5 bg-rose-900 transition-all duration-300 ease-in-out z-0"
          ></div>
        </div>
        <Tabs.Content value="login">
          <Card.Root class="border-neutral-200/50 dark:border-neutral-800/50">
            <Card.Header>
              <Card.Title class="text-lg tracking-tight">Login</Card.Title>
              <Card.Description class="text-neutral-500 dark:text-neutral-400">
                Enter your credentials to access your account.
              </Card.Description>
            </Card.Header>
            <Card.Content class="space-y-3">
              <div class="space-y-1">
                <Label for="login-username" class="text-sm font-medium">Username</Label>
                <Input id="login-username" type="text" bind:value={loginUsername} class={loginErrors.username ? "focus-visible:ring-rose-900 border-red-300" : "focus-visible:ring-rose-900"} />
                {#if loginErrors.username}
                  <p class="text-xs font-medium text-rose-500 flex items-center gap-1.5 mt-1.5" transition:fly={{ y: -10, duration: 150 }}>
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" class="w-3.5 h-3.5">
                      <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z" clip-rule="evenodd" />
                    </svg>
                    {loginErrors.username}
                  </p>
                {/if}
              </div>
              <div class="space-y-1">
                <Label for="login-password" class="text-sm font-medium">Password</Label>
                <Input id="login-password" type="password" bind:value={loginPassword} class={loginErrors.password ? "focus-visible:ring-rose-900 border-red-300" : "focus-visible:ring-rose-900"} />
                {#if loginErrors.password}
                  <p class="text-xs font-medium text-rose-500 flex items-center gap-1.5 mt-1.5" transition:fly={{ y: -10, duration: 150 }}>
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" class="w-3.5 h-3.5">
                      <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z" clip-rule="evenodd" />
                    </svg>
                    {loginErrors.password}
                  </p>
                {/if}
              </div>
              {#if loginErrors.message}
                <div class="flex items-center gap-1.5 mt-3" transition:fly={{ y: -10, duration: 200 }}>
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" class="w-3.5 h-3.5 text-rose-500 flex-shrink-0">
                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z" clip-rule="evenodd" />
                  </svg>
                  <p class="text-xs font-medium text-rose-600">{loginErrors.message}</p>
                </div>
              {/if}
            </Card.Content>
            <Card.Footer>
              <Button 
                class="bg-neutral-900 hover:bg-rose-900 text-white transition-colors w-full" 
                on:click={() => handleLogin()}
                disabled={loginLoading}
              >
                {#if loginLoading}
                  <span class="inline-block h-4 w-4 border-2 border-white/20 border-t-white rounded-full animate-spin mr-2"></span>
                  Logging in...
                {:else}
                  Login
                {/if}
              </Button>
            </Card.Footer>
          </Card.Root>
        </Tabs.Content>
        <Tabs.Content value="register">
          <Card.Root class="border-neutral-200/50 dark:border-neutral-800/50">
            <Card.Header>
              <Card.Title class="text-lg tracking-tight">Register</Card.Title>
              <Card.Description class="text-neutral-500 dark:text-neutral-400">
                Create a new account to join our community!
              </Card.Description>
            </Card.Header>
            <Card.Content class="space-y-3">
              <div class="space-y-1">
                <Label for="register-username" class="text-sm font-medium">Username</Label>
                <Input id="register-username" type="text" bind:value={registerUsername} class={registerErrors.username ? "focus-visible:ring-rose-900 border-red-300" : "focus-visible:ring-rose-900"} />
                {#if registerErrors.username}
                  <p class="text-xs font-medium text-rose-500 flex items-center gap-1.5 mt-1.5" transition:fly={{ y: -10, duration: 150 }}>
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" class="w-3.5 h-3.5">
                      <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z" clip-rule="evenodd" />
                    </svg>
                    {registerErrors.username}
                  </p>
                {/if}
              </div>
              <div class="space-y-1">
                <Label for="register-password" class="text-sm font-medium">Password</Label>
                <Input id="register-password" type="password" bind:value={registerPassword} class={registerErrors.password ? "focus-visible:ring-rose-900 border-red-300" : "focus-visible:ring-rose-900"} />
                {#if registerErrors.password}
                  <p class="text-xs font-medium text-rose-500 flex items-center gap-1.5 mt-1.5" transition:fly={{ y: -10, duration: 150 }}>
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" class="w-3.5 h-3.5">
                      <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z" clip-rule="evenodd" />
                    </svg>
                    {registerErrors.password}
                  </p>
                {/if}
              </div>
              {#if registerSuccess}
                <div class="flex items-center gap-1.5 mt-3" transition:fly={{ y: -10, duration: 200 }}>
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" class="w-3.5 h-3.5 text-emerald-500 flex-shrink-0">
                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.857-9.809a.75.75 0 00-1.214-.882l-3.483 4.79-1.88-1.88a.75.75 0 10-1.06 1.061l2.5 2.5a.75.75 0 001.137-.089l4-5.5z" clip-rule="evenodd" />
                  </svg>
                  <p class="text-xs font-medium text-emerald-600">{registerSuccess}</p>
                </div>
              {/if}
              {#if registerErrors.message}
                <div class="flex items-center gap-1.5 mt-3" transition:fly={{ y: -10, duration: 200 }}>
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" class="w-3.5 h-3.5 text-rose-500 flex-shrink-0">
                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z" clip-rule="evenodd" />
                  </svg>
                  <p class="text-xs font-medium text-rose-600">{registerErrors.message}</p>
                </div>
              {/if}
            </Card.Content>
            <Card.Footer>
              <Button 
                class="bg-neutral-900 hover:bg-rose-900 text-white transition-colors w-full" 
                on:click={handleRegister}
                disabled={registerLoading}
              >
                {#if registerLoading}
                  <span class="inline-block h-4 w-4 border-2 border-white/20 border-t-white rounded-full animate-spin mr-2"></span>
                  Registering...
                {:else}
                  Register
                {/if}
              </Button>
            </Card.Footer>
          </Card.Root>
        </Tabs.Content>
      </Tabs.Root>
    </div>
  </Sheet.Content>
</Sheet.Root>
