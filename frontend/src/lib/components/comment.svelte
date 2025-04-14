<script>
  import * as Card from "$lib/components/ui/card";
  import { activeUser } from "../../userStore";
  import { Button } from "$lib/components/ui/button";
  import { Textarea } from "$lib/components/ui/textarea";
  import { Separator } from "$lib/components/ui/separator";
  import MediaUploader from "$lib/components/mediaUploader.svelte"; // Import MediaUploader
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
  export let mediaFiles = []; // Add mediaFiles array prop

  let currentUser = null;
  let replyInputVisible = false;
  let editMode = false;
  let replyText = "";
  let replyMediaFiles = []; // Add array for reply media files
  let isUploadingMedia = false;
  let isOwner = false;
  let commentText = comment;
  //let editMediaFiles = [...mediaFiles];

  function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
  }

  $: {
    activeUser.subscribe((value) => {
      currentUser = value;
      isOwner = currentUser === threadOwner;
    });
  }

  // Add local state to track if replies are loaded
  let repliesLoaded = false;
  
  // Current media item being viewed in the media carousel
  let currentMediaIndex = 0;

  // Set initial values on mount
  onMount(() => {
    // Initialize vote counts if provided
    if (typeof comment === 'object' && comment !== null) {
      upvotes = comment.upvotes || 0;
      downvotes = comment.downvotes || 0;
      userUpvoted = comment.userUpvoted || false;
      userDownvoted = comment.userDownvoted || false;
    }
    
    // Mark replies as loaded if we have them
    repliesLoaded = true;
  });

  // Toggle reply input visibility
  const toggleReplyInput = () => {
    replyInputVisible = !replyInputVisible;
    if (!replyInputVisible) {
      // Clear media files when hiding the reply input
      replyMediaFiles = [];
    }
  };

  // Handle media update for reply
  function handleReplyMediaUpdate(event) {
    replyMediaFiles = event.detail.mediaFiles;
  }

  // Add a reply to this comment using the backend API
  const addReply = async () => {
    if (!replyText.trim() && replyMediaFiles.length === 0) return;

    try {
      const headers = getAuthHeader();
      if (!headers.Authorization) {
        console.error("Please log in to reply");
        return;
      }
      
      // First, create the reply comment
      const payload = {
        content: replyText,
        postId: threadId,
        parentCommentId: commentId
      };
      
      const response = await fetch(`https://backend-310608491068.europe-west1.run.app/api/comments/create`, {
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
      const replyId = newReply.commentId;
      
      // If there are media files, upload them to the new reply
      if (replyMediaFiles.length > 0) {
        isUploadingMedia = true;
        
        for (let i = 0; i < replyMediaFiles.length; i++) {
          const mediaItem = replyMediaFiles[i];
          const mediaFormData = new FormData();
          mediaFormData.append('file', mediaItem.file);
          mediaFormData.append('type', mediaItem.type || 'image');
          
          try {
            const mediaResponse = await fetch(`https://backend-310608491068.europe-west1.run.app/api/comments/${replyId}/upload-media`, {
              method: 'POST',
              headers: {
                ...headers
              },
              body: mediaFormData
            });
            
            if (!mediaResponse.ok) {
              console.error(`Failed to upload media file ${i+1} for reply`);
              // Continue with other uploads even if one fails
            }
          } catch (mediaError) {
            console.error(`Error uploading media file ${i+1} for reply:`, mediaError);
            // Continue with other uploads
          }
        }
        
        isUploadingMedia = false;
      }
      
      // Reset the form
      replyText = "";
      replyMediaFiles = [];
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
      
      const response = await fetch(`https://backend-310608491068.europe-west1.run.app/api/comments/${isUpvote ? 'upvote' : 'downvote'}/${commentId}`, {
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
      if (!isOwner) {
        console.error("Only the thread owner can mark best answers");
        return;
      }

      const token = getCookie('tokenKey');
      if (!token) {
        console.error("No auth token found");
        return;
      }

      const response = await fetch(`https://backend-310608491068.europe-west1.run.app/api/posts/${threadId}/markBestAnswer/${commentId}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        }
      });
      
      // console.log('Mark best answer response:', response.status);
      
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

  // Functions to control the media carousel
  const nextMedia = () => {
    if (mediaFiles.length > 1) {
      currentMediaIndex = (currentMediaIndex + 1) % mediaFiles.length;
    }
  };
  
  const prevMedia = () => {
    if (mediaFiles.length > 1) {
      currentMediaIndex = (currentMediaIndex - 1 + mediaFiles.length) % mediaFiles.length;
    }
  };
  
  // Handle media errors
  function handleMediaError(event) {
    console.error("Media failed to load:", event);
/*     event.target.src = '/placeholder-image.png';
 */  }
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
        {#if editMode}
          <Textarea
            bind:value={commentText}
            class="w-full p-2 border rounded-lg text-sm bg-white dark:bg-neutral-800 border-neutral-200 dark:border-neutral-700"
            placeholder="Edit your comment..."
          />
        {:else}
          <div class="text-neutral-900 dark:text-neutral-100">
            {comment || "No content"}
          </div>
        {/if}

        <!-- Media display section -->
        {#if mediaFiles && mediaFiles.length > 0}
          <div class="mt-4 bg-neutral-100 dark:bg-neutral-900 rounded-lg overflow-hidden">
            <!-- Media carousel -->
            <div class="relative">
              <!-- Navigation buttons for media carousel -->
              {#if mediaFiles.length > 1}
                <div class="absolute top-0 bottom-0 left-0 flex items-center z-10">
                  <button 
                    on:click|stopPropagation={prevMedia}
                    class="bg-black bg-opacity-40 hover:bg-opacity-60 text-white p-1 rounded-r-lg ml-1 focus:outline-none transform transition hover:scale-110"
                    aria-label="Previous media"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
                    </svg>
                  </button>
                </div>
                
                <div class="absolute top-0 bottom-0 right-0 flex items-center z-10">
                  <button 
                    on:click|stopPropagation={nextMedia}
                    class="bg-black bg-opacity-40 hover:bg-opacity-60 text-white p-1 rounded-l-lg mr-1 focus:outline-none transform transition hover:scale-110"
                    aria-label="Next media"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                    </svg>
                  </button>
                </div>
                
                <!-- Media counter -->
                <div class="absolute bottom-2 left-0 right-0 flex justify-center z-10">
                  <div class="bg-black bg-opacity-60 text-white px-2 py-0.5 rounded-full text-xs">
                    {currentMediaIndex + 1} / {mediaFiles.length}
                  </div>
                </div>
              {/if}
              
              <!-- Current media display -->
              <div class="w-full">
                {#if mediaFiles[currentMediaIndex]}
                  {@const media = mediaFiles[currentMediaIndex]}
                  {#if media.type === 'image'}
                    <img 
                      src={media.url} 
                      alt={media.name || "Comment attachment"} 
                      class="max-h-96 w-full object-contain" 
                      on:error={handleMediaError}
                    />
                  {:else if media.type === 'video'}
                    <!-- svelte-ignore a11y-media-has-caption -->
                    <video 
                      src={media.url} 
                      controls 
                      class="max-h-96 w-full object-contain"
                      on:error={handleMediaError}
                    >
                      Your browser does not support the video tag.
                    </video>
                  {:else if media.type === 'audio'}
                    <div class="flex flex-col items-center justify-center p-4 bg-neutral-200 dark:bg-neutral-800">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 text-neutral-500 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3" />
                      </svg>
                      <audio 
                        src={media.url} 
                        controls 
                        class="w-full max-w-md"
                        on:error={handleMediaError}
                      >
                        Your browser does not support the audio tag.
                      </audio>
                      <p class="mt-2 text-sm text-gray-700 dark:text-gray-300">{media.name || "Audio file"}</p>
                    </div>
                  {:else}
                    <div class="p-4 bg-neutral-200 dark:bg-neutral-800 text-center">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 mx-auto text-neutral-500 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                      </svg>
                      <p class="text-sm text-gray-700 dark:text-gray-300">{media.name || "Attachment"}</p>
                    </div>
                  {/if}
                {/if}
              </div>
            </div>
            
            <!-- Thumbnail navigation for multiple media -->
            {#if mediaFiles.length > 1}
              <div class="flex overflow-x-auto gap-1 p-1 bg-neutral-200 dark:bg-neutral-800">
                {#each mediaFiles as media, i}
                  <button 
                    on:click|stopPropagation={() => currentMediaIndex = i}
                    class="flex-shrink-0 w-12 h-12 rounded overflow-hidden focus:outline-none transition-all {i === currentMediaIndex ? 'ring-2 ring-neutral-500 transform scale-105' : 'opacity-60 hover:opacity-100'}"
                  >
                    {#if media.type === 'image'}
                      <img src={media.url} alt="thumbnail" class="w-full h-full object-cover" />
                    {:else if media.type === 'video'}
                      <div class="w-full h-full bg-neutral-300 dark:bg-neutral-700 flex items-center justify-center">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-neutral-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                      </div>
                    {:else if media.type === 'audio'}
                      <div class="w-full h-full bg-neutral-300 dark:bg-neutral-700 flex items-center justify-center">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-neutral-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3" />
                        </svg>
                      </div>
                    {:else}
                      <div class="w-full h-full bg-neutral-300 dark:bg-neutral-700 flex items-center justify-center">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-neutral-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                        </svg>
                      </div>
                    {/if}
                  </button>
                {/each}
              </div>
            {/if}
          </div>
        {/if}

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

        {#if currentUser === commentator}
          <Button
            on:click={() => {
              editMode = !editMode;
              commentText = comment;
            }}
            variant="outline"
            class="text-xs py-1 px-3 hover:bg-neutral-100 dark:hover:bg-neutral-800"
          >
            {editMode ? "Cancel Edit" : "Edit"}
          </Button>
        {/if}

        <Button
          on:click={toggleReplyInput}
          variant="outline"
          class="text-xs py-1 px-3 hover:bg-neutral-100 dark:hover:bg-neutral-800"
        >
          {replyInputVisible ? "Cancel" : "Reply"}
        </Button>
        {#if editMode}
          <Button
            on:click={async () => {
              try {
                const headers = getAuthHeader();
                if (!headers.Authorization) {
                  console.error("Please log in to edit");
                  return;
                }

                const payload = {
                  content: commentText,
                  postId: threadId,
                };

                const response = await fetch(
                  `https://backend-310608491068.europe-west1.run.app/api/comments/edit/${commentId}`,
                  {
                    method: "PUT",
                    headers: {
                      "Content-Type": "application/json",
                      ...headers,
                    },
                    body: JSON.stringify(payload),
                  }
                );

                if (!response.ok) {
                  const errorText = await response.text();
                  throw new Error(`Failed to edit comment: ${errorText}`);
                }

                // Reset the form
                editMode = false;
                comment = commentText;

                // Dispatch event to refresh comments
                document.dispatchEvent(
                  new CustomEvent("refreshComments", {
                    bubbles: true,
                    detail: { threadId },
                  })
                );
              } catch (error) {
                console.error("Error editing comment:", error);
              }
            }}
            variant="outline"
            class="text-xs py-1 px-3 hover:bg-rose-900 hover:text-white transition-colors"
          >
            Save Edit
          </Button>
        {/if}
      </div>

      <!-- Reply input with media upload -->
      {#if replyInputVisible}
        <div class="p-4 pt-0">
          <Textarea
            bind:value={replyText}
            class="w-full p-2 border rounded-lg text-sm bg-white dark:bg-neutral-800 border-neutral-200 dark:border-neutral-700"
            placeholder="Write your reply..."
          />

          <!-- Media uploader for replies -->
          <div class="my-3">
            <MediaUploader
              bind:mediaFiles={replyMediaFiles}
              on:update={handleReplyMediaUpdate}
            />
          </div>

          <Button
            on:click={addReply}
            variant="outline"
            class="mt-2 text-xs py-1 px-3 hover:bg-rose-900 hover:text-white transition-colors"
            disabled={isUploadingMedia}
          >
            {#if isUploadingMedia}
              <span class="inline-block h-4 w-4 border-2 border-white/30 border-t-white rounded-full animate-spin mr-2"></span>
              Uploading...
            {:else}
              Submit Reply
            {/if}
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
                mediaFiles={reply.mediaFiles || []}
              />
            </div>
          {/each}
        </div>
      {/if}
    </div>
  </Card.Root>
</div>
