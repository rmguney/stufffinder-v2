<script>
    import { ModeWatcher } from "mode-watcher";
    import Header from '$lib/components/header.svelte';
    import NotificationToast from '$lib/components/NotificationToast.svelte';
    import { onMount, afterUpdate } from 'svelte';
    import '$lib/fetchWithAuth.js';
    import { activeUser } from '../userStore';
    import { startNotificationPolling, stopNotificationPolling, fetchNotifications } from '../notificationStore';
    import { page } from '$app/stores';
    import { forceRefreshThreads } from '../threadStore';
    
    // Store the current path to detect navigation changes
    let currentPath = '';
    let initialLoad = true;
    
    // Listen for page changes to refresh notifications and threads
    $: if ($page.url.pathname !== currentPath) {
        currentPath = $page.url.pathname;
        
        // Don't refetch on the notifications page itself to avoid duplicate calls
        if (currentPath !== '/notifications') {
            // console.log('Page changed, fetching notifications');
            if ($activeUser) {
                fetchNotifications();
            }
        }
        
        // If navigating to home, force refresh threads
        if (currentPath === '/' && !initialLoad) {
            // console.log('Navigated to home, refreshing threads');
            forceRefreshThreads();
        }
        
        initialLoad = false;
    }
    
    onMount(() => {
        // Initialize current path
        currentPath = window.location.pathname;
        
        // Start notification polling if user is logged in
        if ($activeUser) {
            startNotificationPolling();
        }
    
        return () => {
            stopNotificationPolling();
        };
    });
</script>

<ModeWatcher />  
<main>
    <Header/>
    <slot/>
    {#if $activeUser}
        <NotificationToast />
    {/if}
</main>