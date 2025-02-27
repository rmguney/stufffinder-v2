<script>
        import { ModeWatcher } from "mode-watcher";
        import Header from '$lib/components/header.svelte';
        import NotificationToast from '$lib/components/NotificationToast.svelte';
        import { onMount, afterUpdate } from 'svelte';
        import '$lib/fetchWithAuth.js';
        import { activeUser } from '../userStore';
        import { startNotificationPolling, stopNotificationPolling, fetchNotifications } from '../notificationStore';
        import { page } from '$app/stores';
    
        // Store the current path to detect navigation changes
        let currentPath = '';
        
        // Listen for page changes to refresh notifications
        $: if ($page.url.pathname !== currentPath && $activeUser) {
            currentPath = $page.url.pathname;
            // Don't refetch on the notifications page itself to avoid duplicate calls
            if (currentPath !== '/notifications') {
                console.log('Page changed, fetching notifications');
                fetchNotifications();
            }
        }
        
        onMount(() => {
            if (performance.navigation.type === 1 && window.location.pathname !== '/') {
                window.location.href = '/';
            }
    
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