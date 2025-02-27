<script>
    import { Button } from "$lib/components/ui/button";
    import { toggleMode } from "mode-watcher";
    import Sun from "lucide-svelte/icons/sun";
    import Moon from "lucide-svelte/icons/moon";
    import * as Sheet from "$lib/components/ui/sheet";
    import Login from "$lib/components/login.svelte";
    import { activeUser } from "../../userStore";
    import { goto } from "$app/navigation";
    import NotificationBell from './NotificationBell.svelte';
  
    let loginBar = false;
  
    function handleLogout() {
        activeUser.set(null);
        localStorage.clear();
        goto("/");
    }
  </script>
  
  <header class="fixed top-0 left-0 right-0 bg-white/80 dark:bg-black/80 backdrop-blur-sm border-b border-neutral-200/50 dark:border-neutral-800/50 z-50 pt-6 lg:pt-0">
      <div class="px-4 max-w-7xl mx-auto">
          <div class="flex h-16 items-center justify-between">
              <!-- Brand and Navigation -->
              <div class="flex items-center gap-2 lg:gap-8">
                  <!-- Logo/Brand -->
                  <a href="/" class="text-lg font-semibold tracking-tight hover:text-rose-900">
                      Stufffinder
                  </a>
                  <!-- Primary Navigation -->
                  <nav class="flex items-center gap-1">
                      <a href="/search" class="inline-flex items-center justify-center h-10 w-10 rounded-full hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-colors" aria-label="Search">
                          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="size-5">
                              <path stroke-linecap="round" stroke-linejoin="round" d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z" />
                          </svg>                          
                      </a>
                      {#if $activeUser}
                          <a href="/create" class="inline-flex items-center justify-center h-10 w-10 rounded-full hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-colors" aria-label="Create new post">
                              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="size-5">
                                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
                              </svg>                              
                          </a>
                      {/if}
                  </nav>
              </div>
  
              <!-- User Controls -->
              <div class="flex items-center gap-2">
                  {#if $activeUser}
                      <!-- Notification Bell Component -->
                      <NotificationBell />
                      
                      <a href={`/user/${$activeUser}`} class="hidden sm:flex items-center justify-center h-10 w-10 rounded-full hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-colors">
                          <span class="text-sm font-medium">
                              {$activeUser[0].toUpperCase()}
                          </span>
                      </a>
                      <Button 
                          on:click={handleLogout}
                          variant="ghost"
                          class="text-sm font-medium hover:bg-rose-900 hover:text-white"
                      >
                          Logout
                      </Button>
                  {:else}
                      <Login />
                  {/if}
  
                  <div class="w-px h-6 bg-neutral-200 dark:bg-neutral-800 mx-2"></div>
  
                  <Button
                      on:click={toggleMode}
                      variant="ghost"
                      size="icon"
                      class="rounded-full"
                  >
                      <Sun class="h-5 w-5 rotate-0 scale-100 transition-transform dark:-rotate-90 dark:scale-0" />
                      <Moon class="absolute h-5 w-5 rotate-90 scale-0 transition-transform dark:rotate-0 dark:scale-100" />
                  </Button>
              </div>
          </div>
      </div>
  </header>
  
  <!-- Add padding to the page content to account for fixed header -->
  <div class="h-16"></div>
  
  <Sheet.Root bind:open={loginBar}>
    <Sheet.Overlay />
    <Sheet.Content side="right" class="w-96">
      <Sheet.Header>
        <Sheet.Close />
      </Sheet.Header>
      <Login />
    </Sheet.Content>
  </Sheet.Root>