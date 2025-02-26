import { writable } from 'svelte/store';

const LOCAL_STORAGE_KEY = 'threadStoreData';

// Utility function to safely check for localStorage
const isClient = typeof window !== 'undefined';

// Load initial data from localStorage (client-only)
const loadFromLocalStorage = () => {
    if (!isClient) return []; // Return an empty array during SSR
    const storedData = localStorage.getItem(LOCAL_STORAGE_KEY);
    return storedData ? JSON.parse(storedData) : [];
};

// Save data to localStorage (client-only)
const saveToLocalStorage = (data) => {
    if (isClient) {
        localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(data));
    }
};

// Initialize the store with client-side data only
export const threadStore = writable(loadFromLocalStorage());

// Subscribe to save updates to localStorage (client-only)
if (isClient) {
    threadStore.subscribe((threads) => {
        saveToLocalStorage(threads);
    });
}

// Function to update or add a thread
export function updateThread(newThread) {
    threadStore.update(threads => {
        const index = threads.findIndex(t => t.id === newThread.id);
        if (index !== -1) {
            // Update existing thread
            const updatedThreads = [...threads];
            updatedThreads[index] = {
                ...updatedThreads[index],
                ...newThread
            };
            return updatedThreads;
        } else {
            // Add new thread
            return [...threads, newThread];
        }
    });
}

// Update vote count for a thread
export async function updateThreadVote(threadId, isUpvote) {
    const endpoint = `http://localhost:8080/api/posts/${isUpvote ? 'upvote' : 'downvote'}/${threadId}`;
    try {
        const response = await fetch(endpoint, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        if (!response.ok) throw new Error('Vote update failed');
        const data = await response.json();
        
        threadStore.update(threads =>
            threads.map(thread =>
                thread.id === threadId ? { 
                    ...thread, 
                    upvotes: data.upvotes,
                    downvotes: data.downvotes,
                    userUpvoted: isUpvote,
                    userDownvoted: !isUpvote
                } : thread
            )
        );
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

        console.log("Adding comment with payload:", payload);

        const authToken = localStorage.getItem('authToken');
        const response = await fetch('http://localhost:8080/api/comments/create', {
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
        console.log("New comment response:", newComment);

        // Update store with new comment
        threadStore.update(threads => {
            return threads.map(thread => {
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
        });
        
        return newComment;
    } catch (error) {
        console.error('Error adding comment:', error);
        throw error;
    }
}

// Update vote count for a comment
export async function updateCommentVote(commentId, isUpvote) {
    const endpoint = `http://localhost:8080/api/comments/${isUpvote ? 'upvote' : 'downvote'}/${commentId}`;
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
        threadStore.update(threads =>
            threads.map(thread => ({

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
            }))
        );
        
        return data;
    } catch (error) {
        console.error('Error updating comment vote:', error);
        throw error;
    }
}

// Mark a comment as the best answer
export async function markBestAnswer(postId, commentId) {
    try {
        const response = await fetch(`http://localhost:8080/api/posts/${postId}/markBestAnswer/${commentId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        if (!response.ok) throw new Error('Failed to mark best answer');
        const data = await response.json();
        
        // Update the comment's bestAnswer status in the thread store
        threadStore.update(threads =>
            threads.map(thread => 
                thread.id === postId
                    ? {
                        ...thread,
                        comments: thread.comments?.map(comment => ({
                            ...comment,
                            bestAnswer: comment.id === commentId
                        })) || []
                      }
                    : thread
            )
        );
        
        return data;
    } catch (error) {
        console.error('Error marking best answer:', error);
        throw error;
    }
}

// Load comments for a specific thread
export async function loadCommentsForThread(threadId) {
    try {
        console.log(`Loading comments for thread ${threadId}`);
        
        const response = await fetch(`http://localhost:8080/api/comments/get/${threadId}`);
        if (!response.ok) throw new Error('Failed to fetch comments');
        
        const rawComments = await response.json();
        console.log("Raw comments received:", rawComments);

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
            return threads.map(thread => {
                if (parseInt(thread.id) === parseInt(threadId)) {
                    return { 
                        ...thread, 
                        comments: organizedComments 
                    };
                }
                return thread;
            });
        });
        
        return organizedComments;
    } catch (error) {
        console.error('Error loading comments:', error);
        throw error;
    }
}
