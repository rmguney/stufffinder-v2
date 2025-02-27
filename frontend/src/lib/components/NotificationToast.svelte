<script>
    import { onMount, onDestroy } from 'svelte';
    import { notifications, unreadCount, markAsRead } from '../../notificationStore';
    import { fly, fade } from 'svelte/transition';
    import { goto } from '$app/navigation';

    let showToast = false;
    let toastNotification = null;
    let currentCount = 0;
    let timer;

    $: if ($unreadCount > currentCount) {
        const newNotifications = $notifications.filter(n => !n.isRead);
        if (newNotifications.length > 0) {
            toastNotification = newNotifications[0];
            showToast = true;
            resetTimer();
        }
        currentCount = $unreadCount;
    }

    function resetTimer() {
        clearTimeout(timer);
        timer = setTimeout(() => {
            showToast = false;
        }, 5000); // Hide toast after 5 seconds
    }

    function handleToastClick() {
        if (toastNotification) {
            // Mark notification as read
            if (toastNotification.id) {
                markAsRead(toastNotification.id);
            }
            
            // Navigate to the related post if available
            if (toastNotification.postId) {
                goto(`/thread/${toastNotification.postId}`);
            }
        }
        showToast = false;
    }

    function getIconForType(type) {
        switch (type) {
            case 'COMMENT':
                return `<svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-blue-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" />
                </svg>`;
            case 'UPVOTE':
                return `<svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-green-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 10h4.764a2 2 0 011.789 2.894l-3.5 7A2 2 0 0115.263 21h-4.017c-.163 0-.326-.02-.485-.06L7 20m7-10V5a2 2 0 00-2-2h-.095c-.5 0-.905.405-.905.905 0 .714-.211 1.412-.608 2.006L7 11v9m7-10h-2M7 20H5a2 2 0 01-2-2v-6a2 2 0 012-2h2.5" />
                </svg>`;
            case 'BEST_ANSWER':
                return `<svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-yellow-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>`;
            default:
                return `<svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-neutral-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                </svg>`;
        }
    }

    onDestroy(() => {
        clearTimeout(timer);
    });
</script>

{#if showToast && toastNotification}
    <div 
        class="fixed bottom-4 right-4 max-w-md z-50 flex"
        transition:fly={{ y: 20, duration: 300 }}
        on:click={handleToastClick}
    >
        <div 
            class="bg-white dark:bg-neutral-900 shadow-lg rounded-lg p-4 border border-neutral-200 dark:border-neutral-800 flex items-start gap-3 cursor-pointer hover:shadow-xl transition-shadow duration-200"
            on:mouseenter={() => clearTimeout(timer)}
            on:mouseleave={resetTimer}
        >
            <!-- Notification icon -->
            <div class="flex-shrink-0 w-10 h-10 flex items-center justify-center rounded-full bg-neutral-100 dark:bg-neutral-800">
                {@html getIconForType(toastNotification.type)}
            </div>
            
            <!-- Content -->
            <div class="flex-1">
                <p class="font-medium text-sm">New Notification</p>
                <p class="text-sm text-neutral-700 dark:text-neutral-300">{toastNotification.message}</p>
                <p class="text-xs text-neutral-500 dark:text-neutral-400 mt-1">Click to view</p>
            </div>
            
            <!-- Close button -->
            <button 
                class="text-neutral-400 hover:text-neutral-700 dark:hover:text-neutral-200" 
                on:click|stopPropagation={() => showToast = false}
            >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
            </button>
        </div>
    </div>
{/if}