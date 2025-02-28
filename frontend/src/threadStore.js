import { writable } from 'svelte/store';
import { PUBLIC_API_URL } from "$env/static/public";

const LOCAL_STORAGE_KEY = 'threadStoreData';
const LAST_UPDATED_KEY = 'threadStoreLastUpdated';
const CACHE_EXPIRY_TIME = 5 * 60 * 1000; // 5 minutes in milliseconds

// Utility function to safely check for localStorage
const isClient = false;

// Initialize store with empty array
export const threadStore = writable([]);
export const isLoading = writable(false);
export const shouldRefreshThreads = writable(false); // Renamed from forceRefreshThreads

// Function to normalize date formats
const normalizeDateFormat = (dateString) => {
    if (!dateString) return '';
    
    try {
        // Try to parse the date string and ensure it's in ISO format
        const date = new Date(dateString);
        if (isNaN(date.getTime())) {
            console.warn("Invalid date encountered:", dateString);
            return '';
        }
        return date.toISOString();
    } catch (error) {
        console.error("Error normalizing date:", error, "for:", dateString);
        return '';
    }
};

// Function to load initial data - will be called on app initialization
export async function initializeThreadStore() {
    //if (!isClient) return; // Skip during SSR

    isLoading.set(true);
    
    try {
        // Check if we should use cache or fetch fresh data        
        await fetchFreshThreads();
    } catch (error) {
        console.error('Thread store initialization error:', error);
        // Fall back to cache if fetch fails
        const cachedData = loadFromLocalStorage();
        threadStore.set(cachedData);
    } finally {
        isLoading.set(false);
    }
}

// Function to determine if we should refresh the cache
function shouldRefreshCache() {
    if (!isClient) return true;
    
    const lastUpdated = localStorage.getItem(LAST_UPDATED_KEY);
    if (!lastUpdated) return true;
    
    const now = Date.now();
    const lastUpdatedTime = parseInt(lastUpdated, 10);
    
    // Check if cache is older than expiry time
    return (now - lastUpdatedTime) > CACHE_EXPIRY_TIME;
}

// Function to fetch fresh threads from API - renamed to avoid conflict
async function refreshThreadsData() {
    try {
        const response = await fetch(`${PUBLIC_API_URL}/api/posts/getForPostList?page=0&size=100`);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();
        const threads = data.content || [];
        
        // Update the store with fresh data
        threadStore.set(threads);
        
        // Save to localStorage with timestamp
        saveToLocalStorage(threads);
        localStorage.setItem(LAST_UPDATED_KEY, Date.now().toString());
        
        return threads;
    } catch (error) {
        console.error('Error fetching fresh threads:', error);
        throw error;
    }
}

// Load data from localStorage
const loadFromLocalStorage = () => {
    if (!isClient) return []; // Return an empty array during SSR
    const storedData = localStorage.getItem(LOCAL_STORAGE_KEY);
    return storedData ? JSON.parse(storedData) : [];
};

// Save data to localStorage
const saveToLocalStorage = (data) => {
    if (isClient) {
        localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(data));
    }
};

// Function to add a thread to the store
export function addThread(thread) {
    // Normalize dates
    const normalizedThread = {
        ...thread,
        createdAt: normalizeDateFormat(thread.createdAt),
        updatedAt: normalizeDateFormat(thread.updatedAt || thread.createdAt)
    };

    threadStore.update(threads => {
        // Check if thread already exists
        const existingThreadIndex = threads.findIndex(t => t.id === normalizedThread.id);
        if (existingThreadIndex >= 0) {
            // Update existing thread
            threads[existingThreadIndex] = normalizedThread;
            return [...threads];
        } else {
            // Add new thread
            return [...threads, normalizedThread];
        }
    });
}

// Function to update or add a thread
export function updateThread(newThread) {
    // Normalize dates
    const normalizedThread = {
        ...newThread,
        createdAt: normalizeDateFormat(newThread.createdAt),
        updatedAt: normalizeDateFormat(newThread.updatedAt || newThread.createdAt)
    };

    threadStore.update(threads => {
        const index = threads.findIndex(t => t.id === normalizedThread.id);
        if (index !== -1) {
            // Update existing thread
            const updatedThreads = [...threads];
            updatedThreads[index] = {
                ...updatedThreads[index],
                ...normalizedThread
            };
            saveToLocalStorage(updatedThreads);
            return updatedThreads;
        } else {
            // Add new thread
            const updatedThreads = [...threads, normalizedThread];
            saveToLocalStorage(updatedThreads);
            return updatedThreads;
        }
    });
}

// Function to load threads from API
export async function loadThreads() {
    try {
        const response = await fetch(`${PUBLIC_API_URL}/api/posts`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        if (!response.ok) {
            throw new Error('Failed to fetch threads');
        }
        
        const data = await response.json();
        
        // Clear existing threads and add normalized ones
        threadStore.set([]);
        data.forEach(thread => {
            // Create a normalized version of each thread
            addThread({
                id: thread.id,
                title: thread.title,
                description: thread.description,
                tags: thread.tags || [],
                mysteryObjectImage: thread.mysteryObjectImage,
                mysteryObject: thread.mysteryObject || null,
                author: thread.author,
                createdAt: thread.createdAt, // This will be normalized in addThread
                updatedAt: thread.updatedAt, // This will be normalized in addThread
                upvotes: thread.upvotes || 0,
                downvotes: thread.downvotes || 0,
                userUpvoted: thread.userUpvoted || false,
                userDownvoted: thread.userDownvoted || false,
                solved: thread.solved || false
            });
        });
        
        console.log("Threads loaded and normalized:", data.length);
        return data;
    } catch (error) {
        console.error('Error loading threads:', error);
        throw error;
    }
}

// Update vote count for a thread
export async function updateThreadVote(threadId, isUpvote) {
    const endpoint = `${PUBLIC_API_URL}/api/posts/${isUpvote ? 'upvote' : 'downvote'}/${threadId}`;
    try {
        const response = await fetch(endpoint, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        if (!response.ok) throw new Error('Vote update failed');
        const data = await response.json();
        
        threadStore.update(threads => {
            const updatedThreads = threads.map(thread =>
                thread.id === threadId ? { 
                    ...thread, 
                    upvotes: data.upvotes,
                    downvotes: data.downvotes,
                    userUpvoted: isUpvote,
                    userDownvoted: !isUpvote
                } : thread
            );
            saveToLocalStorage(updatedThreads);
            return updatedThreads;
        });
    } catch (error) {
        console.error('Error updating vote:', error);
    }
}

// Add a comment to a thread - updated for new API
export async function addCommentToThread(threadId, content, parentCommentId = null) {
    try {
        const payload = {
            content: content,
            postId: threadId,
            parentCommentId: parentCommentId
        };

        // console.log("Adding comment with payload:", payload);

        const authToken = localStorage.getItem('tokenKey');
        const response = await fetch(`${PUBLIC_API_URL}/api/comments/create`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': authToken ? `Bearer ${authToken}` : ''
            },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`Failed to add comment: ${errorText}`);
        }
        
        const newComment = await response.json();
        // console.log("New comment response:", newComment);

        // Update store with new comment
        threadStore.update(threads => {
            const updatedThreads = threads.map(thread => {
                if (thread.id === parseInt(threadId)) {
                    const updatedComments = [...(thread.comments || [])];
                    // If this is a reply, find the parent comment and add to its replies
                    if (parentCommentId) {
                        const findAndAddReply = (comments) => {
                            for (let comment of comments) {
                                if (comment.id === parentCommentId) {
                                    comment.replies = [...(comment.replies || []), newComment];
                                    return true;
                                }
                                if (comment.replies && comment.replies.length > 0) {
                                    if (findAndAddReply(comment.replies)) return true;
                                }
                            }
                            return false;
                        };
                        findAndAddReply(updatedComments);
                    } else {
                        // Add as a root comment
                        updatedComments.push({
                            ...newComment,
                            replies: []
                        });
                    }
                    return { ...thread, comments: updatedComments };
                }
                return thread;
            });
            saveToLocalStorage(updatedThreads);
            return updatedThreads;
        });
        
        return newComment;
    } catch (error) {
        console.error('Error adding comment:', error);
        throw error;
    }
}

// Update vote count for a comment
export async function updateCommentVote(commentId, isUpvote) {
    const endpoint = `${PUBLIC_API_URL}/api/comments/${isUpvote ? 'upvote' : 'downvote'}/${commentId}`;
    try {
        const response = await fetch(endpoint, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        if (!response.ok) throw new Error('Comment vote update failed');
        const data = await response.json();
        
        // Update the comment vote count in the thread store
        threadStore.update(threads => {
            const updatedThreads = threads.map(thread => ({
                ...thread,
                comments: thread.comments?.map(comment =>
                    comment.id === commentId
                        ? { 
                            ...comment, 
                            upvotes: data.upvotes,
                            downvotes: data.downvotes,
                            userUpvoted: isUpvote,
                            userDownvoted: !isUpvote
                        } 
                        : comment
                ) || []
            }));
            saveToLocalStorage(updatedThreads);
            return updatedThreads;
        });
        
        return data;
    } catch (error) {
        console.error('Error updating comment vote:', error);
        throw error;
    }
}

// Mark a comment as the best answer
export async function markBestAnswer(postId, commentId) {
    try {
        const response = await fetch(`${PUBLIC_API_URL}/api/posts/${postId}/markBestAnswer/${commentId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        if (!response.ok) throw new Error('Failed to mark best answer');
        const data = await response.json();
        
        // Update the comment's bestAnswer status in the thread store
        threadStore.update(threads => {
            const updatedThreads = threads.map(thread => 
                thread.id === postId
                    ? {
                        ...thread,
                        comments: thread.comments?.map(comment => ({
                            ...comment,
                            bestAnswer: comment.id === commentId
                        })) || []
                      }
                    : thread
            );
            saveToLocalStorage(updatedThreads);
            return updatedThreads;
        });
        
        return data;
    } catch (error) {
        console.error('Error marking best answer:', error);
        throw error;
    }
}

// Load comments for a specific thread
export async function loadCommentsForThread(threadId) {
    try {
        // console.log(`Loading comments for thread ${threadId}`);
        
        const response = await fetch(`${PUBLIC_API_URL}/api/comments/get/${threadId}`);
        if (!response.ok) throw new Error('Failed to fetch comments');
        
        const rawComments = await response.json();
        // console.log("Raw comments received:", rawComments);

        // Process comments to build hierarchy
        const processComments = (comments) => {
            const commentMap = new Map();
            const rootComments = [];
            
            // First pass: Initialize all comments
            comments.forEach(comment => {
                commentMap.set(comment.id, {
                    ...comment,
                    replies: []
                });
            });
            
            // Second pass: Build hierarchy
            comments.forEach(comment => {
                const currentComment = commentMap.get(comment.id);
                if (comment.parentCommentId) {
                    const parentComment = commentMap.get(comment.parentCommentId);
                    if (parentComment) {
                        parentComment.replies.push(currentComment);
                    } else {
                        rootComments.push(currentComment);
                    }
                } else {
                    rootComments.push(currentComment);
                }
            });
            
            return rootComments;
        };

        const organizedComments = processComments(rawComments);
        
        // Update thread store
        threadStore.update(threads => {
            const updatedThreads = threads.map(thread => {
                if (parseInt(thread.id) === parseInt(threadId)) {
                    return { 
                        ...thread, 
                        comments: organizedComments 
                    };
                }
                return thread;
            });
            saveToLocalStorage(updatedThreads);
            return updatedThreads;
        });
        
        return organizedComments;
    } catch (error) {
        console.error('Error loading comments:', error);
        throw error;
    }
}

// Force refresh all threads - can be called from anywhere in the app
export function forceRefreshThreads() {
    return fetchFreshThreads();
}