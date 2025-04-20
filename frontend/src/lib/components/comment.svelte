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
  export let contributingToResolution = false;
  export let threadOwner;
  export let replies = [];
  export let threadId;
  export let upvotes = 0;
  export let downvotes = 0;
  export let userUpvoted = false;
  export let userDownvoted = false;
  export let mediaFiles = []; // Add mediaFiles array prop
  export let parentCommentId = null; // Add parentCommentId prop

  // Add comment type prop with default value
  export let commentType = 'QUESTION';

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
        parentCommentId: commentId,
        commentType: commentType // Inherit type from parent
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
            // Create specific headers for FormData upload, only including Authorization
            const mediaUploadHeaders = new Headers();
            if (headers.Authorization) {
              mediaUploadHeaders.append('Authorization', headers.Authorization);
            }
            // DO NOT set Content-Type; browser handles it for FormData

            const mediaResponse = await fetch(`${PUBLIC_API_URL}/api/comments/${replyId}/upload-media`, {
              method: 'POST',
              headers: mediaUploadHeaders, // Use the specific headers for media upload
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

<!-- Ensure dark/light mode consistency in comment component -->
<div class="flex w-full py-0.5">
  <Card.Root class="w-full bg-white dark:bg-neutral-950 shadow-sm relative rounded-md
    ${contributingToResolution ? 'border-l-4 border-teal-600 dark:border-teal-500' : 'border border-neutral-200 dark:border-neutral-800'}
    ${commentType === 'QUESTION' ? 'question-comment' : commentType === 'SUGGESTION' ? 'suggestion-comment' : commentType === 'STORY' ? 'story-comment' : ''}"
  >
    <!-- Two-column layout: voting on left, content on right -->
    <div class="flex">
      <!-- Left column: Voting controls - vertically centered -->
      <div class="flex flex-col items-center justify-center px-4 py-4 border-r border-neutral-100 dark:border-neutral-800">
        <!-- Same padding as post for consistency -->
        <button 
          class="flex items-center justify-center hover:bg-neutral-100 dark:hover:bg-neutral-900 rounded-full w-8 h-8 transition-colors
                {userUpvoted ? 'text-teal-600' : 'text-neutral-600'}"
          on:click={() => voteOnComment(true)}
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
            <path d="M5 14l5-5 5 5H5z"/>
          </svg>
        </button>
        <span class="font-medium text-sm my-1">
          {#if downvotes > upvotes}
            <span class="text-rose-600">
              {upvotes - downvotes}
            </span>
          {:else if upvotes > 0}
            <span class="text-teal-600">
              {upvotes - downvotes}
            </span>
          {:else}
            <span class="text-neutral-500">
              0
            </span>
          {/if}
        </span>
        <button 
          class="flex items-center justify-center hover:bg-neutral-100 dark:hover:bg-neutral-900 rounded-full w-8 h-8 transition-colors
                {userDownvoted ? 'text-rose-600' : 'text-neutral-500'}"
          on:click={() => voteOnComment(false)}
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
            <path d="M5 6l5 5 5-5H5z"/>
          </svg>
        </button>
      </div>

      <!-- Right column: Comment content and action buttons -->
      <div class="flex-1 flex flex-col">
        <Card.Header class="p-4 pb-1">
          <!-- Adjusted padding to match post component -->
          <!-- User and metadata header -->
          <div class="flex flex-wrap items-center gap-2 text-sm text-gray-600 mb-2">
            <a href={`/user/${commentator}`} class="font-medium hover:text-rose-900 hover:underline">
              {commentator || "Anonymous"}
            </a>
            <span>•</span>
            <span title={postedDateComment} class="text-neutral-500 dark:text-neutral-400">
              {formatDate(postedDateComment)}
            </span>
            {#if !parentCommentId}
              <span>•</span>
              <!-- Comment type indicators -->
              {#if commentType === 'QUESTION'}
                <span class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium bg-amber-100 dark:bg-amber-900/40 text-amber-800 dark:text-amber-300 border border-amber-200 dark:border-amber-800/50">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mr-1.5" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-8-3a1 1 0 00-.867.5 1 1 0 11-1.731-1A3 3 0 0113 8a3.001 3.001 0 01-2 2.83V11a1 1 0 11-2 0v-1a1 1 0 011-1 1 1 0 100-2zm0 8a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd" />
                  </svg>
                  Question
                </span>
              {:else if commentType === 'SUGGESTION'}
                <span class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium bg-emerald-100 dark:bg-emerald-900/40 text-emerald-800 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800/50">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mr-1.5" viewBox="0 0 20 20" fill="currentColor">
                    <path d="M11 3a1 1 0 10-2 0v1a1 1 0 102 0V3zM15.657 5.757a1 1 0 00-1.414-1.414l-.707.707a1 1 0 001.414 1.414l.707-.707zM18 10a1 1 0 01-1 1h-1a1 1 0 110-2h1a1 1 0 011 1zM5.05 6.464A1 1 0 106.464 5.05l-.707-.707a1 1 0 00-1.414 1.414l.707.707zM5 10a1 1 0 01-1 1H3a1 1 0 110-2h1a1 1 0 011 1zM8 16v-1h4v1a2 2 0 11-4 0zM12 14c.015-.34.208-.646.477-.859a4 4 0 10-4.954 0c.27.213.462.519.476.859h4.002z" />
                  </svg>
                  Suggestion
                </span>
              {:else}
                <span class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium bg-blue-100 dark:bg-blue-900/40 text-blue-800 dark:text-blue-300 border border-blue-200 dark:border-blue-800/50">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mr-1.5" viewBox="0 0 20 20" fill="currentColor">
                    <path d="M9 4.804A7.968 7.968 0 005.5 4c-1.255 0-2.443.29-3.5.804v10A7.969 7.969 0 015.5 14c1.669 0 3.218.51 4.5 1.385A7.962 7.962 0 0114.5 14c1.255 0 2.443.29 3.5.804v-10A7.968 7.968 0 0014.5 4c-1.255 0-2.443.29-3.5.804V12a1 1 0 11-2 0V4.804z" />
                  </svg>
                  Story
                </span>
              {/if}
            {/if}
            {#if contributingToResolution}
              <span>•</span>
              <span class="text-teal-800 dark:text-teal-400 font-medium flex items-center gap-1">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
                </svg>
                Contributing
              </span>
            {/if}
          </div>

          <!-- Comment content -->
          {#if editMode}
            <Textarea
              bind:value={commentText}
              class="w-full p-3 border rounded-md text-sm bg-white dark:bg-neutral-950 border-neutral-200 dark:border-neutral-700 focus:ring-2 focus:ring-teal-500/30 text-left"
              placeholder="Edit your comment..."
            />
          {:else}
            <div class="text-neutral-900 dark:text-neutral-100 leading-relaxed text-left">
              {comment || "No content"}
            </div>
          {/if}

          <!-- Media display section -->
          {#if mediaFiles && mediaFiles.length > 0}
            <div class="mt-3 bg-neutral-100 dark:bg-neutral-900 rounded-md overflow-hidden">
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
                      <div class="flex flex-col items-center justify-center p-4 bg-neutral-200 dark:bg-neutral-900">
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
                      <div class="p-4 bg-neutral-200 dark:bg-neutral-900 text-center">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 mx-auto text-neutral-500 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                        </svg>
                        <p class="text-sm text-gray-700 dark:text-gray-300">{media.name || "Attachment"}</p>
                      </div>
                    {/if}
                  {/if}
                </div>
              </div>
              
              <!-- Thumbnail navigation for multiple media - ensure left alignment -->
              {#if mediaFiles.length > 1}
                <div class="flex justify-start overflow-x-auto gap-1 p-1 bg-neutral-200 dark:bg-neutral-900">
                  {#each mediaFiles as media, i}
                    <button 
                      on:click|stopPropagation={() => currentMediaIndex = i}
                      class="flex-shrink-0 w-12 h-12 rounded overflow-hidden focus:outline-none transition-all {i === currentMediaIndex ? 'ring-2 ring-neutral-500 transform scale-105' : 'opacity-60 hover:opacity-100'}"
                    >
                      {#if media.type === 'image'}
                        <img src={media.url} alt="thumbnail" class="w-full h-full object-cover" />
                      {:else if media.type === 'video'}
                        <div class="w-full h-full bg-neutral-300 dark:bg-neutral-900 flex items-center justify-center">
                          <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-neutral-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                          </svg>
                        </div>
                      {:else if media.type === 'audio'}
                        <div class="w-full h-full bg-neutral-300 dark:bg-neutral-900 flex items-center justify-center">
                          <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-neutral-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3" />
                          </svg>
                        </div>
                      {:else}
                        <div class="w-full h-full bg-neutral-300 dark:bg-neutral-900 flex items-center justify-center">
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
        </Card.Header>

        <!-- Action buttons in a row at the bottom - consistent with post buttons -->
        <div class="px-4 py-2.5 mt-auto border-t border-neutral-100 dark:border-neutral-800">
          <!-- Consistent padding with post action buttons -->
          {#if currentUser === commentator}
            <Button
              on:click={() => {
                editMode = !editMode;
                commentText = comment;
              }}
              variant="outline"
              size="sm"
              class="text-xs py-1 px-3 hover:bg-neutral-100 dark:hover:bg-neutral-900 border-neutral-300 dark:border-neutral-700 rounded-full"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 mr-1" viewBox="0 0 20 20" fill="currentColor">
                <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z" />
              </svg>
              {editMode ? "Cancel" : "Edit"}
            </Button>
          {/if}

          <Button
            on:click={toggleReplyInput}
            variant="outline"
            size="sm"
            class="text-xs py-1 px-3 hover:bg-neutral-100 dark:hover:bg-neutral-900 border-neutral-300 dark:border-neutral-700 rounded-full"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 mr-1" viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M7.707 3.293a1 1 0 010 1.414L5.414 7H11a7 7 0 017 7v2a1 1 0 11-2 0v-2a5 5 0 00-5-5H5.414l2.293 2.293a1 1 0 11-1.414 1.414l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 0z" clip-rule="evenodd" />
            </svg>
            Reply
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
                    `${PUBLIC_API_URL}/api/comments/edit/${commentId}`,
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
              variant="default"
              size="sm"
              class="text-xs py-1 px-3 bg-teal-600 hover:bg-teal-700 text-white rounded-full"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 mr-1" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
              </svg>
              Save
            </Button>
          {/if}
        </div>

        <!-- Reply input with consistent styling -->
        {#if replyInputVisible}
          <div class="p-4 pt-3">
            <!-- Adjusted padding to be consistent -->
            <Textarea
              bind:value={replyText}
              class="w-full p-3 border rounded-md text-sm bg-white dark:bg-neutral-900 border-neutral-200 dark:border-neutral-700 focus:ring-2 focus:ring-teal-500/30 text-left"
              placeholder="Write your reply..."
            />

            <!-- Media uploader for replies -->
            <div class="my-2 text-left">
              <MediaUploader
                bind:mediaFiles={replyMediaFiles}
              />
            </div>

            <!-- Left-aligned reply button -->
            <div class="flex justify-end">
              <Button
                on:click={addReply}
                variant="default"
                class="text-xs py-1 px-4 bg-teal-600 hover:bg-teal-700 text-white rounded-full"
                disabled={isUploadingMedia}
              >
                {#if isUploadingMedia}
                  <span class="inline-block h-3.5 w-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin mr-1.5"></span>
                  Uploading...
                {:else}
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 mr-1.5" viewBox="0 0 20 20" fill="currentColor">
                    <path d="M10.894 2.553a1 1 0 00-1.788 0l-7 14a1 1 0 001.169 1.409l5-1.429A1 1 0 009 15.571V11a1 1 0 112 0v4.571a1 1 0 00.725.962l5 1.428a1 1 0 001.17-1.408l-7-14z" />
                  </svg>
                  Submit Reply
                {/if}
              </Button>
            </div>
          </div>
        {/if}

        <!-- Nested Replies with consistent styling -->
        {#if replies && replies.length > 0}
          <div class="pl-4 border-l-2 border-neutral-200 dark:border-neutral-700 ml-4 mb-3">
            <!-- Adjusted padding for consistency -->
            {#each replies as reply (reply.id)}
              <div class="mt-1">
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
                  commentType={reply.commentType || 'QUESTION'}
                />
              </div>
            {/each}
          </div>
        {/if}
      </div>
    </div>
  </Card.Root>
</div>

<style>
  .question-comment {
    border-left: 3px solid rgb(251 191 36); /* amber-400 */
  }
  
  .suggestion-comment {
    border-left: 3px solid rgb(52 211 153); /* emerald-400 */
  }
  
  .story-comment {
    border-left: 3px solid rgb(96 165 250); /* blue-400 */
  }
  
  /* When a comment is contributing to resolution, the contribution border takes precedence */
  .question-comment.border-l-4,
  .suggestion-comment.border-l-4,
  .story-comment.border-l-4 {
    border-left-width: 4px !important;
    border-left-color: rgb(13 148 136) !important; /* teal-600 */
  }
  
  /* Dark mode adjustments with better contrast */
  :global(.dark) .question-comment {
    border-left-color: rgb(251 191 36 / 0.6); /* amber-400 at 60% for better visibility */
  }
  
  :global(.dark) .suggestion-comment {
    border-left-color: rgb(52 211 153 / 0.6); /* emerald-400 at 60% */
  }
  
  :global(.dark) .story-comment {
    border-left-color: rgb(96 165 250 / 0.6); /* blue-400 at 60% */
  }
  
  :global(.dark) .question-comment.border-l-4,
  :global(.dark) .suggestion-comment.border-l-4,
  :global(.dark) .story-comment.border-l-4 {
    border-left-color: rgb(20 184 166 / 0.8) !important; /* teal-500 at 80% for better visibility */
  }
</style>
