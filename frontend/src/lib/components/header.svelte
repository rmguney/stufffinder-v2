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

    function toggleLogin() {
        loginBar = !loginBar;
    }
</script>
  
<header class="fixed top-0 left-0 right-0 bg-white/80 dark:bg-black/80 backdrop-blur-sm border-b border-neutral-200/50 dark:border-neutral-800/50 z-50 pt-2 md:pt-6 lg:pt-0">
    <div class="px-2 sm:px-4 max-w-7xl mx-auto">
        <div class="flex h-14 sm:h-16 items-center justify-between">
            <!-- Brand and Navigation -->
            <div class="flex items-center gap-1 sm:gap-2 lg:gap-8">
                <!-- Logo/Brand - show icon on mobile, text on larger screens -->
                <a href="/" class="flex items-center" aria-label="Home">
                    <!-- Home icon for mobile -->
                    <div class="flex sm:hidden items-center justify-center h-8 w-8 rounded-full hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-colors">
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="size-4">
                            <path stroke-linecap="round" stroke-linejoin="round" d="m2.25 12 8.954-8.955c.44-.439 1.152-.439 1.591 0L21.75 12M4.5 9.75v10.125c0 .621.504 1.125 1.125 1.125H9.75v-4.875c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21h4.125c.621 0 1.125-.504 1.125-1.125V9.75M8.25 21h8.25" />
                        </svg>
                    </div>
                    <!-- Text logo for larger screens -->
                    <span class="hidden sm:block text-2xl font-semibold tracking-tight hover:scale-105 transition-transform">
                        Stufffinder
                    </span>
                </a>
                <!-- Primary Navigation -->
                <nav class="flex items-center gap-1 lg:ml-1">
                    <a href="/search" class="inline-flex items-center justify-center h-8 w-8 sm:h-10 sm:w-10 rounded-full hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-colors" aria-label="Search">
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="size-4 sm:size-5">
                            <path stroke-linecap="round" stroke-linejoin="round" d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z" />
                        </svg>                          
                    </a>
                    {#if $activeUser}
                        <a href="/create" class="inline-flex items-center justify-center h-8 w-8 sm:h-10 sm:w-10 rounded-full hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-colors" aria-label="Create new post">
                            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="size-4 sm:size-5">
                                <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
                            </svg>                              
                        </a>
                    {/if}
                </nav>
            </div>

            <!-- User Controls -->
            <div class="flex items-center gap-1 sm:gap-2">
                {#if $activeUser}
                    <!-- Notification Bell Component - ensure it's properly sized for mobile -->
                    <div class="flex items-center justify-center h-8 w-8 sm:h-10 sm:w-10">
                        <NotificationBell />
                    </div>
                    
                    <a href={`/user/${$activeUser}`} class="flex items-center justify-center h-8 w-8 sm:h-10 sm:w-10 rounded-full hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-colors">
                        <span class="text-lg font-lighter">
                            {$activeUser[0].toUpperCase()}
                        </span>
                    </a>
                    <!-- Show text button on larger screens, icon on mobile -->
                    <Button 
                        on:click={handleLogout}
                        variant="ghost"
                        class="hidden sm:flex text-sm font-medium rounded-full hover:bg-rose-900 hover:text-white"
                    >
                        Logout
                    </Button>
                    <Button 
                        on:click={handleLogout}
                        variant="ghost"
                        size="icon"
                        class="flex sm:hidden h-8 w-8 rounded-full hover:bg-rose-900 hover:text-white"
                        aria-label="Logout"
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="size-4">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0 0 13.5 3h-6a2.25 2.25 0 0 0-2.25 2.25v13.5A2.25 2.25 0 0 0 7.5 21h6a2.25 2.25 0 0 0 2.25-2.25V15m3 0 3-3m0 0-3-3m3 3H9" />
                        </svg>
                    </Button>
                {:else}
                    <!-- Show text button on larger screens, icon on mobile -->
                    <Button 
                        variant="ghost"
                        class="hidden sm:flex text-sm font-medium rounded-full hover:bg-rose-900 hover:text-white" 
                        on:click={toggleLogin}
                    >
                        Sign In
                    </Button>
                    <Button 
                        variant="ghost"
                        size="icon"
                        class="flex sm:hidden h-8 w-8 rounded-full hover:bg-rose-900 hover:text-white"
                        on:click={toggleLogin}
                        aria-label="Sign In"
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="size-4">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0 0 13.5 3h-6a2.25 2.25 0 0 0-2.25 2.25v13.5A2.25 2.25 0 0 0 7.5 21h6a2.25 2.25 0 0 0 2.25-2.25V15M12 9l-3 3m0 0 3 3m-3-3h12.75" />
                        </svg>
                    </Button>
                {/if}

                <div class="w-px h-5 sm:h-6 bg-neutral-200 dark:bg-neutral-800 mx-1 sm:mx-2"></div>

                <Button
                    on:click={toggleMode}
                    variant="ghost"
                    size="icon"
                    class="h-8 w-8 sm:h-10 sm:w-10 rounded-full"
                >
                    <Sun class="h-4 w-4 sm:h-5 sm:w-5 rotate-0 scale-100 transition-transform dark:-rotate-90 dark:scale-0" />
                    <Moon class="absolute h-4 w-4 sm:h-5 sm:w-5 rotate-90 scale-0 transition-transform dark:rotate-0 dark:scale-100" />
                </Button>
            </div>
        </div>
    </div>
</header>

<!-- Add padding to the page content to account for fixed header -->
<div class="h-14 sm:h-16"></div>

<!-- Connect to Login component's sheet with bind:open -->
<Login bind:loginBar={loginBar} />