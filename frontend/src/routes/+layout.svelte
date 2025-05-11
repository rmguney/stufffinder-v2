<script>
    import "../app.css";
    import { ModeWatcher } from "mode-watcher";
    import Header from '$lib/components/header.svelte';
    import NotificationToast from '$lib/components/NotificationToast.svelte';
    import { onMount, afterUpdate } from 'svelte';
    import '$lib/fetchWithAuth.js';
    import { activeUser } from '../userStore';
    import { startNotificationPolling, stopNotificationPolling, fetchNotifications } from '../notificationStore';
    import { page } from '$app/stores';
    import { forceRefreshThreads } from '../threadStore';
    import { goto } from '$app/navigation';
    
    // Capacitor swipe handling
    let isCapacitorEnvironment = false;
    let App;
    let navigationHistory = [];
    
    // Store the current path to detect navigation changes
    let currentPath = '';
    let initialLoad = true;
    
    // Listen for page changes to refresh notifications and threads
    $: if ($page.url.pathname !== currentPath) {
        // Add current path to history before updating
        if (currentPath && !initialLoad) {
            navigationHistory.push(currentPath);
            // Limit history size
            if (navigationHistory.length > 10) navigationHistory.shift();
        }
        
        currentPath = $page.url.pathname;
        // console.log("currentpath" + currentPath)
        
        // Don't refetch on the notifications page itself to avoid duplicate calls
        if (currentPath !== '/notifications') {
            // console.log('Page changed, fetching notifications');
            if ($activeUser) {
                fetchNotifications();
                // console.log("notif called from layout")
            }
        }
        
        // If navigating to home, force refresh threads
        if (currentPath === '/' && !initialLoad) {
            // console.log('Navigated to home, refreshing threads');
            //forceRefreshThreads();
        }
        
        initialLoad = false;
    }
    
    onMount(async () => {
        // Initialize current path
        currentPath = window.location.pathname;
        
        // Start notification polling if user is logged in
        if ($activeUser) {
            startNotificationPolling();
        }
        
        // Dynamically import Capacitor App to avoid issues in browser environment
        try {
            const capacitorApp = await import('@capacitor/app');
            App = capacitorApp.App;
            isCapacitorEnvironment = true;
            
            // Set up back button handler
            App.addListener('backButton', ({ canGoBack }) => {
                if (navigationHistory.length > 0) {
                    // Navigate to previous page in our history
                    const prevPath = navigationHistory.pop();
                    goto(prevPath);
                } else if (currentPath !== '/') {
                    // If no history but not on home, go home
                    goto('/');
                } else {
                    // If on home with no history, exit app
                    App.exitApp();
                }
            });
        } catch (error) {
            // console.log('Not running in Capacitor environment');
        }
    
        return () => {
            stopNotificationPolling();
            // Clean up listeners if in Capacitor environment
            if (isCapacitorEnvironment && App) {
                App.removeAllListeners();
            }
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