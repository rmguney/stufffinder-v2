import { getAuthHeader } from '$lib/utils/auth';
import { PUBLIC_API_URL } from "$env/static/public";
import { activeUser } from "./userStore";
import { derived, writable, get } from 'svelte/store';

// Create a store for notifications
export const notifications = writable([]);
export const unreadCount = writable(0);
export const followerNotifications = writable([]);
export const unreadFollowerCount = writable(0);
export const showNotifications = writable(false);

let currentUser = '';

function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}

// Function to fetch notifications for the current user
export async function fetchNotifications() {
    try {
        // First get the current user's ID
        const token = getCookie('tokenKey');
        if (!token) return;

        activeUser.subscribe((value) => {
            currentUser = value;
          });

        const username = currentUser;
        if (!username) return;

        // Get user ID from username
        const userIdResponse = await fetch(`${PUBLIC_API_URL}/api/auth/username/${username}`, {
            headers: getAuthHeader()
        });
        
        if (!userIdResponse.ok) {
            throw new Error('Failed to get user ID');
        }
        
        const userData = await userIdResponse.json();
        const userId = userData.userId;
        
        // Fetch notifications for this user
        const notificationsResponse = await fetch(`${PUBLIC_API_URL}/api/notifications/${userId}`, {
            headers: getAuthHeader()
        });
        
        if (!notificationsResponse.ok) {
            throw new Error('Failed to fetch notifications');
        }
        
        const notificationsData = await notificationsResponse.json();
        
        // Update stores
        notifications.set(notificationsData);
        updateUnreadCount(notificationsData);
    } catch (error) {
        console.error('Error fetching notifications:', error);
    }
}

export async function fetchFollowerNotifications() {
    try {
        const token = getCookie('tokenKey');
        if (!token) return;

        activeUser.subscribe((value) => {
            currentUser = value;
        });

        const username = currentUser;
        if (!username) return;

        const userIdResponse = await fetch(`${PUBLIC_API_URL}/api/auth/username/${username}`, {
            headers: getAuthHeader()
        });

        if (!userIdResponse.ok) {
            throw new Error('Failed to get user ID');
        }

        const userData = await userIdResponse.json();
        const userId = userData.userId;

        const response = await fetch(`${PUBLIC_API_URL}/api/follower-notifications/${userId}`, {
            headers: getAuthHeader()
        });

        if (!response.ok) {
            throw new Error('Failed to fetch follower notifications');
        }

        const data = await response.json();
        followerNotifications.set(data);
        updateUnreadFollowerCount(data);
    } catch (error) {
        console.error('Error fetching follower notifications:', error);
    }
}


// Function to mark a notification as read
export async function markAsRead(notificationId) {
    try {
        const response = await fetch(`${PUBLIC_API_URL}/api/notifications/${notificationId}/read`, {
            method: 'PUT',
            headers: getAuthHeader()
        });
        
        if (!response.ok) {
            throw new Error('Failed to mark notification as read');
        }
        
        // Update the store to reflect the read status
        notifications.update(items => {
            return items.map(item => {
                if (item.id === notificationId) {
                    return { ...item, isRead: true };
                }
                return item;
            });
        });
        
        // Update unread count
        notifications.subscribe(items => {
            updateUnreadCount(items);
        })();
    } catch (error) {
        console.error('Error marking notification as read:', error);
    }
}

export async function markFollowerNotificationAsRead(notificationId) {
    try {
        const response = await fetch(`${PUBLIC_API_URL}/api/follower-notifications/${notificationId}/read`, {
            method: 'PUT',
            headers: getAuthHeader()
        });

        if (!response.ok) {
            throw new Error('Failed to mark follower notification as read');
        }

        followerNotifications.update(items => {
            return items.map(item => {
                if (item.id === notificationId) {
                    return { ...item, read: true };
                }
                return item;
            });
        });

        followerNotifications.subscribe(items => {
            updateUnreadFollowerCount(items);
        })();
    } catch (error) {
        console.error('Error marking follower notification as read:', error);
    }
}


// Function to mark all notifications as read
export async function markAllAsRead() {
    try {
        const notificationsValue = get(notifications);
        for (const notification of notificationsValue) {
            if (!notification.isRead) {
                await markAsRead(notification.id);
            }
        }

        fetchNotifications();
    } catch (error) {
        console.error('Error marking all notifications as read:', error);
    }
}

export async function markAllFollowerNotificationsAsRead() {
    try {
        const followerNotifValue = get(followerNotifications);
        for (const notif of followerNotifValue) {
            if (!notif.read) {
                await markFollowerNotificationAsRead(notif.id);
            }
        }

        fetchFollowerNotifications();
    } catch (error) {
        console.error('Error marking all follower notifications as read:', error);
    }
}


// Helper function to update unread count
function updateUnreadCount(notificationsArray) {
    const count = notificationsArray.filter(notification => !notification.isRead).length;
    unreadCount.set(count);
}

function updateUnreadFollowerCount(notificationsArray) {
    const count = notificationsArray.filter(n => !n.read).length;
    unreadFollowerCount.set(count);
}




// Poll for new notifications every minute
let pollingInterval;

export function startNotificationPolling() {
    // Clear any existing interval
    if (pollingInterval) {
        clearInterval(pollingInterval);
    }

    // Initial fetch
    fetchNotifications();
    
    // Set up polling every minute
    pollingInterval = setInterval(fetchNotifications, 60000);
}

export function stopNotificationPolling() {
    if (pollingInterval) {
        clearInterval(pollingInterval);
        pollingInterval = null;
    }
}

export const allNotifications = derived(
    [notifications, followerNotifications],
    ([$notifications, $followerNotifications]) => {
        return [...$notifications, ...$followerNotifications].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    }
);

export const allUnreadCount = derived(
    [unreadCount, unreadFollowerCount],
    ([$unreadCount, $unreadFollowerCount]) => $unreadCount + $unreadFollowerCount
);