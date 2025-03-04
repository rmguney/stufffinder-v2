<script>
    import { onMount } from 'svelte';
    import * as Card from "$lib/components/ui/card";
    import { Button } from "$lib/components/ui/button";
    import { Separator } from "$lib/components/ui/separator";
    import { activeUser } from "../../userStore";
    import { 
        notifications, 
        unreadCount, 
        fetchNotifications,
        markAsRead,
        markAllAsRead
    } from "../../notificationStore";
    import { goto } from "$app/navigation";

    let loading = true;

    onMount(async () => {
        if (!$activeUser) {
            goto('/');
            return;
        }
        
        //await fetchNotifications();
        loading = false;
    });

    function formatDate(timestamp) {
        if (!timestamp) return '';
        
        const date = new Date(timestamp);
        return date.toLocaleString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric',
            hour: 'numeric',
            minute: '2-digit',
            hour12: true
        });
    }

    function getTypeIcon(type) {
        switch(type) {
            case 'COMMENT':
                return `
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-blue-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" />
                    </svg>
                `;
            case 'UPVOTE':
                return `
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-green-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 10h4.764a2 2 0 011.789 2.894l-3.5 7A2 2 0 0115.263 21h-4.017c-.163 0-.326-.02-.485-.06L7 20m7-10V5a2 2 0 00-2-2h-.095c-.5 0-.905.405-.905.905 0 .714-.211 1.412-.608 2.006L7 11v9m7-10h-2M7 20H5a2 2 0 01-2-2v-6a2 2 0 012-2h2.5" />
                    </svg>
                `;
            case 'BEST_ANSWER':
                return `
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-yellow-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                `;
            default:
                return `
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                    </svg>
                `;
        }
    }

    function handleNotificationClick(notification) {
        markAsRead(notification.id);
        
        if (notification.postId) {
            goto(`/thread/${notification.postId}`);
        }
    }
</script>

<div class="flex justify-center p-6 lg:py-10 bg-change dark:bg-dark shifting">
    <div class="w-full lg:w-2/3">
        <Card.Root class="bg-opacity-90">
            <Card.Header class="flex flex-col sm:flex-row justify-between items-center gap-2 p-6">
                <Card.Title class="text-2xl font-bold">Your Notifications</Card.Title>
                
                {#if $unreadCount > 0}
                    <Button 
                        variant="outline"
                        class="hover:bg-rose-900 hover:text-white transition-colors"
                        on:click={markAllAsRead}
                    >
                        Mark all as read
                    </Button>
                {/if}
            </Card.Header>
            
            <Separator />
            
            {#if loading}
                <div class="flex justify-center items-center py-16">
                    <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-rose-900"></div>
                </div>
            {:else if $notifications.length === 0}
                <div class="flex flex-col items-center justify-center py-16 text-center">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-16 w-16 text-neutral-400 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                    </svg>
                    <p class="text-xl font-medium text-neutral-700 dark:text-neutral-300 mb-2">No notifications yet</p>
                    <p class="text-neutral-500 dark:text-neutral-400 max-w-md">
                        Notifications will appear here when someone comments on your posts, upvotes your comments, or marks your comment as the best answer.
                    </p>
                </div>
            {:else}
                <div class="divide-y divide-neutral-200 dark:divide-neutral-700">
                    {#each $notifications as notification}
                        <div 
                            class="p-4 flex items-start gap-4 cursor-pointer transition-colors hover:bg-neutral-50 dark:hover:bg-neutral-900 {notification.isRead ? 'opacity-75' : 'bg-neutral-50 dark:bg-neutral-900/40'}"
                            on:click={() => handleNotificationClick(notification)}
                        >
                            <!-- Icon based on notification type -->
                            <div class="flex-shrink-0 mt-1">
                                {@html getTypeIcon(notification.type)}
                            </div>
                            
                            <!-- Content -->
                            <div class="flex-1">
                                <p class="text-sm md:text-base">{notification.message}</p>
                                <p class="text-xs text-neutral-500 dark:text-neutral-400 mt-1">
                                    {formatDate(notification.createdAt)}
                                </p>
                            </div>
                            
                            <!-- Read status -->
                            {#if !notification.isRead}
                                <span class="inline-block w-2 h-2 bg-rose-600 rounded-full flex-shrink-0 mt-2"></span>
                            {/if}
                        </div>
                    {/each}
                </div>
            {/if}
        </Card.Root>
    </div>
</div>