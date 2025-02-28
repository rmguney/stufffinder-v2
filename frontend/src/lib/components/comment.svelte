<script>
  import * as Card from "$lib/components/ui/card";
  import { activeUser } from "../../userStore";
  import { Button } from "$lib/components/ui/button";
  import { Textarea } from "$lib/components/ui/textarea";
  import { Separator } from "$lib/components/ui/separator";
  import { onMount } from 'svelte';
  import { getAuthHeader } from '$lib/utils/auth';
  import { PUBLIC_API_URL } from "$env/static/public";

  // Import Comment for recursion (self-import for recursive component)
  import Comment from "./comment.svelte";

  // Export props with defaults
  export let commentId;
  export let comment;
  export let commentator;
  export let postedDateComment;
  export let selected = false;
  export let threadOwner;
  export let replies = [];
  export let threadId;
  export let upvotes = 0;
  export let downvotes = 0;
  export let userUpvoted = false;
  export let userDownvoted = false;
  export let parentCommentId = null;

  let currentUser = null;
  let replyInputVisible = false;
  let replyText = "";
  
  // Add debug variable
  let isOwner = false;

  // Subscribe to the active user
  $: {
    activeUser.subscribe((value) => {
      currentUser = value;
      isOwner = currentUser === threadOwner;
      console.log('Auth Debug:', {
        currentUser,
        threadOwner,
        isOwner,
        match: currentUser === threadOwner
      });
    });
  }

  // Add debug logging for replies
  $: {
    if (replies && replies.length > 0) {
      console.log(`Comment ${commentId} has ${replies.length} replies:`, 
        replies.map(r => ({
          id: r.id,
          content: r.content,
          parentId: r.parentCommentId || commentId
        }))
      );
    }
  }

  // Debug log when replies prop changes
  $: {
    if (replies?.length > 0) {
      console.log(`Comment ${commentId} received replies:`, replies);
    }
  }

  // Add local state to track if replies are loaded
  let repliesLoaded = false;

  // Set initial values on mount
  onMount(() => {
    console.log("Comment mounted:", { commentId, comment });
    
    // Initialize vote counts if provided
    if (typeof comment === 'object' && comment !== null) {
      upvotes = comment.upvotes || 0;
      downvotes = comment.downvotes || 0;
      userUpvoted = comment.userUpvoted || false;
      userDownvoted = comment.userDownvoted || false;
    }
    
    // Debug the replies
    if (replies && replies.length > 0) {
      console.log(`Comment ${commentId} has ${replies.length} replies`);
      replies.forEach(reply => {
        console.log(` - Reply ID: ${reply.id}, Content: ${reply.content}`);
      });
    }

    // Add debug logging for replies
    if (replies?.length > 0) {
      console.log(`Mounting comment ${commentId} with replies:`, replies);
    }

    // Mark replies as loaded if we have them
    repliesLoaded = true;
  });

  // Toggle reply input visibility
  const toggleReplyInput = () => {
    replyInputVisible = !replyInputVisible;
  };

  // Add a reply to this comment using the backend API
  const addReply = async () => {
    if (!replyText.trim()) return;

    try {
      const headers = getAuthHeader();
      if (!headers.Authorization) {
        console.error("Please log in to reply");
        return;
      }
      
      const payload = {
        content: replyText,
        postId: threadId,
        parentCommentId: commentId
      };
      
      const response = await fetch(`${PUBLIC_API_URL}/api/comments/create`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...headers
        },
        body: JSON.stringify(payload)
      });
      
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Failed to add reply: ${errorText}`);
      }
      
      const newReply = await response.json();
      console.log("Reply created:", newReply);
      
      // Reset the form
      replyText = "";
      replyInputVisible = false;
      
      // Dispatch event to refresh comments
      document.dispatchEvent(new CustomEvent('refreshComments', {
        bubbles: true,
        detail: { threadId }
      }));
    } catch (error) {
      console.error("Error adding reply:", error);
    }
  };

  // Vote on a comment
  const voteOnComment = async (isUpvote) => {
    try {
      const headers = getAuthHeader();
      if (!headers.Authorization) {
        console.error("Please log in to vote");
        return;
      }
      
      const response = await fetch(`${PUBLIC_API_URL}/api/comments/${isUpvote ? 'upvote' : 'downvote'}/${commentId}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...headers
        }
      });
      
      if (!response.ok) {
        throw new Error(`Vote failed: ${response.status}`);
      }
      
      const data = await response.json();
      if (data) {
        upvotes = data.upvotes || upvotes;
        downvotes = data.downvotes || downvotes;
        userUpvoted = isUpvote;
        userDownvoted = !isUpvote;
      }
    } catch (error) {
      console.error("Error voting:", error);
    }
  };

  // Format the date to a human-readable format
  const formatDate = (isoDate) => {
    if (!isoDate) return "";
    const date = new Date(isoDate);

    // Extract time components
    const timeOptions = { hour: "numeric", minute: "numeric", hour12: false };
    const timeString = new Intl.DateTimeFormat("en-US", timeOptions).format(date);

    // Extract date components with day before month and full year
    const day = date.getDate();
    const shortMonth = date.toLocaleString("en-US", { month: "short" }); // Shortened month
    const fullYear = date.getFullYear(); // Full year

    // Combine components with time first
    return `${timeString}, ${day} ${shortMonth} ${fullYear}`;
  };

  // Function to toggle the best answer status
  const toggleBestAnswer = async () => {
    try {
      console.log('Toggle best answer debug:', {
        currentUser,
        threadOwner,
        isOwner,
        commentId,
        threadId
      });

      if (!isOwner) {
        console.error("Only the thread owner can mark best answers");
        return;
      }

      const token = localStorage.getItem('tokenKey');
      if (!token) {
        console.error("No auth token found");
        return;
      }

      console.log('Request details:', {
        url: `${PUBLIC_API_URL}/api/posts/${threadId}/markBestAnswer/${commentId}`,
        token: token.substring(0, 10) + '...',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        }
      });

      const response = await fetch(`${PUBLIC_API_URL}/api/posts/${threadId}/markBestAnswer/${commentId}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        }
      });
      
      console.log('Mark best answer response:', response.status);
      
      if (response.status === 403) {
        console.error("You don't have permission to mark best answers");
        return;
      }
      
      if (!response.ok) {
        throw new Error(`Failed to mark best answer: ${response.status}`);
      }

      // Update the UI and refresh
      selected = !selected;
      document.dispatchEvent(new CustomEvent('refreshComments', {
        bubbles: true,
        detail: { threadId }
      }));
      
    } catch (error) {
      console.error("Error toggling best answer status:", error);
    }
  };

  // Debug the replies structure
  console.log(`Comment ${commentId} replies:`, replies);

  $: {
    if (replies && replies.length > 0) {
      console.log(`Comment ${commentId} details:`, {
        id: commentId,
        content: comment,
        parentId: parentCommentId,
        repliesCount: replies.length,
        replies: replies.map(r => ({
          id: r.id,
          content: r.content,
          parentId: r.parentCommentId
        }))
      });
    }
  }
</script>

<div class="flex w-full py-1">
  <Card.Root class={`w-full bg-opacity-90 hover:bg-opacity-100 relative ${selected ? 'border-2 border-teal-600 dark:border-teal-800' : ''}`}>
    <div class="flex flex-col w-full">
      <Card.Header class="p-4">
        <!-- User and metadata header -->
        <div class="flex flex-wrap items-center gap-2 text-sm text-gray-600 mb-2">
          <a href={`/user/${commentator}`} class="font-medium hover:text-rose-900 hover:underline">
            {commentator || "Anonymous"}
          </a>
          <span>•</span>
          <span>{formatDate(postedDateComment)}</span>
          {#if selected}
            <span>•</span>
            <span class="text-teal-800 dark:text-teal-600 font-medium flex items-center gap-1">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
              </svg>
              Best Answer
            </span>
          {/if}
        </div>

        <!-- Comment content -->
        <div class="text-neutral-900 dark:text-neutral-100">
          {comment || "No content"}
        </div>

        <!-- Vote buttons with counts -->
        <div class="flex items-center gap-2 mt-3">
          <button 
            on:click={() => voteOnComment(true)} 
            class="flex items-center justify-center hover:bg-neutral-100 dark:hover:bg-neutral-800 rounded-full w-8 h-8 transition-colors
                   {userUpvoted ? 'text-teal-600' : 'text-gray-600'}"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
              <path d="M5 14l5-5 5 5H5z"/>
            </svg>
          </button>
          <span class="text-sm font-medium">{upvotes - downvotes}</span>
          <button 
            on:click={() => voteOnComment(false)} 
            class="flex items-center justify-center hover:bg-neutral-100 dark:hover:bg-neutral-800 rounded-full w-8 h-8 transition-colors
                   {userDownvoted ? 'text-rose-600' : 'text-gray-500'}"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
              <path d="M5 6l5 5 5-5H5z"/>
            </svg>
          </button>
        </div>
      </Card.Header>

      <Separator />

      <!-- Actions section -->
      <div class="p-4 flex flex-wrap gap-2">
        {#if isOwner && !selected}
          <Button 
            on:click={toggleBestAnswer} 
            variant="outline"
            class="text-xs py-1 px-3 hover:bg-neutral-100 dark:hover:bg-neutral-800"
          >
            Mark as Best Answer
          </Button>
        {/if}

        <Button 
          on:click={toggleReplyInput} 
          variant="outline"
          class="text-xs py-1 px-3 hover:bg-neutral-100 dark:hover:bg-neutral-800"
        >
          {replyInputVisible ? "Cancel" : "Reply"}
        </Button>
      </div>

      <!-- Reply input -->
      {#if replyInputVisible}
        <div class="p-4 pt-0">
          <Textarea 
            bind:value={replyText} 
            class="w-full p-2 border rounded-lg text-sm bg-white dark:bg-neutral-800 border-neutral-200 dark:border-neutral-700"
            placeholder="Write your reply..."
          />
          <Button 
            on:click={addReply} 
            class="mt-2 text-xs py-1 px-3 hover:bg-rose-900"
          >
            Submit Reply
          </Button>
        </div>
      {/if}

      <!-- Nested Replies -->
      {#if replies && replies.length > 0}
        <div class="pl-4 border-l-2 border-neutral-200 dark:border-neutral-700 ml-4 mb-4">
          {#each replies as reply (reply.id)}
            <div class="mt-2">
              <svelte:self 
                {threadId}
                commentId={reply.id}
                comment={reply.content}
                commentator={reply.author}
                postedDateComment={reply.createdAt}
                selected={reply.bestAnswer || false}
                {threadOwner}
                replies={reply.replies || []}
                upvotes={reply.upvotes}
                downvotes={reply.downvotes}
                userUpvoted={reply.userUpvoted}
                userDownvoted={reply.userDownvoted}
                parentCommentId={commentId}
              />
            </div>
          {/each}
        </div>
      {/if}
    </div>
  </Card.Root>
</div>
