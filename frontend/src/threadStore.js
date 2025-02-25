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

// Add a comment to a thread
export function addCommentToThread(threadId, newComment) {
    threadStore.update((threads) =>
        threads.map((thread) =>
            thread.id === threadId
                ? {
                    ...thread,
                    comments: thread.comments
                        ? [...thread.comments, { ...newComment, replies: [] }]
                        : [{ ...newComment, replies: [] }],
                  }
                : thread
        )
    );
}

// Add a reply to a specific comment
export function addReplyToComment(threadId, commentId, newReply) {
    threadStore.update((threads) =>
        threads.map((thread) =>
            thread.id === threadId
                ? {
                    ...thread,
                    comments: thread.comments.map((comment) =>
                        comment.id === commentId
                            ? {
                                ...comment,
                                replies: [...(comment.replies || []), newReply],
                              }
                            : comment
                    ),
                  }
                : thread
        )
    );
}
