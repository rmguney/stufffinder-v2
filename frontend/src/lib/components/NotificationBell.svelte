<script>
    import { onMount, onDestroy } from 'svelte';
    import * as Popover from "$lib/components/ui/popover/index.js";
    import * as Card from "$lib/components/ui/card";
    import { Button } from "$lib/components/ui/button";
    import { Separator } from "$lib/components/ui/separator";
    import { activeUser } from "../../userStore";
    import { 
        notifications, 
        unreadCount, 
        showNotifications,
        fetchNotifications, 
        markAsRead,
        markAllAsRead,
        startNotificationPolling,
        stopNotificationPolling
    } from "../../notificationStore";
    import { goto } from "$app/navigation";

    let isOpen = false;

    // Format the notification timestamp
    function formatTimestamp(timestamp) {
        const date = new Date(timestamp);
        const now = new Date();
        const diff = now - date;
        
        // If less than 24 hours, show relative time
        if (diff < 24 * 60 * 60 * 1000) {
            // Less than a minute
            if (diff < 60 * 1000) {
                return 'Just now';
            }
            // Less than an hour
            if (diff < 60 * 60 * 1000) {
                const minutes = Math.floor(diff / (60 * 1000));
                return `${minutes} ${minutes === 1 ? 'minute' : 'minutes'} ago`;
            }
            // Less than a day
            const hours = Math.floor(diff / (60 * 60 * 1000));
            return `${hours} ${hours === 1 ? 'hour' : 'hours'} ago`;
        }
        
        // Show date and time for older notifications
        return date.toLocaleString('en-US', { 
            month: 'short', 
            day: 'numeric',
            hour: 'numeric',
            minute: '2-digit'
        });
    }

    // Handle notification click
    function handleNotificationClick(notification) {
        // Mark notification as read
        markAsRead(notification.id);

        // Navigate to the appropriate page based on the notification type
        if (notification.postId) {
            isOpen = false;
            goto(`/thread/${notification.postId}`);
        }
    }

    // Bell animation when receiving new notifications
    $: if ($unreadCount > 0) {
        const bell = document.querySelector('.notification-bell');
        if (bell) {
            bell.classList.add('animate-ring');
            setTimeout(() => {
                bell.classList.remove('animate-ring');
            }, 1000);
        }
    }

    // Start and stop notification polling based on user login status
    $: if ($activeUser) {
        startNotificationPolling();
    } else {
        stopNotificationPolling();
    }

    onMount(() => {
        if ($activeUser) {
            fetchNotifications();
        }
    });

    onDestroy(() => {
        stopNotificationPolling();
    });
</script>

<style>
    /* Bell ring animation */
    @keyframes ring {
        0% { transform: rotate(0); }
        5% { transform: rotate(15deg); }
        10% { transform: rotate(-15deg); }
        15% { transform: rotate(13deg); }
        20% { transform: rotate(-13deg); }
        25% { transform: rotate(11deg); }
        30% { transform: rotate(-11deg); }
        35% { transform: rotate(9deg); }
        40% { transform: rotate(-9deg); }
        45% { transform: rotate(7deg); }
        50% { transform: rotate(-7deg); }
        55% { transform: rotate(5deg); }
        60% { transform: rotate(-5deg); }
        65% { transform: rotate(3deg); }
        70% { transform: rotate(-3deg); }
        75% { transform: rotate(1deg); }
        80% { transform: rotate(-1deg); }
        85% { transform: rotate(0); }
        100% { transform: rotate(0); }
    }

    .animate-ring {
        animation: ring 1s ease;
        transform-origin: top center;
    }

    /* Badge animation */
    @keyframes pulse {
        0% { transform: scale(0.95); }
        50% { transform: scale(1.05); }
        100% { transform: scale(0.95); }
    }

    .badge-pulse {
        animation: pulse 2s infinite;
    }

    /* Notification type indicators */
    .indicator {
        @apply w-2 h-2 rounded-full absolute left-1 top-1/2 transform -translate-y-1/2;
    }
    
    .comment-indicator {
        @apply bg-blue-500;
    }
    
    .upvote-indicator {
        @apply bg-green-500;
    }
    
    .best-answer-indicator {
        @apply bg-yellow-500;
    }
</style>

{#if $activeUser}
    <Popover.Root bind:open={isOpen}>
        <Popover.Trigger asChild let:builder>
            <Button
                variant="ghost"
                size="icon"
                class="relative notification-bell rounded-full h-8 w-8 sm:h-10 sm:w-10 p-0"
                builders={[builder]}
            >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 sm:h-5 sm:w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
                    <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
                </svg>
                
                {#if $unreadCount > 0}
                    <span class="absolute -top-1 -right-1 inline-flex items-center justify-center px-1.5 sm:px-2 py-0.5 sm:py-1 text-[10px] sm:text-xs font-bold leading-none text-white transform translate-x-1/2 -translate-y-1/2 bg-rose-900 rounded-full badge-pulse min-w-[16px] sm:min-w-[20px] h-[16px] sm:h-[20px]">
                        {$unreadCount > 9 ? '9+' : $unreadCount}
                    </span>
                {/if}
            </Button>
        </Popover.Trigger>
        
        <Popover.Content 
            align="end" 
            class="w-[280px] sm:w-80 p-0 bg-white dark:bg-neutral-950 rounded-lg shadow-lg border border-neutral-200 dark:border-neutral-800"
        >
            <Card.Root class="border-0 shadow-none">
                <Card.Header class="px-3 sm:px-4 py-2 sm:py-3 flex flex-row justify-between items-center space-y-0">
                    <Card.Title class="text-base sm:text-lg">Notifications</Card.Title>
                    {#if $unreadCount > 0}
                        <Button 
                            variant="ghost" 
                            size="sm" 
                            class="text-xs hover:bg-rose-900 hover:text-white transition-colors h-7 sm:h-8"
                            on:click={markAllAsRead}
                        >
                            Mark all as read
                        </Button>
                    {/if}
                </Card.Header>
                
                <Separator />
                
                <div class="overflow-y-auto max-h-[300px] sm:max-h-[350px] py-1">
                    {#if $notifications.length === 0}
                        <div class="py-6 sm:py-8 text-center text-neutral-500 dark:text-neutral-400">
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 sm:h-12 sm:w-12 mx-auto mb-2 text-neutral-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                            </svg>
                            <p class="text-sm sm:text-base">No notifications yet</p>
                        </div>
                    {:else}
                        {#each $notifications as notification}
                            <div 
                                class="relative pl-6 px-3 sm:px-4 py-2 sm:py-3 cursor-pointer transition-colors hover:bg-neutral-100 dark:hover:bg-neutral-900 {notification.isRead ? 'opacity-75' : 'bg-opacity-50 bg-neutral-50 dark:bg-neutral-950'}"
                                on:click={() => handleNotificationClick(notification)}
                            >
                                <!-- Type indicator -->
                                <div class="indicator 
                                    {notification.type === 'COMMENT' ? 'comment-indicator' : 
                                    notification.type === 'UPVOTE' ? 'upvote-indicator' : 
                                    'best-answer-indicator'}">
                                </div>
                                
                                <!-- Content -->
                                <div class="flex flex-col">
                                    <span class="text-xs sm:text-sm">
                                        {notification.message}
                                    </span>
                                    <span class="text-[10px] sm:text-xs text-neutral-500 dark:text-neutral-400 mt-1">
                                        {formatTimestamp(notification.createdAt)}
                                    </span>
                                </div>
                            </div>
                        {/each}
                    {/if}
                </div>
                
                <Separator />
                
                <Card.Footer class="p-2 flex justify-center">
                    <Button 
                        variant="ghost" 
                        size="sm" 
                        class="w-full text-xs text-neutral-500 dark:text-neutral-400 hover:bg-rose-900 hover:text-white h-7 sm:h-8"
                        on:click={() => isOpen = false}
                    >
                        Close
                    </Button>
                </Card.Footer>
            </Card.Root>
        </Popover.Content>
    </Popover.Root>
{/if}