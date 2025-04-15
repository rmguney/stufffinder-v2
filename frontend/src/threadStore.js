import { writable } from 'svelte/store';
import { PUBLIC_API_URL } from "$env/static/public";
import { browser } from '$app/environment';

const LOCAL_STORAGE_KEY = 'threadStoreData';
const LAST_UPDATED_KEY = 'threadStoreLastUpdated';
const CACHE_EXPIRY_TIME = 5 * 60 * 1000; // 5 minutes in milliseconds

// Properly detect client environment
const isClient = browser;

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
    const debouncedFn = function(...args) {
        const context = this;
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(context, args), wait);
    };
    
    // Add a cancel method to clear the timeout
    debouncedFn.cancel = function() {
        clearTimeout(timeout);
    };
    
    return debouncedFn;
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
    isLoading.set(true);

    try {
        await fetchThreadsPage(0);
    } catch (error) {
        console.error('Thread store initialization error:', error);
    } finally {
        isLoading.set(false);
    }
}


// Function to determine if we should refresh the cache
function shouldRefreshCache() {
    return true;
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
        
        // Get the current value PROPERLY without creating a persistent subscription
        let currentThreads;
        const unsubscribe = threadStore.subscribe(value => {
            currentThreads = value;
        });
        unsubscribe(); // Properly unsubscribe
        
        // Save to localStorage with timestamp
        //debouncedSaveToLocalStorage(currentThreads);
        /* if (isClient) {
            localStorage.setItem(LAST_UPDATED_KEY, Date.now().toString());
        } */
        
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
    const unsubscribe = currentPage.subscribe(value => {
        pageValue = value;
    });
    unsubscribe(); // Properly unsubscribe
    
    return fetchThreadsPage(pageValue + 1);
}


// Force refresh all threads - now uses pagination
export function forceRefreshThreads() {
    // Reset cache
    commentCache.clear();
    return fetchThreadsPage(0);
}

// Function to update or add a thread
export function updateThread(updatedThread) {
    threadStore.update(threads => {
        const index = threads.findIndex(t => t.id == updatedThread.id);
        let updatedThreads;
        
        if (index !== -1) {
            // Update existing thread
            updatedThreads = [...threads];
            
            // Preserve comments if they exist
            const existingComments = updatedThreads[index].comments || [];
            
            updatedThreads[index] = {
                ...updatedThreads[index],
                ...updatedThread,
                comments: existingComments // Keep existing comments
            };
            
            // For UI updates - updated timestamp
            updatedThreads[index].updatedAt = new Date().toISOString();
        } else {
            // Add new thread
            updatedThreads = [...threads, updatedThread];
        }
        
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
            //debouncedSaveToLocalStorage(updatedThreads);
            return updatedThreads;
        });
    } catch (error) {
        console.error('Error updating vote:', error);
    }
}

function getCookie(name) {
    if (!isClient) return null;
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}

// Add a comment to a thread - optimized
export async function addCommentToThread(threadId, content, parentCommentId = null, commentType) {
    try {
        const payload = {
            content: content,
            postId: threadId,
            parentCommentId: parentCommentId,
            commentType: commentType
        };

        const authToken = getCookie('tokenKey');
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
            //debouncedSaveToLocalStorage(updatedThreads);
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
            
            //debouncedSaveToLocalStorage(updatedThreads);
            return updatedThreads;
        });
        
        return data;
    } catch (error) {
        console.error('Error updating comment vote:', error);
        throw error;
    }
}

// Mark a post as resolved with explanation and contributing comments
export async function resolvePost(postId, resolutionData) {
    try {
        const response = await fetch(`${PUBLIC_API_URL}/api/posts/${postId}/resolve`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(resolutionData)
        });
        
        if (!response.ok) throw new Error('Failed to resolve post');
        const data = await response.json();
        
        // Clear cache for this thread
        commentCache.delete(postId);
        
        threadStore.update(threads => {
            const updatedThreads = threads.map(thread => 
                thread.id === postId
                    ? {
                        ...thread,
                        solved: true,
                        resolution: resolutionData
                      }
                    : thread
            );
            return updatedThreads;
        });
        
        return data;
    } catch (error) {
        console.error('Error resolving post:', error);
        throw error;
    }
}

// Revert a post's resolution status
export async function unresolvePost(postId) {
    try {
        const response = await fetch(`${PUBLIC_API_URL}/api/posts/${postId}/unresolve`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        if (!response.ok) throw new Error('Failed to unresolve post');
        const data = await response.json();
        
        // Clear cache for this thread
        commentCache.delete(postId);
        
        threadStore.update(threads => {
            const updatedThreads = threads.map(thread => 
                thread.id === postId
                    ? {
                        ...thread,
                        solved: false,
                        resolution: null
                      }
                    : thread
            );
            return updatedThreads;
        });
        
        return data;
    } catch (error) {
        console.error('Error unresolving post:', error);
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
            //debouncedSaveToLocalStorage(updatedThreads);
            return updatedThreads;
        });
        
        return organizedComments;
    } catch (error) {
        console.error('Error loading comments:', error);
        throw error;
    }
}
