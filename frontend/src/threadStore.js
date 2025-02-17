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

// Update vote count for a thread
export function updateThreadVote(threadId, newVoteCount) {
    threadStore.update((threads) =>
        threads.map((thread) =>
            thread.id === threadId ? { ...thread, voteCount: newVoteCount } : thread
        )
    );
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
