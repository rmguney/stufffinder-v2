<script>
  import * as Card from "$lib/components/ui/card";
  import { activeUser } from "../../userStore";
  import { Button } from "$lib/components/ui/button";
  import { Textarea } from "$lib/components/ui/textarea";
  import { Separator } from "$lib/components/ui/separator";
  import { onMount } from 'svelte';
  import { getAuthHeader } from '$lib/utils/auth';

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
      
      const response = await fetch('http://localhost:8080/api/comments/create', {
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
      
      const response = await fetch(`http://localhost:8080/api/comments/${isUpvote ? 'upvote' : 'downvote'}/${commentId}`, {
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
        url: `http://localhost:8080/api/posts/${threadId}/markBestAnswer/${commentId}`,
        token: token.substring(0, 10) + '...',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        }
      });

      const response = await fetch(`http://localhost:8080/api/posts/${threadId}/markBestAnswer/${commentId}`, {
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
  <Card.Root class={`w-full bg-opacity-90 hover:bg-opacity-100 relative ${selected ? 'border-4 border-teal-600 dark:border-teal-800' : ''}`}>
    <div class="flex flex-col w-full">
      <Card.Header>
        <Card.Title class="w-full flex items-center">
          {comment || "No content"}
          {#if selected}
            <!-- Fix SVG viewBox attribute -->
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="ml-2 size-5 text-teal-800">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6.633 10.25c.806 0 1.533-.446 2.031-1.08a9.041 9.041 0 0 1 2.861-2.4c.723-.384 1.35-.956 1.653-1.715a4.498 4.498 0 0 0 .322-1.672V2.75a.75.75 0 0 1 .75-.75 2.25 2.25 0 0 1 2.25 2.25c0 1.152-.26 2.243-.723 3.218-.266.558.107 1.282.725 1.282m0 0h3.126c1.026 0 1.945.694 2.054 1.715.045.422.068.85.068 1.285a11.95 11.95 0 0 1-2.649 7.521c-.388.482-.987.729-1.605.729H13.48c-.483 0-.964-.078-1.423-.23l-3.114-1.04a4.501 4.501 0 0 0-1.423-.23H5.904m10.598-9.75H14.25M5.904 18.5c.083.205.173.405.27.602.197.4-.078.898-.523.898h-.908c-.889 0-1.713-.518-1.972-1.368a12 12 0 0 1-.521-3.507c0-1.553.295-3.036.831-4.398C3.387 9.953 4.167 9.5 5 9.5h1.053c.472 0 .745.556.5.96a8.958 8.958 0 0 0-1.302 4.665c0 1.194.232 2.333.654 3.375Z" />
            </svg>
          {/if}
        </Card.Title>
      </Card.Header>

      <Card.Description class="flex flex-col w-full px-6">
        <span>
          <a href={`/user/${commentator}`} class="hover:text-rose-900 hover:underline font-bold">
            {commentator || "Anonymous"}
          </a>
          at {formatDate(postedDateComment)}
        </span>
        
        <!-- Vote buttons with counts -->
        <div class="flex flex-row gap-4 mt-2">
          <button 
            on:click={() => voteOnComment(true)} 
            class={`text-xs hover:text-rose-900 flex items-center ${userUpvoted ? 'font-bold text-green-600' : ''}`}
          >
            ⬆️ Upvote ({upvotes})
          </button>
          <button 
            on:click={() => voteOnComment(false)} 
            class={`text-xs hover:text-rose-900 flex items-center ${userDownvoted ? 'font-bold text-red-600' : ''}`}
          >
            ⬇️ Downvote ({downvotes})
          </button>
        </div>
        
        <Separator class="mt-4"/>
        <div class="flex flex-row justify-center mt-4 gap-2">
          {#if isOwner}
            {#if !selected}
              <Button 
                on:click={toggleBestAnswer} 
                class="w-1/4 text-xs py-1 px-2 hover:bg-rose-900"
              >
                Mark as Best Answer
              </Button>
            {/if}
          {/if}

          <Button 
            on:click={toggleReplyInput} 
            class="w-1/4 text-xs py-1 px-2 hover:bg-rose-900"
          >
            {replyInputVisible ? "Cancel" : "Reply"}
          </Button>
          
          {#if replyInputVisible}
            <Button 
              on:click={addReply} 
              class="w-1/4 text-xs py-1 px-2 hover:bg-rose-900"
            >
              Submit
            </Button>
          {/if}
        </div>
        {#if replyInputVisible}
          <div class="mt-2">
            <Textarea 
              bind:value={replyText} 
              class="w-full p-2 border rounded-lg text-sm"
              placeholder="Write your reply..."></Textarea>
          </div>
        {/if}

        <!-- Render Nested Replies with better debugging -->
        {#if replies && replies.length > 0}
          <div class="mt-4 pl-4 border-l-2 border-gray-300">
            <div class="text-xs text-gray-500 mb-2">
              {replies.length} {replies.length === 1 ? 'reply' : 'replies'}
            </div>
            {#each replies as reply (reply.id)}
              <div class="mb-2">
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
        {:else if repliesLoaded}
          <!-- Show this when we know there are no replies -->
          <div class="mt-2 text-xs text-gray-500">No replies yet</div>
        {/if}
      </Card.Description>
    </div>
  </Card.Root>
</div>
