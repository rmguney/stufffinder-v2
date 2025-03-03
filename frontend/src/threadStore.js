import { writable } from 'svelte/store';
import { PUBLIC_API_URL } from "$env/static/public";

const LOCAL_STORAGE_KEY = 'threadStoreData';
const LAST_UPDATED_KEY = 'threadStoreLastUpdated';
const CACHE_EXPIRY_TIME = 5 * 60 * 1000; // 5 minutes in milliseconds

// Properly detect client environment
const isClient = typeof window !== 'undefined';

// Initialize store with empty array
export const threadStore = writable([]);
export const isLoading = writable(false);
export const currentPage = writable(0);
export const hasMorePages = writable(true);
const PAGE_SIZE = 20; // Reduced from 100 to improve performance

// Comment cache to avoid rebuilding hierarchies
const commentCache = new Map();

// Debounce function to limit localStorage writes
function debounce(func, wait) {
    let timeout;
    return function(...args) {
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(this, args), wait);
    };
}

// Load data from localStorage
const loadFromLocalStorage = () => {
    if (!isClient) return []; 
    try {
        const storedData = localStorage.getItem(LOCAL_STORAGE_KEY);
        return storedData ? JSON.parse(storedData) : [];
    } catch (error) {
        console.error('Error loading from localStorage:', error);
        return [];
    }
};

// Save data to localStorage
const saveToLocalStorage = (data) => {
    if (isClient) {
        try {
            localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(data));
        } catch (error) {
            console.error('Error saving to localStorage:', error);
        }
    }
};

// Debounced version of saveToLocalStorage to reduce writes
const debouncedSaveToLocalStorage = debounce(saveToLocalStorage, 500);

// Function to initialize store
export async function initializeThreadStore() {
    if (!isClient) return; // Skip during SSR

    isLoading.set(true);
    
    try {
        if (shouldRefreshCache()) {
            await fetchThreadsPage(0);
        } else {
            // Use cached data
            const cachedData = loadFromLocalStorage();
            threadStore.set(cachedData);
        }
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

// Function to fetch threads with pagination
export async function fetchThreadsPage(page = 0) {
    try {
        isLoading.set(true);
        const response = await fetch(`${PUBLIC_API_URL}/api/posts/getForPostList?page=${page}&size=${PAGE_SIZE}`);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();
        const newThreads = data.content || [];
        
        // Check if we have more pages
        hasMorePages.set(!data.last);
        
        // Update the store
        if (page === 0) {
            // First page, replace data
            threadStore.set(newThreads);
        } else {
            // Subsequent pages, append data
            threadStore.update(threads => [...threads, ...newThreads]);
        }
        
        // Save to localStorage with timestamp (use current store value)
        threadStore.subscribe(value => {
            debouncedSaveToLocalStorage(value);
            if (isClient) {
                localStorage.setItem(LAST_UPDATED_KEY, Date.now().toString());
            }
        })();
        
        currentPage.set(page);
        return newThreads;
    } catch (error) {
        console.error('Error fetching threads:', error);
        throw error;
    } finally {
        isLoading.set(false);
    }
}

// Load next page of threads
export function loadMoreThreads() {
    let pageValue;
    currentPage.subscribe(value => {
        pageValue = value;
    })();
    
    return fetchThreadsPage(pageValue + 1);
}

// Force refresh all threads - now uses pagination
export function forceRefreshThreads() {
    // Reset cache
    commentCache.clear();
    return fetchThreadsPage(0);
}

// Function to update or add a thread
export function updateThread(newThread) {
    threadStore.update(threads => {
        const index = threads.findIndex(t => t.id === newThread.id);
        let updatedThreads;
        
        if (index !== -1) {
            // Update existing thread
            updatedThreads = [...threads];
            updatedThreads[index] = {
                ...updatedThreads[index],
                ...newThread
            };
        } else {
            // Add new thread
            updatedThreads = [...threads, newThread];
        }
        
        // Use debounced save
        debouncedSaveToLocalStorage(updatedThreads);
        return updatedThreads;
    });
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
            debouncedSaveToLocalStorage(updatedThreads);
            return updatedThreads;
        });
    } catch (error) {
        console.error('Error updating vote:', error);
    }
}

// Add a comment to a thread - optimized
export async function addCommentToThread(threadId, content, parentCommentId = null) {
    try {
        const payload = {
            content: content,
            postId: threadId,
            parentCommentId: parentCommentId
        };

        const authToken = isClient ? localStorage.getItem('tokenKey') : null;
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
        
        // Invalidate comment cache for this thread
        commentCache.delete(threadId);
        
        // Update store with new comment
        threadStore.update(threads => {
            const updatedThreads = threads.map(thread => {
                if (thread.id === parseInt(threadId)) {
                    // Only clone the affected thread
                    const updatedComments = [...(thread.comments || [])];
                    
                    if (parentCommentId) {
                        const findAndAddReply = (comments) => {
                            for (let i = 0; i < comments.length; i++) {
                                if (comments[i].id === parentCommentId) {
                                    comments[i] = {
                                        ...comments[i],
                                        replies: [...(comments[i].replies || []), newComment]
                                    };
                                    return true;
                                }
                                if (comments[i].replies && comments[i].replies.length > 0) {
                                    if (findAndAddReply(comments[i].replies)) return true;
                                }
                            }
                            return false;
                        };
                        findAndAddReply(updatedComments);
                    } else {
                        updatedComments.push({
                            ...newComment,
                            replies: []
                        });
                    }
                    return { ...thread, comments: updatedComments };
                }
                return thread;
            });
            debouncedSaveToLocalStorage(updatedThreads);
            return updatedThreads;
        });
        
        return newComment;
    } catch (error) {
        console.error('Error adding comment:', error);
        throw error;
    }
}

// Optimized recursive comment update function
function updateCommentRecursively(comments, commentId, updateFn) {
    return comments.map(comment => {
        if (comment.id === commentId) {
            return updateFn(comment);
        }
        if (comment.replies && comment.replies.length) {
            return {
                ...comment,
                replies: updateCommentRecursively(comment.replies, commentId, updateFn)
            };
        }
        return comment;
    });
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
        
        threadStore.update(threads => {
            const updatedThreads = threads.map(thread => {
                if (!thread.comments) return thread;
                
                return {
                    ...thread,
                    comments: updateCommentRecursively(
                        thread.comments,
                        commentId,
                        comment => ({
                            ...comment,
                            upvotes: data.upvotes,
                            downvotes: data.downvotes,
                            userUpvoted: isUpvote,
                            userDownvoted: !isUpvote
                        })
                    )
                };
            });
            
            debouncedSaveToLocalStorage(updatedThreads);
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
        
        // Clear cache for this thread
        commentCache.delete(postId);
        
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
            debouncedSaveToLocalStorage(updatedThreads);
            return updatedThreads;
        });
        
        return data;
    } catch (error) {
        console.error('Error marking best answer:', error);
        throw error;
    }
}

// Load comments for a specific thread - with caching
export async function loadCommentsForThread(threadId) {
    try {
        // Check cache first
        if (commentCache.has(threadId)) {
            return commentCache.get(threadId);
        }
        
        const response = await fetch(`${PUBLIC_API_URL}/api/comments/get/${threadId}`);
        if (!response.ok) throw new Error('Failed to fetch comments');
        
        const rawComments = await response.json();

        // More efficient comment hierarchy building
        const processComments = (comments) => {
            const commentMap = new Map();
            const rootComments = [];
            
            // First pass - build map
            comments.forEach(comment => {
                commentMap.set(comment.id, {
                    ...comment,
                    replies: []
                });
            });
            
            // Second pass - build tree in one go
            comments.forEach(comment => {
                const processedComment = commentMap.get(comment.id);
                
                if (comment.parentCommentId) {
                    const parent = commentMap.get(comment.parentCommentId);
                    if (parent) {
                        parent.replies.push(processedComment);
                    } else {
                        rootComments.push(processedComment);
                    }
                } else {
                    rootComments.push(processedComment);
                }
            });
            
            return rootComments;
        };

        const organizedComments = processComments(rawComments);
        
        // Cache the processed comments
        commentCache.set(threadId, organizedComments);
        
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
            debouncedSaveToLocalStorage(updatedThreads);
            return updatedThreads;
        });
        
        return organizedComments;
    } catch (error) {
        console.error('Error loading comments:', error);
        throw error;
    }
}